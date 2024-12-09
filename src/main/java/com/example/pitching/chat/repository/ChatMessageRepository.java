package com.example.pitching.chat.repository;

import com.example.pitching.chat.domain.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepository {
    private final DynamoDbClient dynamoDbClient;
    private final String TABLE_NAME = "ChatMessages";

    public Mono<ChatMessage> save(ChatMessage message) {
        return Mono.fromCallable(() -> {
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(Map.of(
                            "messageId", AttributeValue.builder().s(message.getMessageId()).build(),
                            "timestamp", AttributeValue.builder().s(message.getTimestamp()).build(),
                            "channelId", AttributeValue.builder().s(message.getChannelId()).build(),
                            "type", AttributeValue.builder().s(message.getType().toString()).build(),
                            "sender", AttributeValue.builder().s(message.getSender()).build(),
                            "content", AttributeValue.builder().s(message.getContent()).build(),
                            "profileImage", AttributeValue.builder().s(message.getProfileImage()).build()
                    ))
                    .build();

            dynamoDbClient.putItem(request);
            return message;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<ChatMessage> findByChannelId(String channelId) {
        return Mono.fromCallable(() -> {
                    QueryRequest queryRequest = QueryRequest.builder()
                            .tableName(TABLE_NAME)
                            .indexName("channelId-timestamp-index")
                            .keyConditionExpression("channelId = :channelId")
                            .expressionAttributeValues(Map.of(
                                    ":channelId", AttributeValue.builder().s(channelId).build()
                            ))
                            .build();

                    QueryResponse response = dynamoDbClient.query(queryRequest);

                    return response.items().stream()
                            .map(item -> {
                                ChatMessage message = ChatMessage.createChatMessage(
                                        item.get("channelId").s(),
                                        ChatMessage.MessageType.valueOf(item.get("type").s()),
                                        item.get("sender").s(),
                                        item.get("content").s(),
                                        item.get("profileImage").s()
                                );
                                message.setMessageId(item.get("messageId").s());
                                message.setTimestamp(item.get("timestamp").s());
                                return message;
                            })
                            .collect(Collectors.toList());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }
}
