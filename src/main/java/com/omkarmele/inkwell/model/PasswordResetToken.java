package com.omkarmele.inkwell.model;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("password_reset_tokens")
@Getter @Setter @NoArgsConstructor
public class PasswordResetToken {
    @Id private String id;
    @Indexed(unique = true) private String tokenHash;
    @Indexed private String userId;
    @Indexed(expireAfter = "0s") private Instant expiresAt;
}
