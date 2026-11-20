package com.example.social.notifications.auth;

import com.example.social.notifications.users.UsersClient;
import com.example.social.notifications.users.UsersUnavailableException;
import com.example.social.notifications.users.VerifiedUser;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthFilterTest {

    private final UsersClient users = mock(UsersClient.class);
    private final AuthFilter filter = new AuthFilter(users);

    @Test
    void get_validToken_setsAttribute() throws Exception {
        when(users.verify("T")).thenReturn(new VerifiedUser(1L, "alice"));
        var req = new MockHttpServletRequest("GET", "/notifications");
        req.addHeader("Authorization", "Bearer T");
        var res = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertNotNull(req.getAttribute("verifiedUser"));
        verify(chain).doFilter(req, res);
    }

    @Test
    void get_missingHeader_returns401() throws Exception {
        var req = new MockHttpServletRequest("GET", "/notifications");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, mock(FilterChain.class));
        assertEquals(401, res.getStatus());
    }

    @Test
    void patch_invalidToken_returns401() throws Exception {
        when(users.verify(any())).thenThrow(new UsersClient.InvalidTokenException());
        var req = new MockHttpServletRequest("PATCH", "/notifications/1/read");
        req.addHeader("Authorization", "Bearer bad");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, mock(FilterChain.class));
        assertEquals(401, res.getStatus());
    }

    @Test
    void get_usersDown_returns503() throws Exception {
        when(users.verify(any())).thenThrow(new UsersUnavailableException("down"));
        var req = new MockHttpServletRequest("GET", "/notifications");
        req.addHeader("Authorization", "Bearer T");
        var res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        assertEquals(503, res.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(
            res.getContentAsString().contains("users service unavailable"));
    }
}
