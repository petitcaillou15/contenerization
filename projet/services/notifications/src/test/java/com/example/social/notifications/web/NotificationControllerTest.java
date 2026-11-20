package com.example.social.notifications.web;

import com.example.social.notifications.domain.Notification;
import com.example.social.notifications.service.NotificationService;
import com.example.social.notifications.service.exception.ForbiddenException;
import com.example.social.notifications.users.VerifiedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean NotificationService service;

    private static Notification demo() {
        var n = new Notification(1L, "comment_on_post",
                "{\"post_id\":1,\"comment_id\":10,\"comment_author_pseudo\":\"bob\"}");
        try { var f = Notification.class.getDeclaredField("id"); f.setAccessible(true); f.set(n, 7L); }
        catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        return n;
    }

    @Test
    void list_returnsCallersNotifications_withParsedPayload() throws Exception {
        when(service.listFor(1L)).thenReturn(List.of(demo()));

        mvc.perform(get("/notifications")
                        .requestAttr("verifiedUser", new VerifiedUser(1L, "alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].type").value("comment_on_post"))
                .andExpect(jsonPath("$[0].payload.comment_id").value(10))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void markRead_returns204() throws Exception {
        mvc.perform(patch("/notifications/7/read")
                        .requestAttr("verifiedUser", new VerifiedUser(1L, "alice")))
                .andExpect(status().isNoContent());
    }

    @Test
    void markRead_notRecipient_returns403() throws Exception {
        doThrow(new ForbiddenException()).when(service).markRead(anyLong(), anyLong());
        mvc.perform(patch("/notifications/7/read")
                        .requestAttr("verifiedUser", new VerifiedUser(2L, "bob")))
                .andExpect(status().isForbidden());
    }
}
