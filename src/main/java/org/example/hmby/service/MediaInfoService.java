package org.example.hmby.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.hmby.config.PropertiesConfig;
import org.example.hmby.emby.Metadata;
import org.example.hmby.entity.MediaInfo;
import org.example.hmby.entity.MediaMark;
import org.example.hmby.enumerate.MediaConvertType;
import org.example.hmby.enumerate.MediaStatus;
import org.example.hmby.exception.BusinessException;
import org.example.hmby.mapstract.MediaInfoConvertMapper;
import org.example.hmby.repository.MediaInfoRepository;
import org.example.hmby.repository.MediaMarkRepository;
import org.example.hmby.sceurity.EmbyUser;
import org.example.hmby.sceurity.SecurityUtils;
import org.example.hmby.vo.MediaInfoDTO;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Limit;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class MediaInfoService {

    private final MediaInfoRepository mediaInfoRepository;
    private final MediaInfoConvertMapper mediainfoConvertMapper;
    private final MediaMarkRepository mediaMarkRepository;
    private final PropertiesConfig propertiesConfig;

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
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.Direction.DESC, "updatedAt", "createdAt");
        return mediaInfoRepository.findAll((Specification<MediaInfo>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(params.getFileName())) {
                predicates.add(criteriaBuilder.like(root.get("fileName"), "%" + params.getFileName() + "%"));
            }
            if (params.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), params.getStatus()));
            }
            if (params.getType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), params.getType()));
            }

            CriteriaBuilder.In<Object> inClause = criteriaBuilder.in(root.get("status"));
            inClause.value(MediaStatus.DELETE_EMBY);
            predicates.add(criteriaBuilder.not(inClause));

            String userId = SecurityUtils.getUserInfo().map(EmbyUser::getUserId).orElse("Unknown");
            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageRequest);
    }

    public void updateById(MediaInfo mediainfo) {
        mediaInfoRepository.save(mediainfo);
    }

    public MediaInfoDTO getMediaAndMarks(Long id) {
        MediaInfo mediaInfo = mediaInfoRepository.findById(id).orElseThrow(() -> new BusinessException("未查询到记录"));
        MediaInfoDTO mediaInfoDTO = mediainfoConvertMapper.toMediaInfoDTO(mediaInfo);
        if (mediaInfoDTO.getType() == MediaConvertType.CUT) {
            List<MediaMark> mediaMarks = mediaMarkRepository.findByMediaId(id).stream().sorted(Comparator.comparing(MediaMark::getStart)).collect(Collectors.toList());
            mediaInfoDTO.setMarks(mediaMarks);
        }
        return mediaInfoDTO;
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

    @Transactional
    public void removeAndMarks(Long id) {
        mediaInfoRepository.deleteById(id);
        mediaMarkRepository.deleteMediaMarksByMediaId(id);
    }

    /**
     * @param operate OVERRIDE / DELETE
     */
    @Transactional(rollbackOn = Exception.class)
    public void handlerSourceMedia(Long id, String operate) throws ChangeSetPersister.NotFoundException, IOException {
        MediaInfo mediaInfo = mediaInfoRepository.findById(id).orElseThrow(ChangeSetPersister.NotFoundException::new);
        String outputPath = this.handlerVolumeBind(mediaInfo.getOutputPath());
        String inputPath = this.handlerVolumeBind(mediaInfo.getInputPath());

        Path source = Paths.get(inputPath);
        if (!Files.exists(source)) {
            throw new ChangeSetPersister.NotFoundException();
        }
        String recycleTargetPath = Paths.get(this.handlerVolumeBind(propertiesConfig.getOutputMediaPath()),
                "recycle").toAbsolutePath().toString();
        Path path1 = Paths.get(recycleTargetPath);
        if (!Files.exists(path1)) {
            Files.createDirectories(path1);
        }

        if ("DELETE".equals(operate)) {
            // 删除源文件
            Files.move(source, Paths.get(recycleTargetPath, "done_" + mediaInfo.getFileName() + "_" + RandomUtils.insecure().randomInt(100, 999) + "." + mediaInfo.getSuffix()), StandardCopyOption.REPLACE_EXISTING);
        } else if ("OVERRIDE".equals(operate)) {
            Files.move(Paths.get(outputPath), source, StandardCopyOption.REPLACE_EXISTING);
        }

        String nfo = inputPath.substring(0, outputPath.lastIndexOf(".")) + ".nfo";
        Path nfoPath = Paths.get(nfo);
        if (Files.exists(nfoPath)) {
            Files.delete(nfoPath);
        }
        mediaInfo.setStatus(MediaStatus.DONE);
        mediaInfoRepository.save(mediaInfo);
    }

    public void createByMetadata(Metadata itemMetadata) {
        try {
            MediaInfo mediaInfo = new MediaInfo();
            mediaInfo.setFileName(itemMetadata.getSortName());
            mediaInfo.setFileSize(itemMetadata.getSize());
            mediaInfo.setInputPath(itemMetadata.getPath());
            mediaInfo.setEmbyId(itemMetadata.getId());
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
        MediaInfo mediaInfo = mediaInfoRepository.findByInputPathOrOutputPath(inputPath, inputPath, Limit.of(1));
        if (mediaInfo == null) {
            return null;
        }
        return this.getMediaAndMarks(mediaInfo.getId());
    }

    public MediaInfoDTO getSubtitleMediaInfo(Long embyId) {
        MediaInfo query = new MediaInfo();
        query.setEmbyId(embyId);
        query.setSuffix("Japanese");
        return mediaInfoRepository.findAll(Example.of(query))
                .stream().findFirst().map(mediainfoConvertMapper::toMediaInfoDTO).orElse(null);
    }

    public String handlerVolumeBind(String path) {
        path = String.valueOf(path);
        if (propertiesConfig.getVolumeBind() != null && !propertiesConfig.getVolumeBind().isEmpty()) {
            // 卷路径映射处理
            for (String item : propertiesConfig.getVolumeBind()) {
                String[] split = item.trim().split("->");
                if (split.length != 2) {
                    throw new BusinessException("Volume bind exception!");
                }
                String hostVolume = split[0];
                String embyVolume = split[1];
                if (path.startsWith(hostVolume)) {
                    return path;
                }
                if (path.startsWith(embyVolume)) {
                    String realPath =  hostVolume.replace(File.separatorChar, '/') + path.substring(embyVolume.length());
//                    String realPath = path.replaceFirst(embyVolume, hostVolume.replace(File.separatorChar, '/'));
                    log.info("Volume path replace : {} ->{}", path, realPath);
                    return realPath.replace('/', File.separatorChar);
                }
            }
        }
        return path;
    }

    public List<MediaInfo> listByEmbyIds(List<Long> ids) {
        return mediaInfoRepository.findByEmbyIdIn(ids, Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    public List<MediaInfo> listByOutput(List<String> paths) {
        return mediaInfoRepository.findByOutputPathIn(paths);
    }
}
