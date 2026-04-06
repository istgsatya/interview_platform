package com.example.interviewgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF needs to be disabled for REST APIs and WebSockets
                .csrf(csrf -> csrf.disable())
                // Allow all requests to pass through without a password for now
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws/**").permitAll() // Explicitly open the WebSocket tunnel
                        .requestMatchers("/api/interview/**").permitAll()
                        .anyRequest().permitAll()              // Open standard HTTP endpoints for testing
                );

        return http.build();
    }
}