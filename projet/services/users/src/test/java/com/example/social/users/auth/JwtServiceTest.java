package com.example.social.users.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";

    private JwtService newService(long ttlSeconds) {
        return new JwtService(SECRET, ttlSeconds);
    }

    @Test
    void issueThenParse_roundTrips() {
        JwtService jwt = newService(3600);

        String token = jwt.issue(42L, "alice");
        AuthenticatedUser user = jwt.parse(token);

        assertEquals(42L, user.id());
        assertEquals("alice", user.pseudo());
    }

    @Test
    void parse_rejectsGarbage() {
        JwtService jwt = newService(3600);
        assertThrows(JwtService.InvalidTokenException.class, () -> jwt.parse("not.a.jwt"));
    }

    @Test
    void parse_rejectsWrongSignature() {
        String token = new JwtService(SECRET, 3600).issue(1L, "x");
        JwtService other = new JwtService("another-secret-another-secret-0123456789xx", 3600);
        assertThrows(JwtService.InvalidTokenException.class, () -> other.parse(token));
    }

    @Test
    void parse_rejectsExpiredToken() {
        JwtService jwt = newService(-1); // déjà expiré à l'émission
        String token = jwt.issue(7L, "bob");
        assertThrows(JwtService.InvalidTokenException.class, () -> jwt.parse(token));
    }
}
