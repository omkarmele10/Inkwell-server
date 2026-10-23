package com.omkarmele.inkwell.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import com.omkarmele.inkwell.config.AppProperties;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expiryMinutes;

    public JwtService(AppProperties appProperties) {
        String encodedSecret = appProperties.jwt().secret();
        byte[] secretBytes;
        try {
            var decoder = encodedSecret.contains("_") || encodedSecret.contains("-")
                    ? Decoders.BASE64URL : Decoders.BASE64;
            secretBytes = decoder.decode(encodedSecret);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("app.jwt.secret (JWT_SECRET) must be a valid Base64-encoded key", exception);
        }
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException(
                    "app.jwt.secret (JWT_SECRET) must decode to at least 32 bytes for HS256; generate one with `openssl rand -base64 48`");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.expiryMinutes = appProperties.jwt().expiryMinutes();
    }

    public String issue(String userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiryMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    /** @return the user id, or empty when the token is missing, tampered with or expired. */
    public Optional<String> verify(String jwtToken) {
        try {
            return Optional.ofNullable(Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(jwtToken).getPayload().getSubject());
        } catch (JwtException | IllegalArgumentException invalidTokenException) {
            return Optional.empty();
        }
    }
}
