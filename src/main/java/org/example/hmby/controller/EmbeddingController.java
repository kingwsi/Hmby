package org.example.hmby.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.Response;
import org.example.hmby.service.EmbeddingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ws </br>
 * 2025/6/22
 */
@Slf4j
@RestController
@RequestMapping("/api/embedding")
@AllArgsConstructor
public class EmbeddingController {
    private final EmbeddingService embeddingService;
    
    @GetMapping("/{type}")
    public Response<?> embedding(@PathVariable String type) {
        if (type.equalsIgnoreCase("tag")) {
            embeddingService.embeddingTags();
        } else if (type.equalsIgnoreCase("movie")) {
            embeddingService.embeddingMovies();
        }
        return Response.success();
    }
}
