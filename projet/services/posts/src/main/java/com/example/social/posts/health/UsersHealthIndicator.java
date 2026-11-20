package com.example.social.posts.health;

import com.example.social.posts.users.UsersClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

/**
 * Composant de santé "usersDependency".
 * users OK -> UP. users injoignable -> DEGRADED (et NON DOWN) : /health reste
 * en HTTP 200 (spec §2.3.1), seules les écritures sont bloquées (voir AuthFilter).
 */
@Component("usersDependency")
public class UsersHealthIndicator implements HealthIndicator {

    /** Statut custom, ordonné entre DOWN et OUT_OF_SERVICE via application.yml. */
    public static final Status DEGRADED = new Status("DEGRADED");

    private final UsersClient users;

    public UsersHealthIndicator(UsersClient users) {
        this.users = users;
    }

    @Override
    public Health health() {
        if (users.ping()) {
            return Health.up().build();
        }
        return Health.status(DEGRADED).withDetail("users", "unreachable").build();
    }
}
