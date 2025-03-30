package org.example.hmby.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.example.hmby.entity.ChatAssistant;
import org.example.hmby.entity.ChatMessage;
import org.example.hmby.entity.ChatTopic;
import org.example.hmby.exception.BusinessException;
import org.example.hmby.openai.ChatCompletionRequest;
import org.example.hmby.openai.ChatRequest;
import org.example.hmby.openai.Message;
import org.example.hmby.openai.MessageRole;
import org.example.hmby.repository.ChatAssistantRepository;
import org.example.hmby.repository.ChatMessageRepository;
import org.example.hmby.repository.ChatTopicRepository;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // 连接超时
            .readTimeout(30, TimeUnit.SECONDS)  // 读取超时
            .build();

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final ChatTopicRepository chatTopicRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    private final ChatAssistantRepository chatAssistantRepository;
    
    @PostMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCompletions(@org.springframework.web.bind.annotation.RequestBody ChatRequest chatRequest) {
        if (StringUtils.isBlank(chatRequest.getContent()) || chatRequest.getAssistantId() == null) {
            throw new IllegalArgumentException("Invalid request");
        }
        ChatAssistant chatAssistant = chatAssistantRepository.findById(chatRequest.getAssistantId()).orElseThrow(() -> new BusinessException("assistantId not found"));
        Long topicId;
        List<Message> messages = new ArrayList<>();
        if (chatRequest.getTopicId() == null) {
            ChatTopic chatTopic = new ChatTopic();
            chatTopic.setTitle(chatRequest.getContent().length() > 10 ? chatRequest.getContent().substring(0, 10) : chatRequest.getContent());
            chatTopic.setModelName(chatAssistant.getModelName());
            chatTopic.setAssistantId(chatRequest.getAssistantId());
            chatTopicRepository.save(chatTopic);
            topicId = chatTopic.getId();
            
            messages.add(new Message(MessageRole.system, chatAssistant.getPrompt()));
            messages.add(new Message(MessageRole.user, chatRequest.getContent()));
            List<ChatMessage> chatMessages = messages.stream().map(message -> {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setTopicId(chatTopic.getId());
                chatMessage.setRole(message.getRole());
                chatMessage.setContent(message.getContent());
                return chatMessage;
            }).collect(Collectors.toList());
            chatMessageRepository.saveAll(chatMessages);
        } else {
            topicId = chatRequest.getTopicId();
            List<ChatMessage> messageList = chatMessageRepository.findAllByTopicId(chatRequest.getTopicId());
            ArrayList<Message> collect = messageList.stream().map(m -> new Message(m.getRole(), m.getContent())).collect(Collectors.toCollection(ArrayList::new));
            messages.addAll(collect);
            messages.add(new Message(MessageRole.user, chatRequest.getContent()));
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setTopicId(topicId);
            chatMessage.setRole(MessageRole.user);
            chatMessage.setContent(chatRequest.getContent());
            chatMessageRepository.save(chatMessage);
        }
        SecurityContext context = SecurityContextHolder.getContext();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        executor.execute(() -> {
            try {
                ChatCompletionRequest request = new ChatCompletionRequest();
                request.setMessages(messages);
                request.setModel(chatAssistant.getModelName());
                request.setTemperature(chatAssistant.getTemperature());
                String requestBody = objectMapper.writeValueAsString(request);

                // 构建 OkHttp 请求
                Request okRequest = new Request.Builder()
                        .url("http://localhost:1234/v1/chat/completions")
                        .post(RequestBody.create(requestBody, okhttp3.MediaType.get("application/json")))
                        .header("Authorization", "Bearer YOUR_OPENAI_API_KEY")
                        .build();

                SecurityContextHolder.setContext(context);
                // 执行请求并处理流式响应
                try (Response response = client.newCall(okRequest).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    try (ResponseBody body = response.body();
                         BufferedReader reader = new BufferedReader(
                                 new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8))) {
                        StringBuilder assistantMessage = new StringBuilder();
                        String json = "";
                        String line;
                        while ((line = reader.readLine()) != null) {
                            log.debug("Raw response from OpenAI: {}", line);
                            if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                                json = line.substring(6);
                                assistantMessage.append(extractContent(json));
                                emitter.send(SseEmitter.event().data(json.getBytes(StandardCharsets.UTF_8)));
                            } else if (line.equals("data: [DONE]")) {
                                if (StringUtils.isNotBlank(assistantMessage.toString())) {
                                    ChatMessage chatMessage = new ChatMessage();
                                    chatMessage.setContent(assistantMessage.toString());
                                    chatMessage.setTopicId(topicId);
                                    chatMessage.setRole(MessageRole.assistant);
                                    chatMessageRepository.save(chatMessage);
                                }
                                if (chatRequest.getTopicId() == null) {
                                    emitter.send(SseEmitter.event().data(("{\"topicId\": " + topicId + "}").getBytes(StandardCharsets.UTF_8)));
                                }
                                break;
                            }
                        }
                    }
                }

                log.info("Stream completed");
                emitter.complete(); // 正常结束流

            } catch (Exception e) {
                log.error("Error during streaming: {}", e.getMessage(), e);
                try {
                    emitter.send("Error: " + e.getMessage());
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    log.error("Failed to send error: {}", ex.getMessage());
                }
            }
        });
        return emitter;
    }

    private String extractContent(String json) {
        try {
            var mapper = new ObjectMapper();
            var node = mapper.readTree(json);
            var deltaNode = node.path("choices").path(0).path("delta");
            var contentNode = deltaNode.path("content");
            return contentNode.isMissingNode() || contentNode.asText().isEmpty() ? "" : contentNode.asText();
        } catch (Exception e) {
            log.error("Failed to parse response: {}, error: {}", json, e.getMessage());
            return "";
        }
    }
}
