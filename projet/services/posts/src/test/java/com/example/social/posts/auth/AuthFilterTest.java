package com.example.social.posts.auth;

import com.example.social.posts.users.UsersClient;
import com.example.social.posts.users.UsersUnavailableException;
import com.example.social.posts.users.VerifiedUser;
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
    void get_passesThrough_withoutCallingUsers() throws Exception {
        var req = new MockHttpServletRequest("GET", "/posts");
        var res = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        verifyNoInteractions(users);
    }

    @Test
    void write_validToken_setsAttribute() throws Exception {
        when(users.verify("T")).thenReturn(new VerifiedUser(3L, "carol"));
        var req = new MockHttpServletRequest("POST", "/posts");
        req.addHeader("Authorization", "Bearer T");
        var res = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        VerifiedUser u = (VerifiedUser) req.getAttribute("verifiedUser");
        assertNotNull(u);
        assertEquals(3L, u.id());
        verify(chain).doFilter(req, res);
    }

    @Test
    void write_missingHeader_returns401() throws Exception {
        var req = new MockHttpServletRequest("POST", "/posts");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, mock(FilterChain.class));
        assertEquals(401, res.getStatus());
    }

    @Test
    void write_invalidToken_returns401() throws Exception {
        when(users.verify(any())).thenThrow(new UsersClient.InvalidTokenException());
        var req = new MockHttpServletRequest("DELETE", "/posts/1");
        req.addHeader("Authorization", "Bearer bad");
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, mock(FilterChain.class));
        assertEquals(401, res.getStatus());
    }

    @Test
    void write_usersDown_returns503_withSpecMessage() throws Exception {
        when(users.verify(any())).thenThrow(new UsersUnavailableException("down"));
        var req = new MockHttpServletRequest("POST", "/posts/1/comments");
        req.addHeader("Authorization", "Bearer T");
        var res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        assertEquals(503, res.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(
            res.getContentAsString().contains("writes temporarily disabled"));
    }
}
