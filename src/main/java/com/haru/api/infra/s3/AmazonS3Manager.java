package com.haru.api.infra.s3;

import com.haru.api.global.common.entity.Uuid;
import com.haru.api.global.config.AmazonConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmazonS3Manager{

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AmazonConfig amazonConfig;


    public String uploadFile(String keyName, MultipartFile file) {

        try {
            software.amazon.awssdk.services.s3.model.PutObjectRequest putObjectRequest =
                    software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
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


    public String generatePresignedUrl(String keyName) {
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

    public String generateKeyName(String path, UUID uuid) {
        return path + '/' + uuid.toString();
    }

    public String generateKeyName(Uuid uuid, String path) {
        return path + '/' + uuid.getUuid();
    }
}
