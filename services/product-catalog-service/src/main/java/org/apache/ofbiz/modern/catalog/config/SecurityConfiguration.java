package org.apache.ofbiz.modern.catalog.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {
    @Bean
    @ConditionalOnProperty(name = "catalog.security.mode", havingValue = "entra")
    SecurityWebFilterChain entraSecurity(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .pathMatchers("/api/catalog/v1/products").hasAuthority("SCOPE_catalog.read")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> { }))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "catalog.security.mode", havingValue = "local", matchIfMissing = true)
    SecurityWebFilterChain localSecurity(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }
}
