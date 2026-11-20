package com.example.social.notifications.web;

import com.example.social.notifications.auth.AuthFilter;
import com.example.social.notifications.service.NotificationService;
import com.example.social.notifications.users.VerifiedUser;
import com.example.social.notifications.web.dto.NotificationView;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class NotificationController {

    private final NotificationService notifications;
    private final ObjectMapper mapper;

    public NotificationController(NotificationService notifications, ObjectMapper mapper) {
        this.notifications = notifications;
        this.mapper = mapper;
    }

    /** Notifications du porteur du token, plus récentes d'abord. */
    @GetMapping("/notifications")
    public List<NotificationView> list(HttpServletRequest request) {
        VerifiedUser me = (VerifiedUser) request.getAttribute(AuthFilter.ATTR);
        return notifications.listFor(me.id()).stream()
                .map(n -> NotificationView.of(n, mapper))
                .toList();
    }

    /** Marque une notification comme lue. 403 si ce n'est pas la sienne. */
    @PatchMapping("/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(HttpServletRequest request, @PathVariable long id) {
        VerifiedUser me = (VerifiedUser) request.getAttribute(AuthFilter.ATTR);
        notifications.markRead(id, me.id());
    }
}
