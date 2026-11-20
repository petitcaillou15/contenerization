package com.example.social.posts.web;

import com.example.social.posts.auth.AuthFilter;
import com.example.social.posts.service.PostService;
import com.example.social.posts.users.VerifiedUser;
import com.example.social.posts.web.dto.CreatePostRequest;
import com.example.social.posts.web.dto.PageResponse;
import com.example.social.posts.web.dto.PostDetail;
import com.example.social.posts.web.dto.PostSummary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class PostController {

    private final PostService posts;

    public PostController(PostService posts) {
        this.posts = posts;
    }

    /** Fil d'actualité : lecture directe Postgres, tri date DESC, paginé. Public. */
    @GetMapping("/posts")
    public PageResponse<PostSummary> feed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return posts.feed(page, limit);
    }

    /** Détail d'un post + ses commentaires. Public. */
    @GetMapping("/posts/{id}")
    public PostDetail detail(@PathVariable long id) {
        return posts.detail(id);
    }

    // --- écritures (Task 7) : identité fournie par AuthFilter via l'attribut "verifiedUser" ---

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> create(HttpServletRequest request,
                                    @Valid @RequestBody CreatePostRequest body) {
        VerifiedUser me = (VerifiedUser) request.getAttribute(AuthFilter.ATTR);
        var saved = posts.create(body.title(), body.content(), me.id());
        return Map.of("id", saved.getId());
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(HttpServletRequest request, @PathVariable long id) {
        VerifiedUser me = (VerifiedUser) request.getAttribute(AuthFilter.ATTR);
        posts.delete(id, me.id());
    }
}
