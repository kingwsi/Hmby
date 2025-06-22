package org.example.hmby.repository;

import org.example.hmby.entity.ChatAssistant;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatAssistantRepository extends JpaRepository<ChatAssistant, Long> {

    Optional<ChatAssistant> findByCode(String type);

    List<ChatAssistant> findAllByUserIdOrderByUpdatedAtDesc(String userId);
}
