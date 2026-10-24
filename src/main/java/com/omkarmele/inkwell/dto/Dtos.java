package com.omkarmele.inkwell.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import java.time.Instant;
import java.util.List;
import com.omkarmele.inkwell.model.ArticleStatus;
import com.omkarmele.inkwell.model.NotificationType;
import com.omkarmele.inkwell.model.ReactionType;
import com.omkarmele.inkwell.model.User;

/** API contracts. Entities are never returned directly. */
public final class Dtos {
    private Dtos() {}

    public record AuthorDto(String id, String name, String avatarUrl) {
        public static AuthorDto of(User user) {
            return user == null ? new AuthorDto(null, "Deleted user", null)
                    : new AuthorDto(user.getId(), user.getName(), user.getAvatarUrl());
        }
    }

    public record MeResponse(String id, String name, String email, String avatarUrl) {}

    public record AuthResponse(String accessToken, String id, String name, String email, String avatarUrl) {}

    public record EmailAuthRequest(@NotBlank @Email @Size(max = 254) String email,
                                  @NotBlank @Size(min = 8, max = 72) String password,
                                  @Size(min = 1, max = 60) String name) {}

    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 254) String email) {}

    public record ForgotPasswordResponse(String status) {}

    public record ResetPasswordRequest(@NotBlank String token,
                                       @NotBlank @Size(min = 8, max = 72) String password) {}

    public record UserProfile(String id, String name, String avatarUrl, String bio, long followerCount,
                              long followingCount, boolean followedByMe, boolean self, Instant joinedAt) {}

    public record UserCard(String id, String name, String avatarUrl, String bio, boolean followedByMe) {}

    public record UpdateProfileRequest(@NotBlank @Size(max = 60) String name, @Size(max = 280) String bio) {}

    public record ArticleRequest(
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 50_000) String content,
            @Pattern(regexp = "^https://res\\.cloudinary\\.com/[A-Za-z0-9_-]+/image/upload/[A-Za-z0-9_./-]+$",
                    message = "must be an uploaded image") String coverImageUrl,
            @Size(max = 5) List<@Pattern(regexp = "^#?[A-Za-z0-9_]{2,30}$", message = "invalid hashtag") String> hashtags,
            @NotNull ArticleStatus status
    ) {}

    public record ArticleResponse(String id, String title, String content, String excerpt, String coverImageUrl,
                                  List<String> hashtags, String summary, AuthorDto author, long likeCount,
                                  long dislikeCount, long commentCount, ReactionType myReaction, ArticleStatus status,
                                  int readMinutes, Instant createdAt, Instant updatedAt
    ) {}

    /** type == null removes the reaction. */
    public record ReactionRequest(ReactionType type) {}

    public record ReactionResponse(long likeCount, long dislikeCount, ReactionType myReaction) {}

    public record CommentRequest(@NotBlank @Size(max = 2000) String content, String parentId) {}

    public record CommentNode(String id, String parentId, AuthorDto author, String content, boolean deleted,
                              long likeCount, long dislikeCount, ReactionType myReaction,
                              Instant createdAt, List<CommentNode> replies) {}

    public record NotificationResponse(String id, NotificationType type, AuthorDto actor, String articleId,
                                       String articleTitle, boolean read, Instant createdAt) {}

    public record TagCount(String tag, long count) {}

    public record UploadResponse(String url) {}

    public record PageResponse<T>(List<T> items, int page, int size, long total, boolean hasNext) {
        public static <T> PageResponse<T> of(List<T> items, int page, int size, long total) {
            return new PageResponse<>(items, page, size, total, (long) (page + 1) * size < total);
        }
    }
}
