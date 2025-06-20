package org.example.hmby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.example.hmby.entity.converter.EncryptionConverter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "chat_assistants")
public class ChatAssistant extends AuditableEntity {

    @Column(name = "code", unique = true)
    private String code;
    
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "embedding_model", nullable = false, length = 100)
    private String embeddingModel;

    @ColumnDefault("0.8")
    @Column(name = "temperature", nullable = false)
    private Double temperature;

    @Column(name = "prompt", length = 1000)
    private String prompt;

    @Column(name = "base_url", length = 120)
    private String baseUrl;

    @Convert(converter = EncryptionConverter.class)
    @Column(name = "api_key", length = 120)
    private String apiKey;

    @Column(name = "remark", length = 1000)
    private String remark;

}