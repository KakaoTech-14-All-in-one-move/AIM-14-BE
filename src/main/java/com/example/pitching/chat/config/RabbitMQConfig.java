package com.example.pitching.chat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String CHAT_QUEUE = "chat.queue.v2";
    public static final String CHAT_EXCHANGE = "chat.exchange.v2";
    public static final String CHAT_ROUTING_KEY = "chat.routing.#";

    @Bean
    public Queue chatQueue() {
        return QueueBuilder.durable(CHAT_QUEUE)
                .withArgument("x-max-length", 100)
                .withArgument("x-overflow", "drop-head")
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE);
    }

    @Bean
    public Binding chatBinding(Queue chatQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(chatQueue)
                .to(chatExchange)
                .with(CHAT_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}