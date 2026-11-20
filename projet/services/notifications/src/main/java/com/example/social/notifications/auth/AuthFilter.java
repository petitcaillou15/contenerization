package com.example.social.notifications.auth;

import com.example.social.notifications.users.UsersClient;
import com.example.social.notifications.users.UsersUnavailableException;
import com.example.social.notifications.users.VerifiedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Toutes les routes /notifications* exigent un token valide.
 * Token invalide -> 401. users injoignable -> 503 (pas de mode dégradé ici, spec §2.4).
 */
public class AuthFilter extends OncePerRequestFilter {

    public static final String ATTR = "verifiedUser";

    private final UsersClient users;

    public AuthFilter(UsersClient users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

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
            write(response, 503, "{\"error\":\"users service unavailable\"}");
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
