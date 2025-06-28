package org.example.hmby.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.example.hmby.emby.EmbyFeignClient;
import org.example.hmby.emby.ItemTag;
import org.example.hmby.emby.MovieItem;
import org.example.hmby.emby.PageWrapper;
import org.example.hmby.emby.request.EmbyItemRequest;
import org.example.hmby.entity.Config;
import org.example.hmby.enumerate.CacheKey;
import org.example.hmby.enumerate.ConfigKey;
import org.example.hmby.repository.ConfigRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author ws </br>
 * 2025/6/22
 */
@Service
public class EmbeddingService {

    private final PgVectorStore vectorStore;
    private final EmbyFeignClient embyFeignClient;
    private final ConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    public EmbeddingService(PgVectorStore vectorStore, EmbyFeignClient embyFeignClient, ConfigRepository configRepository, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.embyFeignClient = embyFeignClient;
        this.configRepository = configRepository;
        this.objectMapper = objectMapper;
    }

    public void embeddingTags() {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        vectorStore.delete(b.eq("index", "tag").build());

        int pageSize = 100;
        PageWrapper<ItemTag> pageWrapper = embyFeignClient.getTags(null, 0, pageSize);
        for (int i = 0; i < pageWrapper.getTotalPages(); i++) {
            pageWrapper = embyFeignClient.getTags(null, i * pageSize, pageSize);
            List<Document> list = pageWrapper.getItems().stream()
                    .map(t -> new Document(t.getName(), Map.of("id", t.getId(), "index", "tag")))
                    .toList();
            vectorStore.add(list);
        }
    }

    public void embeddingMovies() {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        vectorStore.delete(b.eq("index", "movie").build());

        int pageSize = 5;
        EmbyItemRequest embyItemRequest = new EmbyItemRequest();
        embyItemRequest.setLimit(pageSize);
        embyItemRequest.setStartIndex(0);
        embyItemRequest.setParentId("6");

        PageWrapper<MovieItem> pageWrapper = embyFeignClient.getItems(embyItemRequest);
        for (int i = 0; i < pageWrapper.getTotalPages(); i++) {
            embyItemRequest.setStartIndex(i * pageSize);
            pageWrapper = embyFeignClient.getItems(embyItemRequest);
            List<Document> list = Optional.ofNullable(pageWrapper.getItems())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(item -> {
                        String name = Optional.ofNullable(item.getName()).orElse("");
                        String tags = Optional.ofNullable(item.getTagItems())
                                .map(tagItems -> tagItems.stream()
                                        .map(tag -> Optional.ofNullable(tag.getName()).orElse(""))
                                        .collect(Collectors.joining(" ")))
                                .orElse("");
                        Map<String, Object> meta = new HashMap<>();
                        meta.put("Id", item.getId());
                        meta.put("index", "movie");
                        meta.put("ImageTags", Optional.ofNullable(item.getImageTags()).orElse(new MovieItem.ImageTags()));
                        meta.put("Name", name);
                        String text = String.join(" ", tags);
                        return new Document(text, meta);
                    })
                    .toList();
            vectorStore.add(list);
        }
    }
    
    public List<Document> similaritySearch(String type, String content) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(content)
                .topK(10)
                .similarityThreshold(0.6)
                .filterExpression(b.eq("index", type).build()).build());
        return documents;
    }
}
