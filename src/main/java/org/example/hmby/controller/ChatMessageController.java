package org.example.hmby.controller;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.example.hmby.Response;
import org.example.hmby.entity.ChatAssistant;
import org.example.hmby.entity.ChatMessage;
import org.example.hmby.entity.ChatTopic;
import org.example.hmby.repository.ChatAssistantRepository;
import org.example.hmby.repository.ChatMessageRepository;
import org.example.hmby.repository.ChatTopicRepository;
import org.example.hmby.sceurity.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/chat-message")
public class ChatMessageController {


    private final ChatTopicRepository chatTopicRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatAssistantRepository chatAssistantRepository;

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

    @GetMapping("/topics")
    public Response<Page<ChatTopic>> pageMessageTopic(Pageable pageable) {
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.Direction.DESC, "lastUpdateDate");
        Page<ChatTopic> page = chatTopicRepository.findAll(pageable);
        return Response.success(page);
    }

    @Transactional
    @DeleteMapping("/topics/{id}")
    public Response<Page<ChatTopic>> deleteMessageTopic(@PathVariable Long id) {
        chatTopicRepository.deleteById(id);
        chatMessageRepository.deleteByTopicId(id);
        return Response.success();
    }

    @GetMapping("/messages/{id}")
    public Response<List<ChatMessage>> pageMessageTopic(@PathVariable Long id) {
        List<ChatMessage> messageList = chatMessageRepository.findMessagesByTopic(id);
        return Response.success(messageList);
    }
}
