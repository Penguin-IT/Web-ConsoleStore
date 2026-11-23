package com.example.consolestore.Config;

import com.example.consolestore.Service.ChatSocketServer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "store.chat",
        name = "enabled",
        havingValue = "true"
)
public class ChatSocketConfig {

    @Bean(
            initMethod = "start",
            destroyMethod = "close"
    )
    public ChatSocketServer chatSocketServer(
            @Value("${store.chat.port:9100}") int port
    ) {
        return new ChatSocketServer(port);
    }
}