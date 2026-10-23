package com.omkarmele.inkwell.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String frontendUrl, boolean cookieSecure, Jwt jwt, Ai ai) {
    public record Jwt(String secret, long expiryMinutes) {}
    public record Ai(String apiKey, String model) {}
}
