package org.example.hmby.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hmby.dto.AuditableDTO;
import org.example.hmby.entity.MediaMark;
import org.example.hmby.enumerate.MediaConvertType;
import org.example.hmby.enumerate.MediaStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class MediaInfoDTO extends AuditableDTO {

    /**
     * 绝对路径
     */
    private String inputPath;

    private String outputPath;

    /**
     * 状态
     */
    private MediaStatus status;
    
    /**
     * embyId
     */
    private Long embyId;
    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 处理后大小
     */
    private Long processedSize;

    /**
     * 处理耗时 hh:mm:ss
     */
    private String timeCost;


    /**
     * file name
     */
    private String fileName;

    private String suffix;

    private List<MediaMark> marks;

    private String remark;

    private MediaConvertType type;

    private Double progress;

    private String codec;

    /**
     * 视频码率
     */
    private Long bitRate;

    private String metaTitle;
    
    private Integer crf;
}
