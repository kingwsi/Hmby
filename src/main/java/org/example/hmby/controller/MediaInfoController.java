package org.example.hmby.controller;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.hmby.Response;
import org.example.hmby.config.PropertiesConfig;
import org.example.hmby.emby.MovieItem;
import org.example.hmby.entity.MediaInfo;
import org.example.hmby.enumerate.CacheKey;
import org.example.hmby.enumerate.MediaStatus;
import org.example.hmby.exception.BusinessException;
import org.example.hmby.ffmpeg.FfmpegExecutorRunnable;
import org.example.hmby.service.MediaInfoService;
import org.example.hmby.vo.MediaInfoDTO;
import org.example.hmby.vo.MediaQueueVO;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/media-info")
public class MediaInfoController {
    private final MediaInfoService mediaInfoService;
    
    private final ThreadPoolExecutor singleThreadExecutor;

    private final ConcurrentHashMap<Object, Object> localCache;
    private final PropertiesConfig propertiesConfig;

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
        propertiesConfig.check();
        MediaInfoDTO mediaAndMarks = mediaInfoService.getMediaAndMarks(id);
        if (mediaAndMarks == null || mediaAndMarks.getMarks() == null) {
            return Response.fail("没有找到对应的信息！");
        }
        if (mediaAndMarks.getStatus() == MediaStatus.PROCESSING) {
            return Response.fail("处理中");
        } else {
            MediaQueueVO mediaQueueVO = new MediaQueueVO();
            mediaQueueVO.setId(id);
            mediaQueueVO.setFileName(mediaQueueVO.getFileName());
            mediaQueueVO.setConvertType(mediaQueueVO.getConvertType());
            mediaQueueVO.setFileSize(mediaQueueVO.getFileSize());
            mediaQueueVO.setStatus(mediaQueueVO.getStatus());
            FfmpegExecutorRunnable ffmpegExecutorRunnable = new FfmpegExecutorRunnable(mediaInfoService, mediaQueueVO);
            if (!singleThreadExecutor.getQueue().contains(ffmpegExecutorRunnable)) {
                singleThreadExecutor.execute(ffmpegExecutorRunnable);
            } else {
                return Response.fail("已在队列等待！");
            }
        }
        return Response.success();
    }

    @PostMapping("/move-media/{id}")
    public Response<String> moveMedia(@PathVariable("id") Long id, boolean override) throws ChangeSetPersister.NotFoundException, IOException {
        mediaInfoService.moveMedia(id, override);
        return Response.success();
    }

    @GetMapping("/output/page")
    public Response<?> getOutputPage(Page<MovieItem> page) {
        return Response.success(mediaInfoService.listOutputMedia(page));
    }

    @GetMapping("/progress/{id}")
    public SseEmitter getEncodingProgress(@PathVariable("id") String id) {
        SseEmitter emitter = new SseEmitter(10000L);

        AtomicBoolean isCompleted = new AtomicBoolean(false);

        emitter.onTimeout(() -> {
            isCompleted.set(true);
            log.info("连接超时！{}", id);
            emitter.complete();
        });

        emitter.onCompletion(() -> {
            isCompleted.set(true);
            log.info("连接已完成！{}", id);
        });

        // 启动后台线程，每秒发送一次进度
        new Thread(() -> {
            try {
                while (!isCompleted.get()) {
                    Object encodingProgress = localCache.get(String.format("%s-%s", CacheKey.CACHE_ENCODING_PROGRESS.name(), id));
                    if (encodingProgress != null) {
                        emitter.send(SseEmitter.event()
                                .name("progress")
                                .data(encodingProgress));
                    }
                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (IOException e) {
                // 处理发送异常（例如客户端断开连接）
                isCompleted.set(true);
                log.error("SseEmitter encountered IOException for ID: {}", id);
                emitter.completeWithError(e);
            } catch (InterruptedException e) {
                // 处理线程中断
                Thread.currentThread().interrupt();
                log.error("SseEmitter thread interrupted for ID: {}", id);
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }
}
