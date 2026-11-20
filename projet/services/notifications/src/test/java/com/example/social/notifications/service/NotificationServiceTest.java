package com.example.social.notifications.service;

import com.example.social.notifications.domain.Notification;
import com.example.social.notifications.domain.NotificationRepository;
import com.example.social.notifications.events.CommentCreatedEvent;
import com.example.social.notifications.service.exception.ForbiddenException;
import com.example.social.notifications.service.exception.NotificationNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final PubsubLagMetric lag = new PubsubLagMetric(registry);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-28T10:00:05Z"), ZoneOffset.UTC);
    private final NotificationService service =
            new NotificationService(repo, mapper, registry, lag, clock);

    private CommentCreatedEvent event(long postAuthor, long commentAuthor) {
        return new CommentCreatedEvent(1L, postAuthor, commentAuthor, "bob", 10L,
                Instant.parse("2026-08-28T10:00:00Z"));
    }

    @Test
    void onCommentCreated_createsNotificationForPostAuthor_incrementsCounter_setsLag() {
        when(repo.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        service.onCommentCreated(event(1L, 2L));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repo).save(saved.capture());
        assertEquals(1L, saved.getValue().getUserId());              // destinataire = auteur du post
        assertEquals("comment_on_post", saved.getValue().getType());
        assertTrue(saved.getValue().getPayload().contains("\"comment_id\":10"));

        assertEquals(1.0, registry.counter("notifications_sent_total").count());
        assertEquals(5.0, registry.get("notifications_pubsub_lag_seconds").gauge().value(), 0.001);
    }

    @Test
    void onCommentCreated_skipsSelfComment() {
        service.onCommentCreated(event(3L, 3L)); // auteur du post == auteur du commentaire
        verifyNoInteractions(repo);
        assertEquals(0.0, registry.counter("notifications_sent_total").count());
    }

    @Test
    void markRead_wrongUser_throwsForbidden() {
        Notification n = new Notification(1L, "comment_on_post", "{}");
        setId(n, 7L);
        when(repo.findById(7L)).thenReturn(Optional.of(n));
        assertThrows(ForbiddenException.class, () -> service.markRead(7L, 999L));
    }

    @Test
    void markRead_unknown_throwsNotFound() {
        when(repo.findById(42L)).thenReturn(Optional.empty());
        assertThrows(NotificationNotFoundException.class, () -> service.markRead(42L, 1L));
    }

    private static void setId(Object e, Long id) {
        try { var f = e.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(e, id); }
        catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
    }
}
