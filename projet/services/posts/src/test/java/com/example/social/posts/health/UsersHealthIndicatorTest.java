package com.example.social.posts.health;

import com.example.social.posts.users.UsersClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsersHealthIndicatorTest {

    private final UsersClient users = mock(UsersClient.class);
    private final UsersHealthIndicator indicator = new UsersHealthIndicator(users);

    @Test
    void up_whenPingSucceeds() {
        when(users.ping()).thenReturn(true);
        assertEquals(Status.UP, indicator.health().getStatus());
    }

    @Test
    void degraded_whenPingFails() {
        when(users.ping()).thenReturn(false);
        Health h = indicator.health();
        assertEquals("DEGRADED", h.getStatus().getCode());
        assertEquals("unreachable", h.getDetails().get("users"));
    }
}
