package com.spamdetection.controller;

import com.spamdetection.entity.EmailAnalysis;
import com.spamdetection.service.EmailAnalysisService;
import com.spamdetection.service.PDFReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

    @Autowired
    private EmailAnalysisService emailAnalysisService;

    @Autowired
    private PDFReportService pdfReportService;

    @GetMapping("/report/pdf/{id}")
    public ResponseEntity<byte[]> generatePdfReport(@PathVariable Long id) {
        try {
            EmailAnalysis analysis = emailAnalysisService.getAnalysisById(id)
                    .orElseThrow(() -> new RuntimeException("Analysis not found"));

            byte[] pdfContent = pdfReportService.generatePdfReport(analysis);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "spam-report-" + id + ".pdf");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
