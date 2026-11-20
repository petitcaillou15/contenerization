package com.example.social.users.web;

import com.example.social.users.auth.AuthenticatedUser;
import com.example.social.users.auth.JwtService;
import com.example.social.users.domain.User;
import com.example.social.users.service.UserService;
import com.example.social.users.service.exception.DuplicateUserException;
import com.example.social.users.service.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean UserService users;
    @MockitoBean JwtService jwt;

    @Test
    void register_returns201() throws Exception {
        when(users.register("alice", "alice@example.com", "s3cret")).thenReturn(null);
        mvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pseudo\":\"alice\",\"email\":\"alice@example.com\",\"password\":\"s3cret\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void register_duplicate_returns409() throws Exception {
        doThrow(new DuplicateUserException()).when(users).register(any(), any(), any());
        mvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pseudo\":\"alice\",\"email\":\"alice@example.com\",\"password\":\"s3cret\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void register_blankPseudo_returns400() throws Exception {
        mvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pseudo\":\"\",\"email\":\"alice@example.com\",\"password\":\"s3cret\"}"))
                .andExpect(status().isBadRequest());
    }

    /** User avec un id non nul (l'id reste null tant que JPA ne l'a pas persisté). */
    private static User userWithId(long id, String pseudo) {
        User u = new User(pseudo, pseudo + "@example.com", "H");
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return u;
    }

    @Test
    void login_returnsToken() throws Exception {
        when(users.authenticate("alice@example.com", "s3cret")).thenReturn(userWithId(1L, "alice"));
        when(jwt.issue(anyLong(), any())).thenReturn("TOKEN123");

        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"s3cret\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"token\":\"TOKEN123\"}"));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        doThrow(new InvalidCredentialsException()).when(users).authenticate(any(), any());
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@example.com\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verify_returnsClaims_whenFilterAuthenticated() throws Exception {
        mvc.perform(get("/verify").requestAttr("authUser", new AuthenticatedUser(9L, "carol")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"valid\":true,\"user_id\":9,\"pseudo\":\"carol\"}"));
    }
}
