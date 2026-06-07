package com.rankwise.chat.repository;

import com.rankwise.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.sessionId = :sessionId
            ORDER BY m.createdAt DESC
            """)
    List<ChatMessage> findRecent(@Param("sessionId") Long sessionId, Pageable pageable);

    long countBySessionId(Long sessionId);
}
