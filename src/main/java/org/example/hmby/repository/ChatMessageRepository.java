package org.example.hmby.repository;

import org.example.hmby.entity.ChatMessage;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    void deleteByConversationId(String conversationId);

    List<ChatMessage> findByConversationId(String conversationId, Sort timestamp);
}
