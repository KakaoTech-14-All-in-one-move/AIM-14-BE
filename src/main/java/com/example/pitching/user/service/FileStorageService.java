package com.example.pitching.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.max-file-size}")
    private DataSize maxFileSize;

    public Mono<String> store(FilePart file) {
        return validateFile(file)
                .then(generateFilePath(file))
                .flatMap(path -> saveFile(file, path))
                .map(this::generateFileUrl)
                .doOnSuccess(url -> log.info("File stored successfully: {}", url))
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다."));
    }

    public Mono<Void> delete(String fileUrl) {
        return Mono.just(fileUrl)
                .filter(url -> url != null && !url.isEmpty())
                .map(this::extractFilenameFromUrl)
                .map(filename -> Path.of(uploadDir, filename))
                .flatMap(this::deleteFile)
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

    private Mono<Path> generateFilePath(FilePart file) {
        return Mono.just(file)
                .map(FilePart::filename)
                .map(this::generateUniqueFilename)
                .map(filename -> createUploadPath().resolve(filename))
                .onErrorMap(e -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "파일 경로 생성 중 오류가 발생했습니다."));
    }

    private Path createUploadPath() {
        Path uploadPath = Path.of(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            return uploadPath;
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "업로드 디렉토리 생성 중 오류가 발생했습니다.");
        }
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

    private Mono<Path> saveFile(FilePart file, Path path) {
        return file.transferTo(path)
                .then(Mono.just(path))
                .doOnSuccess(__ -> log.info("File saved successfully: {}", path.getFileName()))
                .onErrorMap(e -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다."));
    }

    private String generateFileUrl(Path path) {
        return "/uploads/" + path.getFileName().toString();
    }

    private Mono<Void> deleteFile(Path path) {
        return Mono.fromCallable(() -> {
                    if (Files.exists(path)) {
                        try {
                            Files.delete(path);
                            log.info("Successfully deleted file: {}", path.getFileName());
                            return true;
                        } catch (IOException e) {
                            throw new ResponseStatusException(
                                    HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제 중 오류가 발생했습니다.");
                        }
                    }
                    log.info("File does not exist, skipping deletion: {}", path.getFileName());
                    return false;
                })
                .then();
    }

    private String extractFilenameFromUrl(String fileUrl) {
        return Optional.ofNullable(fileUrl)
                .map(url -> url.substring(url.lastIndexOf('/') + 1))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "잘못된 파일 URL입니다."));
    }
}