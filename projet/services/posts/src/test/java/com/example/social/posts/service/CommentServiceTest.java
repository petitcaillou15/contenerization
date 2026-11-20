package com.example.social.posts.service;

import com.example.social.posts.domain.*;
import com.example.social.posts.events.CommentCreatedEvent;
import com.example.social.posts.events.RedisPublisher;
import com.example.social.posts.service.exception.PostNotFoundException;
import com.example.social.posts.users.VerifiedUser;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommentServiceTest {

    private final PostRepository posts = mock(PostRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);
    private final RedisPublisher publisher = mock(RedisPublisher.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final CommentService service = new CommentService(posts, comments, publisher, registry);

    @Test
    void addComment_savesIncrementsAndPublishesEvent() {
        Post post = new Post("t", "c", 1L);       // auteur du post = 1
        setId(post, 5L);
        when(posts.findById(5L)).thenReturn(Optional.of(post));
        when(comments.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0); setId(c, 10L); return c;
        });

        service.addComment(5L, new VerifiedUser(2L, "bob"), "Bien vu");

        assertEquals(1.0, registry.counter("comments_created_total").count());

        ArgumentCaptor<CommentCreatedEvent> ev = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(publisher).publishCommentCreated(ev.capture());
        assertEquals(5L, ev.getValue().postId());
        assertEquals(1L, ev.getValue().postAuthorId());
        assertEquals(2L, ev.getValue().commentAuthorId());
        assertEquals("bob", ev.getValue().commentAuthorPseudo());
        assertEquals(10L, ev.getValue().commentId());
    }

    @Test
    void addComment_unknownPost_throws_andDoesNotPublish() {
        when(posts.findById(99L)).thenReturn(Optional.empty());
        assertThrows(PostNotFoundException.class,
                () -> service.addComment(99L, new VerifiedUser(2L, "bob"), "x"));
        verifyNoInteractions(publisher);
    }

    // reflète l'id généré par JPA pour les besoins du test
    private static void setId(Object entity, Long id) {
        try {
            var f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
}
