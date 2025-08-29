package org.example.hmby.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * @author ws </br>
 * 2025/6/20
 */
@Getter
@Setter
@Entity
@Table(name = "config")
public class Config extends AuditableEntity {
    private String modelName;
    private String openaiBaseUrl;
    private String defaultLibrary;
    private String embeddingModelName;
}
