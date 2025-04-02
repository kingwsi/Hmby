package org.example.hmby.controller;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.info.Codec;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.shared.CodecType;
import org.apache.commons.lang3.StringUtils;
import org.example.hmby.Response;
import org.example.hmby.config.PropertiesConfig;
import org.example.hmby.emby.MovieItem;
import org.example.hmby.entity.MediaInfo;
import org.example.hmby.enumerate.CacheKey;
import org.example.hmby.enumerate.MediaCodec;
import org.example.hmby.enumerate.MediaStatus;
import org.example.hmby.exception.BusinessException;
import org.example.hmby.ffmpeg.FFmpegManager;
import org.example.hmby.ffmpeg.FfmpegExecutorRunnable;
import org.example.hmby.ffmpeg.ProgressInfo;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/media-info")
public class MediaInfoController {
    private final MediaInfoService mediaInfoService;

    private final ThreadPoolExecutor singleThreadExecutor;

    private final ConcurrentHashMap<Object, Object> localCache;
    private final PropertiesConfig propertiesConfig;
    private final FFmpegManager ffmpegManager;

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

    @PutMapping("/progress")
    public Response<Object> progress(@RequestBody Map<String, Object> progress) {
        return Response.success(localCache.put(CacheKey.CACHE_ENCODING_PROGRESS.name(), progress));
    }

    @GetMapping("/output/page")
    public Response<?> getOutputPage(Page<MovieItem> page) {
        return Response.success(mediaInfoService.listOutputMedia(page));
    }

    @GetMapping("/progress")
    public Response<Object> progress() {
//        ProgressInfo progressInfo = new ProgressInfo("Hello World.mp4", Progress.Status.CONTINUE, 12, String.format("%.0f%%", 0.89 * 100), 2, "", "00:12:21");
        return Response.success(localCache.get(CacheKey.CACHE_ENCODING_PROGRESS.name()));
    }
    
    @GetMapping("/codecs")
    public Response<List<Codec>> codecs() throws IOException {
        List<String> mediaCodecs = MediaCodec.getCodecs();
        
        List<Codec> codecs = ffmpegManager.getFfmpeg().codecs().stream()
                .filter(codec -> codec.getType() == CodecType.VIDEO && mediaCodecs.contains(codec.getName()))
                .toList();
        return Response.success(codecs);
    }

    @GetMapping("/status")
    public Response<?> status() throws IOException {
        return Response.success(MediaStatus.values());
    }
}
