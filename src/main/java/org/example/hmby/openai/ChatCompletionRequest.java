package org.example.hmby.openai;

import lombok.Data;

import java.util.List;

@Data
public class ChatCompletionRequest {
    private String model;
    private List<Message> messages;
    private boolean stream = true;
    private double temperature = 0.7;
}
