package com.haru.api.infra.s3;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import java.io.*;

@Slf4j
@Component
public class MarkdownToPdfConverter {

    private static final byte[] FONT_BYTES;

    // 클래스가 로드될 때 폰트 파일을 딱 한 번만 읽어서 byte 배열에 저장합니다.
    static {
        String fontPath = "templates/NotoSansKR-Regular.ttf";
        try (InputStream in = MarkdownToPdfConverter.class.getClassLoader().getResourceAsStream(fontPath)) {
            if (in == null) {
                throw new IOException("폰트 파일을 클래스패스에서 찾을 수 없습니다: " + fontPath);
            }
            FONT_BYTES = in.readAllBytes();
            log.info("폰트 파일 로드 성공: {}, Size: {} bytes", fontPath, FONT_BYTES.length);
        } catch (Exception e) {
            log.error("초기화 중 폰트 파일을 로드하는 데 실패했습니다.", e);
            throw new RuntimeException("폰트 파일 로딩 실패", e);
        }
    }

    public byte[] convert(String markdownText) {
        try {
            // 1. Markdown -> HTML
            Parser parser = Parser.builder().build();
            Node document = parser.parse(markdownText);
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            String htmlContent = renderer.render(document);

            // 2. HTML -> PDF
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();

                builder.useFont(() -> new ByteArrayInputStream(FONT_BYTES), "NotoSansKR");

                // [수정] meta 태그를 /> 로 닫아주어 XML 파싱 오류 해결
                String styledHtml = "<html>"
                        + "<head>"
                        + "<meta charset=\"UTF-8\" />"
                        + "<style>"
                        + "body { font-family: 'NotoSansKR'; font-size: 12px; }"
                        + "h1, h2, h3, h4, h5, h6 { font-weight: bold; margin-top: 1.2em; margin-bottom: 0.6em; }"
                        + "h1 { font-size: 2em; }"
                        + "h2 { font-size: 1.5em; }"
                        + "p { margin-bottom: 1em; line-height: 1.6; }"
                        + "strong { font-weight: bold; }"
                        + "ul, ol { padding-left: 25px; margin-bottom: 1em; }"
                        + "li { margin-bottom: 0.5em; }"
                        + "hr { border: 0; border-top: 1px solid #ccc; margin: 2em 0; }"
                        + "</style>"
                        + "</head>"
                        + "<body>"
                        + htmlContent
                        + "</body>"
                        + "</html>";

                builder.withHtmlContent(styledHtml, null);
                builder.toStream(os);
                builder.run();
                log.info("Markdown to PDF 변환 성공");
                return os.toByteArray();
            }
        } catch (Exception e) {
            log.error("Markdown to PDF 변환 중 오류 발생", e);
            throw new RuntimeException("PDF 데이터 생성에 실패했습니다.", e);
        }
    }
}
