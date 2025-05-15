package org.example.hmby.repository;

import org.example.hmby.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {
    List<ChatConversation> findByAssistantIdAndActivated(Long assistantId, Boolean activated);
}
