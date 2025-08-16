package org.example.hmby.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.entity.ChatConversation;
import org.example.hmby.entity.ChatMessage;
import org.example.hmby.repository.ChatConversationRepository;
import org.example.hmby.repository.ChatMessageRepository;
import org.example.hmby.repository.ChatPromptRepository;
import org.example.hmby.utils.TextUtil;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatPromptRepository chatPromptRepository;

    public SseEmitter completions(String conversationId, Long promptId, String content) {
        // todo
        return null;
    }
    
    @Async
    public void activateConversation(SecurityContext securityContext, Long promptId, String conversationId) {
        SecurityContextHolder.setContext(securityContext);
        ChatConversation chatConversation = chatConversationRepository.findByConversationId(conversationId);
        if (chatConversation == null) {
            chatConversation = new ChatConversation();
            chatConversation.setPromptId(promptId);
            chatConversation.setConversationId(conversationId);
        }
        List<ChatMessage> messageList = chatMessageRepository.findByConversationId(conversationId, Sort.by(Sort.Direction.ASC, "timestamp"));
        if (messageList != null && messageList.size() >= 2) {
            messageList = messageList.subList(0, 2);
        }
        chatConversationRepository.save(chatConversation);
    }
    
    public ChatConversation getConversation(Long promptId) {
        return Optional.ofNullable(chatConversationRepository.findByPromptId(promptId))
                .flatMap(l -> l.stream().findFirst())
                .orElse(null);
    }

    public Page<ChatConversation> listConversation(Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.Direction.DESC, "updatedAt", "createdAt");
        return chatConversationRepository.findAll(pageRequest);
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
}
