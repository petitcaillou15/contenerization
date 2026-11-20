package com.example.social.posts.web;

import com.example.social.posts.auth.AuthFilter;
import com.example.social.posts.service.CommentService;
import com.example.social.posts.users.VerifiedUser;
import com.example.social.posts.web.dto.CreateCommentRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class CommentController {

    private final CommentService comments;

    public CommentController(CommentService comments) {
        this.comments = comments;
    }

    @PostMapping("/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> add(HttpServletRequest request,
                                 @PathVariable("id") long postId,
                                 @Valid @RequestBody CreateCommentRequest body) {
        VerifiedUser me = (VerifiedUser) request.getAttribute(AuthFilter.ATTR);
        var saved = comments.addComment(postId, me, body.content());
        return Map.of("id", saved.getId());
    }
}
