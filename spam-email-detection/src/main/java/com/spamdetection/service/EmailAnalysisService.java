package com.spamdetection.service;

import com.spamdetection.entity.EmailAnalysis;
import com.spamdetection.entity.User;
import com.spamdetection.repository.EmailAnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EmailAnalysisService {

    @Autowired
    private EmailAnalysisRepository emailAnalysisRepository;

    @Autowired
    private SpamDetectionService spamDetectionService;

    public EmailAnalysis analyzeEmail(User user, String subject, String content) {
        // Perform spam detection
        SpamDetectionService.SpamAnalysisResult result = spamDetectionService.detectSpam(subject, content);

        EmailAnalysis analysis = new EmailAnalysis();
        analysis.setUser(user);
        analysis.setSubject(subject);
        analysis.setContent(content);
        analysis.setResult(result.getClassification());
        analysis.setSpamScore(result.getSpamScore());
        analysis.setKeywordsDetected(String.join(", ", result.getDetectedKeywords()));

        return emailAnalysisRepository.save(analysis);
    }

    public List<EmailAnalysis> getUserAnalysis(User user) {
        return emailAnalysisRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<EmailAnalysis> getAllAnalysis() {
        return emailAnalysisRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<EmailAnalysis> getAnalysisById(Long id) {
        return emailAnalysisRepository.findById(id);
    }

    public long getTotalAnalysisCount() {
        return emailAnalysisRepository.count();
    }

    public long getSpamCount() {
        return emailAnalysisRepository.countByResult("SPAM");
    }

    public long getSafeCount() {
        return emailAnalysisRepository.countByResult("NOT_SPAM");
    }

    public long getUserAnalysisCount(User user) {
        return emailAnalysisRepository.countByUser(user);
    }
}
