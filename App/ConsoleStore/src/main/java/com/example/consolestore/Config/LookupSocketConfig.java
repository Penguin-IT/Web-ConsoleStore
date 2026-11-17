package com.example.consolestore.Config;

import com.example.consolestore.Service.LookupService;
import com.example.consolestore.Service.LookupSocketServer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "store.lookup",
        name = "enabled",
        havingValue = "true"
)
public class LookupSocketConfig {

    @Bean(
            initMethod = "start",
            destroyMethod = "close"
    )
    public LookupSocketServer lookupSocketServer(
            LookupService service,
            @Value("${store.lookup.port:9000}") int port
    ) {
        return new LookupSocketServer(service, port);
    }
}