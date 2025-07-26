package org.example.hmby.controller;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.hmby.Response;
import org.example.hmby.entity.Config;
import org.example.hmby.entity.MediaInfo;
import org.example.hmby.enumerate.CacheKey;
import org.example.hmby.enumerate.ConfigKey;
import org.example.hmby.enumerate.MediaStatus;
import org.example.hmby.exception.BusinessException;
import org.example.hmby.exception.ConfigNotFoundException;
import org.example.hmby.repository.ConfigRepository;
import org.example.hmby.service.MediaInfoService;
import org.example.hmby.vo.MediaInfoDTO;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/media-info")
public class MediaInfoController {
    private final MediaInfoService mediaInfoService;

    private final ConcurrentHashMap<Object, Object> localCache;
    private final ConfigRepository configRepository;

    @PostMapping
    public Response<String> save(@RequestBody MediaInfoDTO mediaInfoDTO) {
        if (StringUtils.isEmpty(mediaInfoDTO.getInputPath()) || mediaInfoDTO.getEmbyId() == null) {
            throw new BusinessException("参数错误");
        }
        mediaInfoService.save(mediaInfoDTO);
        return Response.success();
    }

    @DeleteMapping("/{id}")
    public Response<String> deleteById(@PathVariable Long id) {
        mediaInfoService.removeAndMarks(id);
        return Response.success();
    }

    @GetMapping("/page")
    public Response<Page<MediaInfo>> listOfPage(MediaInfoDTO mediaInfoDTO, Pageable pageable) {
        return Response.success(mediaInfoService.listOfPage(mediaInfoDTO, pageable));
    }

    @PostMapping("/execute/{id}")
    public Response<?> execute(@PathVariable Long id) {
        return Response.success();
    }

    @PostMapping("/source-file/{id}")
    public Response<String> handlerSourceMedia(@PathVariable("id") Long id, @RequestParam("operate") String operate) throws ChangeSetPersister.NotFoundException, IOException {
        mediaInfoService.handlerSourceMedia(id, operate);
        return Response.success();
    }

    @PutMapping("/progress")
    public Response<Object> progress(@RequestBody Map<String, Object> progress) {
        return Response.success(localCache.put(CacheKey.CACHE_ENCODING_PROGRESS.name(), progress));
    }

    @GetMapping("/progress")
    public Response<Object> progress() {
//        ProgressInfo progressInfo = new ProgressInfo("Hello World.mp4", Progress.Status.CONTINUE, 12, String.format("%.0f%%", 0.89 * 100), 2, "", "00:12:21");
        return Response.success(localCache.get(CacheKey.CACHE_ENCODING_PROGRESS));
    }

    @GetMapping("/status")
    public Response<?> status() throws IOException {
        return Response.success(MediaStatus.values());
    }
}
