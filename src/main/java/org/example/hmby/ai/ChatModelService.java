package org.example.hmby.ai;

import org.example.hmby.entity.Config;
import org.example.hmby.exception.ConfigNotFoundException;
import org.example.hmby.repository.ConfigRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * @author ws
 * @since 2025/8/16
 */
@Service
public class ChatModelService {
    private final ConfigRepository configRepository;
    private final JdbcTemplate jdbcTemplate;

    public ChatModelService(ConfigRepository configRepository, JdbcTemplate jdbcTemplate) {
        this.configRepository = configRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ChatModel createChatModel() {
        String model_name = Optional.ofNullable(configRepository.findConfigBy())
                .map(Config::getModelName)
                .orElseThrow(() -> new ConfigNotFoundException("model_name"));


        return OpenAiChatModel.builder()
                .defaultOptions(OpenAiChatOptions.builder().model(model_name).build())
                .openAiApi(this.getOpenaiApi()).build();
    }
    
    public OpenAiApi getOpenaiApi(){
        String openai_base_url = Optional.ofNullable(configRepository.findConfigBy())
                .map(Config::getOpenaiBaseUrl)
                .orElseThrow(() -> new ConfigNotFoundException("openai_base_url"));

        return OpenAiApi.builder()
                .baseUrl(openai_base_url)
                .completionsPath("/chat/completions")
                .embeddingsPath("/embeddings")
                .apiKey(new NoopApiKey())
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
    
    public EmbeddingModel createEmbeddingModel() {
        String embedding_model_name = Optional.ofNullable(configRepository.findConfigBy())
                .map(Config::getEmbeddingModelName)
                .orElseThrow(() -> new ConfigNotFoundException("embedding_model_name"));
        return new OpenAiEmbeddingModel(
                this.getOpenaiApi(),
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(embedding_model_name)
                        .build(),
                RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }
    
    public VectorStore createVectorStore() {
        EmbeddingModel embeddingModel = createEmbeddingModel();
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1024)                    // Optional: defaults to model dimensions or 1536
                .distanceType(COSINE_DISTANCE)       // Optional: defaults to COSINE_DISTANCE
                .indexType(HNSW)                     // Optional: defaults to HNSW
                .initializeSchema(true)              // Optional: defaults to false
                .schemaName("public")                // Optional: defaults to "public"
                .vectorTableName("vector_store")     // Optional: defaults to "vector_store"
                .maxDocumentBatchSize(10000)         // Optional: defaults to 10000
                .build();
    }
}
