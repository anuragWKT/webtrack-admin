package com.webknot.webtrak_admin.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webknot.webtrak_admin.entity.Allocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocationEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.allocation-events}")
    private String allocationEventsTopic;

    public void publishAllocationEvent(Allocation allocation, String eventType) {
        try {
            AllocationEvent event = AllocationEvent.builder()
                    .eventType(eventType)
                    .allocationId(allocation.getId())
                    .userId(allocation.getUserId())
                    .projectCode(allocation.getProject().getCode())
                    .allocationType(allocation.getAllocationType())
                    .startDate(allocation.getStartDate())
                    .endDate(allocation.getEndDate())
                    .hoursPerDay(allocation.getHoursPerDay())
                    .manager(allocation.isManager())
                    .active(allocation.isActive())
                    .eventTime(LocalDateTime.now())
                    .build();

            String payload = Objects.requireNonNull(objectMapper.writeValueAsString(event), "payload must not be null");
            String key = Objects.requireNonNull(String.valueOf(allocation.getUserId()), "key must not be null");
            String topic = Objects.requireNonNull(allocationEventsTopic, "topic must not be null");
            kafkaTemplate.send(topic, key, payload);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize allocation event: {}", eventType, ex);
        } catch (Exception ex) {
            log.error("Failed to publish allocation event: {}", eventType, ex);
        }
    }
}
