package com.spamdetection.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class SpamDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(SpamDetectionService.class);

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private NotificationService notificationService;

    private final java.util.concurrent.atomic.AtomicLong requestCount = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong totalLatency = new java.util.concurrent.atomic.AtomicLong(0);


    private static final Set<String> SPAM_KEYWORDS = new HashSet<>(Arrays.asList(
            "free", "winner", "congratulations", "urgent", "click here", "claim prize",
            "verify account", "confirm identity", "update payment", "act now", "limited time",
            "exclusive offer", "unsubscribe", "no-reply", "do not reply", "viagra",
            "pharmacy", "casino", "lottery", "inheritance", "prince", "wire transfer",
            "bank account", "credit card", "social security", "password", "username",
            "confirm", "re-confirm", "validate", "authenticate", "refund", "tax return",
            "increase sales", "work from home", "make money", "easy money", "quick cash",
            "no credit check", "bad credit", "forex", "penny stock", "bitcoin", "crypto",
            "dear friend", "dear sir", "dear madam", "beloved", "greetings", "hello friend"));

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public long getAiRequestCount() {
        return requestCount.get();
    }

    public double getAiAverageResponseTime() {
        long count = requestCount.get();
        return count > 0 ? (double) totalLatency.get() / count : 0.0;
    }

    public boolean isValidEmailFormat(String subject, String content) {
        if (subject == null || content == null) {
            return false;
        }
        String cleanSubject = subject.trim();
        String cleanContent = content.trim();
        
        // 1. Minimum length checks
        if (cleanSubject.isEmpty() || cleanContent.isEmpty()) {
            return false;
        }
        if (cleanContent.length() < 10) {
            return false;
        }
        
        // 2. Must contain at least some alphabetic characters
        long letterCount = cleanContent.chars().filter(Character::isLetter).count();
        if (letterCount < 5) {
            return false;
        }
        
        // 3. Must contain some spaces or structure (not just a single block of text or a single URL)
        String[] words = cleanContent.split("\\s+");
        if (words.length < 3) {
            if (cleanContent.matches("https?://[^\\s]+")) {
                return false;
            }
            return false;
        }
        
        // 4. Check if it's just random gibberish (e.g. no vowels at all or extreme non-word sequence)
        long vowelCount = cleanContent.toLowerCase().chars()
                .filter(ch -> ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u').count();
        if (letterCount > 15 && vowelCount == 0) {
            return false;
        }
        
        return true;
    }

    public SpamAnalysisResult detectSpam(String subject, String content) {
        if (!isValidEmailFormat(subject, content)) {
            return new SpamAnalysisResult("INVALID", 0.0, List.of("Invalid format: Not a valid email content structure"));
        }

        String provider = systemSettingService.getSetting("ai.provider", "Google Gemini");
        String activeKey = systemSettingService.getSetting("ai.api.key", geminiApiKey);
        String activeModel = systemSettingService.getSetting("ai.model", "gemini-2.5-flash");

        if ("Mock Provider".equalsIgnoreCase(provider) || "Rule-Based".equalsIgnoreCase(provider)) {
            // Rule-based logic with local keyword analysis
            SpamAnalysisResult ruleResult = detectSpamFallback(subject, content);
            String finalClass = ruleResult.getClassification();
            if ("SPAM".equals(finalClass)) {
                String lowerText = (subject + " " + content).toLowerCase();
                if (lowerText.contains("verify account") || lowerText.contains("confirm identity") || 
                    lowerText.contains("social security") || lowerText.contains("wire transfer") || 
                    lowerText.contains("bank account") || lowerText.contains("password") ||
                    lowerText.contains("update payment")) {
                    finalClass = "PHISHING";
                }
            }
            return new SpamAnalysisResult(finalClass, ruleResult.getSpamScore(), ruleResult.getDetectedKeywords());
        }

        // Google Gemini
        if (activeKey != null && !activeKey.isBlank()) {
            long startTime = System.currentTimeMillis();
            requestCount.incrementAndGet();
            try {
                SpamAnalysisResult aiResult = detectSpamWithAI(subject, content, activeKey, activeModel);
                long latency = System.currentTimeMillis() - startTime;
                totalLatency.addAndGet(latency);
                return aiResult;
            } catch (Exception e) {
                logger.error("Error in Gemini AI spam detection. Falling back to keyword-based detection.", e);
                notificationService.createNotification("FAILED_AI", "Gemini AI API Request Failed: " + e.getMessage() + ". Used fallback rule-based system.");
                SpamAnalysisResult fallbackResult = detectSpamFallback(subject, content);
                List<String> keywords = new ArrayList<>(fallbackResult.getDetectedKeywords());
                keywords.add(0, "Fallback (Rules)");
                keywords.add(1, "AI Error: " + e.getMessage());
                
                String finalClass = fallbackResult.getClassification();
                if ("SPAM".equals(finalClass)) {
                    String lowerText = (subject + " " + content).toLowerCase();
                    if (lowerText.contains("verify account") || lowerText.contains("confirm identity") || 
                        lowerText.contains("social security") || lowerText.contains("wire transfer") || 
                        lowerText.contains("bank account") || lowerText.contains("password") ||
                        lowerText.contains("update payment")) {
                        finalClass = "PHISHING";
                    }
                }
                return new SpamAnalysisResult(
                        finalClass,
                        fallbackResult.getSpamScore(),
                        keywords
                );
            }
        } else {
            logger.warn("Gemini API key is not configured. Falling back to keyword-based detection.");
            SpamAnalysisResult fallbackResult = detectSpamFallback(subject, content);
            List<String> keywords = new ArrayList<>(fallbackResult.getDetectedKeywords());
            keywords.add(0, "Fallback (Rules)");
            
            String finalClass = fallbackResult.getClassification();
            if ("SPAM".equals(finalClass)) {
                String lowerText = (subject + " " + content).toLowerCase();
                if (lowerText.contains("verify account") || lowerText.contains("confirm identity") || 
                    lowerText.contains("social security") || lowerText.contains("wire transfer") || 
                    lowerText.contains("bank account") || lowerText.contains("password") ||
                    lowerText.contains("update payment")) {
                    finalClass = "PHISHING";
                }
            }
            return new SpamAnalysisResult(
                    finalClass,
                    fallbackResult.getSpamScore(),
                    keywords
            );
        }
    }

    private SpamAnalysisResult detectSpamWithAI(String subject, String content, String apiKey, String model) throws Exception {
        String prompt = "You are a professional spam email detection system. Analyze the following email subject and content, and determine if it is clean (NOT_SPAM), advertisement/junk (SPAM), or social engineering/credential harvesting (PHISHING), or not in the format/content of an email message (INVALID).\n" +
                "Note: If the input text is not a communication message (like random gibberish, programming code, general web search queries, recipes, or completely unrelated non-email text), classify it as INVALID.\n" +
                "Provide a confidence score from 0.0 to 100.0 (where 100.0 is definitely spam/phishing, and 0.0 is definitely safe, or 0.0 for INVALID) and a list of key red flags or reasoning patterns found (maximum 5 reasons, concise phrases, e.g., 'Urgent demand for money', 'Suspicious URL redirection', 'Generic greeting', 'Invalid non-email content').\n\n" +
                "Subject: " + subject + "\n" +
                "Content:\n" + content;

        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("parts", List.of(part));

        requestBody.put("contents", List.of(contentMap));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> responseSchema = new HashMap<>();
        responseSchema.put("type", "OBJECT");

        Map<String, Object> classificationProp = new HashMap<>();
        classificationProp.put("type", "STRING");
        classificationProp.put("enum", List.of("SPAM", "NOT_SPAM", "PHISHING", "INVALID"));

        Map<String, Object> scoreProp = new HashMap<>();
        scoreProp.put("type", "NUMBER");

        Map<String, Object> reasonsProp = new HashMap<>();
        reasonsProp.put("type", "ARRAY");
        Map<String, Object> itemsProp = new HashMap<>();
        itemsProp.put("type", "STRING");
        reasonsProp.put("items", itemsProp);

        Map<String, Object> properties = new HashMap<>();
        properties.put("classification", classificationProp);
        properties.put("score", scoreProp);
        properties.put("reasons", reasonsProp);

        responseSchema.put("properties", properties);
        responseSchema.put("required", List.of("classification", "score", "reasons"));

        generationConfig.put("responseSchema", responseSchema);
        requestBody.put("generationConfig", generationConfig);

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP error code: " + response.statusCode() + ", body: " + response.body());
        }

        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(response.body());
        com.fasterxml.jackson.databind.JsonNode textNode = rootNode.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

        if (textNode.isMissingNode()) {
            throw new RuntimeException("Invalid response structure from Gemini API: " + response.body());
        }

        String aiResponseJson = textNode.asText();
        com.fasterxml.jackson.databind.JsonNode aiResultNode = objectMapper.readTree(aiResponseJson);

        String classification = aiResultNode.path("classification").asText("NOT_SPAM");
        double score = aiResultNode.path("score").asDouble(0.0);
        
        List<String> detectedReasons = new ArrayList<>();
        detectedReasons.add("AI (" + model + ")");

        com.fasterxml.jackson.databind.JsonNode reasonsNode = aiResultNode.path("reasons");
        if (reasonsNode.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode reason : reasonsNode) {
                detectedReasons.add(reason.asText());
            }
        }

        return new SpamAnalysisResult(classification, score, detectedReasons);
    }

    public SpamAnalysisResult detectSpamFallback(String subject, String content) {
        Set<String> detectedKeywords = new HashSet<>();
        double spamScore = 0.0;

        String combinedText = (subject + " " + content).toLowerCase();

        // Check for spam keywords
        for (String keyword : SPAM_KEYWORDS) {
            if (combinedText.contains(keyword.toLowerCase())) {
                detectedKeywords.add(keyword);
                spamScore += 10.0;
            }
        }

        // Check for excessive punctuation
        long exclamationCount = combinedText.chars().filter(ch -> ch == '!').count();
        long questionCount = combinedText.chars().filter(ch -> ch == '?').count();
        if (exclamationCount > 5) {
            spamScore += 15.0;
            detectedKeywords.add("excessive_exclamation");
        }
        if (questionCount > 5) {
            spamScore += 10.0;
            detectedKeywords.add("excessive_questions");
        }

        // Check for all caps
        long capsCount = combinedText.chars().filter(Character::isUpperCase).count();
        long totalLetters = combinedText.chars().filter(Character::isLetter).count();
        if (totalLetters > 0 && (capsCount / (double) totalLetters) > 0.7) {
            spamScore += 15.0;
            detectedKeywords.add("excessive_caps");
        }

        // Check for suspicious URLs
        if (combinedText.matches(".*https?://[^\\s]+.*")) {
            spamScore += 10.0;
            detectedKeywords.add("contains_urls");
        }

        // Check for multiple email addresses
        int emailCount = countEmails(combinedText);
        if (emailCount > 3) {
            spamScore += 15.0;
            detectedKeywords.add("multiple_emails");
        }

        // Normalize spam score to 0-100
        spamScore = Math.min(spamScore, 100.0);

        String classification = spamScore >= 50 ? "SPAM" : "NOT_SPAM";

        return new SpamAnalysisResult(classification, spamScore, new ArrayList<>(detectedKeywords));
    }

    private int countEmails(String text) {
        int count = 0;
        String[] parts = text.split("\\s+");
        for (String part : parts) {
            if (EMAIL_PATTERN.matcher(part).matches()) {
                count++;
            }
        }
        return count;
    }

    public static class SpamAnalysisResult {
        private String classification;
        private Double spamScore;
        private List<String> detectedKeywords;

        public SpamAnalysisResult(String classification, Double spamScore, List<String> detectedKeywords) {
            this.classification = classification;
            this.spamScore = spamScore;
            this.detectedKeywords = detectedKeywords;
        }

        public String getClassification() {
            return classification;
        }

        public Double getSpamScore() {
            return spamScore;
        }

        public List<String> getDetectedKeywords() {
            return detectedKeywords;
        }
    }
}
