package com.rankwise.chat.repository;

import com.rankwise.chat.entity.FaqArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FaqArticleRepository extends JpaRepository<FaqArticle, Long> {

    @Query(value = """
            SELECT TOP (:limit) * FROM faq_article
            WHERE active = 1
            AND (
                LOWER(title) LIKE LOWER(CONCAT('%', :term, '%'))
                OR LOWER(content) LIKE LOWER(CONCAT('%', :term, '%'))
                OR LOWER(tags) LIKE LOWER(CONCAT('%', :term, '%'))
                OR LOWER(category) LIKE LOWER(CONCAT('%', :term, '%'))
            )
            ORDER BY LEN(title)
            """, nativeQuery = true)
    List<FaqArticle> searchByTerm(@Param("term") String term, @Param("limit") int limit);

    List<FaqArticle> findByActiveTrueOrderByCategoryAscTitleAsc();
}
