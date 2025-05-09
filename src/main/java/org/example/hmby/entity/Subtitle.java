package org.example.hmby.entity;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * @author ws </br>
 * 2025/5/9
 */
@Getter
@Setter
@Entity(name = "subtitles")
public class Subtitle extends AuditableEntity {
    private Long mediaId;
    private String language;
    private String sequence;
    private BigDecimal startTime;
    private BigDecimal endTime;
    private String title;
    private String status;
}
