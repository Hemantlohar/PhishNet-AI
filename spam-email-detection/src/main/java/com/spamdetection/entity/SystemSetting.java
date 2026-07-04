package com.spamdetection.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetting {
    @Id
    @Column(name = "setting_key")
    private String settingKey;
    
    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;
}
