package com.omkarmele.inkwell.model;

import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/** Nested comments use the adjacency-list pattern: parentId == null means top level. */
@Document("comments")
@CompoundIndex(name = "article_created", def = "{'articleId': 1, 'createdAt': 1}")
@Getter @Setter @NoArgsConstructor
public class Comment {
    @Id private String id;
    private String articleId;
    private String authorId;
    private String parentId;
    private String content;
    private boolean deleted;     // soft delete keeps the thread intact when a comment has replies
    // soft delete : hide content from ui, so that user can recover it or whatever the business logic
    private long likeCount;
    private long dislikeCount;
    private Map<String, ReactionType> reactions;

    private Instant createdAt;
}
