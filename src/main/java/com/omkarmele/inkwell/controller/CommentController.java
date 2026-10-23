package com.omkarmele.inkwell.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.dto.Dtos.CommentNode;
import com.omkarmele.inkwell.dto.Dtos.CommentRequest;
import com.omkarmele.inkwell.dto.Dtos.ReactionRequest;
import com.omkarmele.inkwell.dto.Dtos.ReactionResponse;
import com.omkarmele.inkwell.security.AuthUtil;
import com.omkarmele.inkwell.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/articles/{articleId}/comments")
    public ResponseEntity<List<CommentNode>> tree(@PathVariable String articleId) {
        return ResponseEntity.ok(commentService.tree(articleId));
    }

    @PostMapping("/articles/{articleId}/comments")
    public ResponseEntity<CommentNode> add(@PathVariable String articleId, @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.add(AuthUtil.requireUserId(), articleId, request));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String commentId) {
        commentService.delete(AuthUtil.requireUserId(), commentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/comments/{id}/reaction")
    public ResponseEntity<ReactionResponse> react(@PathVariable String id, @RequestBody ReactionRequest request) {
        return ResponseEntity.ok(commentService.setReaction(AuthUtil.requireUserId(), id, request.type()));
    }
}
