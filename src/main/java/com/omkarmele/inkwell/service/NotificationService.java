package com.omkarmele.inkwell.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.exception.ApiException;
import com.omkarmele.inkwell.model.Article;
import com.omkarmele.inkwell.model.Follow;
import com.omkarmele.inkwell.model.Notification;
import com.omkarmele.inkwell.model.NotificationType;
import com.omkarmele.inkwell.model.User;
import com.omkarmele.inkwell.dto.Dtos.*;
import com.omkarmele.inkwell.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final FollowRepository followRepository;
    private final MongoTemplate mongoTemplate;

    /** Async so the user's own request never waits on it. Upsert = the same event can never notify twice. */
    @Async
    public void notify(String recipientId, String actorId, NotificationType type, String articleId, String commentId) {
        if (recipientId == null || recipientId.equals(actorId)) return;
        Query notificationQuery = Query.query(Criteria.where("recipientId").is(recipientId).and("actorId").is(actorId)
                .and("type").is(type.name()).and("articleId").is(articleId).and("commentId").is(commentId));
        mongoTemplate.upsert(notificationQuery, new Update().setOnInsert("read", false)
                .setOnInsert("createdAt", Instant.now()), Notification.class);
    }

    /** Fan-out on write, 500 followers at a time. */
    @Async
    public void notifyFollowers(String authorId, String articleId) {
        int page = 0;
        Slice<Follow> followerSlice;
        do {
            followerSlice = followRepository.findSliceByFollowingId(authorId, PageRequest.of(page++, 500));
            List<Notification> notificationBatch = new ArrayList<>();
            for (Follow follow : followerSlice.getContent()) {
                Notification notification = new Notification();
                notification.setRecipientId(follow.getFollowerId());
                notification.setActorId(authorId);
                notification.setType(NotificationType.NEW_ARTICLE);
                notification.setArticleId(articleId);
                notification.setCreatedAt(Instant.now());
                notificationBatch.add(notification);
            }
            if (!notificationBatch.isEmpty()) notificationRepository.insert(notificationBatch);
        } while (followerSlice.hasNext());
    }

    public PageResponse<NotificationResponse> list(String userId, int page, int size) {
        Page<Notification> notificationPage = notificationRepository.findByRecipientId(
                userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Set<String> actorIds = notificationPage.stream().map(Notification::getActorId).collect(Collectors.toSet());
        Set<String> articleIds = notificationPage.stream().map(Notification::getArticleId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<String, User> actorsById = userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<String, String> articleTitlesById = articleRepository.findAllById(articleIds).stream()
                .collect(Collectors.toMap(Article::getId, Article::getTitle));
        List<NotificationResponse> notificationResponses = notificationPage.stream()
                .map(notification -> new NotificationResponse(notification.getId(), notification.getType(),
                        AuthorDto.of(actorsById.get(notification.getActorId())), notification.getArticleId(),
                        articleTitlesById.get(notification.getArticleId()), notification.isRead(), notification.getCreatedAt()))
                .toList();
        return PageResponse.of(notificationResponses, page, size, notificationPage.getTotalElements());
    }

    public long unreadCount(String userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    public void markRead(String userId, String notificationId) {
        long matchedNotifications = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(notificationId).and("recipientId").is(userId)),
                new Update().set("read", true), Notification.class).getMatchedCount();
        if (matchedNotifications == 0) throw ApiException.notFound("Notification");
    }

    public void markAllRead(String userId) {
        mongoTemplate.updateMulti(Query.query(Criteria.where("recipientId").is(userId).and("read").is(false)),
                new Update().set("read", true), Notification.class);
    }
}
