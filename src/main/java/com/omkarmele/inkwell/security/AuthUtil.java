package com.omkarmele.inkwell.security;

import java.util.Optional;
import com.omkarmele.inkwell.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthUtil {
    private AuthUtil() {}

    public static Optional<String> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) return Optional.empty();
        return Optional.of(authentication.getName());
    }

    public static String requireUserId() {
        return currentUserId().orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sign in required"));
    }
}
