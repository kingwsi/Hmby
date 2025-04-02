package org.example.hmby.service;

import org.example.hmby.entity.ChatAssistant;
import org.example.hmby.repository.ChatAssistantRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {

    private final ChatAssistantRepository chatAssistantRepository;

    public AssistantService(ChatAssistantRepository chatAssistantRepository) {
        this.chatAssistantRepository = chatAssistantRepository;
    }

    public ChatClient getChatClient(Long assistantId) {
        ChatAssistant chatAssistant = chatAssistantRepository.findById(assistantId).orElseThrow();
        String apiKey = chatAssistant.getApiKey();
        String model = chatAssistant.getModelName();
        String baseUrl = chatAssistant.getBaseUrl();
        OpenAiApi openAiApi = OpenAiApi.builder().baseUrl(baseUrl)
                .apiKey(apiKey)
                .baseUrl(chatAssistant.getBaseUrl())
                .completionsPath("/chat/completions")
                .embeddingsPath("/embeddings")
                .build();

        OpenAiChatModel build = OpenAiChatModel.builder()
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .openAiApi(openAiApi).build();

        return ChatClient.builder(build)
                .defaultSystem("You are useful assistant")
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    public void save(ChatAssistant chatAssistant) {
        chatAssistantRepository.save(chatAssistant);
    }
}
