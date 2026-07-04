package com.spamdetection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.dao.DataAccessException;
import com.spamdetection.config.CustomUserDetailsService;
import com.spamdetection.service.NotificationService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler(org.springframework.context.ApplicationContext context) {
        return (request, response, exception) -> {
            String code = "invalid";
            String username = request.getParameter("username");

            if (exception instanceof UsernameNotFoundException) {
                code = "notfound";
            } else if (exception instanceof BadCredentialsException) {
                code = "invalid";
            } else if (exception.getCause() instanceof DataAccessException) {
                code = "db";
            }

            try {
                NotificationService notificationService = context.getBean(NotificationService.class);
                String msg = "Suspicious login attempt for username: '" + (username != null ? username : "unknown") + 
                             "' - Reason: " + exception.getMessage();
                notificationService.createNotification("SUSPICIOUS_LOGIN", msg);
            } catch (Exception ex) {
                // Ignore if bean is not resolved
            }

            response.sendRedirect(request.getContextPath() + "/login?error=" + code);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider, AuthenticationFailureHandler authenticationFailureHandler) throws Exception {
        http
            .authenticationProvider(authenticationProvider)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/register", "/login", "/css/**", "/js/**", "/403").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .failureHandler(authenticationFailureHandler)
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(handling -> handling
                .accessDeniedPage("/403")
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
