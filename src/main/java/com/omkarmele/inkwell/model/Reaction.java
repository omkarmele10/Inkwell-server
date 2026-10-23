package com.omkarmele.inkwell.model;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/** One row per (user, article) */
@Document("reactions")
@CompoundIndex(name = "user_article", def = "{'userId': 1, 'articleId': 1}", unique = true)
@Getter @Setter @NoArgsConstructor
public class Reaction {
    @Id private String id;
    private String userId;
    private String articleId;
    private ReactionType type;
    private Instant createdAt;
}
