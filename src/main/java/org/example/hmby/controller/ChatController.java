package org.example.hmby.controller;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.example.hmby.Response;
import org.example.hmby.entity.ChatAssistant;
import org.example.hmby.enumerate.AssistantType;
import org.example.hmby.service.AssistantService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public String completions(String userInput) {
        ChatAssistant assistant = assistantService.getAssistantByType(AssistantType.CHAT);
        ChatClient chatClient = assistantService.buildChatClient(assistant);

        ChatClient.ChatClientRequestSpec user = chatClient.prompt().user(userInput);
        return user.call().content();
    }
}
