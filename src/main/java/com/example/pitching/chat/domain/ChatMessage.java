package com.example.pitching.chat.domain;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Long channelId;
    private Long timestamp;
    private String messageId;
    private MessageType type;
    private String sender;    // email
    private String message;

    @DynamoDbPartitionKey
    public Long getChannelId() {
        return channelId;
    }

    @DynamoDbSortKey
    public Long getTimestamp() {
        return timestamp;
    }

    public enum MessageType {
        ENTER, TALK, LEAVE, USER_UPDATE  // USER_UPDATE 추가
    }

    public static ChatMessage createTalkMessage(Long channelId, String sender, String message) {
        return new ChatMessage(
                channelId,
                Instant.now().toEpochMilli(),
                UUID.randomUUID().toString(),
                MessageType.TALK,
                sender,
                message
        );
    }

    public static ChatMessage createEnterMessage(Long channelId, String sender) {
        return new ChatMessage(
                channelId,
                Instant.now().toEpochMilli(),
                UUID.randomUUID().toString(),
                MessageType.ENTER,
                sender,
                "님이 입장하셨습니다."
        );
    }

    public static ChatMessage createLeaveMessage(Long channelId, String sender) {
        return new ChatMessage(
                channelId,
                Instant.now().toEpochMilli(),
                UUID.randomUUID().toString(),
                MessageType.LEAVE,
                sender,
                "님이 퇴장하셨습니다."
        );
    }
}