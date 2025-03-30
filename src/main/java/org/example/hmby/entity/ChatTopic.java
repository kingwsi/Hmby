package org.example.hmby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chat_topic")
public class ChatTopic extends AuditableEntity {

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "assistant_id")
    private Long assistantId;
}