package org.example.hmby.enumerate;

import java.util.Arrays;
import java.util.List;

public enum MediaCodec {
    hevc,
    h264,
    h264_qsv,
    hevc_qsv,
    hevc_nvenc;
    
    public static List<String> getCodecs() {
        return Arrays.stream(MediaCodec.values()).map(Enum::name).toList();
    }
}
