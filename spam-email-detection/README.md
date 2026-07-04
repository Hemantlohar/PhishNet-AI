# Spam Email Detection and Analysis System

A Spring Boot web application designed to detect and analyze spam emails using rule-based detection methods. The system provides a comprehensive analysis with spam scoring, keyword detection, and PDF report generation.

## Project Overview

The Spam Email Detection and Analysis System helps users identify whether an email is **Spam** or **Not Spam** by analyzing email content and subject lines using predefined rules. The application stores analysis history and generates detailed PDF reports.

## Features

### Core Features
- 📧 **Email Analysis**: Analyze email subject and content for spam characteristics
- 🔐 **User Authentication**: Secure login and registration with password encryption (BCrypt)
- 📊 **Dashboard**: Visual statistics with pie and bar charts
- 📋 **History Management**: View and search all email analyses
- 📄 **PDF Report Generation**: Download detailed analysis reports
- 👥 **Role-Based Access Control**: Separate dashboards for Admin and User roles

### Spam Detection Features
- 🎯 **Spam Keywords Detection**: Identify suspicious keywords
- 📈 **Spam Score Calculation**: Quantifiable spam probability (0-100%)
- 🔤 **Text Pattern Analysis**: Detect excessive caps, punctuation, URLs
- 📧 **Email Count Detection**: Identify suspicious email addresses

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.0
- **Security**: Spring Security + BCrypt
- **ORM**: Spring Data JPA
- **Database**: MySQL 8.0
- **PDF Generation**: iText 5.5.13

### Frontend
- **Template Engine**: Thymeleaf
- **Styling**: Bootstrap 5.3
- **Charts**: Chart.js 4.4
- **Responsive Design**: Mobile-friendly UI

### Database
- **MySQL 8.0+**
- **Tables**: users, email_analysis

## Project Structure

```
spam-email-detection/
├── src/
│   ├── main/
│   │   ├── java/com/spamdetection/
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── DashboardController.java
│   │   │   │   ├── EmailAnalysisController.java
│   │   │   │   └── ReportController.java
│   │   │   ├── service/
│   │   │   │   ├── UserService.java
│   │   │   │   ├── EmailAnalysisService.java
│   │   │   │   ├── SpamDetectionService.java
│   │   │   │   └── PDFReportService.java
│   │   │   ├── entity/
│   │   │   │   ├── User.java
│   │   │   │   └── EmailAnalysis.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── EmailAnalysisRepository.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── CustomUserDetailsService.java
│   │   │   ├── dto/
│   │   │   │   ├── EmailAnalysisRequest.java
│   │   │   │   ├── EmailAnalysisResponse.java
│   │   │   │   └── UserRegistrationRequest.java
│   │   │   └── SpamEmailDetectionApplication.java
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── login.html
│   │       │   ├── register.html
│   │       │   ├── dashboard-user.html
│   │       │   ├── dashboard-admin.html
│   │       │   ├── analyze.html
│   │       │   ├── history.html
│   │       │   └── analysis-detail.html
│   │       ├── static/
│   │       │   ├── css/
│   │       │   └── js/
│   │       └── application.properties
├── pom.xml
└── README.md
```

## Setup Instructions

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+

### Installation Steps

1. **Create Database**
   ```sql
   CREATE DATABASE spam_detection_db;
   ```

2. **Configure Database Connection**
   
   Edit `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/spam_detection_db
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

3. **Build the Project**
   ```bash
   mvn clean install
   ```

4. **Run the Application**
   ```bash
   mvn spring-boot:run
   ```
   
   Or using Java:
   ```bash
   java -jar target/spam-email-detection-1.0.0.jar
   ```

5. **Access the Application**
   - Open browser and navigate to: `http://localhost:8080`

## Usage Guide

### First-Time Setup

1. **Create Admin User**
   - Register a user with username and email
   - Manually update the role in database:
     ```sql
     UPDATE users SET role = 'ROLE_ADMIN' WHERE username = 'admin_username';
     ```

2. **Login**
   - Use your credentials to login

### User Workflows

#### As a Regular User
1. Go to "Analyze Email"
2. Paste email subject and content
3. Click "Analyze Email"
4. View results with spam score and detected keywords
5. Access "History" to view all analyses
6. Download PDF reports for any analysis

#### As an Administrator
1. View system-wide statistics on the admin dashboard
2. See pie charts and bar charts of spam distribution
3. Access all user analyses
4. View and analyze emails same as regular users

## Spam Detection Algorithm

The system uses rule-based spam detection with the following factors:

### Keywords Detection (High Priority)
- Suspicious phrases: "free", "winner", "urgent", "click here", etc.
- Financial terms: "wire transfer", "bank account", "credit card"
- Phishing indicators: "verify account", "confirm identity"

### Text Analysis
- **Excessive Punctuation**: Multiple exclamation marks or question marks (>5)
- **Excessive Caps**: Text with >70% uppercase letters
- **URL Detection**: Presence of HTTP/HTTPS URLs
- **Email Count**: Multiple email addresses in content

### Scoring
- Each spam indicator adds points (0-100 scale)
- Final score ≥ 50% classified as SPAM
- Score < 50% classified as NOT SPAM

## API Endpoints

### Authentication
- `GET /login` - Login page
- `POST /login` - Submit login credentials
- `GET /register` - Registration page
- `POST /register` - Create new account

### Dashboard
- `GET /dashboard` - View dashboard (user/admin based on role)

### Email Analysis
- `GET /analyze` - Analyze email form
- `POST /analyze` - Submit email for analysis
- `GET /analysis/{id}` - View analysis details

### History
- `GET /history` - View all user analyses

### Reports
- `GET /report/pdf/{id}` - Download PDF report

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL
);
```

### Email Analysis Table
```sql
CREATE TABLE email_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    subject VARCHAR(500) NOT NULL,
    content LONGTEXT NOT NULL,
    result VARCHAR(50) NOT NULL,
    spam_score DOUBLE NOT NULL,
    keywords_detected TEXT,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## Security Features

- ✅ BCrypt password hashing
- ✅ Spring Security authentication
- ✅ Role-based access control
- ✅ Session management
- ✅ CSRF protection
- ✅ Secure password storage
- ✅ User authorization checks

## Performance

- Analysis results generated within 2 seconds
- Optimized database queries with JPA
- Lazy loading for large datasets
- Efficient spam detection algorithm

## Success Criteria

✅ Users can analyze email content  
✅ System classifies Spam/Not Spam  
✅ Results stored in database  
✅ Dashboard displays statistics  
✅ PDF reports can be generated  
✅ Login and role-based access work correctly  

## Future Enhancements

- Machine learning-based spam detection
- Advanced user search filters
- Batch email analysis
- Email forwarding integration
- Real-time notifications
- API for third-party integrations

## Support & Troubleshooting

### Common Issues

**Q: Database connection error**
- A: Verify MySQL is running and credentials in `application.properties` are correct

**Q: Port 8080 already in use**
- A: Change port in `application.properties`: `server.port=8081`

**Q: Tables not created**
- A: Ensure `spring.jpa.hibernate.ddl-auto=update` is set in `application.properties`

## License

This project is provided as-is for educational and development purposes.

## Contributors

Developed as per the Spam Email Detection and Analysis System Product Requirements Document.

---

**Last Updated**: 2024-01-01  
**Version**: 1.0.0
