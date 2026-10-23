package com.omkarmele.inkwell.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.config.AppProperties;
import com.omkarmele.inkwell.model.User;
import com.omkarmele.inkwell.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** After Google confirms the user: upsert the account, set our own JWT as an HttpOnly cookie, go back to the SPA. */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthCookies authCookies;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        if (!Boolean.TRUE.equals(oidcUser.getEmailVerified())) { // user is not verified by Google, so we don't allow login
            response.sendRedirect(appProperties.frontendUrl() + "/login?error=email_not_verified");
            return;
        }
        User user;
        try {
            user = userService.upsertFromGoogle(
                    oidcUser.getSubject(), oidcUser.getEmail(), oidcUser.getFullName(), oidcUser.getPicture());
        } catch (com.omkarmele.inkwell.exception.ApiException
                | org.springframework.dao.DuplicateKeyException accountConflict) {
            response.sendRedirect(appProperties.frontendUrl() + "/login?error=account_exists");
            return;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, authCookies.issue(jwtService.issue(user.getId())).toString());
        HttpSession session = request.getSession(false);   // the session only existed for the OAuth handshake
        if (session != null) session.invalidate();
        response.sendRedirect(appProperties.frontendUrl() + "/");
    }
}
