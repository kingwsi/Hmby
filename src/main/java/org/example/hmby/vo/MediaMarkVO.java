package org.example.hmby.vo;

import lombok.Data;

/**
* description: 标记数据 <br>
* date: 2022-05-16 <br>
* author:  <br>
*/
@Data
public class MediaMarkVO {

    private Integer id;

    /**
     * 媒体id
     */
    private Integer mediaId;

    /**
     * 开始时间
     */
    private Long start;

    /**
     * 结束时间
     */
    private Long end;


}
