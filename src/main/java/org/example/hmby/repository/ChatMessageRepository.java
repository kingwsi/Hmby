package org.example.hmby.repository;

import org.example.hmby.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByTopicId(Long topicId);

    List<ChatMessage> findTop10ByTopicIdOrderByContentAsc(Long topicId);

    Page<ChatMessage> findByUserId(String userId, Pageable pageable);

    Page<ChatMessage> findByTopicId(Long topicId, Pageable pageable);

    void deleteByTopicId(Long topicId);

    @Query(value = "select * from chat_messages cm WHERE topic_id = :id and role != 'system' order by created_date", nativeQuery = true)
    List<ChatMessage> findMessagesByTopic(Long id);
}
