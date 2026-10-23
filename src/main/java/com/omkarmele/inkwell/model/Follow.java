package com.omkarmele.inkwell.model;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("follows")
@CompoundIndexes({
    @CompoundIndex(name = "follower_following", def = "{'followerId': 1, 'followingId': 1}", unique = true),
    @CompoundIndex(name = "following_created", def = "{'followingId': 1, 'createdAt': -1}")
})
@Getter @Setter @NoArgsConstructor
public class Follow {
    @Id private String id;
    private String followerId;
    private String followingId;
    private Instant createdAt;
}
