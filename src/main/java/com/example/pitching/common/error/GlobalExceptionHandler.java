package com.example.pitching.common.error;

import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // TODO: 전체 API 예외처리를 해당 Handler를 통해서 해야 함

    private Mono<ResponseEntity<ApiError>> toErrorResponse(ApiError error) {
        return Mono.just(ResponseEntity
                .status(error.getStatus())
                .body(error));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ApiError>> handleResponseStatusException(ResponseStatusException ex) {
        ApiError error = switch (ex.getStatusCode().value()) {
            case 404 -> new ApiError.NotFound(ex.getReason());
            case 400 -> new ApiError.BadRequest(ex.getReason());
            default -> new ApiError.ServerError(ex.getReason());
        };
        return toErrorResponse(error);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, R2dbcDataIntegrityViolationException.class})
    public Mono<ResponseEntity<ApiError>> handleDataIntegrityViolation(Exception ex) {
        return handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "중복된 데이터가 존재합니다.")
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiError>> handleIllegalArgument(IllegalArgumentException ex) {
        return toErrorResponse(new ApiError.BadRequest(ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<ApiError>> handleRuntimeException(RuntimeException ex) {
        return toErrorResponse(new ApiError.ServerError(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiError>> handleGenericException(Exception ex) {
        return toErrorResponse(new ApiError.ServerError("서버 내부 오류가 발생했습니다."));
    }
}