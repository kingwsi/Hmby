package org.example.hmby.vo;

import lombok.Data;

import java.util.List;

@Data
public class TagVO {

    private Long id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签使用次数
     */
    private Long count;

    private Boolean show;
    
    private Long embyMediaId;
    
    private List<String> names; 

}
