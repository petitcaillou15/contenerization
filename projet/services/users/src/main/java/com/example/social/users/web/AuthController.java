package com.example.social.users.web;

import com.example.social.users.auth.AuthenticatedUser;
import com.example.social.users.auth.JwtAuthFilter;
import com.example.social.users.auth.JwtService;
import com.example.social.users.service.UserService;
import com.example.social.users.web.dto.LoginRequest;
import com.example.social.users.web.dto.LoginResponse;
import com.example.social.users.web.dto.RegisterRequest;
import com.example.social.users.web.dto.VerifyResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserService users;
    private final JwtService jwt;

    public AuthController(UserService users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    /** Inscription publique. */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest req) {
        users.register(req.pseudo(), req.email(), req.password());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Authentification : renvoie un JWT HS256 signé. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        var user = users.authenticate(req.email(), req.password());
        return new LoginResponse(jwt.issue(user.getId(), user.getPseudo()));
    }

    /** Endpoint interne. Le JwtAuthFilter a déjà validé le token localement. */
    @GetMapping("/verify")
    public VerifyResponse verify(HttpServletRequest request) {
        var authUser = (AuthenticatedUser) request.getAttribute(JwtAuthFilter.ATTR);
        return new VerifyResponse(true, authUser.id(), authUser.pseudo());
    }
}
