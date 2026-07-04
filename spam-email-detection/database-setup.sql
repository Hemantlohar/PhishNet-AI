-- Create Database
CREATE DATABASE IF NOT EXISTS spam_detection_db;
USE spam_detection_db;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Email Analysis Table
CREATE TABLE IF NOT EXISTS email_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subject VARCHAR(500) NOT NULL,
    content LONGTEXT NOT NULL,
    result VARCHAR(50) NOT NULL,
    spam_score DOUBLE NOT NULL,
    keywords_detected TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_result (result)
);

-- Sample Admin User (password: admin123)
-- BCrypt Hash: $2a$10$3gE9G9H5B0X5H0X5H0X5H0X5H0X5H0X5H0X5H0X5H0X5H0X5H0X5H
INSERT INTO users (username, email, password, role) VALUES 
('admin', 'admin@example.com', 'admin', 'ROLE_ADMIN')
ON DUPLICATE KEY UPDATE email = email;

-- Create stored procedure for getting dashboard statistics
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS GetDashboardStats()
BEGIN
    SELECT 
        COUNT(*) as total_analyses,
        SUM(CASE WHEN result = 'SPAM' THEN 1 ELSE 0 END) as spam_count,
        SUM(CASE WHEN result = 'NOT_SPAM' THEN 1 ELSE 0 END) as not_spam_count,
        AVG(spam_score) as avg_spam_score;
END //
DELIMITER ;
