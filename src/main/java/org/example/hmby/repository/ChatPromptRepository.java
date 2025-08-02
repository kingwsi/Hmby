package org.example.hmby.repository;

import org.example.hmby.entity.ChatPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author ws
 * @since 2025/8/1
 */
@Repository
public interface ChatPromptRepository extends JpaRepository<ChatPrompt,Long> {
}
