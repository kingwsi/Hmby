package org.example.hmby.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.emby.EmbyFeignClient;
import org.example.hmby.emby.Metadata;
import org.example.hmby.entity.ChatAssistant;
import org.example.hmby.entity.MediaInfo;
import org.example.hmby.entity.Subtitle;
import org.example.hmby.enumerate.AssistantCode;
import org.example.hmby.enumerate.MediaConvertType;
import org.example.hmby.enumerate.MediaStatus;
import org.example.hmby.enumerate.SubtitleStatus;
import org.example.hmby.ffmpeg.FfmpegService;
import org.example.hmby.repository.MediaInfoRepository;
import org.example.hmby.repository.SubtitleRepository;
import org.example.hmby.utils.FixedSizeQueue;
import org.example.hmby.utils.SRTParser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author ws </br>
 * 2025/5/9
 */
@Slf4j
@Service
public class SubtitleService {

    private final AssistantService assistantService;
    private final SubtitleRepository subtitleRepository;
    private final MediaInfoRepository mediaInfoRepository;
    private final FfmpegService ffmpegService;
    private final EmbyFeignClient embyFeignClient;
    private final ObjectMapper objectMapper;

    public SubtitleService(AssistantService assistantService, SubtitleRepository subtitleRepository, MediaInfoRepository mediaInfoRepository, FfmpegService ffmpegService, EmbyFeignClient embyFeignClient, ObjectMapper objectMapper) {
        this.assistantService = assistantService;
        this.subtitleRepository = subtitleRepository;
        this.mediaInfoRepository = mediaInfoRepository;
        this.ffmpegService = ffmpegService;
        this.embyFeignClient = embyFeignClient;
        this.objectMapper = objectMapper;
    }

    public List<Subtitle> listSubtitles(Long mediaId) {
        return subtitleRepository.findAllByMediaId(mediaId, Sort.by(Sort.Direction.ASC, "sequence"));
    }

    public String translateHandler(Long mediaId) {
        int chunkSize = 20;
        List<Subtitle> fullSubtitles = subtitleRepository.findAllByMediaId(mediaId, Sort.by(Sort.Direction.ASC, "sequence"));
        FixedSizeQueue<Subtitle> fixedSizeQueue = new FixedSizeQueue<>(chunkSize);
        ChatAssistant assistant = assistantService.getAssistantByCode(AssistantCode.TRANSLATE_SUBTITLE.name());
        int beginIndex = 0;
        int size = fullSubtitles.size();
        while (beginIndex < size) {
            // 获取起始下标
            fullSubtitles = subtitleRepository.findAllByMediaId(mediaId, Sort.by(Sort.Direction.ASC, "sequence"));
            for (int i = 0; i < fullSubtitles.size(); i++) {
                Subtitle subtitle = fullSubtitles.get(i);
                if (subtitle.getStatus() == SubtitleStatus.PENDING || !StringUtils.hasText(subtitle.getTranslatedText())) {
                    beginIndex = i;
                    break;
                }
                fixedSizeQueue.add(subtitle);
            }
            int endIndex = Math.min(beginIndex + chunkSize, fullSubtitles.size() - 1);
            List<Subtitle> inputSubtitles = fullSubtitles.subList(beginIndex, endIndex);

            // 上下文
            Map<String, String> collect = fixedSizeQueue.stream().collect(Collectors.toMap(Subtitle::getText, Subtitle::getTranslatedText, (o1, o2) -> o1));
            String translatedText = null;
            try {
                translatedText = objectMapper.writeValueAsString(collect);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            if (inputSubtitles.isEmpty()) {
                break;
            }
            List<String> inputs = inputSubtitles.stream().map(Subtitle::getText).toList();

            boolean finish = false;
            int retryCount = 0;
            while (!finish && retryCount < 5) {
                try {
                    int errNum = 0;
                    Map<String, String> resultMap = this.translateChunk(assistant, inputs, translatedText);
                    for (Subtitle s : inputSubtitles) {
                        String text = resultMap.get(s.getText());
                        s.setTranslatedText(text);
                        s.setStatus(SubtitleStatus.FINISHED);
                        if (!StringUtils.hasText(text)) {
                            log.warn("翻译失败,尝试补偿: {}", s.getText());
                            s.setStatus(SubtitleStatus.ERROR);
                            text = translateBySubtitleId(s.getId(), AssistantCode.TRANSLATE_SINGLE);
                            if (!StringUtils.hasText(text)) {
                                log.warn("翻译失败: {}", s.getText());
                                errNum++;
                            } else {
                                log.warn("翻译补偿：{} -> {}", s.getText(), s);
                                s.setTranslatedText(text);
                                s.setStatus(SubtitleStatus.COMPENSATE);
                            }
                        }
                    }
//                    List<String> texts = this.translateChunk(assistant, inputs, translatedText);
//                    if (texts.size() != inputSubtitles.size()) {
//                        log.error("重试{}: 返回结果长度不一致", retryCount);
//                        retryCount++;
//                        continue;
//                    }for (int i = 0; i < inputSubtitles.size(); i++) {
//                        Subtitle subtitle = inputSubtitles.get(i);
//                        String result = texts.get(i);
//                        subtitle.setStatus(SubtitleStatus.FINISHED);
//                        subtitle.setTranslatedText(result);
//                        
//                        if (!StringUtils.hasText(result)) {
//                            log.warn("翻译失败,尝试补偿: {}", subtitle.getText());
//                            subtitle.setStatus(SubtitleStatus.ERROR);
//                            String s = translateBySubtitleId(subtitle.getId());
//                            if (!StringUtils.hasText(s)) {
//                                log.warn("翻译失败: {}", subtitle.getText());
//                                errNum++;
//                            } else {
//                                log.warn("翻译补偿：{} -> {}", subtitle.getText(), s);
//                                subtitle.setTranslatedText(s);
//                                subtitle.setStatus(SubtitleStatus.COMPENSATE);
//                            }
//                        }
//                    }
                    if (errNum >= chunkSize * 0.2) {
                        log.error("重试{}: 失败数量超过设定阈值！", errNum);
                        retryCount++;
                    } else {
                        subtitleRepository.saveAll(inputSubtitles);
                        beginIndex += chunkSize;
                        finish = true;
                    }
                } catch (JsonProcessingException e) {
                    log.error("重试{}: 返回结果不合法{}", retryCount, e.getMessage());
                    retryCount++;
                }
            }
            if (!finish) {
                throw new RuntimeException("达到最大重试次数，停止任务！");
            }
        }
        this.outputToFile(mediaId);
        return null;
    }


    /**
     * 单条翻译
     */
    public String translateBySubtitleId(Long subtitleId, AssistantCode type) {
        ChatAssistant assistant = assistantService.getAssistantByCode(type.name());
        Subtitle subtitle = subtitleRepository.findById(subtitleId).orElseThrow(() -> new RuntimeException("No subtitle found"));
        List<Subtitle> translatedSubtitles = subtitleRepository.findChunks(subtitle.getMediaId(), subtitle.getSequence() - 5, subtitle.getSequence() + 5);
        String translatedText = translatedSubtitles.stream().map(Subtitle::getText).collect(Collectors.joining("\n"));

        String prompt = assistant.getPrompt();
        ChatClient chatClient = assistantService.buildChatClient(assistant);
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt().user(subtitle.getText());
        if (StringUtils.hasText(translatedText)) {
            prompt = prompt.replace("{TRANSLATED}", translatedText);
        }
        chatClientRequestSpec.system(prompt);
        return chatClientRequestSpec.call().content();
    }

    public Map<String, String> translateChunk(ChatAssistant assistant, List<String> inputs, String preContext) throws JsonProcessingException {
        String text = inputs.stream().map(t -> String.format("\"%s\"", t))
                .collect(Collectors.joining(","));
        text = "[%s]".formatted(text);
        log.info("translateChunk: \n{}", text);
        String prompt = assistant.getPrompt();
        ChatClient chatClient = assistantService.buildChatClient(assistant);
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt().user(text);
        if (StringUtils.hasText(preContext)) {
            prompt = prompt.replace("{TRANSLATED}", preContext);
        }
        chatClientRequestSpec.system(prompt);
        String content = chatClientRequestSpec.call().content();
        if (content != null) {
            if (content.contains("</think>")) {
                int thinkIndex = content.indexOf("</think>") + 8;
                String thinkContent = content.substring(0, thinkIndex);
                log.info("<think>:\n{}", thinkContent);
                content = content.substring(thinkIndex).trim();
            }
        }
        log.info("translateChunk: \n{}", content);
        return objectMapper.readValue(content, new TypeReference<>() {
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public MediaInfo initSubtitle(Long embyId, String language) throws IOException {
        Metadata metadata = embyFeignClient.getItemMetadata(embyId);
        HashMap<String, Object> stringObjectHashMap = Optional.ofNullable(metadata)
                .map(Metadata::getMediaSources)
                .flatMap(o -> o.stream().findFirst()).orElseThrow(() -> new RuntimeException("Not Found Media Source " + embyId));
        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsBytes(stringObjectHashMap));
        String subtitleFilePath = null;
        for (JsonNode streamNode : jsonNode.get("MediaStreams")) {
            if ("Subtitle".equals(streamNode.get("Type").asText())) {
                if (language.equals(streamNode.get("DisplayLanguage").asText())) {
                    subtitleFilePath = streamNode.get("Path").asText();
                    break;
                }
            }
        }
        if (subtitleFilePath == null) {
            throw new IllegalArgumentException(language + " is not found");
        }
        MediaInfo mediaInfo = mediaInfoRepository.findByInputPath(subtitleFilePath);
        if (mediaInfo != null && mediaInfo.getType() == MediaConvertType.TRANSLATE) {
            return mediaInfo;
        }

        Path path = Paths.get(ffmpegService.handlerVolumeBind(subtitleFilePath));
        if (!path.toFile().exists()) {
            throw new RuntimeException("subtitle file does not exist: " + path);
        }

        List<Subtitle> subtitles = SRTParser.parseSRT(path);

        mediaInfo = new MediaInfo();
        mediaInfo.setFileName(metadata.getSortName());
        mediaInfo.setFileSize(metadata.getSize());
        mediaInfo.setInputPath(subtitleFilePath);
        mediaInfo.setStatus(MediaStatus.PENDING);
        mediaInfo.setType(MediaConvertType.TRANSLATE);
        mediaInfo.setSuffix(metadata.getContainer());
        mediaInfo.setSuffix(language);
        mediaInfo.setEmbyId(metadata.getId());
        mediaInfoRepository.save(mediaInfo);

        for (Subtitle subtitle : subtitles) {
            subtitle.setMediaId(mediaInfo.getId());
            subtitle.setStatus(SubtitleStatus.PENDING);
        }

        subtitleRepository.saveAll(subtitles);
        return mediaInfo;
    }

    public void outputToFile(Long mediaId) {
        MediaInfo mediaInfo = mediaInfoRepository.findById(mediaId).orElseThrow(() -> new RuntimeException("Not Found Media Id " + mediaId));
        List<Subtitle> subtitles = subtitleRepository.findAllByMediaId(mediaId, Sort.by(Sort.Direction.ASC, "sequence"));
        if (subtitles.isEmpty()) {
            throw new RuntimeException("subtitles not empty");
        }
        String inputPath = mediaInfo.getInputPath();
        String inputFileName = inputPath.substring(inputPath.lastIndexOf("/") + 1);

        String outputFileName = inputFileName.substring(0, inputFileName.indexOf(".")) + ".zh.srt";
        String outputPath = inputPath.substring(0, inputPath.lastIndexOf("/"));
        String fullPath = outputPath + "/" + outputFileName;
        String s = ffmpegService.handlerVolumeBind(fullPath);
        SRTParser.writeSRT(s, subtitles);
        mediaInfo.setOutputPath(fullPath);
        mediaInfoRepository.save(mediaInfo);
    }
    
    public String commonTranslate(String input, AssistantCode type) {
        ChatAssistant assistant = assistantService.getAssistantByCode(type.name());
        ChatClient chatClient = assistantService.buildChatClient(assistant);
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt(assistant.getPrompt())
                .user(input);
        return chatClientRequestSpec.call().content();
    }
}
