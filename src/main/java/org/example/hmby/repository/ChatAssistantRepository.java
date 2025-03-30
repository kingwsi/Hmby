package org.example.hmby.repository;

import org.example.hmby.entity.ChatAssistant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ChatAssistantRepository extends JpaRepository<ChatAssistant, Long> {
    List<ChatAssistant> findAllByUserIdOrderByLastUpdateDateDesc(String userId);
}
