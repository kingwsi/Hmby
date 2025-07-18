package org.example.hmby.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.dto.Completion;
import org.example.hmby.entity.ChatAssistant;
import org.example.hmby.entity.ChatConversation;
import org.example.hmby.entity.ChatMessage;
import org.example.hmby.enumerate.AssistantCode;
import org.example.hmby.enumerate.SseEventType;
import org.example.hmby.repository.ChatConversationRepository;
import org.example.hmby.repository.ChatMessageRepository;
import org.example.hmby.utils.TextUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.jdbc.JdbcChatMemoryRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author ws </br>
 * 2025/5/12
 */
@Slf4j
@Service
@AllArgsConstructor
public class ChatService {
    private final AssistantService assistantService;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final JdbcChatMemoryRepository chatMemoryRepository;

    public SseEmitter completions(Completion completion){
        String content = completion.getContent();
        String conversationId = completion.getConversationId();
        SseEmitter sseEmitter = new SseEmitter(180000L); // 设置3分钟超时
        sseEmitter.onCompletion(() -> log.info("SSE completed"));
        sseEmitter.onTimeout(() -> log.warn("SSE timeout"));
        sseEmitter.onError(ex -> log.error("SSE error", ex));
        new Thread(() -> {
            try {
                ChatAssistant assistant = assistantService.getAssistantByCode(completion.getAssistantCode());
                ChatClient chatClient = assistantService.buildChatClient(assistant);
                ChatMemory chatMemory = MessageWindowChatMemory.builder()
                        .chatMemoryRepository(chatMemoryRepository)
                        .maxMessages(50)
                        .build();
                chatClient.prompt(assistant.getPrompt())
                        .advisors(a -> a.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)
                                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build()))
                        .advisors(new SimpleLoggerAdvisor())
                        .user(content)
                        .stream()
                        .content()
                        .doOnComplete(()->{
                            sseEmitter.complete();
                            this.activateConversation(conversationId);
                        })
                        .doOnError(sseEmitter::completeWithError)
                        .subscribe(t -> {
                            try {
                                sseEmitter.send(SseEmitter.event().name(SseEventType.MESSAGE.name()).data(t).build());
                            } catch (Exception e) {
                                log.error("Error sending SSE content", e);
                            }
                        });
            } catch (Exception e) {
                log.error("SSE error", e);
                sseEmitter.completeWithError(e);
            }
        }).start();
        return sseEmitter;
    }

    @Async
    public void activateConversation(String conversationId) {
        ChatConversation chatConversation = chatConversationRepository.findById(conversationId).orElseThrow(() -> new RuntimeException("Conversation not found"));
        if (!chatConversation.getActivated()) {
            List<ChatMessage> messageList = chatMessageRepository.findByConversationId(conversationId, Sort.by(Sort.Direction.ASC, "timestamp"));
            if (messageList != null && messageList.size() >= 2) {
                messageList = messageList.subList(0, 2);
            }
            chatConversation.setActivated(true);
            chatConversation.setTitle(this.getConversationTitle(messageList));
            chatConversationRepository.save(chatConversation);
        }
    }

    public ChatConversation getConversation(String assistantCode) {
        ChatAssistant chatAssistant = assistantService.getAssistantByCode(assistantCode);
        ChatConversation chatConversation = Optional.ofNullable(chatConversationRepository.findByAssistantIdAndActivated(chatAssistant.getId(), false))
                .flatMap(l -> l.stream().findFirst())
                .orElse(null);
        if (chatConversation == null) {
            chatConversation = new ChatConversation();
            chatConversation.setAssistantId(chatAssistant.getId());
            chatConversation.setActivated(false);
            chatConversation.setTitle("New Conversation");
            chatConversationRepository.save(chatConversation);
        }
        return chatConversation;
    }

    public Page<ChatConversation> listConversation(Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.Direction.DESC, "updatedAt", "createdAt");
        
        return chatConversationRepository.findByActivated(true, pageRequest);
    }

    public List<ChatMessage> listConversationMessage(String conversationId) {
        Sort sort = Sort.by(Sort.Direction.ASC, "timestamp");
        ChatMessage query = new ChatMessage();
        query.setConversationId(conversationId);
        return chatMessageRepository.findAll(Example.of(query), sort);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(String conversationId) {
        chatConversationRepository.deleteById(conversationId);
        chatMessageRepository.deleteByConversationId(conversationId);
    }
    
    public String getConversationTitle(List<ChatMessage> messageList) {
        if (messageList != null && !messageList.isEmpty()) {
            String content = messageList.stream().map(m -> "%s: %s".formatted(m.getType(), TextUtil.removeXmlTag(m.getContent(), "think"))).collect(Collectors.joining("\n"));
            String prompt = "/no_think \n 请根据这段对话内容，总结一个简洁、准确的标题，能够概括主题，长度不超过12个字。";
            ChatAssistant chatAssistant = assistantService.getAssistantByCode(AssistantCode.CHAT);
            ChatClient chatClient = assistantService.buildChatClient(chatAssistant);
            String result = chatClient.prompt(prompt).user(content).call().content();
            if (result != null) {
                result = TextUtil.removeXmlTag(result, "think").trim();
            }
            return result;
        }
        return "New Conversation";
    }
}
