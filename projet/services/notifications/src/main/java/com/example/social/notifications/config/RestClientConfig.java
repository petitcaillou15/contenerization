package com.example.social.notifications.config;

import com.example.social.notifications.users.UsersClient;
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
        // Timeout court : si users ne répond pas vite, on renvoie 503 (pas de mode dégradé ici).
        var settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(timeoutMs))
                .withReadTimeout(Duration.ofMillis(timeoutMs));
        // .simple() et NON .detect() : le client JDK HttpClient lève une
        // java.util.concurrent.CancellationException sur timeout de lecture (pas une
        // RestClientException) -> elle échappe au catch de UsersClient.verify() et
        // produit un 500 au lieu d'un 503. SimpleClientHttpRequestFactory lève des
        // IOException enveloppées par RestClient en ResourceAccessException.
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
