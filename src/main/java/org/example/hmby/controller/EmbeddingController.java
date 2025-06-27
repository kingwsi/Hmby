package org.example.hmby.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.Response;
import org.example.hmby.service.EmbeddingService;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/similarity-search/{type}")
    public Response<?> similaritySearch(@PathVariable String type, String keyword) {
        List<Document> documents = embeddingService.similaritySearch(type, keyword);
        return Response.success(documents);
    }
}
