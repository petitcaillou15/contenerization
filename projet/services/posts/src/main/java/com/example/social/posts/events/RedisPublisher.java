package com.example.social.posts.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Publie sur le canal Redis. Aucun appel direct vers notifications. */
@Component
public class RedisPublisher {

    public static final String CHANNEL = "comment.created";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisPublisher(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    public void publishCommentCreated(CommentCreatedEvent event) {
        try {
            redis.convertAndSend(CHANNEL, mapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            // Ne doit jamais arriver avec ce record ; on ne bloque pas l'écriture du commentaire.
            throw new IllegalStateException("sérialisation de l'événement impossible", e);
        }
    }
}
