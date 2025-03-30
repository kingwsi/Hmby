package org.example.hmby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import org.example.hmby.enumerate.MediaConvertType;
import org.example.hmby.enumerate.MediaStatus;

@Getter
@Setter
@Entity(name = "media_infos")
public class MediaInfo extends AuditableEntity {

    /**
     * 绝对路径
     */
    @Column(name = "input_path", length = 500)
    private String inputPath;

    @Column(name = "output_path", length = 500)
    private String outputPath;

    /**
     * file name
     */
    @Column(name = "file_name", length = 500)
    private String fileName;

    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MediaStatus status;

    /**
     * embyId
     */
    @Column(name = "emby_id", nullable = false)
    private Long embyId;

    /**
     * 文件大小
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 处理后大小
     */
    @Column(name = "processed_size")
    private Long processedSize;

    /**
     * 处理耗时 hh:mm:ss
     */
    @Column(name = "time_cost")
    private String timeCost;

    /**
     * 后缀
     */
    @Column(name = "suffix")
    private String suffix;

    /**
     * 备注
     */
    @Column(name = "remark", length = 1000)
    private String remark;

    /**
     * 转换类型 CUT / ENCODE
     */
    @Column(name = "type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private MediaConvertType type;

    /**
     * 编码类型
     */
    @Column(name = "codec", length = 50)
    private String codec;


    /**
     * 视频码率
     */
    @Column(name = "bit_rate")
    private Long bitRate;

    @Column(name = "meta_title", length = 50)
    private String metaTitle;
}
