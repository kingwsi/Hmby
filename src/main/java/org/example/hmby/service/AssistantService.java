package org.example.hmby.service;

import org.example.hmby.entity.ChatAssistant;
import org.example.hmby.enumerate.AssistantType;
import org.example.hmby.repository.ChatAssistantRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Service
public class AssistantService {

    private final ChatAssistantRepository chatAssistantRepository;

    public AssistantService(ChatAssistantRepository chatAssistantRepository) {
        this.chatAssistantRepository = chatAssistantRepository;
    }
    
    public ChatAssistant getAssistantByType(AssistantType type) {
        return chatAssistantRepository.findByType(type).orElseThrow(() -> new IllegalArgumentException("No chat assistant found for type: " + type));
    }

    public ChatClient buildChatClient(ChatAssistant chatAssistant) {
        String apiKey = chatAssistant.getApiKey();
        String model = chatAssistant.getModelName();
        String baseUrl = chatAssistant.getBaseUrl();
        OpenAiApi openAiApi = OpenAiApi.builder()
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

        OpenAiChatModel build = OpenAiChatModel.builder()
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .openAiApi(openAiApi).build();

        return ChatClient.builder(build)
                .defaultSystem(chatAssistant.getPrompt())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    public void save(ChatAssistant chatAssistant) {
        chatAssistantRepository.save(chatAssistant);
    }
}
