package org.example.hmby.ffmpeg;

import lombok.Getter;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;

import java.io.IOException;

/**
 * description:  <br>
 * date: 2024/2/4 11:31 <br>
 */
@Getter
public class FFmpegManager extends FFmpegExecutor {
    
    private FFmpeg ffmpeg;

    private FFprobe ffprobe;
    
    public FFmpegManager() throws IOException {
    }

    public FFmpegManager(FFmpeg ffmpeg) throws IOException {
        super(ffmpeg);
        this.ffmpeg = ffmpeg;
    }

    public FFmpegManager(FFmpeg ffmpeg, FFprobe ffprobe) {
        super(ffmpeg, ffprobe);
        this.ffmpeg = ffmpeg;
        this.ffprobe = ffprobe;
    }
}
