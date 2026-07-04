package com.spamdetection;

import com.spamdetection.service.UserService;
import com.spamdetection.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseRestartPersistenceTest {

    @Test
    void dataRemainsAvailableAfterApplicationRestart() throws IOException {
        Path databaseBase = Path.of("target", "restart-test-db", "spam-persistence").toAbsolutePath();
        Files.createDirectories(databaseBase.getParent());
        Files.deleteIfExists(Path.of(databaseBase + ".mv.db"));
        Files.deleteIfExists(Path.of(databaseBase + ".trace.db"));

        String jdbcUrl = "jdbc:h2:file:" + databaseBase.toString().replace('\\', '/') + ";MODE=MySQL;AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1";

        String uniqueUsername = "restartuser_" + System.currentTimeMillis();
        try (ConfigurableApplicationContext firstContext = startContext(jdbcUrl)) {
            UserService userService = firstContext.getBean(UserService.class);
            userService.registerUser(uniqueUsername, uniqueUsername + "@example.com", "Password123!");
        }

        try (ConfigurableApplicationContext secondContext = startContext(jdbcUrl)) {
            UserRepository userRepository = secondContext.getBean(UserRepository.class);
            assertThat(userRepository.findByUsername(uniqueUsername)).isPresent();
        }
    }

    private ConfigurableApplicationContext startContext(String jdbcUrl) {
        return new SpringApplicationBuilder(SpamEmailDetectionApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.profiles.active=test",
                        "spring.datasource.url=" + jdbcUrl,
                        "spring.datasource.username=sa",
                        "spring.datasource.password=",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "spring.jpa.hibernate.ddl-auto=update",
                        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
                        "spring.main.banner-mode=off"
                )
                .run();
    }
}