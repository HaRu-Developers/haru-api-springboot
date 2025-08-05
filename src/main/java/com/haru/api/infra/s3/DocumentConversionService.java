package com.haru.api.infra.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentConversionService {

    private final AmazonS3Manager amazonS3Manager;
    private static final int DPI = 150;//해상도
    private static final String IMAGE_FORMAT = "png";

    // 썸네일 저장 + key 반환
    // amazonS3Manager.generatePresignedUrl(key)
    public String createThumbnailAndGetKey(byte[] pdfBytes, String originalFileName, String featurePath) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF 데이터가 비어있습니다.");
        }
        log.info("PDF 썸네일 생성 시작. 원본 파일: {}", originalFileName);

        String fullPath = "thumbnails/" + featurePath;
        String keyName = amazonS3Manager.generateKeyName(fullPath, UUID.randomUUID()) + "." + IMAGE_FORMAT;

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            // PDFBox를 사용하여 첫 페이지를 이미지로 변환
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(0, DPI);

            // 변환된 이미지를 byte 배열로 다시 변환
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, IMAGE_FORMAT, baos);
            byte[] imageBytes = baos.toByteArray();

            log.info("썸네일 변환 완료. S3 업로드 시작. key: {}", keyName);

            // AmazonS3Manager를 사용하여 비공개로 S3에 업로드
            amazonS3Manager.uploadFile(keyName, imageBytes, "image/" + IMAGE_FORMAT);

            log.info("S3 업로드 성공. key: {}", keyName);

            // 생성된 고유 키 반환
            return keyName;

        } catch (Exception e) {
            log.error("PDF 썸네일 생성 또는 업로드 중 오류 발생. 원본 파일: {}", originalFileName, e);
            throw new RuntimeException("PDF 썸네일 생성에 실패했습니다.", e);
        }
    }
}

