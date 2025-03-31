package org.example.hmby.ffmpeg;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpegUtils;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegFileInputBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import net.bramp.ffmpeg.builder.Strict;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.commons.lang3.StringUtils;
import org.example.hmby.config.PropertiesConfig;
import org.example.hmby.entity.MediaInfo;
import org.example.hmby.entity.MediaMark;
import org.example.hmby.exception.BusinessException;
import org.example.hmby.service.MediaInfoService;
import org.example.hmby.vo.MediaInfoDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.joining;

/**
 * description:  <br>
 * document: <a href="https://github.com/bramp/ffmpeg-cli-wrapper">https://github.com/bramp/ffmpeg-cli-wrapper</a> <br>
 * date: 2022/6/5 14:17 <br>
 */
@Slf4j
@Service
@AllArgsConstructor
public class FfmpegService {

    private final FFmpegManager ffmpegManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final PropertiesConfig propertiesConfig;


    public String encoding(MediaInfo mediaInfo) throws IOException {

        String inputPath = this.handlerVolumeBind(mediaInfo.getInputPath());
        String outputPath = this.handlerVolumeBind(mediaInfo.getOutputPath());
        FFmpegFileInputBuilder inputBuilder = new FFmpegBuilder()
                .setInput(inputPath)
                .setVideoCodec("h264_cuvid")
                .addExtraArgs("-hwaccel", "cuvid")
                .setStrict(Strict.EXPERIMENTAL);

        FFmpegOutputBuilder fFmpegOutputBuilder = inputBuilder.done().addOutput(outputPath)
                .setVideoCodec("hevc_nvenc")
                .setFormat("mp4");
        if (mediaInfo.getBitRate() != null && mediaInfo.getBitRate() > 1000) {
            fFmpegOutputBuilder.setVideoBitRate(mediaInfo.getBitRate())
                    .addExtraArgs("-bufsize", mediaInfo.getBitRate().toString());
        }
        if (!StringUtils.isEmpty(mediaInfo.getMetaTitle())) {
            fFmpegOutputBuilder.addMetaTag("title", mediaInfo.getMetaTitle());
        }
        
        FFmpegBuilder fFmpegBuilder = fFmpegOutputBuilder.done().overrideOutputFiles(true);

        FFmpegProbeResult in = ffmpegManager.getFfprobe().probe(inputPath);

        ProgressInfo progressInfo = new ProgressInfo();
        progressInfo.setMediaName(mediaInfo.getFileName());

        FFmpegJob job = ffmpegManager.createJob(fFmpegBuilder, progress -> {
            if (progress.out_time_ns < 0) {
                return;
            }
            double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);
            double percentage = progress.out_time_ns / duration_ns;
            double leftTime_ns = (duration_ns - progress.out_time_ns) / progress.speed;

            String timecode = FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS);
            String leftTime = FFmpegUtils.toTimecode(TimeUnit.NANOSECONDS.toSeconds((long) leftTime_ns), TimeUnit.SECONDS);
            log.info(String.format(
                    "[%.0f%%] status:%s frame:%d time:%s ms fps:%.0f speed:%.2fx left time:%s",
                    percentage * 100,
                    progress.status,
                    progress.frame,
                    timecode,
                    progress.fps.doubleValue(),
                    progress.speed,
                    leftTime
            ));
            HashMap<String, Object> progressMap = new HashMap<>();
            progressMap.put("percentage", String.format("%.0f%%", percentage * 100));
            progressMap.put("status", progress.status);
            progressMap.put("fileName", mediaInfo.getFileName());
            progressMap.put("frame", progress.frame);
            progressMap.put("time", timecode);
            progressMap.put("fps", progress.fps.doubleValue());
            progressMap.put("speed", progress.speed);
            progressMap.put("bitrate", progress.bitrate);
            progressMap.put("leftTime", leftTime);
            messagingTemplate.convertAndSend("/topic/encode-progress", progressMap);
        });
        try {
            job.run();
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public void cutAndConcat(MediaInfoDTO mediaInfoDTO) throws Exception {
        Path target = Paths.get(getTmpPath());
        // 创建临时文件
        Path listOfFiles = Files.createTempFile(target, "ffmpeg-list-", ".txt");
        // 剪切
        ArrayList<String> tmpFileList = new ArrayList<>();
        try {
            String inputPath = this.handlerVolumeBind(mediaInfoDTO.getInputPath());
            String outputPath = this.handlerVolumeBind(mediaInfoDTO.getOutputPath());
            
            if (mediaInfoDTO.getMarks().size()==1) {
                MediaMark mark = mediaInfoDTO.getMarks().get(0);
                FFmpegBuilder builder = new FFmpegBuilder()
                        .setInput(inputPath)
                        .setStartOffset(mark.getStart(), TimeUnit.SECONDS)
                        .setDuration(mark.getEnd() - mark.getStart(), TimeUnit.SECONDS)
                        .done().addOutput(outputPath)
                        .setVideoCodec("copy")
                        .setAudioCodec("copy")
                        .done().overrideOutputFiles(true);
                ffmpegManager.createJob(builder).run();
                // 获取转换后文件大小
                long size = Files.size(Paths.get(outputPath));
                mediaInfoDTO.setProcessedSize(size);
                return;
            }
            for (MediaMark mark : mediaInfoDTO.getMarks()) {
                String outPath = Paths.get(getTmpPath(), mediaInfoDTO.getId() + "_" + mark.getStart() + "." + mediaInfoDTO.getSuffix()).toString();
                FFmpegBuilder builder = new FFmpegBuilder()
                        .setInput(inputPath)
                        .setStartOffset(mark.getStart(), TimeUnit.SECONDS)
                        .setDuration(mark.getEnd() - mark.getStart(), TimeUnit.SECONDS)
                        .done().addOutput(outPath)
                        .setVideoCodec("copy")
                        .setAudioCodec("copy")
                        .done().overrideOutputFiles(true);
                ffmpegManager.createJob(builder).run();
                tmpFileList.add(outPath);
            }
            String fileListText = tmpFileList.stream().map(p -> "file '" + p + "'")
                    .collect(joining(System.lineSeparator()));
            Files.writeString(listOfFiles, fileListText);
            FFmpegFileInputBuilder inputBuilder = new FFmpegBuilder()
                    .addExtraArgs("-safe", "0")
                    .setInput(listOfFiles.toAbsolutePath().toString()).setFormat("concat");

            FFmpegOutputBuilder outputBuilder = inputBuilder.done()
                    .addOutput(outputPath)
                    .setVideoCodec("copy")
                    .setAudioCodec("copy");

            if (!StringUtils.isEmpty(mediaInfoDTO.getMetaTitle())) {
                outputBuilder.addMetaTag("title", mediaInfoDTO.getMetaTitle());
            }
            FFmpegBuilder fFmpegBuilder = outputBuilder.done().overrideOutputFiles(true);

            ffmpegManager.createJob(fFmpegBuilder).run();

            // 获取转换后文件大小
            long size = Files.size(Paths.get(outputPath));
            mediaInfoDTO.setProcessedSize(size);
        } finally {
            // 删除临时文件
            Files.deleteIfExists(listOfFiles);
            tmpFileList.forEach(p -> {
                try {
                    Files.deleteIfExists(Paths.get(p));
                } catch (IOException e) {
                    log.error("删除临时文件失败：{}", e.getMessage());
                }
            });
        }
    }

    /**
     * 移动文件并添加水印
     */
    public void moveAndTitle(MediaInfo mediaInfo) throws IOException {
        if (StringUtils.isEmpty(mediaInfo.getMetaTitle())) {
            Files.move(Paths.get(mediaInfo.getInputPath()),
                    Paths.get(mediaInfo.getOutputPath()),
                    StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        FFmpegBuilder fFmpegBuilder = new FFmpegBuilder()
                .setInput(mediaInfo.getInputPath())
                .done().addOutput(mediaInfo.getOutputPath())
                .addMetaTag("title", mediaInfo.getMetaTitle())
                .setVideoCodec("copy")
                .done().overrideOutputFiles(true);
        ffmpegManager.createJob(fFmpegBuilder).run();

    }

    public String getTmpPath() {
        return System.getProperty("java.io.tmpdir");
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
                    String realPath = path.replaceFirst(embyVolume, hostVolume.replace(File.separatorChar, '/'));
                    log.info("Volume path replace : {} ->{}", path, realPath);
                    return realPath.replace('/', File.separatorChar);
                }
            }
        }
        return path;
    }
}
