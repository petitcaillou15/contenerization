package com.example.social.posts.service;

import com.example.social.posts.domain.*;
import com.example.social.posts.service.exception.ForbiddenException;
import com.example.social.posts.service.exception.PostNotFoundException;
import com.example.social.posts.web.dto.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final PostRepository posts;
    private final CommentRepository comments;
    private final Counter postsCreated;

    public PostService(PostRepository posts, CommentRepository comments, MeterRegistry registry) {
        this.posts = posts;
        this.comments = comments;
        this.postsCreated = Counter.builder("posts_created_total")
                .description("Nombre total de posts créés").register(registry);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummary> feed(int page, int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        int safePage = Math.max(page, 0);
        var result = posts.findAllByOrderByCreatedAtDesc(PageRequest.of(safePage, safeLimit));
        return new PageResponse<>(
                result.map(PostSummary::of).getContent(),
                safePage, safeLimit, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PostDetail detail(long id) {
        Post p = posts.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        List<CommentView> cs = comments.findByPostIdOrderByCreatedAtAsc(id)
                .stream().map(CommentView::of).toList();
        return new PostDetail(p.getId(), p.getTitle(), p.getContent(),
                p.getAuthorId(), p.getCreatedAt(), cs);
    }

    @Transactional
    public Post create(String title, String content, long authorId) {
        Post saved = posts.save(new Post(title, content, authorId));
        postsCreated.increment();
        return saved;
    }

    @Transactional
    public void delete(long id, long requesterId) {
        Post p = posts.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        if (p.getAuthorId() != requesterId) {
            throw new ForbiddenException();
        }
        posts.delete(p); // les commentaires partent en cascade (FK ON DELETE CASCADE)
    }
}
