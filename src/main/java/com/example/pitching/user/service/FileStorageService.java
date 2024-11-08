package com.example.pitching.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    public Mono<String> store(FilePart file) {
        String filename = UUID.randomUUID().toString() + getExtension(file.filename());
        Path uploadPath = Path.of(uploadDir);

        return Mono.just(uploadPath)
                .map(path -> {
                    if (!Files.exists(path)) {
                        try {
                            Files.createDirectories(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Could not create upload directory", e);
                        }
                    }
                    return path.resolve(filename);
                })
                .flatMap(path -> file.transferTo(path)
                        .then(Mono.just("/uploads/" + filename)));
    }

    private String getExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".")))
                .orElse("");
    }
}