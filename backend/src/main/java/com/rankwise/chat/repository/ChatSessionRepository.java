package com.rankwise.chat.repository;

import com.rankwise.chat.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Page<ChatSession> findByChatUserIdOrderByUpdatedAtDesc(Long chatUserId, Pageable pageable);

    @Query("""
            SELECT s FROM ChatSession s
            WHERE (:q IS NULL OR :q = '' OR LOWER(s.title) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY s.updatedAt DESC
            """)
    Page<ChatSession> search(@Param("q") String q, Pageable pageable);

    long countByActiveTrue();
}
