package com.spamdetection.repository;

import com.spamdetection.entity.EmailAnalysis;
import com.spamdetection.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmailAnalysisRepository extends JpaRepository<EmailAnalysis, Long> {
    List<EmailAnalysis> findByUserOrderByCreatedAtDesc(User user);
    List<EmailAnalysis> findAllByOrderByCreatedAtDesc();
    long countByResult(String result);
    long countByUser(User user);
}
