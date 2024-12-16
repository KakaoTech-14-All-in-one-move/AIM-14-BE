package com.example.pitching.chat.domain;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.UUID;

// ChatMessage.java
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
    private String sender;
    private String senderName;
    private String message;
    private String profile_image;  // 추가

    @DynamoDbPartitionKey
    public Long getChannelId() {
        return channelId;
    }

    @DynamoDbSortKey
    public Long getTimestamp() {
        return timestamp;
    }

    public enum MessageType {
        ENTER, TALK, LEAVE
    }

    // 새로운 TALK 메시지 생성
    public static ChatMessage createTalkMessage(Long channelId, String sender, String senderName, String message, String profileImage) {
        return new ChatMessage(
                channelId,
                Instant.now().toEpochMilli(),
                UUID.randomUUID().toString(),
                MessageType.TALK,
                sender,
                senderName,
                message,
                profileImage
        );
    }

    // 입장 메시지 생성
    public static ChatMessage createEnterMessage(Long channelId, String sender, String senderName, String profileImage) {
        return new ChatMessage(
                channelId,
                Instant.now().toEpochMilli(),
                UUID.randomUUID().toString(),
                MessageType.ENTER,
                sender,
                senderName,
                senderName + "님이 입장하셨습니다.",
                profileImage
        );
    }

    // 퇴장 메시지 생성
    public static ChatMessage createLeaveMessage(Long channelId, String sender, String senderName, String profileImage) {
        return new ChatMessage(
                channelId,
                Instant.now().toEpochMilli(),
                UUID.randomUUID().toString(),
                MessageType.LEAVE,
                sender,
                senderName,
                senderName + "님이 퇴장하셨습니다.",
                profileImage
        );
    }
}