package com.example.pitching.call.controller;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.dto.response.UrlRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoiceController {

    // TODO: 메서드가 늘어나면 GlobalExceptionHandler로 분리할 예정
    // TODO: 프론트에서 요청할 필요가 없다고 느껴지면 삭제할 수도 있음
    private final static List<String> channels = List.of("voice", "video");
    private final ServerProperties serverProperties;

    @GetMapping(value = "/call/{channel}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<UrlRes> getUrl(@PathVariable(value = "channel") String channel) {
        if (!channels.contains(channel))
            return Mono.error(new IllegalArgumentException("Invalid channel"));
        return Mono.just(UrlRes.of(serverProperties.getUrl(channel)));
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Map<String, String>> handleResponseStatusException(IllegalArgumentException ex) {
        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("error", ex.getMessage());

        return Mono.just(errorBody);
    }
}
