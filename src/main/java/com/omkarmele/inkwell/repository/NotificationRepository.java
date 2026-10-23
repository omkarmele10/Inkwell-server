package com.omkarmele.inkwell.repository;

import com.omkarmele.inkwell.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    Page<Notification> findByRecipientId(String recipientId, Pageable pageable);
    long countByRecipientIdAndReadFalse(String recipientId);
    void deleteByArticleId(String articleId);
}
