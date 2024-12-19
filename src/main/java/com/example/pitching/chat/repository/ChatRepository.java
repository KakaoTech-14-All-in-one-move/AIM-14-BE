package com.example.pitching.chat.repository;

import com.example.pitching.chat.domain.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ChatRepository {
    private final DynamoDbAsyncTable<ChatMessage> chatMessageTable;

    public Mono<ChatMessage> save(ChatMessage message) {
        return Mono.fromFuture(chatMessageTable.putItem(message))
                .doOnError(e -> log.error("Error saving message: {}", e.getMessage()))
                .thenReturn(message);
    }

    public Flux<ChatMessage> findByChannelIdOrderByTimestampAsc(Long channelId) {
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(
                        QueryConditional.keyEqualTo(kb -> kb.partitionValue(channelId))
                )
                .scanIndexForward(true) // ascending order
                .build();

        return Flux.from(chatMessageTable.query(queryRequest))
                .doOnError(e -> log.error("Error querying messages: {}", e.getMessage()))
                .flatMap(page -> Flux.fromIterable(page.items()));
    }

    public Mono<Void> deleteByChannelId(Long channelId) {
        return findByChannelIdOrderByTimestampAsc(channelId)
                .flatMap(message -> Mono.fromFuture(chatMessageTable.deleteItem(message)))
                .then();
    }
}