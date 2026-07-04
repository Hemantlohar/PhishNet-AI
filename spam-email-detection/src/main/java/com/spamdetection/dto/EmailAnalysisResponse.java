package com.spamdetection.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailAnalysisResponse {
    private Long id;
    private String subject;
    private String result;
    private Double spamScore;
    private String keywordsDetected;
    private String createdAt;
}
