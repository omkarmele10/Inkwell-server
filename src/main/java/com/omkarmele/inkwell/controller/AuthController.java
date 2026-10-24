package com.omkarmele.inkwell.controller;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import com.omkarmele.inkwell.dto.Dtos.EmailAuthRequest;
import com.omkarmele.inkwell.dto.Dtos.ForgotPasswordRequest;
import com.omkarmele.inkwell.dto.Dtos.ForgotPasswordResponse;
import com.omkarmele.inkwell.dto.Dtos.AuthResponse;
import com.omkarmele.inkwell.dto.Dtos.ResetPasswordRequest;
import com.omkarmele.inkwell.security.AuthGrantService;
import com.omkarmele.inkwell.service.PasswordResetService;
import com.omkarmele.inkwell.service.UserService;
import com.omkarmele.inkwell.security.JwtService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthGrantService authGrantService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody EmailAuthRequest request) {
        var user = userService.register(request.email(), request.password(), request.name());
        return authenticated(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody EmailAuthRequest request) {
        return authenticated(userService.login(request.email(), request.password()));
    }

    @PostMapping("/exchange")
    public AuthResponse exchange(@Valid @RequestBody ExchangeRequest request) {
        String userId = authGrantService.consume(request.code());
        var user = userService.me(userId);
        return new AuthResponse(jwtService.issue(user.id()), user.id(), user.name(), user.email(), user.avatarUrl());
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

    private AuthResponse authenticated(com.omkarmele.inkwell.model.User user) {
        return new AuthResponse(jwtService.issue(user.getId()), user.getId(), user.getName(),
                user.getEmail(), user.getAvatarUrl());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    public record ExchangeRequest(@NotBlank String code) {}
}
