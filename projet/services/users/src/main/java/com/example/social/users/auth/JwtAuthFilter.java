package com.example.social.users.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtre servlet léger : extrait "Authorization: Bearer <token>", valide le JWT
 * LOCALEMENT via {@link JwtService}, et dépose l'identité en attribut de requête.
 * Token absent ou invalide -> 401, la chaîne s'arrête.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATTR = "authUser";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(request, response);
            return;
        }
        try {
            AuthenticatedUser user = jwtService.parse(header.substring(7));
            request.setAttribute(ATTR, user);
        } catch (JwtService.InvalidTokenException e) {
            unauthorized(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        // GET /verify a un contrat de corps particulier ({valid:false}).
        String body = request.getRequestURI().endsWith("/verify")
                ? "{\"valid\":false}"
                : "{\"error\":\"unauthorized\"}";
        response.getWriter().write(body);
    }
}
