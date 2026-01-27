package com.amazingshop.personal.userservice.kafka.producer;

import com.amazingshop.personal.userservice.kafka.events.NotificationEvent;
import com.amazingshop.personal.userservice.models.User;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationProducer
 */
@ExtendWith(MockitoExtension.class)
class NotificationProducerTest {

    @Mock
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @InjectMocks
    private NotificationProducer notificationProducer;

    @Captor
    private ArgumentCaptor<NotificationEvent> eventCaptor;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(123L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
    }

    @Test
    void shouldSendWelcomeEmailSuccessfully() {
        // Given
        CompletableFuture<SendResult<String, NotificationEvent>> future =
                CompletableFuture.completedFuture(createSuccessfulSendResult());

        when(kafkaTemplate.send(eq("notifications"), any(NotificationEvent.class)))
                .thenReturn(future);

        // When
        notificationProducer.sendWelcomeEmail(testUser);

        // Then
        verify(kafkaTemplate, times(1))
                .send(eq("notifications"), eventCaptor.capture());

        NotificationEvent sentEvent = eventCaptor.getValue();
        assertThat(sentEvent.type()).isEqualTo("WELCOME");
        assertThat(sentEvent.userId()).isEqualTo("123");
        assertThat(sentEvent.email()).isEqualTo("test@example.com");
        assertThat(sentEvent.data().get("username")).isEqualTo("testuser");
        assertThat(sentEvent.timestamp()).isNotNull();
    }

    @Test
    void shouldSendToCorrectTopic() {
        // Given
        CompletableFuture<SendResult<String, NotificationEvent>> future =
                CompletableFuture.completedFuture(createSuccessfulSendResult());

        when(kafkaTemplate.send(anyString(), any(NotificationEvent.class)))
                .thenReturn(future);

        // When
        notificationProducer.sendWelcomeEmail(testUser);

        // Then
        verify(kafkaTemplate).send(eq("notifications"), any(NotificationEvent.class));
    }

    @Test
    void shouldHandleKafkaFailureGracefully() {
        // Given
        CompletableFuture<SendResult<String, NotificationEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka broker is down"));

        when(kafkaTemplate.send(eq("notifications"), any(NotificationEvent.class)))
                .thenReturn(future);

        // When
        notificationProducer.sendWelcomeEmail(testUser);

        // Then
        verify(kafkaTemplate, times(1)).send(eq("notifications"), any(NotificationEvent.class));
        // Should not throw exception - error is logged
    }

    @Test
    void shouldCreateEventWithCorrectStructure() {
        // Given
        CompletableFuture<SendResult<String, NotificationEvent>> future =
                CompletableFuture.completedFuture(createSuccessfulSendResult());

        when(kafkaTemplate.send(anyString(), any(NotificationEvent.class)))
                .thenReturn(future);

        User user = new User();
        user.setId(999L);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");

        // When
        notificationProducer.sendWelcomeEmail(user);

        // Then
        verify(kafkaTemplate).send(eq("notifications"), eventCaptor.capture());

        NotificationEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo("WELCOME");
        assertThat(event.userId()).isEqualTo("999");
        assertThat(event.email()).isEqualTo("john@example.com");
        assertThat(event.data()).containsEntry("username", "john_doe");
    }

    @Test
    void shouldHandleExceptionDuringEventCreation() {
        // Given
        User invalidUser = null;

        // When/Then - should handle null gracefully
        try {
            notificationProducer.sendWelcomeEmail(invalidUser);
        } catch (Exception e) {
            // Expected - or should be handled in producer
        }

        // Verify no Kafka interaction if user is null
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    private SendResult<String, NotificationEvent> createSuccessfulSendResult() {
        NotificationEvent event = NotificationEvent.welcome(testUser);
        ProducerRecord<String, NotificationEvent> producerRecord =
                new ProducerRecord<>("notifications", event);

        RecordMetadata metadata = new RecordMetadata(
                null, 0, 0, 0L, 0, 0
        );

        return new SendResult<>(producerRecord, metadata);
    }
}