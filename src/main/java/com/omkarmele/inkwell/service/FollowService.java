package com.omkarmele.inkwell.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.exception.ApiException;
import com.omkarmele.inkwell.model.Follow;
import com.omkarmele.inkwell.model.NotificationType;
import com.omkarmele.inkwell.model.User;
import com.omkarmele.inkwell.dto.Dtos.*;
import com.omkarmele.inkwell.repository.FollowRepository;
import com.omkarmele.inkwell.repository.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final NotificationService notifications;

    public void follow(String followerId, String targetId) {
        if (followerId.equals(targetId)) throw ApiException.badRequest("You cannot follow yourself");
        if (!userRepository.existsById(targetId)) throw ApiException.notFound("User");
        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(targetId);
        follow.setCreatedAt(Instant.now());
        try {
            followRepository.insert(follow);
        } catch (DuplicateKeyException already) {
            return; // idempotent
        }
        bump(targetId, "followerCount", 1);
        bump(followerId, "followingCount", 1);
        notifications.notify(targetId, followerId, NotificationType.FOLLOW, null, null);
    }

    public void unfollow(String followerId, String targetId) {
        long deleted = mongoTemplate.remove(Query.query(Criteria.where("followerId").is(followerId).and("followingId").is(targetId)),
                Follow.class).getDeletedCount();
        if (deleted > 0) {
            bump(targetId, "followerCount", -1);
            bump(followerId, "followingCount", -1);
        }
    }

    public PageResponse<UserCard> followers(String userId, String viewerId, int page, int size) {
        Page<Follow> followPage = followRepository.findByFollowingId(
                userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return userCards(followPage.map(Follow::getFollowerId).getContent(), viewerId, page, size, followPage.getTotalElements());
    }

    public PageResponse<UserCard> following(String userId, String viewerId, int page, int size) {
        Page<Follow> followPage = followRepository.findByFollowerId(
                userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return userCards(followPage.map(Follow::getFollowingId).getContent(), viewerId, page, size, followPage.getTotalElements());
    }

    private PageResponse<UserCard> userCards(List<String> userIds, String viewerId, int page, int size, long totalUsers) {
        Map<String, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Set<String> followedUserIds = viewerId == null ? Set.of()
                : followRepository.findByFollowerIdAndFollowingIdIn(viewerId, userIds).stream()
                        .map(Follow::getFollowingId).collect(Collectors.toSet());
        List<UserCard> userCards = userIds.stream().filter(usersById::containsKey).map(userId -> {
            User user = usersById.get(userId);
            return new UserCard(user.getId(), user.getName(), user.getAvatarUrl(), user.getBio(),
                    followedUserIds.contains(userId));
        }).toList();
        return PageResponse.of(userCards, page, size, totalUsers);
    }

    private void bump(String userId, String counterField, int countDelta) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(userId)),
                new Update().inc(counterField, countDelta), User.class);
    }
}
