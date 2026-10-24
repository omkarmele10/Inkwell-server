package com.omkarmele.inkwell.security;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.omkarmele.inkwell.exception.ApiException;

@Service
public class AuthGrantService {
    private static final long GRANT_LIFETIME_SECONDS = 60;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, Grant> grants = new ConcurrentHashMap<>();

    public String issue(String userId) {
        grants.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(Instant.now()));
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        grants.put(code, new Grant(userId, Instant.now().plusSeconds(GRANT_LIFETIME_SECONDS)));
        return code;
    }

    public String consume(String code) {
        Grant grant = grants.remove(code);
        if (grant == null || grant.expiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired sign-in code");
        }
        return grant.userId();
    }

    private record Grant(String userId, Instant expiresAt) {}
}
