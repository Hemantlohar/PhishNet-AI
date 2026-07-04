package com.spamdetection.service;

import com.spamdetection.entity.Feedback;
import com.spamdetection.entity.User;
import com.spamdetection.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository repository;

    public Feedback submitFeedback(User user, String name, String email, String subject, String message) {
        Feedback f = new Feedback();
        f.setUser(user);
        f.setName(name);
        f.setEmail(email);
        f.setSubject(subject);
        f.setMessage(message);
        return repository.save(f);
    }

    public List<Feedback> getAllFeedback() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Feedback> getFeedbackById(Long id) {
        return repository.findById(id);
    }

    public void resolveFeedback(Long id) {
        repository.findById(id).ifPresent(f -> {
            f.setResolved(true);
            repository.save(f);
        });
    }

    public void deleteFeedback(Long id) {
        repository.deleteById(id);
    }
}
