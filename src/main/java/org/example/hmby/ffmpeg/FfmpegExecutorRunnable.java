package org.example.hmby.ffmpeg;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.service.MediaInfoService;
import org.example.hmby.vo.MediaQueueVO;

/**
 * description:  <br>
 * date: 2022/12/6 16:54 <br>
 */
@Slf4j
public class FfmpegExecutorRunnable implements Runnable {

    private final MediaInfoService mediaInfoService;
    
    @Getter
    private final MediaQueueVO mediaInfo;

    public FfmpegExecutorRunnable(MediaInfoService mediaInfoService, MediaQueueVO mediaInfo) {
        this.mediaInfoService = mediaInfoService;
        this.mediaInfo = mediaInfo;
    }

    @Override
    public void run() {
        mediaInfoService.handleMedia(this.mediaInfo.getId());
    }

    @Override
    public boolean equals(Object obj) {
        FfmpegExecutorRunnable runnable = (FfmpegExecutorRunnable) obj;
        return this.mediaInfo.getId().equals(runnable.getMediaInfo().getId());
    }

    @Override
    public int hashCode() {
        return this.mediaInfo.getId().hashCode();
    }
}
