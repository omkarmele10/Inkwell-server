package com.omkarmele.inkwell.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl, Cloudinary cloudinary, Mail mail, Jwt jwt, Ai ai) {
    public record Cloudinary(String url) {}
    public record Mail(String from) {}
    public record Jwt(String secret, long expiryMinutes) {}
    public record Ai(String apiKey, String model) {}
}
