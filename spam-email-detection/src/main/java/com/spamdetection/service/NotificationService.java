package com.spamdetection.service;

import com.spamdetection.entity.Notification;
import com.spamdetection.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository repository;

    public void createNotification(String type, String message) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setMessage(message);
        repository.save(notification);
    }

    public List<Notification> getAllNotifications() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public long getUnreadCount() {
        return repository.countByIsReadFalse();
    }

    public void markAllAsRead() {
        List<Notification> notifications = repository.findAll();
        for (Notification n : notifications) {
            n.setRead(true);
        }
        repository.saveAll(notifications);
    }

    public void markAsRead(Long id) {
        repository.findById(id).ifPresent(n -> {
            n.setRead(true);
            repository.save(n);
        });
    }

    public void deleteNotification(Long id) {
        repository.deleteById(id);
    }

    public void clearAll() {
        repository.deleteAll();
    }
}
