package com.spamdetection;

import com.spamdetection.entity.User;
import com.spamdetection.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.spamdetection.repository.EmailAnalysisRepository emailAnalysisRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        emailAnalysisRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registrationStoresHashedPasswordInDatabase() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "alice")
                        .param("email", "alice@gmail.com")
                        .param("password", "Password123!")
                        .param("confirmPassword", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));

        User savedUser = userRepository.findByUsername("alice").orElseThrow();
        assertThat(savedUser.getEmail()).isEqualTo("alice@gmail.com");
        assertThat(savedUser.getPassword()).isNotEqualTo("Password123!");
        assertThat(passwordEncoder.matches("Password123!", savedUser.getPassword())).isTrue();
    }

    @Test
    void invalidEmailFormatIsRejected() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "alice")
                        .param("email", "alicegmail.com")
                        .param("password", "Password123!")
                        .param("confirmPassword", "Password123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        assertThat(userRepository.findByUsername("alice")).isEmpty();
    }

    @Test
    void duplicateEmailRegistrationIsBlocked() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "alice")
                        .param("email", "alice@gmail.com")
                        .param("password", "Password123!")
                        .param("confirmPassword", "Password123!"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/register")
                        .param("username", "alice2")
                        .param("email", "alice@gmail.com")
                        .param("password", "Password123!")
                        .param("confirmPassword", "Password123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void registeredUserCanLogIn() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "bob")
                        .param("email", "bob@outlook.in")
                        .param("password", "Password123!")
                        .param("confirmPassword", "Password123!"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/login")
                        .param("username", "bob")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void unknownAccountIsRejectedOnLogin() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "missing")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=notfound"));
    }
}