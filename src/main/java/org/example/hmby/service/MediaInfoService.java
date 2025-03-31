package org.example.hmby.service;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.hmby.config.PropertiesConfig;
import org.example.hmby.emby.Metadata;
import org.example.hmby.emby.MovieItem;
import org.example.hmby.emby.PageWrapper;
import org.example.hmby.emby.request.EmbyItemRequest;
import org.example.hmby.entity.MediaInfo;
import org.example.hmby.entity.MediaMark;
import org.example.hmby.enumerate.CacheKey;
import org.example.hmby.enumerate.MediaConvertType;
import org.example.hmby.enumerate.MediaStatus;
import org.example.hmby.enumerate.ParamCode;
import org.example.hmby.exception.BusinessException;
import org.example.hmby.feign.EmbyFeignClient;
import org.example.hmby.ffmpeg.FfmpegExecutorRunnable;
import org.example.hmby.ffmpeg.FfmpegService;
import org.example.hmby.mapstract.MediaInfoConvertMapper;
import org.example.hmby.repository.MediaInfoRepository;
import org.example.hmby.repository.MediaMarkRepository;
import org.example.hmby.repository.ParamRepository;
import org.example.hmby.sceurity.EmbyUser;
import org.example.hmby.sceurity.SecurityUtils;
import org.example.hmby.vo.MediaInfoDTO;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class MediaInfoService {

    private final MediaInfoRepository mediaInfoRepository;
    private final MediaInfoConvertMapper mediainfoConvertMapper;
    private final MediaMarkRepository mediaMarkRepository;
    private final PropertiesConfig propertiesConfig;
    private final FfmpegService ffmpegService;
    private final EmbyFeignClient embyClient;
    private final ConcurrentHashMap<Object, Object> localCache;
    private final ThreadPoolExecutor singleThreadExecutor;
    private final ParamRepository paramRepository;

    @Transactional(rollbackOn = Exception.class)
    public void save(MediaInfoDTO mediainfoDTO) {
        if (mediainfoDTO.getMetaTitle() != null) {
            mediainfoDTO.setMetaTitle(mediainfoDTO.getMetaTitle().toUpperCase());
        }
        MediaInfo mediaInfo = mediainfoConvertMapper.toMediaInfo(mediainfoDTO);
        MediaInfo exist = mediaInfoRepository.findByInputPath(mediainfoDTO.getInputPath());
        if (exist != null) {
            mediaInfo.setId(exist.getId());
        }
        mediaInfoRepository.save(mediaInfo);
        if (mediainfoDTO.getMarks() != null && !mediainfoDTO.getMarks().isEmpty()) {
            checkTime(mediainfoDTO.getMarks());
            mediaMarkRepository.deleteMediaMarksByMediaId(mediaInfo.getId());
            mediainfoDTO.getMarks().forEach(mediaMark -> {
                mediaMark.setMediaId(mediaInfo.getId());
                mediaMark.setId(null);
                mediaMarkRepository.save(mediaMark);
            });
        }

    }

    /**
     * 获取分页信息
     */
    public Page<MediaInfo> listOfPage(MediaInfoDTO params, Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.Direction.DESC, "lastUpdateDate", "createdDate");
        Page<MediaInfo> mediaInfoPage = mediaInfoRepository.findAll((Specification<MediaInfo>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(params.getFileName())) {
                predicates.add(criteriaBuilder.like(root.get("fileName"), "%" + params.getFileName() + "%"));
            }
            if (params.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), params.getStatus()));
            }
            if (params.getType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("convertType"), params.getType()));
            }
            String userId = SecurityUtils.getUserInfo().map(EmbyUser::getUserId).orElse("Unknown");
            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageRequest);
        
        Set<Long> queueSet = singleThreadExecutor.getQueue()
                .stream()
                .map(runnable -> ((FfmpegExecutorRunnable) runnable).getMediaInfo().getId())
                .collect(Collectors.toSet());
        if (mediaInfoPage.getTotalPages() > 0) {
            for (MediaInfo record : mediaInfoPage.getContent()) {
                if (queueSet.contains(record.getId())) {
                    record.setStatus(MediaStatus.WAITING);
                }
            }
        }
        return mediaInfoPage;
    }

    public void updateById(MediaInfo mediainfo) {
        mediaInfoRepository.save(mediainfo);
    }

    public MediaInfoDTO getMediaAndMarks(Long id) {
        MediaInfo mediaInfo = mediaInfoRepository.findById(id).orElseThrow(() -> new BusinessException("未查询到记录"));
        MediaInfoDTO mediaInfoDTO = mediainfoConvertMapper.toMediaInfoDTO(mediaInfo);
        List<MediaMark> mediaMarks = mediaMarkRepository.findByMediaId(id).stream().sorted(Comparator.comparing(MediaMark::getStart)).collect(Collectors.toList());
        mediaInfoDTO.setMarks(mediaMarks);
        return mediaInfoDTO;
    }

    public void handleMedia(Long id) {
        long startTime = System.currentTimeMillis();
        MediaInfoDTO mediaInfoAndMarks = this.getMediaAndMarks(id);
        MediaInfo mediaInfo = mediainfoConvertMapper.toMediaInfo(mediaInfoAndMarks);
        LocalDateTime now = LocalDateTime.now();
        Path folderPath = Paths.get(propertiesConfig.getOutputMediaPath(), now.format(DateTimeFormatter.ofPattern("yyyyMM")));
        if (!Files.exists(folderPath)) {
            try {
                Files.createDirectories(folderPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        String outputPath = Paths.get(folderPath.toString(), mediaInfo.getFileName() + ".mp4").toString();
        if (!StringUtils.isEmpty(mediaInfo.getMetaTitle())) {
            outputPath = Paths.get(folderPath.toString(), mediaInfo.getMetaTitle() + ".mp4").toString();
        }
        // 二次处理已经处理过的文件，要重命名
        if (mediaInfo.getInputPath().contains(propertiesConfig.getOutputMediaPath())) {
            outputPath = Paths.get(folderPath.toString(), mediaInfo.getFileName() + "_1.mp4").toString();
        }
        mediaInfo.setOutputPath(outputPath.replace(File.separator, "/"));
        mediaInfo.setStatus(MediaStatus.PROCESSING);
        mediaInfoRepository.save(mediaInfo);
        String result = null;
        try {
            switch (mediaInfo.getType()){
                case CUT:
                    if (mediaInfoAndMarks == null || mediaInfoAndMarks.getMarks().isEmpty()) {
                        throw new RuntimeException("mediaInfoAndMarks is empty");
                    }
                    mediaInfoAndMarks.setOutputPath(mediaInfo.getOutputPath());
                    ffmpegService.cutAndConcat(mediaInfoAndMarks);
                    mediaInfo.setStatus(MediaStatus.SUCCESS);
                    break;
                case MOVE:
                    ffmpegService.moveAndTitle(mediaInfo);
                    mediaInfo.setRemark(null);
                    mediaInfo.setStatus(MediaStatus.DONE);
                    break;
                case ENCODE:
                    result = ffmpegService.encoding(mediaInfo);
                    mediaInfo.setRemark(null);
                    mediaInfo.setStatus(MediaStatus.SUCCESS);
                    break;
            }
            long size = Files.size(Paths.get(ffmpegService.handlerVolumeBind(mediaInfo.getOutputPath())));
            mediaInfo.setProcessedSize(size);
        } catch (Exception e) {
            log.error("处理失败", e);
            result = e.toString();
        }
        if (StringUtils.isNotBlank(result)) {
            log.error("处理失败 {}", result);
            mediaInfo.setStatus(MediaStatus.FAIL);
            mediaInfo.setRemark(result);
        }
        mediaInfo.setTimeCost(makeReadable((System.currentTimeMillis() - startTime) / 1000));
        this.updateById(mediaInfo);
    }

    /**
     * 校验时间合法性
     *
     * @param marks
     */
    public void checkTime(List<MediaMark> marks) {
        List<MediaMark> list = marks.stream()
                .sorted(Comparator.comparing(MediaMark::getStart)).collect(Collectors.toList());
        for (int i = 0; i < list.size(); i++) {
            if (i != list.size() - 1) {
                MediaMark cur = list.get(i);
                MediaMark next = list.get(i + 1);
                if (cur.getEnd() <= cur.getStart()) {
                    throw new BusinessException("开始时间不能大于结束时间");
                }
                if (cur.getEnd() > next.getStart()) {
                    throw new BusinessException("时间有重合");
                }
            }
        }
    }

    public String makeReadable(long seconds) {
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
    }

    public void removeAndMarks(Long id) {
        mediaInfoRepository.deleteById(id);
        mediaMarkRepository.deleteMediaMarksByMediaId(id);
    }

    /**
     * 移动媒体 覆盖
     *
     */
    @Transactional(rollbackOn = Exception.class)
    public void moveMedia(Long id, boolean override) throws ChangeSetPersister.NotFoundException, IOException {
        MediaInfo mediaInfo = mediaInfoRepository.findById(id).orElseThrow(ChangeSetPersister.NotFoundException::new);
        String outputPath = ffmpegService.handlerVolumeBind(mediaInfo.getOutputPath());
        String inputPath = ffmpegService.handlerVolumeBind(mediaInfo.getInputPath());

        Path source = Paths.get(inputPath);
        if (!Files.exists(source)) {
            throw new ChangeSetPersister.NotFoundException();
        }
        Path recyclePath = Paths.get(propertiesConfig.getOutputMediaPath(), "recycle");
        if (!Files.exists(recyclePath)) {
            Files.createDirectories(recyclePath);
        }

        Path target = Paths.get(propertiesConfig.getOutputMediaPath(), "recycle", "done_" + mediaInfo.getFileName() + "." + mediaInfo.getSuffix());
        if (override) {
            // 覆盖源文件
            source = Paths.get(outputPath);
            target = Paths.get(inputPath);
        }
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            String nfo = inputPath.substring(0, outputPath.lastIndexOf("."))+".nfo";
            Path nfoPath = Paths.get(nfo);
            if (Files.exists(nfoPath)) {
                Files.delete(nfoPath);
            }
        } catch (IOException e) {
            throw new BusinessException("移动文件失败！");
        }
        if (override) {
            mediaMarkRepository.deleteMediaMarksByMediaId(id);
        }
        mediaInfo.setStatus(MediaStatus.DONE);
        mediaInfoRepository.save(mediaInfo);
    }

    public Page<MovieItem> listOutputMedia(Page<MovieItem> page) {
        String outputID = paramRepository.findValueByParamCode(ParamCode.EMBY_OUTPUT_ID.name());
        if (StringUtils.isEmpty(outputID)) {
            throw new BusinessException("未配置输出目录:" + ParamCode.EMBY_OUTPUT_ID);
        }
        EmbyItemRequest embyItemRequest = new EmbyItemRequest();
        embyItemRequest.setLimit((long) page.getSize());
        long startIndex = (long) page.getSize() * page.getNumber() - page.getSize();
        embyItemRequest.setStartIndex(startIndex);
        embyItemRequest.setParentId(outputID);
        PageWrapper<MovieItem> pageWrapper = embyClient.getItems(embyItemRequest);
        List<MovieItem> items = pageWrapper.getItems();
        for (MovieItem item : items) {
            item.setCover(String.format(propertiesConfig.getEmbyServer() + "/emby/Items/%s/Images/Primary?maxWidth=200&quality=100", item.getId()));
            item.setStreamUrl(String.format(propertiesConfig.getEmbyServer() + "/Videos/%d/stream.%s?static=true&api_key=%s", item.getId(), item.getContainer(), localCache.get(CacheKey.EMBY_TOKEN)));
            item.setDetailPage(String.format(propertiesConfig.getEmbyServer() + "/web/index.html#!/item?id=%s&serverId=%s", item.getId(), item.getServerId()));
            item.setMediaInfo(mediaInfoRepository.findByInputPath(item.getPath()));
        }
        return page;
    }

    public void createByMetadata(Metadata itemMetadata) {
        try {
            MediaInfo mediaInfo = new MediaInfo();
            mediaInfo.setFileName(itemMetadata.getSortName());
            mediaInfo.setFileSize(itemMetadata.getSize());
            mediaInfo.setInputPath(itemMetadata.getPath());
            mediaInfo.setStatus(MediaStatus.DELETE_EMBY);
            mediaInfo.setType(MediaConvertType.DELETE);
            mediaInfo.setSuffix(itemMetadata.getContainer());
            mediaInfoRepository.save(mediaInfo);
        } catch (Exception e) {
            log.error("createByMetadata异常 {}", e.toString());
        }
    }

    public MediaInfo getByInputPath(String inputPath) {
        return mediaInfoRepository.findByInputPath(inputPath);
    }

    public MediaInfo getByOutputPath(String outputPath) {
        return mediaInfoRepository.findByOutputPath(outputPath);
    }

    public MediaInfoDTO getMediaDetail(String inputPath) {
        MediaInfo mediaInfo = mediaInfoRepository.findByInputPathOrOutputPath(inputPath, inputPath);
        if (mediaInfo == null) {
            return null;
        }
        return this.getMediaAndMarks(mediaInfo.getId());
    }
}
