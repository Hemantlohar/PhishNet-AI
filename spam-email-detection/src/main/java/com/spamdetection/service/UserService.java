package com.spamdetection.service;

import com.spamdetection.entity.User;
import com.spamdetection.entity.EmailAnalysis;
import com.spamdetection.repository.UserRepository;
import com.spamdetection.repository.EmailAnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailAnalysisRepository emailAnalysisRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(String username, String email, String password) {
        String normalizedUsername = normalize(username);
        String normalizedEmail = normalizeEmail(email);

        if (normalizedUsername == null) {
            throw new IllegalArgumentException("Username is required.");
        }

        if (normalizedEmail == null) {
            throw new IllegalArgumentException("Email is required.");
        }

        if (!normalizedEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new RuntimeException("Username already exists.");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new RuntimeException("Email is already registered.");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ROLE_USER");
        user.setActive(true);

        try {
            User saved = userRepository.save(user);
            notificationService.createNotification("NEW_USER", "New user registered: " + saved.getUsername() + " (" + saved.getEmail() + ")");
            return saved;
        } catch (DataAccessException ex) {
            throw new IllegalStateException("Database connection is temporarily unavailable. Please try again later.", ex);
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void updateUser(Long id, String username, String email, String role, boolean active) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(active);
        userRepository.save(user);
    }

    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeEmail(String email) {
        String normalized = normalize(email);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    @PostConstruct
    public void seedDatabase() {
        if (userRepository.existsByUsernameIgnoreCase("admin")) {
            return;
        }

        // Create Admin User
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@spamdetection.local");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ROLE_ADMIN");
        admin.setActive(true);
        userRepository.save(admin);

        // Create 5 regular users
        String[] usernames = {"john_sec", "alice_analyst", "bob_user", "charlie_net", "threat_hunter"};
        String[] emails = {"john@example.com", "alice@example.com", "bob@example.com", "charlie@example.com", "hunter@example.com"};
        List<User> seededUsers = new ArrayList<>();
        
        for (int i = 0; i < usernames.length; i++) {
            User u = new User();
            u.setUsername(usernames[i]);
            u.setEmail(emails[i]);
            u.setPassword(passwordEncoder.encode("password123"));
            u.setRole("ROLE_USER");
            u.setActive(true);
            seededUsers.add(userRepository.save(u));
        }

        // Templates for emails
        String[] safeSubjects = {"Weekly Design Sync", "Code review request: login page", "Lunch plans for Friday", "Maven package repository updates", "API integration docs for OAuth", "Project status update Q2"};
        String[] safeContents = {"Hi team, here is the agenda for our weekly sync. Please review and add items.",
                                 "Can someone review my pull request for the login page fixes? It is building fine.",
                                 "Hey, we are going to the pizza place down the street for lunch around 1 PM. Let me know if you want to join.",
                                 "I updated the pom.xml dependencies to version 3.2. Let's make sure we test it.",
                                 "Attached is the OAuth documentation for the integration next sprint. Please review the security scopes.",
                                 "We are tracking green for our deliverables this quarter. Thanks everyone for the effort."};

        String[] spamSubjects = {"Exclusive VIP offer: Buy cryptocurrency now!", "Get high returns in 24 hours guaranteed", "Cheap pharmacy medications online without prescription", "Work from home - Earn $500/day easy money"};
        String[] spamContents = {"Do not miss this exclusive cryptocurrency VIP deal. Click here to purchase immediately.",
                                 "Earn massive returns on Forex trading with our automated AI bot. Act now for a 50% discount.",
                                 "Save big on top prescription meds. Online ordering is fast, easy, and secure. Cash on delivery.",
                                 "We are hiring remote typists. Earn money from the comfort of your couch. No experience necessary."};

        String[] phishingSubjects = {"URGENT: Suspicious activity on your account - Verify now", "Office 365 Security Alert: Reset password immediately", "Update your direct deposit payroll information", "Action Required: Tax refund pending confirmation"};
        String[] phishingContents = {"We detected a suspicious login attempt from IP 192.168.4.12. Please click here to verify your identity.",
                                     "Your Office 365 password has expired. Click this link to authenticate and reset it.",
                                     "Dear employee, please verify your bank account details to ensure your next payroll deposit is processed successfully.",
                                     "The internal revenue service has determined you have a pending refund of $450. Verify your details here to claim."};

        String[][] spamKeywords = {{"VIP", "cryptocurrency", "exclusive"}, {"returns", "guaranteed", "Forex"}, {"medications", "cheap", "pharmacy"}, {"work from home", "easy money"}};
        String[][] phishingKeywords = {{"suspicious login", "verify", "verify account"}, {"Office 365", "reset password", "authenticate"}, {"direct deposit", "bank account", "payroll"}, {"tax return", "refund", "validate"}};

        Random rand = new Random();
        LocalDateTime now = LocalDateTime.now();

        // Seed 60 analyses over 30 days
        for (int day = 30; day >= 1; day--) {
            int dailyCount = 1 + rand.nextInt(3); // 1 to 3 per day
            for (int k = 0; k < dailyCount; k++) {
                User user = seededUsers.get(rand.nextInt(seededUsers.size()));
                int type = rand.nextInt(3); // 0: safe, 1: spam, 2: phishing

                EmailAnalysis analysis = new EmailAnalysis();
                analysis.setUser(user);
                analysis.setCreatedAt(now.minusDays(day).minusHours(rand.nextInt(23)).minusMinutes(rand.nextInt(59)));

                if (type == 0) {
                    int index = rand.nextInt(safeSubjects.length);
                    analysis.setSubject(safeSubjects[index]);
                    analysis.setContent(safeContents[index]);
                    analysis.setResult("NOT_SPAM");
                    analysis.setSpamScore(5.0 + rand.nextDouble() * 25.0);
                    analysis.setKeywordsDetected("Safe, Clean Content");
                } else if (type == 1) {
                    int index = rand.nextInt(spamSubjects.length);
                    analysis.setSubject(spamSubjects[index]);
                    analysis.setContent(spamContents[index]);
                    analysis.setResult("SPAM");
                    analysis.setSpamScore(65.0 + rand.nextDouble() * 30.0);
                    analysis.setKeywordsDetected(String.join(", ", spamKeywords[index]));
                } else {
                    int index = rand.nextInt(phishingSubjects.length);
                    analysis.setSubject(phishingSubjects[index]);
                    analysis.setContent(phishingContents[index]);
                    analysis.setResult("PHISHING");
                    analysis.setSpamScore(80.0 + rand.nextDouble() * 20.0);
                    analysis.setKeywordsDetected(String.join(", ", phishingKeywords[index]));
                }

                emailAnalysisRepository.save(analysis);
            }
        }

        // Seed some system notifications
        notificationService.createNotification("SYSTEM_ERROR", "System monitoring module initialized successfully.");
        notificationService.createNotification("NEW_USER", "Seeded 5 analyst accounts successfully.");
        notificationService.createNotification("DB_FAILURE", "Failed database backup connection: Retried and resolved.");
    }
}
