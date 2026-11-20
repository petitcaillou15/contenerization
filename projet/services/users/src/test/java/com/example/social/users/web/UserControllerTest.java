package com.example.social.users.web;

import com.example.social.users.auth.AuthenticatedUser;
import com.example.social.users.domain.User;
import com.example.social.users.service.UserService;
import com.example.social.users.service.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean UserService users;

    /** User avec un id non nul (l'id reste null tant que JPA ne l'a pas persisté). */
    private static User userWith(long id, String pseudo, String bio) {
        User u = new User(pseudo, pseudo + "@example.com", "H");
        u.setBio(bio);
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
    void getProfile_returnsPublicFields() throws Exception {
        when(users.getById(1L)).thenReturn(userWith(1L, "alice", null));
        mvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pseudo").value("alice"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void getProfile_unknown_returns404() throws Exception {
        when(users.getById(anyLong())).thenThrow(new UserNotFoundException(99));
        mvc.perform(get("/users/99")).andExpect(status().isNotFound());
    }

    @Test
    void patchMe_updatesBio() throws Exception {
        when(users.updateBio(5L, "nouvelle bio")).thenReturn(userWith(5L, "alice", "nouvelle bio"));

        mvc.perform(patch("/users/me")
                        .requestAttr("authUser", new AuthenticatedUser(5L, "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"nouvelle bio\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("nouvelle bio"));
    }
}
