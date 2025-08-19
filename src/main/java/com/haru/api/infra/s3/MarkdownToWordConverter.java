package com.haru.api.infra.s3;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Slf4j
@Component
public class MarkdownToWordConverter {

    /**
     * Markdown 문자열을 .docx 파일의 byte 배열로 변환합니다.
     *
     * @param markdownText 변환할 Markdown 내용
     * @return .docx 파일의 byte 배열
     */
    public byte[] convert(String markdownText) {
        try {
            // 1. Markdown을 서식이 없는 일반 텍스트로 1차 변환합니다.
            Parser parser = Parser.builder().build();
            Node document = parser.parse(markdownText);
            TextContentRenderer renderer = TextContentRenderer.builder().build();
            String plainText = renderer.render(document);

            // 2. Apache POI를 사용하여 새로운 Word 문서를 생성합니다.
            try (XWPFDocument wordDocument = new XWPFDocument();
                 ByteArrayOutputStream os = new ByteArrayOutputStream()) {

                // 3. 변환된 텍스트를 문단별로 나누어 Word 문서에 추가합니다.
                String[] lines = plainText.split("\\r?\\n");
                for (String line : lines) {
                    XWPFParagraph paragraph = wordDocument.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.setText(line);
                }

                // 4. 생성된 Word 문서를 byte 배열로 저장하여 반환합니다.
                wordDocument.write(os);
                log.info("Markdown to Word 변환 성공 (using Apache POI)");
                return os.toByteArray();
            }

        } catch (Exception e) {
            log.error("Markdown to Word 변환 중 오류 발생 (using Apache POI)", e);
            throw new RuntimeException("Word 데이터 생성에 실패했습니다.", e);
        }
    }
}
