package com.bookmyshow.worker;

import com.bookmyshow.domain.model.OutboxEvent;
import com.bookmyshow.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox Worker - polls the outbox table and publishes events.
 *
 * In production, this would publish to Kafka/RabbitMQ.
 * Here we simulate by logging and marking events as published.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(fixedDelayString = "${booking.outbox.poll-interval-ms:5000}")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> events = outboxEventRepository
            .findTop50ByPublishedFalseOrderByCreatedAtAsc();

        if (events.isEmpty()) {
            return;
        }

        log.info("Outbox worker: publishing {} events", events.size());

        for (OutboxEvent event : events) {
            try {
                // In production: publish to Kafka/RabbitMQ
                // kafkaTemplate.send("booking-events", event.getEventId(), event.getPayload());
                log.info("Publishing outbox event: type={}, aggregateId={}, eventId={}",
                    event.getEventType(), event.getAggregateId(), event.getEventId());

                outboxEventRepository.markAsPublished(event.getId(), LocalDateTime.now());
            } catch (Exception e) {
                log.error("Failed to publish outbox event: {}", event.getEventId(), e);
                // Will be retried on the next poll cycle
            }
        }
    }
}
