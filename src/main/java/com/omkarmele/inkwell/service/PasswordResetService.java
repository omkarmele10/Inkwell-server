package com.omkarmele.inkwell.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.omkarmele.inkwell.config.AppProperties;
import com.omkarmele.inkwell.exception.ApiException;
import com.omkarmele.inkwell.model.PasswordResetToken;
import com.omkarmele.inkwell.model.User;
import com.omkarmele.inkwell.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:noreply@inkwell.local}")
    private String fromAddress;

    public String requestReset(String email) {
        var matchingUser = userRepository.findByEmailIgnoreCase(email.trim());
        if (matchingUser.isEmpty()) return "USER_NOT_FOUND";
        var user = matchingUser.get();

        byte[] tokenBytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(hashToken(token));
        resetToken.setUserId(user.getId());
        resetToken.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        mongoTemplate.save(resetToken);

        String resetUrl = appProperties.frontendUrl() + "/reset-password?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(StringUtils.hasText(fromAddress) ? fromAddress : mailUsername);
        message.setTo(user.getEmail());
        message.setSubject("Reset your Inkwell password");
        message.setText("Use this link to reset your Inkwell password. It expires in 15 minutes:\n\n"
                + resetUrl + "\n\nIf you did not request this, you can ignore this email.");
        try {
            mailSender.send(message);
            return "EMAIL_SENT";
        } catch (MailException exception) {
            mongoTemplate.remove(resetToken);
            log.error("Could not send password reset email for user {}", user.getId(), exception);
            return "EMAIL_UNAVAILABLE";
        }
    }

    public void resetPassword(String token, String password) {
        Instant now = Instant.now();
        PasswordResetToken resetToken = mongoTemplate.findAndRemove(
                Query.query(Criteria.where("tokenHash").is(hashToken(token))
                        .and("expiresAt").gt(now)),
                PasswordResetToken.class);
        if (resetToken == null) throw ApiException.badRequest("Invalid or expired reset link");

        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(resetToken.getUserId())),
                new Update().set("passwordHash", passwordEncoder.encode(password))
                        .set("passwordChangedAt", now),
                User.class);
        mongoTemplate.remove(Query.query(Criteria.where("userId").is(resetToken.getUserId())),
                PasswordResetToken.class);
    }

    private static String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
