package com.spamdetection.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.spamdetection.entity.EmailAnalysis;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PDFReportService {

    public byte[] generatePdfReport(EmailAnalysis analysis) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);

        document.open();

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("Spam Email Analysis Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));

        // Report Details
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 12);

        addReportRow(document, "Analysis Date:", 
                analysis.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                labelFont, valueFont);
        
        addReportRow(document, "Email Subject:", analysis.getSubject(), labelFont, valueFont);
        
        String classificationText = analysis.getResult();
        if ("SPAM".equals(classificationText)) {
            classificationText = "⚠️ SPAM";
        } else if ("PHISHING".equals(classificationText)) {
            classificationText = "🚨 PHISHING";
        } else if ("INVALID".equals(classificationText)) {
            classificationText = "❌ INVALID EMAIL FORMAT";
        } else {
            classificationText = "✓ NOT SPAM";
        }
        addReportRow(document, "Classification:", 
                classificationText, 
                labelFont, valueFont);
        
        addReportRow(document, "Spam Score:", 
                String.format("%.2f%%", analysis.getSpamScore()), 
                labelFont, valueFont);
        
        addReportRow(document, "Suspicious Keywords:", 
                analysis.getKeywordsDetected().isEmpty() ? "None" : analysis.getKeywordsDetected(), 
                labelFont, valueFont);

        document.add(new Paragraph("\n"));

        // Email Content
        Paragraph contentHeader = new Paragraph("Email Content:", labelFont);
        document.add(contentHeader);
        
        Paragraph content = new Paragraph(analysis.getContent(), valueFont);
        content.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(content);

        document.add(new Paragraph("\n"));

        // Footer
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);
        Paragraph footer = new Paragraph("This is an automated spam detection report. Review carefully before taking action.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();

        return outputStream.toByteArray();
    }

    private void addReportRow(Document document, String label, String value, Font labelFont, Font valueFont) throws DocumentException {
        Paragraph row = new Paragraph();
        row.add(new Chunk(label + " ", labelFont));
        row.add(new Chunk(value, valueFont));
        document.add(row);
    }
}
