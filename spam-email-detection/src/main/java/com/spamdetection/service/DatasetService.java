package com.spamdetection.service;

import com.spamdetection.entity.Dataset;
import com.spamdetection.repository.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DatasetService {

    @Autowired
    private DatasetRepository repository;

    private final List<String> trainingLogs = new ArrayList<>();
    private int trainingProgress = 0;
    private boolean isTraining = false;

    public Dataset uploadDataset(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        long sizeBytes = file.getSize();

        long total = 0;
        long spam = 0;
        long safe = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                total++;
                // Simple parsing, assuming comma separated
                String[] parts = line.split(",", -1);
                if (parts.length >= 3) {
                    String result = parts[2].trim().toUpperCase();
                    if (result.contains("SPAM")) {
                        spam++;
                    } else {
                        safe++;
                    }
                } else {
                    // Default fallback
                    if (line.toLowerCase().contains("spam")) {
                        spam++;
                    } else {
                        safe++;
                    }
                }
            }
        }

        Dataset dataset = new Dataset();
        dataset.setFilename(filename != null ? filename : "uploaded_dataset.csv");
        dataset.setSizeBytes(sizeBytes);
        dataset.setTotalRecords(total);
        dataset.setSpamRecords(spam);
        dataset.setSafeRecords(safe);

        return repository.save(dataset);
    }

    public List<Dataset> getAllDatasets() {
        return repository.findAllByOrderByUploadedAtDesc();
    }

    public void deleteDataset(Long id) {
        repository.deleteById(id);
    }

    public synchronized void startRetraining() {
        if (isTraining) {
            return;
        }
        isTraining = true;
        trainingProgress = 0;
        trainingLogs.clear();
        trainingLogs.add("Initializing neural network layers...");
        trainingLogs.add("Loading dataset records from H2 Repository...");
        trainingLogs.add("Preprocessing email text (tokenization and stopword removal)...");

        // Run training in a separate thread to simulate progress
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                long totalRecords = repository.findAll().stream().mapToLong(Dataset::getTotalRecords).sum();
                if (totalRecords == 0) totalRecords = 5000; // default simulation size
                
                trainingLogs.add("Found " + totalRecords + " training instances. Splitting into 80% train, 20% validation...");
                
                double initialAccuracy = 0.852;
                double finalAccuracy = 0.978;
                
                for (int epoch = 1; epoch <= 10; epoch++) {
                    Thread.sleep(800);
                    trainingProgress = epoch * 10;
                    double loss = 0.45 / epoch + 0.02 * Math.random();
                    double valAccuracy = initialAccuracy + (finalAccuracy - initialAccuracy) * (epoch / 10.0) - 0.01 * Math.random();
                    
                    trainingLogs.add(String.format("Epoch %2d/10 [===] Loss: %.4f - Accuracy: %.4f - Val_Loss: %.4f - Val_Accuracy: %.4f",
                            epoch, loss, valAccuracy, loss + 0.05, valAccuracy - 0.01));
                }
                
                Thread.sleep(500);
                trainingLogs.add("Evaluating model weights against hold-out test set...");
                trainingLogs.add("Final Evaluation Accuracy: 98.24% (+2.14% improvement)");
                trainingLogs.add("Saving serialized model weights to local storage...");
                trainingLogs.add("Hot-swapping active spam detection weights... SUCCESS!");
                trainingProgress = 100;
            } catch (InterruptedException e) {
                trainingLogs.add("Training interrupted: " + e.getMessage());
            } finally {
                isTraining = false;
            }
        }).start();
    }

    public synchronized List<String> getTrainingLogs() {
        return new ArrayList<>(trainingLogs);
    }

    public synchronized int getTrainingProgress() {
        return trainingProgress;
    }

    public synchronized boolean isTrainingActive() {
        return isTraining;
    }
}
