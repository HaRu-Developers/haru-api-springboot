package com.haru.api.infra.s3;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class MarkdownToPdfConverter {

    // Markdown 문자열 -> HTML , HTML -> PDF byte[] 변환
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

                try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream("templates/NotoSansKR-Regular.ttf")) {
                    if (fontStream == null) {
                        throw new IOException("폰트 파일을 찾을 수 없습니다.");
                    }
                    builder.useFont(() -> fontStream, "NotoSansKR");
                }

                String styledHtml = "<html><body style=\"font-family: 'NotoSansKR';\">" + htmlContent + "</body></html>";
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

