package com.haru.api.infra.s3;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThumbnailGeneratorService {

    private static final int DPI = 150;
    private static final String IMAGE_FORMAT = "png";

    // pdf byte[] -> 썸네일 이미지 생성
    public byte[] generate(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF 데이터가 비어있습니다.");
        }
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(0, DPI);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(bufferedImage, IMAGE_FORMAT, baos);
                log.info("PDF 썸네일 생성 성공");
                return baos.toByteArray();
            }
        } catch (Exception e) {
            log.error("PDF 썸네일 생성 중 오류 발생", e);
            throw new RuntimeException("썸네일 생성에 실패했습니다.", e);
        }
    }
}

