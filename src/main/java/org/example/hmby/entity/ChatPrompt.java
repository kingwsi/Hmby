package org.example.hmby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * @author ws
 * @since 2025/8/1
 */
@Getter
@Setter
@Entity
@Table(name = "chat_prompt")
public class ChatPrompt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prompt", length = 500, nullable = false)
    private String prompt;
}
