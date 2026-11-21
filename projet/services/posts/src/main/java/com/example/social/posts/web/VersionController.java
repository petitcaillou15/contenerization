package com.example.social.posts.web;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Version applicative du service : permet de voir quelle version répond (ex. pendant un rolling update). */
@RestController
public class VersionController {

    private final String version;

    public VersionController(@Value("${app.version}") String version) {
        this.version = version;
    }

    @GetMapping("/version")
    public Map<String, String> version() {
        return Map.of("service", "posts", "version", version);
    }
}
