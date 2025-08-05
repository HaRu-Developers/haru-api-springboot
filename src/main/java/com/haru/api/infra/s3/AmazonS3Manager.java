package com.haru.api.infra.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.haru.api.global.common.entity.Uuid;
import com.haru.api.global.config.AmazonConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmazonS3Manager{

    private final AmazonS3 amazonS3;

    private final AmazonConfig amazonConfig;

    private final UuidRepository uuidRepository;

    public String uploadFile(String keyName, MultipartFile file){
        if (file == null || file.isEmpty()) {
            return null;
        }

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        try {
            // ACL 설정 부분을 제거합니다.
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    amazonConfig.getBucket(), keyName, file.getInputStream(), metadata
            );

            amazonS3.putObject(putObjectRequest);

        } catch (Exception e) {
            log.error("S3 파일 업로드에 실패했습니다. key: {}", keyName, e);
            throw new RuntimeException("S3 upload failed", e);
        }

        return amazonS3.getUrl(amazonConfig.getBucket(), keyName).toString();
    }

    public String generateKeyName(String path, UUID uuid) {
        return path + '/' + uuid.toString();
    }

    public String generateKeyName(Uuid uuid, String path) {
        return path + '/' + uuid.getUuid();
    }
}
