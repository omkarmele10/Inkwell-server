package com.omkarmele.inkwell.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.exception.ApiException;
import com.omkarmele.inkwell.model.Article;
import com.omkarmele.inkwell.model.ArticleStatus;
import com.omkarmele.inkwell.model.Reaction;
import com.omkarmele.inkwell.model.ReactionType;
import com.omkarmele.inkwell.model.User;
import com.omkarmele.inkwell.dto.Dtos.*;
import com.omkarmele.inkwell.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final ReactionRepository reactionRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final MongoTemplate mongoTemplate;
    private final SummaryService summaryService;
    private final NotificationService notifications;

    public ArticleResponse createArticle(String userId, ArticleRequest articleRequest) {
        Instant now = Instant.now();
        Article article = new Article();
        article.setAuthorId(userId);
        article.setTitle(articleRequest.title().trim());
        article.setContent(articleRequest.content());
        article.setCoverImageUrl(articleRequest.coverImageUrl());
        article.setHashtags(normalizeTags(articleRequest.hashtags()));
        article.setStatus(articleRequest.status());
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        article = articleRepository.save(article);
        if (article.getStatus() == ArticleStatus.PUBLISHED) handlePublishedArticle(article, true);
        return mapArticlesToResponses(userId, List.of(article), true).get(0);
    }

    public ArticleResponse updateArticle(String userId, String articleId, ArticleRequest articleRequest) {
        Article existingArticle = getOwnedArticle(userId, articleId);
        boolean wasPublished = existingArticle.getStatus() == ArticleStatus.PUBLISHED;
        boolean textChanged = !existingArticle.getContent().equals(articleRequest.content())
                || !existingArticle.getTitle().equals(articleRequest.title().trim());
        // Partial update: never overwrite counters that other users may be changing right now.
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(articleId)), new Update()
                .set("title", articleRequest.title().trim()).set("content", articleRequest.content())
                .set("coverImageUrl", articleRequest.coverImageUrl())
                .set("hashtags", normalizeTags(articleRequest.hashtags())).set("status", articleRequest.status().name())
                .set("updatedAt", Instant.now()), Article.class);
        Article updatedArticle = articleRepository.findById(articleId)
                .orElseThrow(() -> ApiException.notFound("Article"));
        boolean isNowPublished = updatedArticle.getStatus() == ArticleStatus.PUBLISHED;
        if (isNowPublished && (!wasPublished || textChanged)) handlePublishedArticle(updatedArticle, !wasPublished);
        return mapArticlesToResponses(userId, List.of(updatedArticle), true).get(0);
    }

    public void deleteArticle(String userId, String articleId) {
        getOwnedArticle(userId, articleId);
        articleRepository.deleteById(articleId);
        reactionRepository.deleteByArticleId(articleId);
        commentRepository.deleteByArticleId(articleId);
        notificationRepository.deleteByArticleId(articleId);
    }

    public void requestArticleSummary(String userId, String articleId) {
        getOwnedArticle(userId, articleId);
        summaryService.generateSummary(articleId);
    }

    public ArticleResponse getArticleById(String articleId, String viewerId) {
        Article article = articleRepository.findById(articleId).orElseThrow(() -> ApiException.notFound("Article"));
        if (article.getStatus() != ArticleStatus.PUBLISHED && !article.getAuthorId().equals(viewerId)) {
            throw ApiException.notFound("Article");      // drafts are invisible to everyone but the author
        }
        return mapArticlesToResponses(viewerId, List.of(article), true).get(0);
    }

    public PageResponse<ArticleResponse> searchArticles(String viewerId, String searchQuery, String tag,
                                                        String authorId, Collection<String> authorIds,
                                                        ArticleStatus status, String sort, int page, int size) {
        if (authorIds != null && authorIds.isEmpty()) {
            return PageResponse.of(List.of(), page, size, 0);
        }
        searchQuery = searchQuery == null ? null : searchQuery.trim();
        String hashtag = tag;
        if (StringUtils.hasText(searchQuery) && searchQuery.startsWith("#")) {
            hashtag = searchQuery;
            searchQuery = null;
        }   // "#java" is a quick hashtag search

        Query query = StringUtils.hasText(searchQuery)
                ? new TextQuery(TextCriteria.forDefaultLanguage().matching(searchQuery)) : new Query();
        if (status != null) query.addCriteria(Criteria.where("status").is(status.name()));
        if (StringUtils.hasText(hashtag)) query.addCriteria(Criteria.where("hashtags").is(cleanTag(hashtag)));
        if (authorId != null) query.addCriteria(Criteria.where("authorId").is(authorId));
        if (authorIds != null) query.addCriteria(Criteria.where("authorId").in(authorIds));

        long total = mongoTemplate.count(query, Article.class);
        query.with(Sort.by(Sort.Direction.DESC, "popular".equals(sort) ? "likeCount" : "createdAt"))
             .skip((long) page * size).limit(size);
        return PageResponse.of(mapArticlesToResponses(viewerId, mongoTemplate.find(query, Article.class), false),
                page, size, total);
    }

    // ---------------------------------------------------------------- helpers

    private void handlePublishedArticle(Article article, boolean firstPublish) {
        summaryService.generateSummary(article.getId());
        if (firstPublish) notifications.notifyFollowers(article.getAuthorId(), article.getId());
    }

    private Article getOwnedArticle(String userId, String articleId) {
        Article article = articleRepository.findById(articleId).orElseThrow(() -> ApiException.notFound("Article"));
        if (!article.getAuthorId().equals(userId)) {
            throw ApiException.forbidden("Only the author can change this article");
        }
        return article;
    }

    private List<ArticleResponse> mapArticlesToResponses( String viewerId, List<Article> articles, boolean includeFullContent) {
        if (articles.isEmpty()) return List.of();
        Set<String> authorIds = articles.stream().map(Article::getAuthorId).collect(Collectors.toSet());
        Map<String, User> authorsById = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<String> articleIds = articles.stream().map(Article::getId).toList();
        Map<String, ReactionType> reactionsByArticleId = viewerId == null ? Map.of()
                : reactionRepository.findByUserIdAndArticleIdIn(viewerId, articleIds).stream()
                        .collect(Collectors.toMap(Reaction::getArticleId, Reaction::getType));
        return articles.stream().map(article -> new ArticleResponse(
                article.getId(), article.getTitle(), includeFullContent ? article.getContent() : null,
                excerpt(article.getContent()), article.getCoverImageUrl(), article.getHashtags(), article.getSummary(),
                AuthorDto.of(authorsById.get(article.getAuthorId())), article.getLikeCount(), article.getDislikeCount(),
                article.getCommentCount(), reactionsByArticleId.get(article.getId()), article.getStatus(),
                Math.max(1, article.getContent().split("\\s+").length / 200),
                article.getCreatedAt(), article.getUpdatedAt())).toList();
    }

    private static String excerpt(String markdownContent) {
        String plainText = markdownContent.replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", "")
                .replaceAll("[#*_>`\\[\\]]", "").replaceAll("\\s+", " ").trim();
        return plainText.length() > 180 ? plainText.substring(0, 180) + "…" : plainText;
    }

    private static String cleanTag(String hashtag) {
        return (hashtag.startsWith("#") ? hashtag.substring(1) : hashtag).toLowerCase(Locale.ROOT);
    }

    private static List<String> normalizeTags(List<String> rawHashtags) {
        return rawHashtags == null ? List.of()
                : rawHashtags.stream().map(ArticleService::cleanTag).distinct().toList();
    }
}
