package com.example.pitching.chat.config;

import com.example.pitching.chat.domain.ChatMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = DynamoDBConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "aws.region=ap-northeast-2",
        "aws.accessKey=test-access-key",
        "aws.secretKey=test-secret-key"
})
class DynamoDBConfigTest {

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Autowired
    private DynamoDbAsyncClient dynamoDbAsyncClient;

    @Autowired
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Autowired
    private DynamoDbAsyncTable<ChatMessage> chatMessageTable;

    @Test
    @DisplayName("DynamoDB 클라이언트 빈이 정상적으로 생성되어야 한다")
    void dynamoDbClient_ShouldBeCreated() {
        assertThat(dynamoDbClient).isNotNull();
    }

    @Test
    @DisplayName("DynamoDB 비동기 클라이언트 빈이 정상적으로 생성되어야 한다")
    void dynamoDbAsyncClient_ShouldBeCreated() {
        assertThat(dynamoDbAsyncClient).isNotNull();
    }

    @Test
    @DisplayName("DynamoDB Enhanced 비동기 클라이언트 빈이 정상적으로 생성되어야 한다")
    void dynamoDbEnhancedAsyncClient_ShouldBeCreated() {
        assertThat(dynamoDbEnhancedAsyncClient).isNotNull();
    }

    @Test
    @DisplayName("ChatMessage 테이블 빈이 정상적으로 생성되어야 한다")
    void chatMessageTable_ShouldBeCreated() {
        assertThat(chatMessageTable).isNotNull();
        assertThat(chatMessageTable.tableName()).isEqualTo("ChatMessages");
    }

    @Test
    @DisplayName("DynamoDB 클라이언트가 설정된 리전 정보를 가지고 있어야 한다")
    void dynamoDbClient_ShouldHaveCorrectRegion() {
        assertThat(dynamoDbClient.serviceClientConfiguration().region().toString())
                .isEqualTo("ap-northeast-2");
    }
}