package org.example.hmby.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;


@Data
@Entity(name = "params")
public class Param {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 值
     */
    private String paramValue;

    /**
     * 编码
     */
    private String paramCode;

    /**
     * 描述
     */
    private String description;


}
