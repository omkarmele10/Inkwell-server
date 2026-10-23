package com.omkarmele.inkwell.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.exception.ApiException;
import com.omkarmele.inkwell.model.Article;
import com.omkarmele.inkwell.model.NotificationType;
import com.omkarmele.inkwell.model.Reaction;
import com.omkarmele.inkwell.model.ReactionType;
import com.omkarmele.inkwell.dto.Dtos.ReactionResponse;
import com.omkarmele.inkwell.repository.ArticleRepository;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReactionService {
    private final ArticleRepository articleRepository;
    private final MongoTemplate mongoTemplate;
    private final NotificationService notifications;

    /**
     * Idempotent "set my reaction". The previous value is read and replaced in ONE atomic operation,
     * so the counters stay correct even with concurrent clicks.
     */
    public ReactionResponse set(String userId, String articleId, ReactionType type) {
        Article article = articleRepository.findById(articleId).orElseThrow(() -> ApiException.notFound("Article"));
        Query userArticleReactionQuery = Query.query(
                Criteria.where("userId").is(userId).and("articleId").is(articleId));

        Reaction previousReaction = type == null
                ? mongoTemplate.findAndRemove(userArticleReactionQuery, Reaction.class)
                : mongoTemplate.findAndModify(userArticleReactionQuery,
                        new Update().set("type", type.name()).setOnInsert("createdAt", Instant.now()),
                        FindAndModifyOptions.options().upsert(true).returnNew(false), Reaction.class);
        ReactionType previousReactionType = previousReaction == null ? null : previousReaction.getType();

        if (previousReactionType != type) {
            Update counterUpdate = new Update();
            if (previousReactionType == ReactionType.LIKE) counterUpdate.inc("likeCount", -1);
            if (previousReactionType == ReactionType.DISLIKE) counterUpdate.inc("dislikeCount", -1);
            if (type == ReactionType.LIKE) counterUpdate.inc("likeCount", 1);
            if (type == ReactionType.DISLIKE) counterUpdate.inc("dislikeCount", 1);
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(articleId)), counterUpdate, Article.class);
            if (type == ReactionType.LIKE) {
                notifications.notify(article.getAuthorId(), userId, NotificationType.LIKE, articleId, null);
            }
        }
        Article updatedArticle = articleRepository.findById(articleId).orElse(article);
        return new ReactionResponse(updatedArticle.getLikeCount(), updatedArticle.getDislikeCount(), type);
    }
}
