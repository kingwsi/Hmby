package org.example.hmby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.example.hmby.enumerate.SubtitleStatus;

import java.math.BigDecimal;

/**
 * @author ws </br>
 * 2025/5/9
 */
@Getter
@Setter
@Entity(name = "subtitles")
public class Subtitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long mediaId;

    @Column(nullable = false)
    private Integer sequence;
    
    private String startTime;
    private String endTime;
    private String text;
    private String translatedText;

    @Enumerated(EnumType.STRING)
    private SubtitleStatus status;
}
