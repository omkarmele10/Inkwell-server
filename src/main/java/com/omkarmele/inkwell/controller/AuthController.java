package com.omkarmele.inkwell.controller;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import com.omkarmele.inkwell.dto.Dtos.EmailAuthRequest;
import com.omkarmele.inkwell.dto.Dtos.ForgotPasswordRequest;
import com.omkarmele.inkwell.dto.Dtos.ForgotPasswordResponse;
import com.omkarmele.inkwell.dto.Dtos.MeResponse;
import com.omkarmele.inkwell.dto.Dtos.ResetPasswordRequest;
import com.omkarmele.inkwell.service.PasswordResetService;
import com.omkarmele.inkwell.service.UserService;
import com.omkarmele.inkwell.security.JwtService;
import com.omkarmele.inkwell.security.AuthCookies;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthCookies authCookies;
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<MeResponse> register(@Valid @RequestBody EmailAuthRequest request) {
        var user = userService.register(request.email(), request.password(), request.name());
        return authenticated(user);
    }

    @PostMapping("/login")
    public ResponseEntity<MeResponse> login(@Valid @RequestBody EmailAuthRequest request) {
        return authenticated(userService.login(request.email(), request.password()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(new ForgotPasswordResponse(passwordResetService.requestReset(request.email())));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<MeResponse> authenticated(com.omkarmele.inkwell.model.User user) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, authCookies.issue(jwtService.issue(user.getId())).toString())
                .body(new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getAvatarUrl()));
    }

    /** Login itself is /oauth2/authorization/google (handled by Spring Security). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookies.clear().toString())
                .build();
    }
}
