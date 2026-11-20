package com.example.social.posts.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentCreatedEventTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void serialisesWithSnakeCaseKeys() throws Exception {
        var event = new CommentCreatedEvent(1L, 1L, 2L, "bob", 10L,
                Instant.parse("2026-08-28T10:00:00Z"));

        String json = mapper.writeValueAsString(event);

        assertTrue(json.contains("\"post_id\":1"), json);
        assertTrue(json.contains("\"post_author_id\":1"), json);
        assertTrue(json.contains("\"comment_author_id\":2"), json);
        assertTrue(json.contains("\"comment_author_pseudo\":\"bob\""), json);
        assertTrue(json.contains("\"comment_id\":10"), json);
        assertTrue(json.contains("\"created_at\":\"2026-08-28T10:00:00Z\""), json);
    }

    @Test
    void deserialisesBackToTheSameEvent() throws Exception {
        String json = "{\"post_id\":3,\"post_author_id\":5,\"comment_author_id\":9,"
                + "\"comment_author_pseudo\":\"zoe\",\"comment_id\":42,"
                + "\"created_at\":\"2026-08-28T12:34:56Z\"}";

        CommentCreatedEvent event = mapper.readValue(json, CommentCreatedEvent.class);

        assertEquals(3L, event.postId());
        assertEquals(9L, event.commentAuthorId());
        assertEquals("zoe", event.commentAuthorPseudo());
        assertEquals(Instant.parse("2026-08-28T12:34:56Z"), event.createdAt());
    }
}
