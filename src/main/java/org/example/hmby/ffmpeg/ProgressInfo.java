package org.example.hmby.ffmpeg;

import lombok.Data;

/**
 * description:  <br>
 * date: 2022/11/25 17:05 <br>
 */
@Data
public class ProgressInfo {
    private String mediaName;
    private String mediaDuration;
    private String mediaOutTme;
    private String status;
    private String fps;
    private String frame;
    private String percentage;
    private String speed;
    private String costTime;
    private String message;
    private String timeLeft;
}
