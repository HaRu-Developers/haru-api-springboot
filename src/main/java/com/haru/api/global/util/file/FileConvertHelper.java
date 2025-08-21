package com.haru.api.global.util.file;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;

@Component
public class FileConvertHelper {

    // dueDate 포맷터: LocalDateTime이면 yyyy-MM-dd HH:mm, 아니면 toString
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public String formatDueDate(Object dueDate) {
        if (dueDate == null) return null;
        try {
            if (dueDate instanceof java.time.LocalDateTime ldt) {
                return ldt.format(DTF);
            }
            if (dueDate instanceof java.time.OffsetDateTime odt) {
                return odt.toLocalDateTime().format(DTF);
            }
            if (dueDate instanceof java.time.ZonedDateTime zdt) {
                return zdt.toLocalDateTime().format(DTF);
            }
        } catch (Exception ignored) { }
        return dueDate.toString();
    }

    public void addHeading(XWPFDocument doc, String text, int fontSize, String fontName) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(100);
        p.setSpacingAfter(100);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(fontSize);
        r.setFontFamily(fontName);
    }

    public void addDocsBullet(XWPFDocument doc, String text, String fontName) {
        // 간단 불릿: 글머리 기호만 붙여 출력
        XWPFParagraph p = doc.createParagraph();
        p.setIndentationLeft(360); // 약 0.5인치
        XWPFRun r = p.createRun();
        r.setText("• " + text);
        r.setFontSize(12);
        r.setFontFamily(fontName);
    }

    public void addParagraph(XWPFDocument doc, String text, int fontSize, boolean bold, String fontName) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setFontSize(fontSize);
        r.setBold(bold);
        // 문장 내 강제 개행(\n) 처리
        String[] parts = text.split("\n");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) r.addBreak();
            r.setText(parts[i]);
            r.setFontFamily(fontName);
        }
    }

    // 마크다운 → HTML (그대로 사용)
    public String markdownToHtml(String markdownText) {
        if (markdownText == null) return "";
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        Node doc = parser.parse(markdownText);
        return renderer.render(doc);
    }

    /**
     * PDF용 제목 스타일 주입: .title-box 가운데 정렬, .title-text 폰트 크기 확대
     * 템플릿의 <head> 안에 <style> 블록을 삽입
     */
    public String injectStyle(String html) {
        String styleBlock = """
        <style>
          .title-box { text-align: center !important; }
          .title-text { font-size: %dpx !important; }
        </style>
        """;
        String lower = html.toLowerCase();
        if (lower.contains("<head>")) {
            return html.replaceFirst("(?i)<head>", "<head>" + styleBlock);
        } else {
            return html.replaceFirst("(?i)<html>", "<html><head>" + styleBlock + "</head>");
        }
    }

    /**
     * PDF용 제목 스타일 주입: 템플릿의 <head> 안에 <style> 블록을 삽입
     */
    public String injectPageMarginStyle(String html) {
        String styleBlock = """
        <style>
            @page {
                size: A4;
                margin-top: 80pt;
                margin-bottom: 80pt;
            }
            @page :first {
                margin-top: 90pt; /* 첫 페이지만 위 여백 크게 */
            }
        </style>
        """;
        String lowerHtml = html.toLowerCase();
        if (lowerHtml.contains("<head>")) {
            // <head> 태그가 있는 경우 → 바로 뒤에 스타일 삽입
            return html.replaceFirst("(?i)<head>", "<head>" + styleBlock);
        } else {
            // <head> 태그가 없는 경우 → <html> 다음에 <head> 생성 후 스타일 삽입
            return html.replaceFirst("(?i)<html>", "<html><head>" + styleBlock + "</head>");
        }
    }

    public byte[] convertHtmlToPdf(String listHtml, byte[] fontBytes) throws Exception {
        // Openhtmltopdf/Flying Saucer를 사용하여 PDF 변환
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(listHtml, null); // ex) "file:/opt/app/static/" or "https://your.cdn/", // base url 설정, 직접css파일 가져오거나 프론트엔드 배포 후 적용
        builder.toStream(baos);
        // 한글 폰트 임베딩
        // byte[] → 임시 파일
        if (fontBytes != null && fontBytes.length > 0) {
            Path tmpFont = Files.createTempFile("NotoSansKR-", ".ttf");
            Files.write(tmpFont, fontBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            builder.useFont(tmpFont.toFile(), "NotoSansKR");
        }
        builder.run();
        return baos.toByteArray();
    }
}
