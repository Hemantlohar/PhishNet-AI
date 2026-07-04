package com.spamdetection.controller;

import com.spamdetection.entity.User;
import com.spamdetection.service.EmailAnalysisService;
import com.spamdetection.service.UserService;
import com.spamdetection.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DashboardController {

    @Autowired
    private EmailAnalysisService emailAnalysisService;

    @Autowired
    private UserService userService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        boolean isAdmin = userDetails.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return "redirect:/admin/dashboard";
        } else {
            // User dashboard
            long totalEmails = emailAnalysisService.getUserAnalysisCount(user);
            long spamEmails = emailAnalysisService.getUserAnalysis(user)
                    .stream()
                    .filter(a -> a.getResult().equals("SPAM"))
                    .count();
            long phishingEmails = emailAnalysisService.getUserAnalysis(user)
                    .stream()
                    .filter(a -> a.getResult().equals("PHISHING"))
                    .count();
            long safeEmails = emailAnalysisService.getUserAnalysis(user)
                    .stream()
                    .filter(a -> a.getResult().equals("NOT_SPAM"))
                    .count();
            
            model.addAttribute("totalEmails", totalEmails);
            model.addAttribute("spamEmails", spamEmails);
            model.addAttribute("phishingEmails", phishingEmails);
            model.addAttribute("safeEmails", safeEmails);
            model.addAttribute("spamRate", totalEmails > 0 ? ((spamEmails + phishingEmails) * 100.0 / totalEmails) : 0);
            model.addAttribute("isAdmin", false);
            
            return "dashboard-user";
        }
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "403";
    }

    @GetMapping("/feedback")
    public String feedbackForm(Model model) {
        return "feedback-submit";
    }

    @PostMapping("/feedback")
    public String submitFeedback(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String subject,
            @RequestParam String message,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            RedirectAttributes redirectAttributes) {
        
        User user = null;
        String senderName = name;
        String senderEmail = email;
        
        if (userDetails != null) {
            user = userService.findByUsername(userDetails.getUsername()).orElse(null);
            if (user != null) {
                senderName = user.getUsername();
                senderEmail = user.getEmail();
            }
        }
        
        feedbackService.submitFeedback(user, senderName, senderEmail, subject, message);
        redirectAttributes.addFlashAttribute("success", "Thank you! Your feedback has been submitted successfully.");
        return "redirect:/dashboard";
    }

    @GetMapping("/profile")
    public String profile(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String confirmPassword,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        
        if (password != null && !password.isBlank()) {
            if (!password.equals(confirmPassword)) {
                model.addAttribute("error", "Passwords do not match.");
                model.addAttribute("user", user);
                return "profile";
            }
            if (password.length() < 8) {
                model.addAttribute("error", "Password must be at least 8 characters long.");
                model.addAttribute("user", user);
                return "profile";
            }
            user.setPassword(passwordEncoder.encode(password));
        }
        
        if (!email.equalsIgnoreCase(user.getEmail())) {
            // Check if email already registered
            if (userService.findByUsername(email).isPresent()) {
                model.addAttribute("error", "Email is already registered.");
                model.addAttribute("user", user);
                return "profile";
            }
            user.setEmail(email.toLowerCase().trim());
        }
        
        userService.save(user);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/dashboard";
    }
}
