package org.example.hmby.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.hmby.Response;
import org.example.hmby.dto.Completion;
import org.example.hmby.entity.ChatAssistant;
import org.example.hmby.entity.ChatConversation;
import org.example.hmby.entity.ChatMessage;
import org.example.hmby.repository.ChatAssistantRepository;
import org.example.hmby.sceurity.SecurityUtils;
import org.example.hmby.service.AssistantService;
import org.example.hmby.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {
    private final AssistantService assistantService;
    private final ChatService chatService;
    private final ChatAssistantRepository chatAssistantRepository;

    public ChatController(AssistantService assistantService, ChatService chatService, ChatAssistantRepository chatAssistantRepository) {
        this.assistantService = assistantService;
        this.chatService = chatService;
        this.chatAssistantRepository = chatAssistantRepository;
    }

    @GetMapping("/assistants")
    public Response<List<ChatAssistant>> listChatAssistants() {
        List<ChatAssistant> list = chatAssistantRepository.findAllByUserIdOrderByLastUpdateDateDesc(SecurityUtils.getUserId());
        return Response.success(list);
    }

    @PostMapping("/assistants")
    public Response<List<ChatAssistant>> saveChatAssistant(@RequestBody ChatAssistant chatAssistant) {
        chatAssistantRepository.save(chatAssistant);
        return Response.success();
    }

    @GetMapping("/conversation")
    public Response<ChatConversation> getConversation(String assistantCode) {
        return Response.success(chatService.getConversation(assistantCode));
    }

    @GetMapping("/conversation/{conversationId}/messages")
    public Response<List<ChatMessage>> listConversationMessage(@PathVariable String conversationId) {
        return Response.success(chatService.listConversationMessage(conversationId));
    }


    @GetMapping("/conversation-list")
    public Response<Page<ChatConversation>> listConversation(Pageable pageable) {
        return Response.success(chatService.listConversation(pageable));
    }

    @PostMapping("/completions")
    public SseEmitter completions(@RequestBody Completion completion) {
        return chatService.completions(completion);
    }

    @DeleteMapping("/conversation/{conversationId}")
    public Response<String> deleteConversation(@PathVariable String conversationId) {
        chatService.deleteConversation(conversationId);
        return Response.success("OK");
    }
}
