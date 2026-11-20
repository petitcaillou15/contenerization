package com.example.social.posts.auth;

import com.example.social.posts.users.UsersClient;
import com.example.social.posts.users.UsersUnavailableException;
import com.example.social.posts.users.VerifiedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ne protège QUE les écritures. Les GET (fil, détail) restent publics.
 * Écriture + token OK -> identité en attribut "verifiedUser".
 * Token invalide -> 401. users injoignable -> 503 (écritures désactivées).
 */
public class AuthFilter extends OncePerRequestFilter {

    public static final String ATTR = "verifiedUser";
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final UsersClient users;

    public AuthFilter(UsersClient users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        if (!WRITE_METHODS.contains(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            write(response, 401, "{\"error\":\"unauthorized\"}");
            return;
        }
        try {
            VerifiedUser user = users.verify(header.substring(7));
            request.setAttribute(ATTR, user);
        } catch (UsersClient.InvalidTokenException e) {
            write(response, 401, "{\"error\":\"unauthorized\"}");
            return;
        } catch (UsersUnavailableException e) {
            write(response, 503, "{\"error\":\"writes temporarily disabled\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private void write(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(body);
    }
}
