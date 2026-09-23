package org.apache.ofbiz.modern.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpClientConfiguration {
    @Bean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
