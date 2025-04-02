package org.example.hmby.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.hmby.Response;
import org.example.hmby.entity.ChatAssistant;
import org.example.hmby.service.AssistantService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {
    private final AssistantService assistantService;

    public ChatController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }
    
    @PostMapping
    public Response<String> saveAssistant(@RequestBody ChatAssistant chatAssistant) {
        assistantService.save(chatAssistant);
        return Response.success("OK");
    }

    @GetMapping("/completions")
    public String generation(String userInput) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("http://localhost:1234/v1")
                .apiKey("1234")
                .completionsPath("/chat/completions")
                .embeddingsPath("/embeddings")
                .build();

        OpenAiChatModel build = OpenAiChatModel.builder()
                .defaultOptions(OpenAiChatOptions.builder().model("qwen2.5-7b-instruct-1m").build())
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).build())
                .openAiApi(openAiApi).build();

        ChatClient chatClient = ChatClient.builder(build)
                .defaultSystem("You are useful assistant")
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        ChatClient.ChatClientRequestSpec user = chatClient.prompt().user(userInput);
        return user.call().content();
    }
}
