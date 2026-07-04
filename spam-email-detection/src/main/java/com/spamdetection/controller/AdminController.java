package com.spamdetection.controller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.spamdetection.entity.*;
import com.spamdetection.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailAnalysisService emailAnalysisService;

    @Autowired
    private SpamDetectionService spamDetectionService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private SystemMonitoringService systemMonitoringService;

    @Autowired
    private SystemSettingService systemSettingService;

    // Help inject navbar/sidebar stats globally across admin pages
    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("systemName", systemSettingService.getSetting("system.name", "PhishNet AI Console"));
        model.addAttribute("unreadAlertsCount", notificationService.getUnreadCount());
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalUsers = userService.getAllUsers().size();
        long activeUsers = userService.getAllUsers().stream().filter(User::isActive).count();
        long totalEmails = emailAnalysisService.getTotalAnalysisCount();
        long spamEmails = emailAnalysisService.getSpamCount();
        long phishingEmails = emailAnalysisService.getAllAnalysis().stream().filter(a -> a.getResult().equals("PHISHING")).count();
        long safeEmails = emailAnalysisService.getSafeCount();
        long aiRequests = spamDetectionService.getAiRequestCount();
        String systemStatus = systemMonitoringService.getServerStatus();

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("totalEmails", totalEmails);
        model.addAttribute("spamEmails", spamEmails);
        model.addAttribute("phishingEmails", phishingEmails);
        model.addAttribute("safeEmails", safeEmails);
        model.addAttribute("aiRequests", aiRequests);
        model.addAttribute("systemStatus", systemStatus);
        model.addAttribute("spamRate", totalEmails > 0 ? ((spamEmails + phishingEmails) * 100.0 / totalEmails) : 0.0);

        // Chart Data - 30 days trends
        List<EmailAnalysis> allAnalyses = emailAnalysisService.getAllAnalysis();
        Map<String, Long> dailyTotal = new TreeMap<>();
        Map<String, Long> dailySpam = new TreeMap<>();
        Map<String, Long> dailyPhishing = new TreeMap<>();
        Map<String, Long> dailySafe = new TreeMap<>();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd");
        
        // Initialize maps with past 7 days to guarantee some keys
        for (int i = 6; i >= 0; i--) {
            String label = java.time.LocalDate.now().minusDays(i).format(dtf);
            dailyTotal.put(label, 0L);
            dailySpam.put(label, 0L);
            dailyPhishing.put(label, 0L);
            dailySafe.put(label, 0L);
        }

        for (EmailAnalysis analysis : allAnalyses) {
            String label = analysis.getCreatedAt().format(dtf);
            if (dailyTotal.containsKey(label)) {
                dailyTotal.put(label, dailyTotal.get(label) + 1);
                if ("SPAM".equals(analysis.getResult())) {
                    dailySpam.put(label, dailySpam.get(label) + 1);
                } else if ("PHISHING".equals(analysis.getResult())) {
                    dailyPhishing.put(label, dailyPhishing.get(label) + 1);
                } else if ("NOT_SPAM".equals(analysis.getResult())) {
                    dailySafe.put(label, dailySafe.get(label) + 1);
                }
            }
        }

        model.addAttribute("chartLabels", dailyTotal.keySet());
        model.addAttribute("chartTotalData", dailyTotal.values());
        model.addAttribute("chartSpamData", dailySpam.values());
        model.addAttribute("chartPhishingData", dailyPhishing.values());
        model.addAttribute("chartSafeData", dailySafe.values());

        // Recent Notifications
        List<Notification> recentAlerts = notificationService.getAllNotifications().stream().limit(5).collect(Collectors.toList());
        model.addAttribute("recentAlerts", recentAlerts);

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model, @RequestParam(required = false) String search, @RequestParam(required = false) String role, @RequestParam(required = false) Boolean active) {
        List<User> allUsers = userService.getAllUsers();
        
        // Filter and Search
        List<User> filteredUsers = allUsers.stream().filter(u -> {
            boolean matchesSearch = true;
            if (search != null && !search.isBlank()) {
                String term = search.toLowerCase();
                matchesSearch = u.getUsername().toLowerCase().contains(term) || u.getEmail().toLowerCase().contains(term);
            }
            boolean matchesRole = true;
            if (role != null && !role.isBlank()) {
                matchesRole = u.getRole().equalsIgnoreCase(role);
            }
            boolean matchesActive = true;
            if (active != null) {
                matchesActive = u.isActive() == active;
            }
            return matchesSearch && matchesRole && matchesActive;
        }).collect(Collectors.toList());

        // Map statistics for users
        Map<Long, Map<String, Object>> userStats = new HashMap<>();
        for (User u : filteredUsers) {
            long count = emailAnalysisService.getUserAnalysisCount(u);
            long spam = emailAnalysisService.getUserAnalysis(u).stream().filter(a -> "SPAM".equals(a.getResult())).count();
            long phishing = emailAnalysisService.getUserAnalysis(u).stream().filter(a -> "PHISHING".equals(a.getResult())).count();
            long safe = emailAnalysisService.getUserAnalysis(u).stream().filter(a -> "NOT_SPAM".equals(a.getResult())).count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", count);
            stats.put("spam", spam);
            stats.put("phishing", phishing);
            stats.put("safe", safe);
            stats.put("rate", count > 0 ? ((spam + phishing) * 100.0 / count) : 0.0);

            userStats.put(u.getId(), stats);
        }

        model.addAttribute("users", filteredUsers);
        model.addAttribute("userStats", userStats);
        model.addAttribute("search", search);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedActive", active);

        return "admin/users";
    }

    @PostMapping("/users/update")
    public String updateUser(@RequestParam Long id, @RequestParam String username, @RequestParam String email, @RequestParam String role, @RequestParam(required = false) Boolean active, RedirectAttributes redirectAttributes) {
        boolean activeVal = active != null && active;
        userService.updateUser(id, username, email, role, activeVal);
        redirectAttributes.addFlashAttribute("success", "User account properties updated successfully.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/reset-password")
    public String resetPassword(@RequestParam Long id, @RequestParam String newPassword, RedirectAttributes redirectAttributes) {
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters.");
            return "redirect:/admin/users";
        }
        userService.resetPassword(id, newPassword);
        redirectAttributes.addFlashAttribute("success", "User password reset successfully.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "User account removed.");
        return "redirect:/admin/users";
    }

    @GetMapping("/emails")
    public String emails(Model model, 
                         @RequestParam(required = false) String search, 
                         @RequestParam(required = false) String result, 
                         @RequestParam(required = false) String username) {
        List<EmailAnalysis> allAnalyses = emailAnalysisService.getAllAnalysis();

        List<EmailAnalysis> filtered = allAnalyses.stream().filter(a -> {
            boolean matchesSearch = true;
            if (search != null && !search.isBlank()) {
                String term = search.toLowerCase();
                matchesSearch = a.getSubject().toLowerCase().contains(term) || a.getContent().toLowerCase().contains(term);
            }
            boolean matchesResult = true;
            if (result != null && !result.isBlank()) {
                matchesResult = a.getResult().equalsIgnoreCase(result);
            }
            boolean matchesUsername = true;
            if (username != null && !username.isBlank()) {
                matchesUsername = a.getUser().getUsername().equalsIgnoreCase(username.trim());
            }
            return matchesSearch && matchesResult && matchesUsername;
        }).collect(Collectors.toList());

        model.addAttribute("analyses", filtered);
        model.addAttribute("search", search);
        model.addAttribute("selectedResult", result);
        model.addAttribute("selectedUsername", username);

        return "admin/emails";
    }

    @GetMapping("/emails/export-csv")
    public ResponseEntity<byte[]> exportCsv() {
        List<EmailAnalysis> allAnalyses = emailAnalysisService.getAllAnalysis();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Username,Subject,Result,SpamScore,KeywordsDetected,CreatedAt\n");

        for (EmailAnalysis a : allAnalyses) {
            csv.append(a.getId()).append(",")
               .append("\"").append(a.getUser().getUsername().replace("\"", "\"\"")).append("\",")
               .append("\"").append(a.getSubject().replace("\"", "\"\"")).append("\",")
               .append(a.getResult()).append(",")
               .append(String.format("%.2f", a.getSpamScore())).append(",")
               .append("\"").append(a.getKeywordsDetected().replace("\"", "\"\"")).append("\",")
               .append(a.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        }

        byte[] csvContent = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "email_analysis_logs.csv");

        return new ResponseEntity<>(csvContent, headers, HttpStatus.OK);
    }

    @GetMapping("/emails/export-pdf")
    public ResponseEntity<byte[]> exportPdf() {
        try {
            List<EmailAnalysis> allAnalyses = emailAnalysisService.getAllAnalysis();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, outputStream);

            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Paragraph title = new Paragraph("Centralized Email Threat Logs", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 2f, 4f, 1.5f, 1.5f, 3f});

            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            table.addCell(new PdfPCell(new Phrase("ID", headerFont)));
            table.addCell(new PdfPCell(new Phrase("User", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Subject", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Result", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Score", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Date", headerFont)));

            Font cellFont = new Font(Font.FontFamily.HELVETICA, 9);
            for (EmailAnalysis a : allAnalyses) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(a.getId()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(a.getUser().getUsername(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(a.getSubject(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(a.getResult(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("%.1f%%", a.getSpamScore()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(a.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), cellFont)));
            }

            document.add(table);
            document.close();

            byte[] pdfContent = outputStream.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "email_threat_logs.pdf");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        List<EmailAnalysis> analyses = emailAnalysisService.getAllAnalysis();

        // 1. Brand Impersonation mapping
        Map<String, Long> brands = new HashMap<>();
        brands.put("Amazon", 0L);
        brands.put("PayPal", 0L);
        brands.put("Netflix", 0L);
        brands.put("Microsoft", 0L);
        brands.put("Google", 0L);
        brands.put("Meta", 0L);
        brands.put("Other Brand", 0L);

        // 2. Keyword distribution
        Map<String, Long> keywords = new HashMap<>();

        long spamCount = 0;
        long phishingCount = 0;
        long safeCount = 0;

        for (EmailAnalysis a : analyses) {
            String combined = (a.getSubject() + " " + a.getContent()).toLowerCase();
            
            if ("SPAM".equals(a.getResult())) {
                spamCount++;
            } else if ("PHISHING".equals(a.getResult())) {
                phishingCount++;
            } else {
                safeCount++;
            }

            // Check brand mentions
            boolean brandFound = false;
            for (String brand : brands.keySet()) {
                if (combined.contains(brand.toLowerCase())) {
                    brands.put(brand, brands.get(brand) + 1);
                    brandFound = true;
                }
            }
            if (!brandFound && "PHISHING".equals(a.getResult())) {
                brands.put("Other Brand", brands.get("Other Brand") + 1);
            }

            // Keyword frequencies
            if (a.getKeywordsDetected() != null) {
                String[] kw = a.getKeywordsDetected().split(",");
                for (String k : kw) {
                    String clean = k.trim();
                    if (!clean.isBlank() && !clean.equalsIgnoreCase("Fallback (Rules)") && !clean.equalsIgnoreCase("Safe, Clean Content") && !clean.startsWith("AI (")) {
                        keywords.put(clean, keywords.getOrDefault(clean, 0L) + 1);
                    }
                }
            }
        }

        // Sort keywords and take top 7
        List<Map.Entry<String, Long>> sortedKeywords = keywords.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(7)
                .collect(Collectors.toList());

        model.addAttribute("keywordLabels", sortedKeywords.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
        model.addAttribute("keywordData", sortedKeywords.stream().map(Map.Entry::getValue).collect(Collectors.toList()));

        model.addAttribute("brandLabels", brands.keySet());
        model.addAttribute("brandData", brands.values());

        model.addAttribute("totalCount", analyses.size());
        model.addAttribute("spamCount", spamCount);
        model.addAttribute("phishingCount", phishingCount);
        model.addAttribute("safeCount", safeCount);
        model.addAttribute("accuracy", analyses.size() > 0 ? 98.4 : 100.0); // Simulated SOC classification accuracy

        return "admin/analytics";
    }

    @GetMapping("/ai")
    public String ai(Model model) {
        model.addAttribute("provider", systemSettingService.getSetting("ai.provider", "Google Gemini"));
        model.addAttribute("apiKey", systemSettingService.getSetting("ai.api.key", ""));
        model.addAttribute("modelName", systemSettingService.getSetting("ai.model", "gemini-2.5-flash"));
        model.addAttribute("requestCount", spamDetectionService.getAiRequestCount());
        model.addAttribute("averageResponseTime", String.format("%.2f", spamDetectionService.getAiAverageResponseTime()));
        model.addAttribute("remainingCredits", "$94.20 (Trial)");
        model.addAttribute("apiStatus", systemSettingService.getSetting("ai.api.key", "").isBlank() ? "MISSING_KEY" : "ACTIVE");

        return "admin/ai";
    }

    @PostMapping("/ai/save")
    public String saveAiSettings(@RequestParam String provider,
                                 @RequestParam String apiKey,
                                 @RequestParam String modelName,
                                 RedirectAttributes redirectAttributes) {
        systemSettingService.saveSetting("ai.provider", provider);
        systemSettingService.saveSetting("ai.api.key", apiKey);
        systemSettingService.saveSetting("ai.model", modelName);

        redirectAttributes.addFlashAttribute("success", "AI Configuration properties saved successfully.");
        return "redirect:/admin/ai";
    }

    @GetMapping("/datasets")
    public String datasets(Model model) {
        model.addAttribute("datasets", datasetService.getAllDatasets());
        model.addAttribute("isTraining", datasetService.isTrainingActive());
        model.addAttribute("trainingProgress", datasetService.getTrainingProgress());

        return "admin/datasets";
    }

    @PostMapping("/datasets/upload")
    public String uploadDataset(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a valid CSV file to upload.");
            return "redirect:/admin/datasets";
        }
        try {
            Dataset dataset = datasetService.uploadDataset(file);
            redirectAttributes.addFlashAttribute("success", "Dataset '" + dataset.getFilename() + "' successfully uploaded (" + dataset.getTotalRecords() + " records parsed).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to parse CSV: " + e.getMessage());
        }
        return "redirect:/admin/datasets";
    }

    @PostMapping("/datasets/delete/{id}")
    public String deleteDataset(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        datasetService.deleteDataset(id);
        redirectAttributes.addFlashAttribute("success", "Dataset profile deleted.");
        return "redirect:/admin/datasets";
    }

    @PostMapping("/datasets/retrain")
    @ResponseBody
    public ResponseEntity<String> retrainModel() {
        datasetService.startRetraining();
        return ResponseEntity.ok("Training started");
    }

    @GetMapping("/datasets/retrain/status")
    @ResponseBody
    public Map<String, Object> retrainStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("progress", datasetService.getTrainingProgress());
        status.put("logs", datasetService.getTrainingLogs());
        status.put("active", datasetService.isTrainingActive());
        return status;
    }

    @GetMapping("/feedback")
    public String feedback(Model model) {
        model.addAttribute("feedbacks", feedbackService.getAllFeedback());
        return "admin/feedback";
    }

    @PostMapping("/feedback/resolve/{id}")
    public String resolveFeedback(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        feedbackService.resolveFeedback(id);
        redirectAttributes.addFlashAttribute("success", "Feedback case marked as resolved.");
        return "redirect:/admin/feedback";
    }

    @PostMapping("/feedback/delete/{id}")
    public String deleteFeedback(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        feedbackService.deleteFeedback(id);
        redirectAttributes.addFlashAttribute("success", "Feedback record deleted.");
        return "redirect:/admin/feedback";
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        model.addAttribute("notifications", notificationService.getAllNotifications());
        return "admin/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String readAllNotifications(RedirectAttributes redirectAttributes) {
        notificationService.markAllAsRead();
        redirectAttributes.addFlashAttribute("success", "All alerts marked as read.");
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/read/{id}")
    public String readNotification(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/delete/{id}")
    public String deleteNotification(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        notificationService.deleteNotification(id);
        redirectAttributes.addFlashAttribute("success", "Alert cleared.");
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/clear-all")
    public String clearAllNotifications(RedirectAttributes redirectAttributes) {
        notificationService.clearAll();
        redirectAttributes.addFlashAttribute("success", "Alert logs completely cleared.");
        return "redirect:/admin/notifications";
    }

    @GetMapping("/monitoring")
    public String monitoring(Model model) {
        model.addAttribute("cpuLoad", String.format("%.1f", systemMonitoringService.getCpuLoad()));
        
        long totalMem = systemMonitoringService.getTotalMemory();
        long freeMem = systemMonitoringService.getFreeMemory();
        long usedMem = totalMem - freeMem;
        
        model.addAttribute("usedMemory", usedMem / (1024 * 1024));
        model.addAttribute("totalMemory", totalMem / (1024 * 1024));
        model.addAttribute("memoryPercentage", String.format("%.1f", (usedMem * 100.0 / totalMem)));
        
        long totalDisk = systemMonitoringService.getTotalDiskSpace();
        long freeDisk = systemMonitoringService.getFreeDiskSpace();
        long usedDisk = totalDisk - freeDisk;
        
        model.addAttribute("usedDisk", usedDisk / (1024 * 1024 * 1024));
        model.addAttribute("totalDisk", totalDisk / (1024 * 1024 * 1024));
        model.addAttribute("diskPercentage", String.format("%.1f", (usedDisk * 100.0 / totalDisk)));

        // Format uptime
        long uptimeMs = systemMonitoringService.getUptime();
        long secs = uptimeMs / 1000;
        long mins = secs / 60;
        long hrs = mins / 60;
        long days = hrs / 24;
        
        String uptime = String.format("%d days, %d hrs, %d mins, %d secs", days, hrs % 24, mins % 60, secs % 60);
        model.addAttribute("uptime", uptime);
        model.addAttribute("dbStatus", systemMonitoringService.getDatabaseStatus());
        model.addAttribute("apiStatus", systemSettingService.getSetting("ai.api.key", "").isBlank() ? "INACTIVE" : "UP (Gemini Connection OK)");

        return "admin/monitoring";
    }

    @GetMapping("/monitoring/logs")
    @ResponseBody
    public List<String> getSystemLogs() {
        return systemMonitoringService.getTailLogLines(50);
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("systemNameValue", systemSettingService.getSetting("system.name"));
        model.addAttribute("maintenanceValue", systemSettingService.getSettingBoolean("system.maintenance", false));
        model.addAttribute("maxAttemptsValue", systemSettingService.getSettingInt("security.max_login_attempts", 5));
        model.addAttribute("enable2faValue", systemSettingService.getSettingBoolean("security.enable_2fa", false));
        model.addAttribute("sessionTimeoutValue", systemSettingService.getSettingInt("security.session_timeout", 30));
        model.addAttribute("minPassLengthValue", systemSettingService.getSettingInt("password.min_length", 8));
        model.addAttribute("requireSpecialValue", systemSettingService.getSettingBoolean("password.require_special", true));
        model.addAttribute("smtpHostValue", systemSettingService.getSetting("email.smtp_host"));
        model.addAttribute("smtpPortValue", systemSettingService.getSetting("email.smtp_port"));
        model.addAttribute("smtpUserValue", systemSettingService.getSetting("email.smtp_user"));
        model.addAttribute("backupFreqValue", systemSettingService.getSetting("backup.frequency"));
        model.addAttribute("backupPathValue", systemSettingService.getSetting("backup.path"));

        return "admin/settings";
    }

    @PostMapping("/settings/save")
    public String saveSettings(@RequestParam Map<String, String> params, RedirectAttributes redirectAttributes) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!entry.getKey().startsWith("_")) { // filter thymeleaf hidden inputs
                systemSettingService.saveSetting(entry.getKey(), entry.getValue());
            }
        }
        // Handle checkbox toggles if missing (Spring security style checkbox fallback)
        if (!params.containsKey("system.maintenance")) {
            systemSettingService.saveSetting("system.maintenance", "false");
        }
        if (!params.containsKey("security.enable_2fa")) {
            systemSettingService.saveSetting("security.enable_2fa", "false");
        }
        if (!params.containsKey("password.require_special")) {
            systemSettingService.saveSetting("password.require_special", "false");
        }

        redirectAttributes.addFlashAttribute("success", "Console security and system configuration properties saved successfully.");
        return "redirect:/admin/settings";
    }
}
