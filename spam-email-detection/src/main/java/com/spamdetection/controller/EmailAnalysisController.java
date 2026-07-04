package com.spamdetection.controller;

import com.spamdetection.entity.User;
import com.spamdetection.entity.EmailAnalysis;
import com.spamdetection.service.EmailAnalysisService;
import com.spamdetection.service.UserService;
import com.spamdetection.dto.EmailAnalysisRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class EmailAnalysisController {

    @Autowired
    private EmailAnalysisService emailAnalysisService;

    @Autowired
    private UserService userService;

    @GetMapping("/analyze")
    public String analyzeForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("analysisRequest", new EmailAnalysisRequest());
        return "analyze";
    }

    @PostMapping("/analyze")
    public String submitAnalysis(
            @ModelAttribute EmailAnalysisRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        
        if (userDetails != null && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }

        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        EmailAnalysis analysis = emailAnalysisService.analyzeEmail(user, request.getSubject(), request.getContent());
        
        model.addAttribute("analysis", analysis);
        return "redirect:/analysis/" + analysis.getId();
    }

    @GetMapping("/analysis/{id}")
    public String viewAnalysis(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        
        EmailAnalysis analysis = emailAnalysisService.getAnalysisById(id)
                .orElseThrow(() -> new RuntimeException("Analysis not found"));

        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        
        // Check authorization - user can view their own or admin can view all
        if (!analysis.getUser().getId().equals(user.getId()) && !userDetails.getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new RuntimeException("Unauthorized");
        }

        model.addAttribute("analysis", analysis);
        return "analysis-detail";
    }

    @GetMapping("/history")
    public String viewHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        
        if (userDetails != null && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }

        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        java.util.List<EmailAnalysis> analyses = emailAnalysisService.getUserAnalysis(user);
        
        model.addAttribute("analyses", analyses);
        return "history";
    }
}
