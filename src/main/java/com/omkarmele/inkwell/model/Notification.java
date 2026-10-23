package com.omkarmele.inkwell.model;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("notifications")
@CompoundIndex(name = "recipient_created", def = "{'recipientId': 1, 'createdAt': -1}")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id private String id;
    private String recipientId;
    private String actorId;
    private NotificationType type;
    private String articleId;
    private String commentId;
    private boolean read;
    private Instant createdAt;
}
