package com.omkarmele.inkwell.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.dto.Dtos.*;
import com.omkarmele.inkwell.security.AuthUtil;
import com.omkarmele.inkwell.service.FollowService;
import com.omkarmele.inkwell.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final FollowService followService;

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me() {
        return ResponseEntity.ok(userService.me(AuthUtil.requireUserId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfile> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(AuthUtil.requireUserId(), request));
    }

//    suggestions to follow user
    @GetMapping("/suggestions")
    public ResponseEntity<List<UserCard>> suggestions() {
        return ResponseEntity.ok(userService.suggestions(AuthUtil.requireUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfile> profile(@PathVariable("id") String userId) {
        return ResponseEntity.ok(userService.profile(userId, AuthUtil.currentUserId().orElse(null)));
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<Void> follow(@PathVariable("id") String userId) {
        followService.follow(AuthUtil.requireUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/follow")
    public ResponseEntity<Void> unfollow(@PathVariable("id") String userId) {
        followService.unfollow(AuthUtil.requireUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/followers")
    public ResponseEntity<PageResponse<UserCard>> followers(@PathVariable("id") String userId, @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(followService.followers(userId, AuthUtil.currentUserId().orElse(null), Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<PageResponse<UserCard>> following(@PathVariable("id") String userId, @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(followService.following(userId, AuthUtil.currentUserId().orElse(null), Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
    }
}
