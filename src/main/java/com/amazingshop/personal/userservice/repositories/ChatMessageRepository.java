package com.amazingshop.personal.userservice.repositories;

import com.amazingshop.personal.userservice.models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatIdOrderByCreatedAtAsc(Long chatId);

    long countByChatIdAndRole(Long chatId, String role);

    // Дополнительно: удаление всех сообщений чата (если нужно для каскадного удаления)
    void deleteByChatId(Long chatId);
}