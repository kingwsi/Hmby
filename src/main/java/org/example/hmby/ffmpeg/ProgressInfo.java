package org.example.hmby.ffmpeg;

import net.bramp.ffmpeg.progress.Progress;

/**
 * description:  <br>
 * date: 2022/11/25 17:05 <br>
 */
public record ProgressInfo(String mediaName, Progress.Status status, double fps, String percentage, float speed, String costTime, String timeLeft) {
}
