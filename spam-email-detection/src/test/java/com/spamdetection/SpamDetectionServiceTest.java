package com.spamdetection;

import com.spamdetection.service.SpamDetectionService;
import com.spamdetection.service.SpamDetectionService.SpamAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SpamDetectionServiceTest {

    @Autowired
    private SpamDetectionService spamDetectionService;

    @BeforeEach
    void resetKey() {
        ReflectionTestUtils.setField(spamDetectionService, "geminiApiKey", "");
    }

    @Test
    void fallbackUsedWhenApiKeyIsEmpty() {
        SpamAnalysisResult result = spamDetectionService.detectSpam(
                "You Won Free Money!!!",
                "Congratulations! You are the winner of $1,000,000!!! Click here now to claim your prize immediately! This is an urgent offer - act now!"
        );
        
        assertThat(result.getClassification()).isEqualTo("SPAM");
        assertThat(result.getDetectedKeywords()).contains("Fallback (Rules)");
        assertThat(result.getDetectedKeywords()).contains("free");
    }

    @Test
    void fallbackUsedWithErrorMsgWhenApiKeyIsInvalid() {
        ReflectionTestUtils.setField(spamDetectionService, "geminiApiKey", "INVALID_API_KEY");
        
        SpamAnalysisResult result = spamDetectionService.detectSpam(
                "You Won Free Money!!!",
                "Congratulations! You are the winner of $1,000,000!!! Click here now to claim your prize immediately! This is an urgent offer - act now!"
        );
        
        assertThat(result.getClassification()).isEqualTo("SPAM");
        assertThat(result.getDetectedKeywords()).contains("Fallback (Rules)");
        assertThat(result.getDetectedKeywords().stream().anyMatch(k -> k.startsWith("AI Error:"))).isTrue();
    }

    @Test
    void detectSpamReturnsInvalidForNonEmailFormats() {
        // Test short subject/content
        SpamAnalysisResult resultShort = spamDetectionService.detectSpam("hi", "test");
        assertThat(resultShort.getClassification()).isEqualTo("INVALID");
        assertThat(resultShort.getDetectedKeywords().get(0)).contains("Invalid format");

        // Test single word gibberish
        SpamAnalysisResult resultGibberish = spamDetectionService.detectSpam("a", "abcdefghijklmnopqrstuvwxyz");
        assertThat(resultGibberish.getClassification()).isEqualTo("INVALID");
    }
}
