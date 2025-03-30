package org.example.hmby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "chat_assistants")
public class ChatAssistant extends AuditableEntity {
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @ColumnDefault("0.8")
    @Column(name = "temperature", nullable = false)
    private Double temperature;

    @Column(name = "prompt", length = 1000)
    private String prompt;

    @Column(name = "remark", length = 1000)
    private String remark;

}