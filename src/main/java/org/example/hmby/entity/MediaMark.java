package org.example.hmby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.experimental.Accessors;

/**
* description: 标记数据 <br>
* date: 2022-05-16 <br>
* author:  <br>
*/

@Data
@Entity(name = "media_marks")
public class MediaMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 媒体id
     */
    private Long mediaId;

    /**
     * 开始时间
     */
    @Column(name = "start_time")
    private Long start;

    /**
     * 结束时间
     */
    @Column(name = "end_time")
    private Long end;
}
