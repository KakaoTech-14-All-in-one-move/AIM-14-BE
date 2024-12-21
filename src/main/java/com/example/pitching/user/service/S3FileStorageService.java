package com.example.pitching.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3FileStorageService {
    private final S3AsyncClient s3AsyncClient;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Value("${app.upload.max-file-size}")
    private DataSize maxFileSize;

    public Mono<String> store(FilePart file) {
        return validateFile(file)
                .then(generateKey(file))
                .flatMap(key -> uploadToS3(file, key))
                .map(this::generateFileUrl)
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다."));
    }

    public Mono<Void> delete(String fileUrl) {
        return Mono.just(fileUrl)
                .filter(url -> url != null && !url.isEmpty())
                .map(this::extractKeyFromUrl)
                .flatMap(this::deleteFromS3)
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제 중 오류가 발생했습니다."));
    }

    private Mono<Void> validateFile(FilePart file) {
        return file.content()
                .map(dataBuffer -> dataBuffer.readableByteCount())
                .reduce(0L, Long::sum)
                .flatMap(totalSize -> {
                    if (totalSize > maxFileSize.toBytes()) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.PAYLOAD_TOO_LARGE,
                                String.format("파일 크기가 제한을 초과했습니다. (최대 %dMB)",
                                        maxFileSize.toBytes() / (1024 * 1024))
                        ));
                    }
                    return Mono.just(file);
                })
                .filter(f -> {
                    String contentType = f.headers().getContentType().toString();
                    return contentType.startsWith("image/");
                })
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드 가능합니다.")
                ))
                .then();
    }

    private Mono<String> generateKey(FilePart file) {
        return Mono.just(file)
                .map(FilePart::filename)
                .map(this::generateUniqueFilename)
                .map(filename -> "uploads/" + filename);
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return UUID.randomUUID().toString() + "_" + timestamp + extension;
    }

    private String getFileExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf(".")))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "파일 확장자가 없습니다."));
    }

    private Mono<String> uploadToS3(FilePart file, String key) {
        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(file.headers().getContentType().toString())
                            .build();

                    return Mono.fromFuture(() -> s3AsyncClient.putObject(putObjectRequest,
                                    AsyncRequestBody.fromBytes(bytes)))
                            .doOnError(error -> log.error("Failed to upload file to S3", error))
                            .thenReturn(key);
                });
    }

    private String generateFileUrl(String key) {
        String fileName = key.substring(key.lastIndexOf('/') + 1);
        return fileName;
    }

    private String extractKeyFromUrl(String fileName) {
        return "uploads/" + fileName;
    }

    private Mono<Void> deleteFromS3(String key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return Mono.fromFuture(() -> s3AsyncClient.deleteObject(deleteRequest))
                .doOnError(error -> log.error("Failed to delete file from S3", error))
                .then();
    }

    public String getFullUrl(String fileName) {
        return String.format("https://%s.s3.%s.amazonaws.com/uploads/%s",
                bucketName, region, fileName);
    }
}