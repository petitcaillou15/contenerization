package com.example.social.posts.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UsersClientTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private UsersClient client;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl("http://users.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new UsersClient(builder.build());
    }

    @Test
    void verify_returnsIdentity_on200() {
        server.expect(requestTo("http://users.test/verify"))
              .andExpect(header("Authorization", "Bearer T"))
              .andRespond(withSuccess("{\"valid\":true,\"user_id\":7,\"pseudo\":\"alice\"}",
                      MediaType.APPLICATION_JSON));

        VerifiedUser u = client.verify("T");

        assertEquals(7L, u.id());
        assertEquals("alice", u.pseudo());
        server.verify();
    }

    @Test
    void verify_throwsInvalidToken_on401() {
        server.expect(requestTo("http://users.test/verify"))
              .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
                      .body("{\"valid\":false}").contentType(MediaType.APPLICATION_JSON));

        assertThrows(UsersClient.InvalidTokenException.class, () -> client.verify("bad"));
    }

    @Test
    void verify_throwsUnavailable_on500() {
        server.expect(requestTo("http://users.test/verify"))
              .andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(UsersUnavailableException.class, () -> client.verify("T"));
    }

    @Test
    void ping_trueOn2xx_falseOnError() {
        server.expect(requestTo("http://users.test/health")).andRespond(withSuccess());
        assertTrue(client.ping());

        MockRestServiceServer s2 = MockRestServiceServer.bindTo(builder).build();
        s2.expect(requestTo("http://users.test/health"))
          .andRespond(withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        assertFalse(new UsersClient(builder.build()).ping());
    }
}
