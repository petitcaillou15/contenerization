package com.example.social.notifications.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommentCreatedListenerTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void validMessage_parsedAndForwarded() {
        AtomicReference<CommentCreatedEvent> seen = new AtomicReference<>();
        var listener = new CommentCreatedListener(mapper, seen::set);

        String json = "{\"post_id\":1,\"post_author_id\":1,\"comment_author_id\":2,"
                + "\"comment_author_pseudo\":\"bob\",\"comment_id\":10,"
                + "\"created_at\":\"2026-08-28T10:00:00Z\"}";
        listener.onMessage(new DefaultMessage("comment.created".getBytes(), json.getBytes()), null);

        CommentCreatedEvent e = seen.get();
        assertEquals(1L, e.postId());
        assertEquals(2L, e.commentAuthorId());
        assertEquals("bob", e.commentAuthorPseudo());
        assertEquals(Instant.parse("2026-08-28T10:00:00Z"), e.createdAt());
    }

    @Test
    void malformedMessage_isSwallowed() {
        AtomicReference<CommentCreatedEvent> seen = new AtomicReference<>();
        var listener = new CommentCreatedListener(mapper, seen::set);

        listener.onMessage(new DefaultMessage("comment.created".getBytes(), "not json".getBytes()), null);

        assertNull(seen.get()); // aucune exception propagée, aucun appel en aval
    }
}
