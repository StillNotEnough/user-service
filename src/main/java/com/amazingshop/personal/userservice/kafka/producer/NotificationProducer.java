package com.amazingshop.personal.userservice.kafka.producer;

import com.amazingshop.personal.userservice.kafka.events.NotificationEvent;
import com.amazingshop.personal.userservice.models.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    public static final String TOPIC = "notifications";

    @Autowired
    public NotificationProducer(KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendWelcomeEmail(User user) {
        log.info("TOPIC NAME: '{}'", TOPIC);
        try {
            NotificationEvent event = NotificationEvent.welcome(user);
            log.info("Sending to topic: '{}', event: {}", TOPIC, event);

            kafkaTemplate.send(TOPIC, NotificationEvent.welcome(user))
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send notification for user {}: {}",
                                    user.getId(), ex.getMessage());
                            // TODO: сохранить в outbox table для retry
                        } else {
                            log.info("✅ Sent successfully. Topic: '{}', Partition: {}, Offset: {}",
                                    TOPIC,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Kafka error: {}", e.getMessage());
        }
    }
}