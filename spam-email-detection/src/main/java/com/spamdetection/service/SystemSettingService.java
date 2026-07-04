package com.spamdetection.service;

import com.spamdetection.entity.SystemSetting;
import com.spamdetection.repository.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class SystemSettingService {

    @Autowired
    private SystemSettingRepository repository;

    private static final Map<String, String> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put("system.name", "PhishNet AI Console");
        DEFAULTS.put("system.maintenance", "false");
        DEFAULTS.put("security.max_login_attempts", "5");
        DEFAULTS.put("security.enable_2fa", "false");
        DEFAULTS.put("security.session_timeout", "30");
        DEFAULTS.put("password.min_length", "8");
        DEFAULTS.put("password.require_special", "true");
        DEFAULTS.put("email.smtp_host", "smtp.example.com");
        DEFAULTS.put("email.smtp_port", "587");
        DEFAULTS.put("email.smtp_user", "alerts@spamdetection.local");
        DEFAULTS.put("email.smtp_pass", "********");
        DEFAULTS.put("backup.frequency", "Daily");
        DEFAULTS.put("backup.path", "./backups");
        DEFAULTS.put("ai.provider", "Google Gemini");
        DEFAULTS.put("ai.model", "gemini-2.5-flash");
        DEFAULTS.put("ai.api.key", ""); // Fallback is reading from application properties
    }

    public String getSetting(String key) {
        return repository.findById(key)
                .map(SystemSetting::getSettingValue)
                .orElse(DEFAULTS.getOrDefault(key, ""));
    }

    public String getSetting(String key, String fallbackValue) {
        return repository.findById(key)
                .map(SystemSetting::getSettingValue)
                .orElse(fallbackValue);
    }

    public boolean getSettingBoolean(String key, boolean fallbackValue) {
        String val = getSetting(key);
        if (val == null || val.isBlank()) {
            return fallbackValue;
        }
        return Boolean.parseBoolean(val);
    }

    public int getSettingInt(String key, int fallbackValue) {
        String val = getSetting(key);
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return fallbackValue;
        }
    }

    public void saveSetting(String key, String value) {
        SystemSetting setting = new SystemSetting(key, value);
        repository.save(setting);
    }
}
