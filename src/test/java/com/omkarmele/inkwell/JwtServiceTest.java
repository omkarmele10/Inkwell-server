package com.omkarmele.inkwell;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import com.omkarmele.inkwell.config.AppProperties;
import com.omkarmele.inkwell.security.JwtService;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static JwtService service(long minutes) {
        String secret = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
        return new JwtService(new AppProperties("http://localhost",
                new AppProperties.Cloudinary(""), new AppProperties.Mail(""),
                new AppProperties.Jwt(secret, minutes), new AppProperties.Ai("", "m")));
    }

    @Test
    void validTokenRoundTrips() {
        JwtService s = service(5);
        assertEquals("user-1", s.verify(s.issue("user-1")).orElseThrow());
    }

    @Test
    void base64UrlSecretRoundTrips() {
        byte[] secretBytes = new byte[32];
        java.util.Arrays.fill(secretBytes, (byte) 0xff);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        JwtService s = new JwtService(new AppProperties("http://localhost",
                new AppProperties.Cloudinary(""), new AppProperties.Mail(""),
                new AppProperties.Jwt(secret, 5), new AppProperties.Ai("", "m")));
        assertEquals("user-1", s.verify(s.issue("user-1")).orElseThrow());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService s = service(5);
        assertTrue(s.verify(s.issue("user-1") + "x").isEmpty());
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService s = service(-1);
        assertTrue(s.verify(s.issue("user-1")).isEmpty());
    }
}
