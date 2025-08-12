package com.haru.api.infra.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarkdownFileUploader {

    private final MarkdownToPdfConverter markdownToPdfConverter;
    private final ThumbnailGeneratorService thumbnailGeneratorService;
    private final AmazonS3Manager amazonS3Manager;

    private static final String IMAGE_FORMAT = "png";

    /**
     * PDF만 생성 및 업로드
     * Markdown 텍스트를 PDF로 변환하여 S3에 업로드하고, 생성된 PDF의 key를 반환
     *
     * @param markdownText PDF로 변환할 Markdown 내용
     * @param featurePath 파일이 저장될 기능별 경로 (예: "aimeeting")
     * @return 생성된 PDF 파일의 S3 key
     */
    public String createOrUpdatePdf(String markdownText, String featurePath, String existingPdfKey) {
        // 1. Markdown을 PDF 데이터로 변환
        byte[] pdfBytes = markdownToPdfConverter.convert(markdownText);

        // 2. 사용할 PDF 키 결정
        String pdfKeyToUse;
        if (existingPdfKey != null && !existingPdfKey.isBlank()) {
            // 기존 키가 제공되면, 그 키를 그대로 사용하여 갱신(덮어쓰기)
            pdfKeyToUse = existingPdfKey;
            log.info("기존 PDF 갱신을 시작합니다. Key: {}", pdfKeyToUse);
        } else {
            // 기존 키가 없으면, 새로운 키를 생성
            pdfKeyToUse = amazonS3Manager.generateKeyName("pdfs/" + featurePath) + ".pdf";
            log.info("새로운 PDF 생성을 시작합니다. New Key: {}", pdfKeyToUse);
        }

        // 3. 결정된 키로 PDF 파일을 S3에 업로드
        amazonS3Manager.uploadFile(pdfKeyToUse, pdfBytes, "application/pdf");
        log.info("PDF 업로드/갱신 성공. Key: {}", pdfKeyToUse);

        // 4. 사용된 PDF의 key를 반환
        return pdfKeyToUse;
    }

    /**
     * 썸네일만 생성 및 업로드
     * 기존 PDF 파일의 key를 받아 썸네일을 생성하고 S3에 업로드
     *
     * @param pdfKey 썸네일을 생성할 원본 PDF의 키
     * @param featurePath 썸네일이 저장될 기능별 경로
     * @return 생성된 썸네일의 새로운 key
     */
    public String createOrUpdateThumbnail(String pdfKey, String featurePath, String existingThumbnailKey) {
        // 1. S3에서 원본 PDF 파일 다운로드
        byte[] pdfBytes = amazonS3Manager.downloadFile(pdfKey);

        // 2. 다운로드한 PDF로 새로운 썸네일 데이터 생성
        byte[] newThumbnailBytes = thumbnailGeneratorService.generate(pdfBytes);

        // 3. 사용할 썸네일 키 결정
        String thumbnailKeyToUse;
        if (existingThumbnailKey != null && !existingThumbnailKey.isBlank()) {
            // 기존 키가 제공되면, 그 키를 그대로 사용하여 갱신(덮어쓰기)
            thumbnailKeyToUse = existingThumbnailKey;
            log.info("기존 썸네일 갱신을 시작합니다. Key: {}", thumbnailKeyToUse);
        } else {
            // 기존 키가 없으면, 새로운 키를 생성
            thumbnailKeyToUse = amazonS3Manager.generateKeyName("thumbnails/" + featurePath) + "." + IMAGE_FORMAT;
            log.info("새로운 썸네일 생성을 시작합니다. New Key: {}", thumbnailKeyToUse);
        }

        // 4. 결정된 키로 썸네일을 S3에 업로드
        amazonS3Manager.uploadFile(thumbnailKeyToUse, newThumbnailBytes, "image/" + IMAGE_FORMAT);
        log.info("썸네일 업로드/갱신 성공. Key: {}", thumbnailKeyToUse);

        // 5. 사용된 썸네일의 key를 반환
        return thumbnailKeyToUse;
    }
}
