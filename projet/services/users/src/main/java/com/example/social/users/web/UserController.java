package com.example.social.users.web;

import com.example.social.users.auth.AuthenticatedUser;
import com.example.social.users.auth.JwtAuthFilter;
import com.example.social.users.service.UserService;
import com.example.social.users.web.dto.PublicProfile;
import com.example.social.users.web.dto.UpdateBioRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    /** Profil public : pas d'email, pas de hash. */
    @GetMapping("/users/{id}")
    public PublicProfile profile(@PathVariable long id) {
        return PublicProfile.of(users.getById(id));
    }

    /** Modification de sa propre bio uniquement. Auth via JwtAuthFilter. */
    @PatchMapping("/users/me")
    public PublicProfile updateMyBio(HttpServletRequest request,
                                     @Valid @RequestBody UpdateBioRequest body) {
        AuthenticatedUser me = (AuthenticatedUser) request.getAttribute(JwtAuthFilter.ATTR);
        return PublicProfile.of(users.updateBio(me.id(), body.bio()));
    }
}
