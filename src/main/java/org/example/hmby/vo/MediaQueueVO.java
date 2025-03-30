package org.example.hmby.vo;

import lombok.Data;
import org.example.hmby.enumerate.MediaStatus;

@Data
public class MediaQueueVO {

    private Long id;
    
    private String fileName;

    private MediaStatus status;

    private Long fileSize;

    private String convertType;
    
    private long timestamp = System.currentTimeMillis();
}
