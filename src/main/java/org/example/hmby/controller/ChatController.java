package org.example.hmby.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.Response;
import org.example.hmby.dto.Completion;
import org.example.hmby.entity.ChatAssistant;
import org.example.hmby.entity.ChatConversation;
import org.example.hmby.entity.ChatMessage;
import org.example.hmby.entity.Subtitle;
import org.example.hmby.repository.ChatAssistantRepository;
import org.example.hmby.sceurity.SecurityUtils;
import org.example.hmby.service.ChatService;
import org.example.hmby.service.SubtitleService;
import org.example.hmby.utils.TextUtil;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@AllArgsConstructor
@Slf4j
public class ChatController {
    private final ChatService chatService;
    private final ChatAssistantRepository chatAssistantRepository;
    private final SubtitleService subtitleService;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

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

    @GetMapping("/translate/{subtitleId}/completions")
    public SseEmitter translateCompletions(@PathVariable Long subtitleId, boolean reasoning) {
        String subtitleContext = subtitleService.listSubtitleContext(subtitleId, 2);
        Subtitle subtitle = subtitleService.findById(subtitleId);
        return subtitleService.commonTranslate(subtitle.getText(), subtitleContext, reasoning);
    }

    @DeleteMapping("/conversation/{conversationId}")
    public Response<String> deleteConversation(@PathVariable String conversationId) {
        chatService.deleteConversation(conversationId);
        return Response.success("OK");
    }

    @GetMapping("/embedding")
    public Response<Integer> embedding(String text) {
        List<String> list = List.of(TextUtil.removeChinesePunctuation(text));
        EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(list);
        List<Embedding> embedding = embeddingResponse.getResults();
        return Response.success(embedding.size());
    }

    @GetMapping("/similarity-search")
    public Response<List<Document>> similaritySearch(String index, String text) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        SearchRequest.builder().filterExpression(b.eq("index", index).build()).query(text);
        List<Document> results = this.vectorStore.similaritySearch(SearchRequest.builder().query(text).topK(5).build());
        return Response.success(results);
    }
}
