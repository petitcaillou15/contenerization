package com.example.social.notifications.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Couplage SYNCHRONE notifications -> users pour valider le token.
 * notifications ne décode jamais le JWT : il délègue la validation à users via GET /verify.
 */
public class UsersClient {

    private final RestClient http;

    public UsersClient(RestClient usersRestClient) {
        this.http = usersRestClient;
    }

    /** Valide le token auprès de users. */
    public VerifiedUser verify(String bearerToken) {
        try {
            VerifyBody body = http.get()
                    .uri("/verify")
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .onStatus(s -> s.value() == 401, (req, res) -> { throw new InvalidTokenException(); })
                    .body(VerifyBody.class);
            if (body == null || !body.valid()) {
                throw new InvalidTokenException();
            }
            return new VerifiedUser(body.userId(), body.pseudo());
        } catch (InvalidTokenException e) {
            throw e;
        } catch (RestClientException e) {
            throw new UsersUnavailableException("users injoignable lors de /verify", e);
        }
    }

    /** Token rejeté par users. Mène à un 401. */
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException() { super("invalid token"); }
    }

    private record VerifyBody(boolean valid,
                              @JsonProperty("user_id") long userId,
                              String pseudo) {}
}
