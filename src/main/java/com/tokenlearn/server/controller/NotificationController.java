package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.ApiResponse;
import com.tokenlearn.server.dto.MarkNotificationsReadRequest;
import com.tokenlearn.server.service.NotificationService;
import com.tokenlearn.server.util.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.tokenlearn.server.controller.ApiResponses.ok;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> unread(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(notificationService.unreadForUser(userId, limit));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Integer lessonId,
            @RequestParam(required = false) String eventType) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(notificationService.listForUser(userId, limit, offset, unreadOnly, lessonId, eventType));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unreadCount(Authentication authentication) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(notificationService.unreadCount(userId));
    }

    @PostMapping("/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markRead(
            Authentication authentication,
            @RequestBody(required = false) MarkNotificationsReadRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        List<Long> ids = request == null ? null : request.getIds();
        return ok(notificationService.markRead(userId, ids));
    }
}
