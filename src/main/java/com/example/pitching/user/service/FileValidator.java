package com.example.pitching.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
@Slf4j
public class FileValidator {
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");

    public Mono<FilePart> validate(FilePart file) { 
        return Mono.just(file)
                .filterWhen(this::validateFileSize)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                                String.format("파일 크기가 제한을 초과했습니다. (최대 %dMB)", MAX_FILE_SIZE / (1024 * 1024)))
                ))
                .filterWhen(this::validateFileExtension)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                String.format("지원하지 않는 파일 형식입니다. (지원 형식: %s)",
                                        String.join(", ", ALLOWED_EXTENSIONS)))
                ));
    }

    private Mono<Boolean> validateFileSize(FilePart file) {
        return Mono.just(file.headers())
                .map(HttpHeaders::getContentLength)
                .map(contentLength -> contentLength <= MAX_FILE_SIZE);
    }

    private Mono<Boolean> validateFileExtension(FilePart file) {
        return Mono.just(file.filename())
                .map(String::toLowerCase)
                .map(filename -> filename.substring(filename.lastIndexOf(".") + 1))
                .map(ALLOWED_EXTENSIONS::contains);
    }
}