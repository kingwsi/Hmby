package org.example.hmby.repository;

import org.example.hmby.entity.ChatTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatTopicRepository extends JpaRepository<ChatTopic, Long> {
}
