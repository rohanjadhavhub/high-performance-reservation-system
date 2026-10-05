package com.bookmyshow.repository;

import com.bookmyshow.domain.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50ByPublishedFalseOrderByCreatedAtAsc();

    @Modifying
    @Query("UPDATE OutboxEvent o SET o.published = true, o.publishedAt = :now WHERE o.id = :id")
    void markAsPublished(@Param("id") Long id, @Param("now") LocalDateTime now);
}
