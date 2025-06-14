package org.example.hmby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedBy;

@Getter
@Setter
@Entity
@Table(name = "chat_conversations")
public class ChatConversation extends AuditableEntity {
    
    @CreatedBy
    private String userId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "assistant_id" ,nullable = false)
    private Long assistantId;

    @Column(name = "activated" ,nullable = false)
    @ColumnDefault("false")
    private Boolean activated;
}