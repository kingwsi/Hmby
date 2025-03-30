package org.example.hmby.openai;

import lombok.Data;

@Data
public class ChatRequest {
    private String content;
    private Long assistantId;
    private Long topicId;
}
