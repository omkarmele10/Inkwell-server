package com.omkarmele.inkwell.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.model.ArticleStatus;
import com.omkarmele.inkwell.model.Follow;
import com.omkarmele.inkwell.dto.Dtos.*;
import com.omkarmele.inkwell.repository.FollowRepository;
import com.omkarmele.inkwell.security.AuthUtil;
import com.omkarmele.inkwell.service.ArticleService;
import com.omkarmele.inkwell.service.ReactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {
//    @RequiredArgsConstructor (lombok) only includes fields that are:
//        final, or
//        annotated with @NonNull
    private final ArticleService articleService;
    private final ReactionService reactionService;
    private final FollowRepository followRepository;

    @GetMapping
    public ResponseEntity<PageResponse<ArticleResponse>> list(@RequestParam(name = "q", required = false) String searchQuery,
                                                              @RequestParam(required = false) String hashtag,
                                                              @RequestParam(required = false) String authorId,
                                                              @RequestParam(defaultValue = "latest") String sort,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                articleService.searchArticles(AuthUtil.currentUserId().orElse(null),
                        searchQuery, hashtag, authorId, null, ArticleStatus.PUBLISHED,
                        sort, clampPage(page), clampSize(size)
                )
        );
    }

    @GetMapping("/following")
    public ResponseEntity<PageResponse<ArticleResponse>> following(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        String currentUserId = AuthUtil.requireUserId();
//        var is a reserved type name that can only appear in local variable type positions.
        var followedAuthorIds = followRepository.findByFollowerId(currentUserId)
                .stream()
                .map(Follow::getFollowingId) // Follow Obj call its getFollowingId() method to get the followingId
                .toList();
        return ResponseEntity.ok(articleService.searchArticles(currentUserId,
                null, null, null, followedAuthorIds, ArticleStatus.PUBLISHED,
                "latest", clampPage(page), clampSize(size)));
    }

// My articles including drafts
    @GetMapping("/mine")
    public ResponseEntity<PageResponse<ArticleResponse>> myArticles(@RequestParam(required = false) ArticleStatus status,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        String currentUserId = AuthUtil.requireUserId();
        return ResponseEntity.ok(
                articleService.searchArticles(currentUserId,
                        null, null, currentUserId, null, status,
                        "latest", clampPage(page), clampSize(size))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> get(@PathVariable("id") String articleId) {
        return ResponseEntity.ok(articleService.getArticleById(articleId, AuthUtil.currentUserId().orElse(null)));
    }

    @PostMapping
    public ResponseEntity<ArticleResponse> create(@Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(articleService.createArticle(AuthUtil.requireUserId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> update(@PathVariable("id") String articleId, @Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.ok(articleService.updateArticle(AuthUtil.requireUserId(), articleId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String articleId) {
        articleService.deleteArticle(AuthUtil.requireUserId(), articleId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reaction")
    public ResponseEntity<ReactionResponse> react(@PathVariable("id") String articleId, @RequestBody ReactionRequest request) {
        return ResponseEntity.ok(reactionService.set(AuthUtil.requireUserId(), articleId, request.type()));
    }

    @PostMapping("/{id}/summary")
    public ResponseEntity<Void> regenerateSummary(@PathVariable("id") String articleId) {
        articleService.requestArticleSummary(AuthUtil.requireUserId(), articleId);
        return ResponseEntity.accepted().build();
    }

    private static int clampPage(int pageNumber) {
        return Math.max(pageNumber, 0);
    }

    private static int clampSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), 50);
    }
}
