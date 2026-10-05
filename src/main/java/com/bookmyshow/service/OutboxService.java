package com.bookmyshow.service;

import com.bookmyshow.domain.model.OutboxEvent;
import com.bookmyshow.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox Service - writes events to the outbox table within the caller's transaction.
 * The OutboxWorker reads and publishes these events asynchronously.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    /**
     * Create an outbox event. Must be called within an existing transaction
     * to guarantee atomicity with the main business operation.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent createEvent(String aggregateType, String aggregateId,
                                    String eventType, String payload) {
        OutboxEvent event = OutboxEvent.builder()
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .eventType(eventType)
            .payload(payload)
            .published(false)
            .build();

        event = outboxEventRepository.save(event);
        log.debug("Outbox event created: type={}, aggregateId={}", eventType, aggregateId);
        return event;
    }
}
