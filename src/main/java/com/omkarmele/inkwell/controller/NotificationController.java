package com.omkarmele.inkwell.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.dto.Dtos.NotificationResponse;
import com.omkarmele.inkwell.dto.Dtos.PageResponse;
import com.omkarmele.inkwell.security.AuthUtil;
import com.omkarmele.inkwell.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.list(AuthUtil.requireUserId(), Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unread() {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(AuthUtil.requireUserId())));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> readAll() {
        notificationService.markAllRead(AuthUtil.requireUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> read(@PathVariable("id") String notificationId) {
        notificationService.markRead(AuthUtil.requireUserId(), notificationId);
        return ResponseEntity.noContent().build();
    }
}
