package com.example.social.users.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JwtAuthFilterTest {

    private final JwtService jwt =
            new JwtService("filter-test-secret-filter-test-secret-0123456789", 3600);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwt);

    @Test
    void validToken_setsAttribute_andContinues() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/verify");
        req.addHeader("Authorization", "Bearer " + jwt.issue(5L, "carol"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        AuthenticatedUser attr = (AuthenticatedUser) req.getAttribute("authUser");
        assertNotNull(attr);
        assertEquals(5L, attr.id());
        verify(chain).doFilter(req, res);
    }

    @Test
    void missingHeader_returns401_andStopsChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/verify");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        verify(chain, never()).doFilter((HttpServletRequest) req, (HttpServletResponse) res);
    }

    @Test
    void badToken_returns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("PATCH", "/users/me");
        req.addHeader("Authorization", "Bearer garbage");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        assertEquals(401, res.getStatus());
    }
}
