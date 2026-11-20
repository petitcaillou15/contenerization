package com.example.social.posts.service;

import com.example.social.posts.domain.*;
import com.example.social.posts.events.CommentCreatedEvent;
import com.example.social.posts.events.RedisPublisher;
import com.example.social.posts.service.exception.PostNotFoundException;
import com.example.social.posts.users.VerifiedUser;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final PostRepository posts;
    private final CommentRepository comments;
    private final RedisPublisher publisher;
    private final Counter commentsCreated;

    public CommentService(PostRepository posts, CommentRepository comments,
                          RedisPublisher publisher, MeterRegistry registry) {
        this.posts = posts;
        this.comments = comments;
        this.publisher = publisher;
        this.commentsCreated = Counter.builder("comments_created_total")
                .description("Nombre total de commentaires créés").register(registry);
    }

    @Transactional
    public Comment addComment(long postId, VerifiedUser author, String content) {
        Post post = posts.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        Comment saved = comments.save(new Comment(postId, author.id(), content));
        commentsCreated.increment();

        // posts publie un événement et ignore qui le consomme (spec §1.1).
        var event = new CommentCreatedEvent(
                post.getId(), post.getAuthorId(),
                author.id(), author.pseudo(),
                saved.getId(), saved.getCreatedAt());
        try {
            publisher.publishCommentCreated(event);
        } catch (RuntimeException e) {
            // Redis down ne doit pas faire échouer l'écriture du commentaire.
            log.warn("publication de comment.created échouée (commentaire {} conservé)", saved.getId(), e);
        }
        return saved;
    }
}
