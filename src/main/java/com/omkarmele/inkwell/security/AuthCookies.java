package com.omkarmele.inkwell.security;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.config.AppProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookies {
    public static final String NAME = "access_token";
    private final AppProperties appProperties;

    public ResponseCookie issue(String jwtToken) {
        return base(jwtToken).maxAge(Duration.ofMinutes(appProperties.jwt().expiryMinutes())).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value).httpOnly(true).secure(appProperties.cookieSecure()).sameSite("Lax").path("/");
    }
}
