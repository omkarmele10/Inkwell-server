package com.omkarmele.inkwell.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.exception.ApiException;
import com.omkarmele.inkwell.model.Article;
import com.omkarmele.inkwell.model.Comment;
import com.omkarmele.inkwell.model.ReactionType;
import com.omkarmele.inkwell.model.NotificationType;
import com.omkarmele.inkwell.model.User;
import com.omkarmele.inkwell.dto.Dtos.*;
import com.omkarmele.inkwell.repository.ArticleRepository;
import com.omkarmele.inkwell.repository.CommentRepository;
import com.omkarmele.inkwell.repository.UserRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {
    static final int MAX_DEPTH = 20;   // 0 = top level; the UI hides "Reply" at this depth

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final NotificationService notifications;

    public List<CommentNode> tree(String articleId) {
        String viewerId = com.omkarmele.inkwell.security.AuthUtil.currentUserId().orElse(null);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> ApiException.notFound("Article"));
        ensureArticleVisible(article, viewerId);
        List<Comment> comments = commentRepository.findByArticleIdOrderByCreatedAtAsc(articleId);
        Map<String, User> authorsById = userRepository.findAllById(
                        comments.stream().map(Comment::getAuthorId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<String, CommentNode> commentNodesById = new LinkedHashMap<>();
        for (Comment comment : comments) {
            ReactionType myReaction = viewerId == null || comment.getReactions() == null
                    ? null : comment.getReactions().get(viewerId);
            commentNodesById.put(comment.getId(), new CommentNode(comment.getId(), comment.getParentId(),
                    comment.isDeleted() ? AuthorDto.of(null) : AuthorDto.of(authorsById.get(comment.getAuthorId())),
                    comment.isDeleted() ? "" : comment.getContent(), comment.isDeleted(),
                    comment.isDeleted() ? 0 : comment.getLikeCount(), comment.isDeleted() ? 0 : comment.getDislikeCount(),
                    comment.isDeleted() ? null : myReaction,
                    comment.getCreatedAt(), new ArrayList<>()));
        }
        List<CommentNode> rootComments = new ArrayList<>();
        for (CommentNode commentNode : commentNodesById.values()) {
            CommentNode parentNode = commentNode.parentId() == null
                    ? null : commentNodesById.get(commentNode.parentId());
            if (parentNode == null) rootComments.add(commentNode);
            else parentNode.replies().add(commentNode);
        }
        return rootComments;
    }

    public CommentNode add(String userId, String articleId, CommentRequest commentRequest) {
        Article article = articleRepository.findById(articleId).orElseThrow(() -> ApiException.notFound("Article"));
        ensureArticleVisible(article, userId);
        Comment parentComment = null;
        if (commentRequest.parentId() != null) {
            parentComment = commentRepository.findById(commentRequest.parentId())
                    .orElseThrow(() -> ApiException.notFound("Comment"));
            if (!parentComment.getArticleId().equals(articleId)) throw ApiException.badRequest("Parent belongs to another article");
            if (parentComment.isDeleted()) throw ApiException.badRequest("Cannot reply to a deleted comment");
            if (depthOf(parentComment) >= MAX_DEPTH) throw ApiException.badRequest("Reply depth limit reached");
        }
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setAuthorId(userId);
        comment.setParentId(commentRequest.parentId());
        comment.setContent(commentRequest.content().trim());
        comment.setCreatedAt(Instant.now());
        comment = commentRepository.save(comment);
        bumpCount(articleId, 1);
        if (parentComment == null) {
            notifications.notify(article.getAuthorId(), userId, NotificationType.COMMENT, articleId, comment.getId());
        } else {
            notifications.notify(parentComment.getAuthorId(), userId, NotificationType.REPLY, articleId, comment.getId());
        }
        return new CommentNode(comment.getId(), comment.getParentId(),
                AuthorDto.of(userRepository.findById(userId).orElse(null)),
                comment.getContent(), false, 0, 0, null, comment.getCreatedAt(), new ArrayList<>());
    }

    public ReactionResponse setReaction(String userId, String commentId, ReactionType type) {
        String reactionPath = "reactions." + userId;
        while (true) {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> ApiException.notFound("Comment"));
            if (comment.isDeleted()) throw ApiException.badRequest("Cannot react to a deleted comment");
            Map<String, ReactionType> reactions = comment.getReactions();
            ReactionType previousType = reactions == null ? null : reactions.get(userId);
            if (previousType == type) {
                return new ReactionResponse(comment.getLikeCount(), comment.getDislikeCount(), type);
            }

            Query query = Query.query(Criteria.where("_id").is(commentId)
                    .and("deleted").ne(true));
            query.addCriteria(previousType == null
                    ? Criteria.where(reactionPath).exists(false)
                    : Criteria.where(reactionPath).is(previousType.name()));
            Update counters = new Update();
            if (type == null) counters.unset(reactionPath);
            else counters.set(reactionPath, type.name());
            if (previousType == ReactionType.LIKE) counters.inc("likeCount", -1);
            if (previousType == ReactionType.DISLIKE) counters.inc("dislikeCount", -1);
            if (type == ReactionType.LIKE) counters.inc("likeCount", 1);
            if (type == ReactionType.DISLIKE) counters.inc("dislikeCount", 1);
            if (mongoTemplate.updateFirst(query, counters, Comment.class).getMatchedCount() > 0) break;
        }
        Comment updated = commentRepository.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Comment"));
        return new ReactionResponse(updated.getLikeCount(), updated.getDislikeCount(), type);
    }

    public void delete(String userId, String commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> ApiException.notFound("Comment"));
        Article article = articleRepository.findById(comment.getArticleId()).orElse(null);
        boolean allowed = comment.getAuthorId().equals(userId)
                || (article != null && article.getAuthorId().equals(userId));
        if (!allowed) throw ApiException.forbidden("You cannot delete this comment");
        if (comment.isDeleted()) return;
        if (commentRepository.existsByParentId(commentId)) {   // keep the thread readable: leave a tombstone
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(commentId)),
                    new Update().set("deleted", true).set("content", ""), Comment.class);
        } else {
            commentRepository.deleteById(commentId);
        }
        bumpCount(comment.getArticleId(), -1);
    }

    private void ensureArticleVisible(Article article, String viewerId) {
        if (article.getStatus() != com.omkarmele.inkwell.model.ArticleStatus.PUBLISHED
                && (viewerId == null || !article.getAuthorId().equals(viewerId))) {
            throw ApiException.notFound("Article");
        }
    }

    private int depthOf(Comment comment) {
        int depth = 0;
        String parentId = comment.getParentId();
        while (parentId != null && depth <= MAX_DEPTH) {
            Comment parentComment = commentRepository.findById(parentId).orElse(null);
            if (parentComment == null) break;
            depth++;
            parentId = parentComment.getParentId();
        }
        return depth;
    }

    private void bumpCount(String articleId, int delta) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(articleId)),
                new Update().inc("commentCount", delta), Article.class);
    }
}
