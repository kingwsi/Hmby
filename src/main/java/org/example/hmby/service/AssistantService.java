package org.example.hmby.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.entity.ChatAssistant;
import org.example.hmby.entity.Config;
import org.example.hmby.enumerate.ConfigKey;
import org.example.hmby.repository.ChatAssistantRepository;
import org.example.hmby.repository.ConfigRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class AssistantService {

    private final ChatAssistantRepository chatAssistantRepository;
    private final ConfigRepository configRepository;

    public ChatAssistant getAssistantByCode(String code) {
        return chatAssistantRepository.findByCode(code).orElseThrow(() -> new IllegalArgumentException("No chat assistant found for type: " + code));
    }
    
    public ChatClient buildChatClient(ChatAssistant chatAssistant) {
        OpenAiApi openAiApi = this.buildOpenAiApi(chatAssistant.getBaseUrl(), chatAssistant.getApiKey());

        OpenAiChatModel build = OpenAiChatModel.builder()
                .defaultOptions(OpenAiChatOptions.builder().model(chatAssistant.getModelName()).build())
                .openAiApi(openAiApi).build();
        
        return ChatClient.builder(build)
                .build();
    }

    public OpenAiApi buildOpenAiApi(String baseUrl, String apiKey) {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .completionsPath("/chat/completions")
                .embeddingsPath("/embeddings")
                .apiKey(apiKey != null ? new SimpleApiKey(apiKey) : new NoopApiKey())
                .webClientBuilder(WebClient.builder()
                        // Force HTTP/1.1 for streaming
                        .clientConnector(new JdkClientHttpConnector(HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1)
                                .connectTimeout(Duration.ofSeconds(100))
                                .build())))
                .restClientBuilder(RestClient.builder()
                        // Force HTTP/1.1 for non-streaming
                        .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1)
                                .connectTimeout(Duration.ofSeconds(100))
                                .build())))
                .build();
    }

    public void save(ChatAssistant chatAssistant) {
        chatAssistantRepository.save(chatAssistant);
    }

    public EmbeddingModel buildEmbeddingModel() {
        JsonNode jsonNode = Optional.ofNullable(configRepository.findOneByKey(ConfigKey.embedding))
                .map(Config::getVal)
                .map(val -> {
                    try {
                        return new ObjectMapper().readValue(val, JsonNode.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to parse embedding json", e);
                    }
                }).orElseThrow();
        String baseUrl = "http://localhost:8080/v1";
        String apiKey = "";
        String modelName = "text-embedding-bge-m3";
        try {
            baseUrl = jsonNode.get("baseUrl").textValue();
            apiKey = jsonNode.get("apiKey").textValue();
            modelName = jsonNode.get("modelName").textValue();
        } catch (Exception e) {
            log.error("Failed to parse embedding json", e);
        }

        OpenAiApi openAiApi = this.buildOpenAiApi(baseUrl, apiKey);

        return new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(modelName)
                        .build(),
                RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }
}
