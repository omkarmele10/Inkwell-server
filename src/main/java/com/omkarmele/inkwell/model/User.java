package com.omkarmele.inkwell.model;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id private String id;
    @Indexed(unique = true) private String googleId;
    @Indexed(unique = true, sparse = true) private String email;
    private String passwordHash;
    private Instant passwordChangedAt;
    private String name;
    private String avatarUrl;
    private String bio;
    @Indexed private long followerCount;
    private long followingCount;
    private Instant createdAt;
}
