package com.rankwise.chat.repository;

import com.rankwise.chat.entity.ChatAnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatAnalyticsEventRepository extends JpaRepository<ChatAnalyticsEvent, Long> {
    long countByEventType(String eventType);

    long countByEventTypeAndCreatedAtAfter(String eventType, LocalDateTime after);

    @Query("""
            SELECT e.eventType, COUNT(e) FROM ChatAnalyticsEvent e
            GROUP BY e.eventType
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> countGroupedByEventType();
}
