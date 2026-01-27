package com.amazingshop.personal.userservice.domain.handlers;

import com.amazingshop.personal.userservice.domain.events.UserRegisteredEvent;
import com.amazingshop.personal.userservice.kafka.producer.NotificationProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserRegisteredEventListener {

    private final NotificationProducer notificationProducer;

    @Autowired
    public UserRegisteredEventListener(NotificationProducer notificationProducer) {
        this.notificationProducer = notificationProducer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event){
        notificationProducer.sendWelcomeEmail(event.user());
    }
}