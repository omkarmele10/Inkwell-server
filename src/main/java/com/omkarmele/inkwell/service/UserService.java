package com.omkarmele.inkwell.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.exception.ApiException;
import com.omkarmele.inkwell.model.Follow;
import com.omkarmele.inkwell.model.User;
import com.omkarmele.inkwell.dto.Dtos.*;
import com.omkarmele.inkwell.repository.FollowRepository;
import com.omkarmele.inkwell.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;

    public User register(String email, String password, String name) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setName(name == null || name.isBlank() ? normalizedEmail.substring(0, normalizedEmail.indexOf('@')) : name.trim());
        user.setCreatedAt(Instant.now());
        try {
            return userRepository.save(user);
        } catch (org.springframework.dao.DuplicateKeyException duplicateEmail) {
            throw ApiException.badRequest("An account with this email already exists");
        }
    }

    public User login(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null || user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash()))
            throw ApiException.forbidden("Invalid email or password");
        return user;
    }

    public User upsertFromGoogle(String googleSubject, String email, String name, String avatarUrl) {
        return userRepository.findByGoogleId(googleSubject).map(existingUser -> {
            if (avatarUrl != null && !avatarUrl.equals(existingUser.getAvatarUrl())) {
                existingUser.setAvatarUrl(avatarUrl);
                return userRepository.save(existingUser);
            }
            return existingUser;
        }).orElseGet(() -> {
            if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
                throw new com.omkarmele.inkwell.exception.ApiException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "An account already exists for this email.");
            }
            User newUser = new User();
            newUser.setGoogleId(googleSubject);
            newUser.setEmail(email);
            newUser.setName(name != null ? name : email);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setCreatedAt(Instant.now());
            return userRepository.save(newUser);
        });
    }

    public MeResponse me(String userId) {
        User user = find(userId);
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getAvatarUrl());
    }

    public UserProfile profile(String userId, String viewerId) {
        User user = find(userId);
        boolean followed = viewerId != null && followRepository.existsByFollowerIdAndFollowingId(viewerId, userId);
        return new UserProfile(user.getId(), user.getName(), user.getAvatarUrl(), user.getBio(), user.getFollowerCount(),
                user.getFollowingCount(), followed, userId.equals(viewerId), user.getCreatedAt());
    }

    public UserProfile updateProfile(String userId, UpdateProfileRequest profileRequest) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(userId)),
                new Update().set("name", profileRequest.name().trim())
                        .set("bio", profileRequest.bio() == null ? null : profileRequest.bio().trim()), User.class);
        return profile(userId, userId);
    }

    /** Popular authors the viewer does not follow yet. */
    public List<UserCard> suggestions(String viewerId) {
        List<String> excludedUserIds = new ArrayList<>(
                followRepository.findByFollowerId(viewerId).stream().map(Follow::getFollowingId).toList());
        excludedUserIds.add(viewerId);
        Query userQuery = Query.query(Criteria.where("_id").nin(excludedUserIds))
                .with(Sort.by(Sort.Direction.DESC, "followerCount")).limit(5);
        return mongoTemplate.find(userQuery, User.class).stream()
                .map(suggestedUser -> new UserCard(suggestedUser.getId(), suggestedUser.getName(),
                        suggestedUser.getAvatarUrl(), suggestedUser.getBio(), false)).toList();
    }

    public User find(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
    }
}
