package org.example.hmby.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.Response;
import org.example.hmby.entity.MediaInfo;
import org.example.hmby.entity.Subtitle;
import org.example.hmby.enumerate.SubtitleStatus;
import org.example.hmby.enumerate.AssistantCode;
import org.example.hmby.repository.SubtitleRepository;
import org.example.hmby.service.SubtitleService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * @author ws </br>
 * 2025/5/9
 */
@RestController
@RequestMapping("/api/subtitle")
@Slf4j
public class SubtitleController {

    private final SubtitleService subtitleService;
    private final SubtitleRepository subtitleRepository;

    public SubtitleController(SubtitleService subtitleService, SubtitleRepository subtitleRepository) {
        this.subtitleService = subtitleService;
        this.subtitleRepository = subtitleRepository;
    }

    @GetMapping("/{embyId}")
    public Response<?> initSubtitle(@PathVariable Long embyId) throws IOException {
        MediaInfo mediaInfo = subtitleService.initSubtitle(embyId, "Japanese");
        List<Subtitle> subtitles = subtitleService.listSubtitles(mediaInfo.getId());
        return Response.success(subtitles);
    }

    @PostMapping("/{id}")
    public Response<?> save(@RequestBody Subtitle subtitle) {
        if (subtitle.getId() == null) {
            throw new IllegalArgumentException("ID cannot be null！");
        }
        Subtitle existSubtitle = subtitleRepository.findById(subtitle.getId()).orElseThrow(() -> new IllegalArgumentException("Subtitle not found"));
        existSubtitle.setTranslatedText(subtitle.getTranslatedText());
        existSubtitle.setStatus(StringUtils.hasText(subtitle.getTranslatedText()) ? SubtitleStatus.FINISHED : SubtitleStatus.PENDING);
        subtitleRepository.save(existSubtitle);
        return Response.success(existSubtitle);
    }

    @PutMapping("/translate/{mediaId}")
    public Response<?> translate(@PathVariable Long mediaId) throws JsonProcessingException {
        subtitleService.translateHandler(mediaId);
        return Response.success();
    }

    @PutMapping("/output/file/{mediaId}")
    public Response<?> outputToFile(@PathVariable Long mediaId) {
        subtitleService.outputToFile(mediaId);
        return Response.success();
    }
}
