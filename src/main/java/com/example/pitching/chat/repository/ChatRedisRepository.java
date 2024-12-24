package com.example.pitching.chat.repository;

import com.example.pitching.chat.dto.ChatMessageDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatRedisRepository {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String KEY_PREFIX = "chat:messages:";
    private static final long MAX_MESSAGES = 100;

    public Mono<ChatMessageDTO> saveMessage(ChatMessageDTO message) {
        try {
            String key = KEY_PREFIX + message.getChannelId();
            String value = objectMapper.writeValueAsString(message);
            return redisTemplate.opsForList().rightPush(key, value)
                    .flatMap(length -> {
                        if (length > MAX_MESSAGES) {
                            return redisTemplate.opsForList().leftPop(key)
                                    .thenReturn(message);
                        }
                        return Mono.just(message);
                    })
                    .doOnError(e -> log.error("Error saving message to Redis: {}", e.getMessage()));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    public Flux<ChatMessageDTO> getRecentMessages(Long channelId) {
        String key = KEY_PREFIX + channelId;
        return redisTemplate.opsForList().range(key, 0, -1)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, ChatMessageDTO.class);
                    } catch (JsonProcessingException e) {
                        log.error("Error deserializing message: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                })
                .doOnError(e -> log.error("Error fetching messages from Redis: {}", e.getMessage()));
    }

    public Mono<Boolean> deleteChannelMessages(Long channelId) {
        String key = KEY_PREFIX + channelId;
        return redisTemplate.delete(key)
                .hasElement()
                .doOnError(e -> log.error("Error deleting channel messages from Redis: {}", e.getMessage()));
    }

    public Mono<Void> updateUserMessages(Long channelId, String email, String newUsername, String newProfileImage) {
        String key = KEY_PREFIX + channelId;
        return redisTemplate.opsForList().range(key, 0, -1)
                .map(json -> {
                    try {
                        ChatMessageDTO message = objectMapper.readValue(json, ChatMessageDTO.class);
                        if (message.getSender().equals(email)) {
                            message.setSenderName(newUsername);
                            message.setProfile_image(newProfileImage);
                        }
                        return message;
                    } catch (JsonProcessingException e) {
                        log.error("Error processing message: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                })
                .collectList()
                .flatMap(messages -> {
                    // 기존 메시지 삭제 후 업데이트된 메시지 저장
                    return redisTemplate.delete(key)
                            .then(Mono.defer(() ->
                                    Flux.fromIterable(messages)
                                            .flatMap(message -> Mono.fromCallable(() -> {
                                                try {
                                                    return objectMapper.writeValueAsString(message);
                                                } catch (JsonProcessingException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }))
                                            .flatMap(value -> redisTemplate.opsForList().rightPush(key, value))
                                            .then()
                            ));
                })
                .doOnError(e -> log.error("Error updating user messages in Redis: {}", e.getMessage()))
                .then();
    }

    public Mono<Long> getChannelMessageCount(Long channelId) {
        String key = KEY_PREFIX + channelId;
        return redisTemplate.opsForList().size(key)
                .doOnError(e -> log.error("Error getting channel message count from Redis: {}", e.getMessage()));
    }

    public Mono<Boolean> clearOldMessages(Long channelId, int keepLatest) {
        String key = KEY_PREFIX + channelId;
        return getChannelMessageCount(channelId)
                .flatMap(count -> {
                    if (count > keepLatest) {
                        long toRemove = count - keepLatest;
                        return redisTemplate.opsForList().trim(key, toRemove, -1);
                    }
                    return Mono.just(true);
                })
                .doOnError(e -> log.error("Error clearing old messages from Redis: {}", e.getMessage()));
    }
}