package org.example.hmby.service;

import org.example.hmby.entity.Subtitle;
import org.example.hmby.repository.ParamRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ws </br>
 * 2025/5/9
 */
@Service
public class SubtitleService {

    private final AssistantService assistantService;
    private final ParamRepository paramRepository;

    public SubtitleService(AssistantService assistantService, ParamRepository paramRepository) {
        this.assistantService = assistantService;
        this.paramRepository = paramRepository;
    }

    public void translatePart(List<Subtitle> subtitles) {
        ChatClient chatClient = assistantService.getChatClient(4L);
        String translatePrompt = paramRepository.findValueByParamCode("translate_prompt");
        ChatClient.ChatClientRequestSpec user = chatClient.prompt(translatePrompt).messages();
        user.call().content();
    }
}
