package com.omkarmele.inkwell.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/** Standard Spring Security recipe for SPAs: the token cookie is written on every response and the SPA echoes it in a header. */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
    private final CsrfTokenRequestHandler plainTokenHandler = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xorTokenHandler = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        xorTokenHandler.handle(request, response, csrfToken);
        csrfToken.get(); // force the deferred token to load so the cookie is written
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String csrfHeaderValue = request.getHeader(csrfToken.getHeaderName());
        CsrfTokenRequestHandler tokenHandler = StringUtils.hasText(csrfHeaderValue)
                ? plainTokenHandler : xorTokenHandler;
        return tokenHandler.resolveCsrfTokenValue(request, csrfToken);
    }
}
