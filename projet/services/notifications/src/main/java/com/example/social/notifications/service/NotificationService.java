package com.example.social.notifications.service;

import com.example.social.notifications.domain.Notification;
import com.example.social.notifications.domain.NotificationRepository;
import com.example.social.notifications.events.CommentCreatedEvent;
import com.example.social.notifications.service.exception.ForbiddenException;
import com.example.social.notifications.service.exception.NotificationNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    public static final String NOTIFICATION_TYPE = "comment_on_post";
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repo;
    private final ObjectMapper mapper;
    private final Counter sentCounter;
    private final PubsubLagMetric lag;
    private final Clock clock;

    public NotificationService(NotificationRepository repo, ObjectMapper mapper,
                               MeterRegistry registry, PubsubLagMetric lag, Clock clock) {
        this.repo = repo;
        this.mapper = mapper;
        this.lag = lag;
        this.clock = clock;
        this.sentCounter = Counter.builder("notifications_sent_total")
                .description("Nombre total de notifications créées").register(registry);
    }

    /** Appelé par CommentCreatedListener à chaque message comment.created. */
    @Transactional
    public void onCommentCreated(CommentCreatedEvent e) {
        // On ne notifie pas quelqu'un qui commente son propre post.
        if (e.commentAuthorId() == e.postAuthorId()) {
            return;
        }
        String payload;
        try {
            payload = mapper.writeValueAsString(e);
        } catch (JsonProcessingException ex) {
            log.warn("payload comment.created non sérialisable, notification ignorée", ex);
            return;
        }
        repo.save(new Notification(e.postAuthorId(), NOTIFICATION_TYPE, payload));
        sentCounter.increment();

        // Lag de traitement pub/sub = maintenant - date de l'événement.
        double seconds = Duration.between(e.createdAt(), clock.instant()).toMillis() / 1000.0;
        lag.set(seconds);
    }

    @Transactional(readOnly = true)
    public List<Notification> listFor(long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markRead(long id, long requesterId) {
        Notification n = repo.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        if (n.getUserId() != requesterId) {
            throw new ForbiddenException();
        }
        n.markRead(); // flush via @Transactional
    }
}
