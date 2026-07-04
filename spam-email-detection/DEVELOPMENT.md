# Spam Email Detection System - Development Guide

## Prerequisites

- Java Development Kit (JDK) 17 or higher
- Apache Maven 3.6 or higher
- MySQL Server 8.0 or higher
- Git (optional, for version control)

## Quick Start

### 1. Database Setup

```bash
# Open MySQL command line
mysql -u root -p

# Run the database setup script
source database-setup.sql;

# Verify tables were created
USE spam_detection_db;
SHOW TABLES;
```

### 2. Configure Database Connection

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/spam_detection_db
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

### 3. Build the Project

```bash
# Navigate to project directory
cd spam-email-detection

# Build using Maven
mvn clean install

# This will download all dependencies and compile the project
```

### 4. Run the Application

**Option 1: Using Maven**
```bash
mvn spring-boot:run
```

**Option 2: Using Java directly**
```bash
java -jar target/spam-email-detection-1.0.0.jar
```

**Option 3: In IDE (VS Code, IntelliJ)**
- Click "Run" on main class `SpamEmailDetectionApplication.java`

### 5. Access the Application

Open your browser and navigate to:
```
http://localhost:8080
```

## Default Credentials

After running `database-setup.sql`, you can login with:

**Admin Account:**
- Username: `admin`
- Email: `admin@example.com`
- Password: `admin123`

**Note**: Change the password immediately in a production environment!

## Development Workflow

### Adding New Features

1. **Create Entity Class**
   - Add to `src/main/java/com/spamdetection/entity/`
   - Annotate with `@Entity` and `@Table`

2. **Create Repository Interface**
   - Add to `src/main/java/com/spamdetection/repository/`
   - Extend `JpaRepository<Entity, ID>`

3. **Create Service Class**
   - Add to `src/main/java/com/spamdetection/service/`
   - Implement business logic

4. **Create Controller Class**
   - Add to `src/main/java/com/spamdetection/controller/`
   - Handle HTTP requests

5. **Create View Template**
   - Add HTML file to `src/main/resources/templates/`
   - Use Thymeleaf for dynamic content

### Testing Spam Detection

1. Go to `http://localhost:8080/analyze`
2. Enter email subject and content
3. Click "Analyze Email"
4. View results with spam score

**Test Email (SPAM):**
```
Subject: You Won Free Money!!!

Congratulations! You are the winner of $1,000,000!!! 
Click here now to claim your prize immediately! 
This is an urgent offer - act now!
```

**Test Email (LEGITIMATE):**
```
Subject: Meeting Reminder

Hi,

Just a friendly reminder about our meeting tomorrow at 2:00 PM.

Best regards,
John
```

## Project Configuration Files

### application.properties
- Database connection settings
- Thymeleaf configuration
- Logging levels
- Server port

### pom.xml
- Maven dependencies
- Plugin configurations
- Build settings

### SecurityConfig.java
- Spring Security configuration
- Authentication settings
- Authorization rules

## Debugging Tips

### Enable SQL Logging
In `application.properties`, set:
```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
```

### Check Application Logs
```bash
# View logs in real-time
tail -f logs/application.log

# Or check console output when running with mvn spring-boot:run
```

### Database Queries
```sql
-- Check all users
SELECT * FROM users;

-- Check all analyses
SELECT * FROM email_analysis ORDER BY created_at DESC;

-- Get user's analyses
SELECT * FROM email_analysis WHERE user_id = 1;
```

## Troubleshooting

### Port 8080 Already in Use
```properties
# In application.properties, change port:
server.port=8081
```

### MySQL Connection Error
```bash
# Verify MySQL is running
mysql -u root -p -e "SELECT 1;"

# Check credentials in application.properties
```

### Missing Dependencies
```bash
# Clear and rebuild
mvn clean
mvn install
```

### JAR File Not Found
```bash
# Rebuild the project
mvn clean package
```

## IDE Setup

### VS Code
1. Install Extension Pack for Java (Microsoft)
2. Open project folder
3. Click "Run" on main method

### IntelliJ IDEA
1. Open project
2. Configure SDK: File → Project Structure → SDK
3. Right-click `SpamEmailDetectionApplication.java` → Run

## Useful Maven Commands

```bash
# Clean build artifacts
mvn clean

# Compile project
mvn compile

# Run tests
mvn test

# Package as JAR
mvn package

# Skip tests during build
mvn clean package -DskipTests

# View dependency tree
mvn dependency:tree

# Update dependencies
mvn versions:display-dependency-updates
```

## File Structure Explanation

```
src/main/java/com/spamdetection/
├── controller/      # Handles HTTP requests
├── service/        # Business logic
├── entity/         # JPA entities
├── repository/     # Database access
├── config/         # Configuration classes
└── dto/            # Data transfer objects

src/main/resources/
├── templates/      # Thymeleaf HTML templates
├── static/         # CSS, JS, images
└── application.properties  # Configuration
```

## Next Steps

1. **Customize Spam Detection**
   - Edit `SpamDetectionService.java`
   - Add more keywords or rules

2. **Enhance UI**
   - Add Bootstrap components
   - Improve dashboard visualizations

3. **Add More Features**
   - Email forwarding
   - Batch analysis
   - API endpoints

4. **Production Deployment**
   - Configure environment variables
   - Set up HTTPS
   - Use production database
   - Configure logging

## Getting Help

- Check logs in the application console
- Review error messages carefully
- Verify all configurations are correct
- Consult README.md for API endpoints

---

**Happy Coding!** 🚀
