package org.example.hmby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
* description:  <br>
* date: 2022-04-08 <br>
* author:  <br>
*/

@Getter
@Setter
@Entity(name = "tags")
public class Tag extends AuditableEntity {

    /**
     * 标签名称
     */
    @Column(name = "name", length = 100, nullable = false)
    private String name;
    
    @ColumnDefault("0")
    @Column(name = "count", nullable = false)
    private Long count;

    @ColumnDefault("true")
    @Column(name = "show", nullable = false)
    private Boolean show;


    public Tag(String name, Long count, Boolean show) {
        this.name = name;
        this.count = count;
        this.show = show;
    }

    public Tag() {
        
    }
}
