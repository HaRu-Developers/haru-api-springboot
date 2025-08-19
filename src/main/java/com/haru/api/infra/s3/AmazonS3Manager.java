package com.haru.api.infra.s3;

import com.haru.api.global.config.AmazonConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmazonS3Manager{

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AmazonConfig amazonConfig;

    //MultipartFile S3에 비공개 업로드
    public String uploadMultipartFile(String keyName, MultipartFile file) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(amazonConfig.getBucket())
                    .key(keyName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        } catch (Exception e) {
            log.error("S3 파일 업로드에 실패했습니다. key: {}", keyName, e);
            throw new RuntimeException("S3 upload failed", e);
        }
        return keyName;
    }

    // uploadFile
    public String uploadFile(String keyName, byte[] fileBytes, String contentType) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("업로드할 파일이 비어있습니다.");
        }
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(amazonConfig.getBucket())
                    .key(keyName)
                    .contentType(contentType)
                    .contentLength((long) fileBytes.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
        } catch (Exception e) {
            log.error("S3 파일 업로드에 실패했습니다. key: {}", keyName, e);
            throw new RuntimeException("S3 upload failed", e);
        }
        return keyName;
    }


    public String uploadFileWithTitle(String keyName, byte[] fileBytes, String contentType, String fileTitle) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("업로드할 파일이 비어있습니다.");
        }
        try {
            String contentDisposition = createContentDisposition(fileTitle);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(amazonConfig.getBucket())
                    .key(keyName)
                    .contentType(contentType)
                    .contentLength((long) fileBytes.length)
                    .contentDisposition(contentDisposition)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
        } catch (Exception e) {
            log.error("S3 파일 업로드에 실패했습니다. key: {}", keyName, e);
            throw new RuntimeException("S3 upload failed", e);
        }
        return keyName;
    }


    // 프론트로 url을 보내기 위해 사용하는 메서드
    public String generatePresignedUrl(String keyName) {
        if (keyName == null || keyName.isBlank()) return null;

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(amazonConfig.getBucket())
                .key(keyName)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    // 프론트로 수정한 파일명으로 다운로드 가능한 url을 보내기 위해 사용하는 메서드
    public String generatePresignedUrlForDownloadPdfAndWord(String keyName, String fileName) {
        if (keyName == null || keyName.isBlank()) return null;

        // RFC 5987 인코딩
        String encodedFilename = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20"); // 공백 처리
        String contentDisposition = "attachment; filename*=UTF-8''" + encodedFilename;

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(amazonConfig.getBucket())
                .key(keyName)
                .responseContentDisposition(contentDisposition)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    //S3에서 key에 해당하는 파일을 다운로드하여 byte 배열로 반환 -> 썸네일 생성시 활용
    public byte[] downloadFile(String keyName) {
        if (keyName == null || keyName.isBlank()) {
            throw new IllegalArgumentException("파일 키가 유효하지 않습니다.");
        }
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(amazonConfig.getBucket())
                    .key(keyName)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return objectBytes.asByteArray();
        } catch (Exception e) {
            log.error("S3 파일 다운로드에 실패했습니다. key: {}", keyName, e);
            throw new RuntimeException("S3 file download failed", e);
        }
    }

    public void deleteFile(String keyName) {
        // keyName이 비어있거나 null인 경우, 삭제 작업을 수행하지 않고 경고 로그를 남깁니다.
        if (keyName == null || keyName.isBlank()) {
            log.warn("삭제할 S3 파일의 keyName이 유효하지 않습니다.");
            return;
        }

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(amazonConfig.getBucket())
                .key(keyName)
                .build();

        try {
            // S3 클라이언트를 통해 삭제 요청을 보냅니다.
            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제에 성공했습니다. Key: {}", keyName);
        } catch (S3Exception e) {
            // S3 API 호출 중 에러가 발생하면 로그를 남기고 런타임 예외를 발생시킵니다.
            log.error("S3 파일 삭제 중 에러가 발생했습니다. Key: {}", keyName, e);
            throw new RuntimeException("S3 파일 삭제에 실패했습니다.", e);
        }
    }



    /**
     * Content-Disposition 헤더 값을 생성합니다.
     * 한글 등 비 ASCII 문자를 RFC 5987 표준에 따라 인코딩합니다.
     *
     * @param displayName 사용자에게 보여줄 파일명
     * @return 생성된 Content-Disposition 헤더 값
     */
    private String createContentDisposition(String displayName) {
        try {
            String encodedFilename = URLEncoder.encode(displayName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
            return "attachment; filename*=UTF-8''" + encodedFilename;
        } catch (Exception e) {
            log.warn("파일명 인코딩에 실패했습니다. displayName: {}", displayName);
            // 인코딩 실패 시, ASCII 문자만 포함된 기본 파일명을 사용하거나 다른 대체 로직을 수행할 수 있습니다.
            return "attachment; filename=\"file\"";
        }
    }


    public String generateKeyName(String path) {
        return path + '/' +UUID.randomUUID();
    }

}
