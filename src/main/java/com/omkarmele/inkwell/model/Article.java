package com.omkarmele.inkwell.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("articles")
//     Compound Index = Filter + Sort in one index, avoids expensive in-memory sort; MongoDB returns results pre-ordered.
@CompoundIndexes({
    @CompoundIndex(name = "author_created", def = "{'authorId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "status_created", def = "{'status': 1, 'createdAt': -1}")
})
@Getter @Setter @NoArgsConstructor
public class Article {
    @Id private String id;
    private String authorId;
    @TextIndexed(weight = 3) private String title; // if title is matched to my search query then give it priority 3X more in results , whose only content match
    @TextIndexed private String content;            // Markdown, rendered safely on the client
    private String coverImageUrl;
    @Indexed private List<String> hashtags = new ArrayList<>();
    private String summary;                         // AI generated summary
    private long likeCount;                         // denormalised counters, changed only with atomic $inc
    private long dislikeCount;
    private long commentCount;
    private com.omkarmele.inkwell.model.ArticleStatus status;
    private Instant createdAt; // Instance is time class which is suitable for machines, store time in UTC(global)
    private Instant updatedAt;
}
