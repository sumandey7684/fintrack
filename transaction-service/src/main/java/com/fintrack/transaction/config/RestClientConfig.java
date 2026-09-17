package com.fintrack.transaction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient accountServiceRestClient(
            RestClient.Builder builder,
            @Value("${fintrack.account-service.base-url}") String accountServiceBaseUrl) {
        return builder.baseUrl(accountServiceBaseUrl).build();
    }
}
