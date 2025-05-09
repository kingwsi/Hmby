package org.example.hmby.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.Response;
import org.example.hmby.emby.EmbyFeignClient;
import org.example.hmby.emby.Metadata;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * @author ws </br>
 * 2025/5/9
 */
@RestController
@RequestMapping("/api/subtitle")
@Slf4j
public class SubtitleController {

    private final EmbyFeignClient embyFeignClient;
    private final ObjectMapper objectMapper;

    public SubtitleController(EmbyFeignClient embyFeignClient, ObjectMapper objectMapper) {
        this.embyFeignClient = embyFeignClient;
        this.objectMapper = objectMapper;
    }

    @PutMapping("/translate/{embyId}/{language}")
    public Response<?> translate(@PathVariable Long embyId, @PathVariable String language) throws IOException {
        Metadata itemMetadata = embyFeignClient.getItemMetadata(embyId);
        HashMap<String, Object> stringObjectHashMap = Optional.ofNullable(itemMetadata)
                .map(Metadata::getMediaSources)
                .flatMap(o -> o.stream().findFirst()).orElseThrow(() -> new RuntimeException("Not Found Media Source " + embyId));
        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsBytes(stringObjectHashMap));
        String subtitleFile = null;
        for (JsonNode streamNode : jsonNode.get("MediaStreams")) {
            if ("Subtitle".equals(streamNode.get("Type").asText())){
                if (language.equals(streamNode.get("DisplayLanguage").asText())) {
                    subtitleFile = streamNode.get("FileName").asText();
                    break;
                }
            }
        }
        if (subtitleFile == null) {
            throw new IllegalArgumentException(language + " is not found");
        }

        return Response.success();
    }
}
