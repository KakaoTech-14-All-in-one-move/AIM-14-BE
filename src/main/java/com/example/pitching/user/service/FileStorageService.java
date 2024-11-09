package com.example.pitching.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
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

    /**
     * 파일을 저장하고 URL을 반환합니다.
     * @param file 저장할 파일
     * @return 저장된 파일의 URL
     */
    public Mono<String> store(FilePart file) {
        return validateFile(file)
                .then(generateFilePath(file))
                .flatMap(path -> saveFile(file, path))
                .map(this::generateFileUrl)
                .doOnSuccess(url -> log.info("File stored successfully: {}", url))
                .doOnError(error -> log.error("Error storing file: {}", file.filename(), error));
    }

    /**
     * 파일을 삭제합니다.
     * @param fileUrl 삭제할 파일의 URL
     * @return 삭제 완료를 나타내는 Mono
     */
    public Mono<Void> delete(String fileUrl) {
        return Mono.just(fileUrl)
                .filter(url -> url != null && !url.isEmpty())
                .map(this::extractFilenameFromUrl)
                .map(filename -> Path.of(uploadDir, filename))
                .flatMap(this::deleteFile)
                .onErrorResume(error -> {
                    log.error("Error deleting file: {}", fileUrl, error);
                    return Mono.empty();
                });
    }

    /**
     * 파일의 유효성을 검사합니다.
     */
    private Mono<Void> validateFile(FilePart file) {
        return file.content()
                .map(dataBuffer -> dataBuffer.readableByteCount())
                .reduce(0L, Long::sum)  // 모든 청크의 크기를 합산
                .flatMap(totalSize -> {
                    if (totalSize > maxFileSize.toBytes()) {
                        return Mono.error(new IllegalArgumentException(
                                String.format("File size %d bytes exceeds maximum limit of %d bytes",
                                        totalSize, maxFileSize.toBytes())
                        ));
                    }
                    return Mono.just(file);
                })
                .filter(f -> {
                    String contentType = f.headers().getContentType().toString();
                    return contentType.startsWith("image/");
                })
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Only image files are allowed")
                ))
                .then();
    }

    /**
     * 파일 저장을 위한 경로를 생성합니다.
     */
    private Mono<Path> generateFilePath(FilePart file) {
        return Mono.just(file)
                .map(FilePart::filename)
                .map(this::generateUniqueFilename)
                .map(filename -> createUploadPath().resolve(filename));
    }

    /**
     * 업로드 디렉토리를 생성하고 경로를 반환합니다.
     */
    private Path createUploadPath() {
        Path uploadPath = Path.of(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            return uploadPath;
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    /**
     * 고유한 파일 이름을 생성합니다.
     */
    private String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return UUID.randomUUID().toString() + "_" + timestamp + extension;
    }

    /**
     * 파일 확장자를 추출합니다.
     */
    private String getFileExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf(".")))
                .orElse("");
    }

    /**
     * 실제 파일을 저장합니다.
     */
    private Mono<Path> saveFile(FilePart file, Path path) {
        return file.transferTo(path)
                .then(Mono.just(path))
                .doOnSuccess(__ -> log.info("File saved successfully: {}", path.getFileName()))
                .doOnError(error -> log.error("Error saving file: {}", path.getFileName(), error));
    }

    /**
     * 파일의 URL을 생성합니다.
     */
    private String generateFileUrl(Path path) {
        return "/uploads/" + path.getFileName().toString();
    }

    /**
     * 파일을 삭제합니다.
     */
    private Mono<Void> deleteFile(Path path) {
        return Mono.fromCallable(() -> {
                    if (Files.exists(path)) {
                        Files.delete(path);
                        log.info("Successfully deleted file: {}", path.getFileName());
                        return true;
                    }
                    log.info("File does not exist, skipping deletion: {}", path.getFileName());
                    return false;
                })
                .then();
    }

    /**
     * URL에서 파일 이름을 추출합니다.
     */
    private String extractFilenameFromUrl(String fileUrl) {
        return Optional.ofNullable(fileUrl)
                .map(url -> url.substring(url.lastIndexOf('/') + 1))
                .orElseThrow(() -> new IllegalArgumentException("Invalid file URL"));
    }
}