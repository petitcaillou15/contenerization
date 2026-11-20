package com.example.social.posts.config;

import com.example.social.posts.users.UsersClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient usersRestClient(@Value("${users.service-url}") String baseUrl,
                                      @Value("${users.http-timeout-ms}") long timeoutMs) {
        // Timeout court : si users ne répond pas vite, on bascule en mode dégradé.
        var settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(timeoutMs))
                .withReadTimeout(Duration.ofMillis(timeoutMs));
        // .simple() (SimpleClientHttpRequestFactory) et NON .detect() : le client JDK
        // HttpClient lève une java.util.concurrent.CancellationException sur timeout de
        // lecture, qui n'est pas une RestClientException -> elle échappe aux catch de
        // UsersClient et fait un 500 au lieu de 503/DEGRADED. SimpleClientHttpRequestFactory
        // lève des IOException (SocketTimeoutException/ConnectException) que RestClient
        // enveloppe en ResourceAccessException (une RestClientException).
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.simple().build(settings))
                .build();
    }

    @Bean
    public UsersClient usersClient(RestClient usersRestClient) {
        return new UsersClient(usersRestClient);
    }
}
