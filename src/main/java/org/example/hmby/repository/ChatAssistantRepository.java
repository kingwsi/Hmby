package org.example.hmby.repository;

import org.example.hmby.entity.ChatAssistant;
import org.example.hmby.enumerate.AssistantType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatAssistantRepository extends JpaRepository<ChatAssistant, Long> {
    List<ChatAssistant> findAllByUserIdOrderByLastUpdateDateDesc(String userId);

    Optional<ChatAssistant> findByType(AssistantType type);
}
