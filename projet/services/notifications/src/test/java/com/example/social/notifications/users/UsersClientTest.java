package com.example.social.notifications.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
              .andRespond(withSuccess("{\"valid\":true,\"user_id\":4,\"pseudo\":\"alice\"}",
                      MediaType.APPLICATION_JSON));

        VerifiedUser u = client.verify("T");

        assertEquals(4L, u.id());
        assertEquals("alice", u.pseudo());
        server.verify();
    }

    @Test
    void verify_throwsInvalidToken_on401() {
        server.expect(requestTo("http://users.test/verify"))
              .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"valid\":false}")
                      .contentType(MediaType.APPLICATION_JSON));
        assertThrows(UsersClient.InvalidTokenException.class, () -> client.verify("bad"));
    }

    @Test
    void verify_throwsUnavailable_on500() {
        server.expect(requestTo("http://users.test/verify"))
              .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThrows(UsersUnavailableException.class, () -> client.verify("T"));
    }
}
