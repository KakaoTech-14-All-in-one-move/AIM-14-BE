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
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

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

    public Flux<ChatMessage> findBySender(String sender) {
        // DynamoDB에서 sender로 필터링하는 식 생성
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":sender", AttributeValue.builder().s(sender).build());

        Expression filterExpression = Expression.builder()
                .expression("sender = :sender")
                .expressionValues(expressionValues)
                .build();

        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        return Flux.from(chatMessageTable.scan(scanRequest))
                .doOnError(e -> log.error("Error scanning messages by sender: {}", e.getMessage()))
                .flatMap(page -> Flux.fromIterable(page.items()));
    }

    public Mono<Void> deleteByChannelId(Long channelId) {
        return findByChannelIdOrderByTimestampAsc(channelId)
                .flatMap(message -> Mono.fromFuture(chatMessageTable.deleteItem(message)))
                .then();
    }

    public Mono<ChatMessage> updateMessage(ChatMessage message) {
        return Mono.fromFuture(chatMessageTable.updateItem(message))
                .doOnError(e -> log.error("Error updating message: {}", e.getMessage()))
                .thenReturn(message);
    }

    public Flux<ChatMessage> findByChannelIdAndSender(Long channelId, String sender) {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":sender", AttributeValue.builder().s(sender).build());

        Expression filterExpression = Expression.builder()
                .expression("sender = :sender")
                .expressionValues(expressionValues)
                .build();

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(kb -> kb.partitionValue(channelId)))
                .filterExpression(filterExpression)
                .scanIndexForward(true)
                .build();

        return Flux.from(chatMessageTable.query(queryRequest))
                .doOnError(e -> log.error("Error querying messages: {}", e.getMessage()))
                .flatMap(page -> Flux.fromIterable(page.items()));
    }

    public Mono<ChatMessage> findByMessageId(String messageId) {
        // MessageId로 전체 스캔하여 찾기
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":messageId", AttributeValue.builder().s(messageId).build());

        Expression filterExpression = Expression.builder()
                .expression("messageId = :messageId")
                .expressionValues(expressionValues)
                .build();

        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        return Flux.from(chatMessageTable.scan(scanRequest))
                .doOnError(e -> log.error("Error scanning message by messageId: {}", e.getMessage()))
                .flatMap(page -> Flux.fromIterable(page.items()))
                .next()
                .switchIfEmpty(Mono.error(new RuntimeException("Message not found")));
    }
}