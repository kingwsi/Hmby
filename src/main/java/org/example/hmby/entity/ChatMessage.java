package org.example.hmby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chat_messages")
public class ChatMessage extends AuditableEntity {

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "content", length = Integer.MAX_VALUE)
    private String content;

    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MessageRole role;

    
    public enum MessageRole {
        system,user,assistant,tool
    }
}