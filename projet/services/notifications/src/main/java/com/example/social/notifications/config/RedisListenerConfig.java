package com.example.social.notifications.config;

import com.example.social.notifications.events.CommentCreatedListener;
import com.example.social.notifications.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Abonnement Redis au canal comment.created.
 * Désactivé sous le profil "test" (application-test.yml exclut RedisAutoConfiguration,
 * donc pas de RedisConnectionFactory). En prod le profil "test" est absent -> actif.
 *
 * NB : ne PAS utiliser @ConditionalOnBean(RedisConnectionFactory.class) ici. Sur une
 * @Configuration utilisateur cette condition est évaluée AVANT l'auto-config Redis,
 * donc elle échoue toujours et l'abonnement ne serait jamais créé, même en prod.
 */
@Configuration
@Profile("!test")
public class RedisListenerConfig {

    public static final String CHANNEL = "comment.created";

    @Bean
    public CommentCreatedListener commentCreatedListener(ObjectMapper mapper,
                                                         NotificationService service) {
        return new CommentCreatedListener(mapper, service::onCommentCreated);
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory factory,
                                                        CommentCreatedListener listener) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listener, new ChannelTopic(CHANNEL));
        return container;
    }
}
