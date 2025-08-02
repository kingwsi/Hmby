package org.example.hmby.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.Response;
import org.example.hmby.dto.Completion;
import org.example.hmby.entity.ChatConversation;
import org.example.hmby.entity.ChatMessage;
import org.example.hmby.entity.ChatPrompt;
import org.example.hmby.entity.Subtitle;
import org.example.hmby.repository.ChatPromptRepository;
import org.example.hmby.service.ChatService;
import org.example.hmby.service.SubtitleService;
import org.example.hmby.utils.TextUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@AllArgsConstructor
@Slf4j
public class ChatController {
    private final ChatService chatService;
    private final SubtitleService subtitleService;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final ChatPromptRepository chatPromptRepository;

    @PostMapping("/prompt/list")
    public Response<List<ChatPrompt>> listPrompts() {
        List<ChatPrompt> all = chatPromptRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        return Response.success(all);
    }

    @PostMapping("/prompt")
    public Response<?> savePrompt(@RequestBody ChatPrompt chatPrompt) {
        chatPromptRepository.save(chatPrompt);
        return Response.success();
    }

    @GetMapping("/conversation")
    public Response<ChatConversation> getConversation(Long promptId) {
        return Response.success(chatService.getConversation(promptId));
    }

    @GetMapping("/conversation/{conversationId}/messages")
    public Response<List<ChatMessage>> listConversationMessage(@PathVariable String conversationId) {
        return Response.success(chatService.listConversationMessage(conversationId));
    }


    @GetMapping("/conversation-list")
    public Response<Page<ChatConversation>> listConversation(Pageable pageable) {
        return Response.success(chatService.listConversation(pageable));
    }

    @GetMapping("/{conversationId}/completions")
    public SseEmitter completions(@PathVariable String conversationId, Long promptId, String text) {
        return chatService.completions(conversationId, promptId, text);
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
