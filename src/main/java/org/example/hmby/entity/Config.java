package org.example.hmby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.example.hmby.enumerate.ConfigKey;

/**
 * @author ws </br>
 * 2025/6/20
 */
@Getter
@Setter
@Entity
@Table(name = "config")
public class Config {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    @Enumerated(EnumType.STRING)
    private ConfigKey key;

    @Column(unique = true, nullable = false)
    private String val;
}
