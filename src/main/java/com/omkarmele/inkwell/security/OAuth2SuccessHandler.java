package com.omkarmele.inkwell.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.config.AppProperties;
import com.omkarmele.inkwell.model.User;
import com.omkarmele.inkwell.service.UserService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** After Google confirms the user, issue a one-time grant and send it to the SPA for token exchange. */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final UserService userService;
    private final AuthGrantService authGrantService;
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
        String code = URLEncoder.encode(authGrantService.issue(user.getId()), StandardCharsets.UTF_8);
        HttpSession session = request.getSession(false);   // the session only existed for the OAuth handshake
        if (session != null) session.invalidate();
        response.sendRedirect(appProperties.frontendUrl() + "/#authCode=" + code);
    }
}
