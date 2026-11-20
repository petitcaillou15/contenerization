package com.example.social.notifications.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * Écoute le canal "comment.created". Un message mal formé est ignoré (log warn)
 * pour ne pas casser l'abonnement.
 */
public class CommentCreatedListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(CommentCreatedListener.class);

    private final ObjectMapper mapper;
    private final Consumer<CommentCreatedEvent> sink;

    public CommentCreatedListener(ObjectMapper mapper, Consumer<CommentCreatedEvent> sink) {
        this.mapper = mapper;
        this.sink = sink;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            CommentCreatedEvent event = mapper.readValue(body, CommentCreatedEvent.class);
            sink.accept(event);
        } catch (Exception e) {
            log.warn("message comment.created illisible, ignoré: {}", body, e);
        }
    }
}
