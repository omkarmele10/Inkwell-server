package com.omkarmele.inkwell.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/** Standard Spring Security SPA handler; exposes the raw token in a CORS-readable response header. */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
    private final CsrfTokenRequestHandler plainTokenHandler = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xorTokenHandler = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        xorTokenHandler.handle(request, response, csrfToken);
        CsrfToken token = csrfToken.get();
        response.setHeader("X-CSRF-TOKEN", token.getToken());
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String csrfHeaderValue = request.getHeader(csrfToken.getHeaderName());
        CsrfTokenRequestHandler tokenHandler = StringUtils.hasText(csrfHeaderValue)
                ? plainTokenHandler : xorTokenHandler;
        return tokenHandler.resolveCsrfTokenValue(request, csrfToken);
    }
}
