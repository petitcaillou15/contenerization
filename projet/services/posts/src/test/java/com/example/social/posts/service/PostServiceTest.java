package com.example.social.posts.service;

import com.example.social.posts.domain.*;
import com.example.social.posts.service.exception.PostNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostServiceTest {

    private final PostRepository posts = mock(PostRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);
    private final io.micrometer.core.instrument.simple.SimpleMeterRegistry registry =
            new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    private final RedisPublisherStub publisher = new RedisPublisherStub();
    private final PostService service = new PostService(posts, comments, registry);

    @Test
    void feed_returnsPageSortedByRepository() {
        Post p = new Post("t", "c", 1L);
        setId(p, 1L); // JPA génère l'id à la persistance ; on le simule ici
        when(posts.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));

        var page = service.feed(0, 20);

        assertEquals(1, page.items().size());
        assertEquals(1L, page.total());
        verify(posts).findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20));
    }

    @Test
    void feed_clampsLimit() {
        when(posts.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));
        service.feed(0, 9999);
        verify(posts).findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100));
    }

    @Test
    void detail_unknownPost_throws() {
        when(posts.findById(42L)).thenReturn(Optional.empty());
        assertThrows(PostNotFoundException.class, () -> service.detail(42L));
    }

    // petit stub local, pas de Mockito nécessaire ici
    static class RedisPublisherStub {}

    // reflète l'id généré par JPA pour les besoins du test
    private static void setId(Object entity, Long id) {
        try {
            var f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
}
