package com.example.social.posts.web;

import com.example.social.posts.domain.Post;
import com.example.social.posts.service.PostService;
import com.example.social.posts.service.exception.ForbiddenException;
import com.example.social.posts.service.exception.PostNotFoundException;
import com.example.social.posts.users.VerifiedUser;
import com.example.social.posts.web.dto.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
@TestPropertySource(properties = "posts.max-content-length=10")
class PostControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean PostService posts;

    @Test
    void feed_returnsItems() throws Exception {
        when(posts.feed(0, 20)).thenReturn(new PageResponse<>(
                List.of(new PostSummary(1, "t", 1, Instant.parse("2026-08-28T10:00:00Z"))),
                0, 20, 1));
        mvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].author_id").value(1))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void detail_unknown_returns404() throws Exception {
        when(posts.detail(anyLong())).thenThrow(new PostNotFoundException(9));
        mvc.perform(get("/posts/9")).andExpect(status().isNotFound());
    }

    @Test
    void createPost_returns201WithId() throws Exception {
        var saved = new Post("t", "c", 3L);
        setId(saved, 7L);
        when(posts.create("t", "c", 3L)).thenReturn(saved);

        mvc.perform(post("/posts")
                        .requestAttr("verifiedUser", new VerifiedUser(3L, "carol"))
                        .contentType("application/json")
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void createPost_contentTooLong_returns400() throws Exception {
        mvc.perform(post("/posts")
                        .requestAttr("verifiedUser", new VerifiedUser(3L, "carol"))
                        .contentType("application/json")
                        .content("{\"title\":\"t\",\"content\":\"12345678901\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("content must be at most 10 characters"));
    }

    @Test
    void deletePost_notAuthor_returns403() throws Exception {
        Mockito.doThrow(new ForbiddenException()).when(posts).delete(1L, 3L);
        mvc.perform(delete("/posts/1")
                        .requestAttr("verifiedUser", new VerifiedUser(3L, "carol")))
                .andExpect(status().isForbidden());
    }

    private static void setId(Object e, Long id) {
        try { var f = e.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(e, id); }
        catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
    }
}
