package com.worldJackpot.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SupabaseAuthService {

    private final String supabaseUrl;
    private final String supabaseKey;
    private final RestTemplate restTemplate;

    public SupabaseAuthService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.key}") String supabaseKey) {
        this.supabaseUrl = supabaseUrl;
        this.supabaseKey = supabaseKey;
        this.restTemplate = new RestTemplate();
    }

    public void sendResetPasswordEmail(String email, String resetLink) {
        sendResetPasswordEmail(email, resetLink, null);
    }

    public void sendResetPasswordEmail(String email, String resetLink, String userName) {
        if (supabaseUrl == null || supabaseUrl.isEmpty() || supabaseKey == null || supabaseKey.isEmpty()) {
            log.error("Supabase configuration is missing (SUPABASE_URL or SUPABASE_KEY). Cannot send reset password email to {}", email);
            throw new RuntimeException("Supabase configuration is missing. Cannot send reset password email.");
        }

        String url = supabaseUrl + "/functions/v1/send-reset-password-email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + supabaseKey);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("resetLink", resetLink);
        if (userName != null) {
            body.put("userName", userName);
        }

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Reset password email sent successfully via Supabase Edge Function for {}", email);
            } else {
                log.error("Failed to send reset password email via Supabase Edge Function: {}", response.getBody());
                throw new RuntimeException("Failed to send reset password email");
            }
        } catch (Exception e) {
            log.error("Exception while calling Supabase Edge Function for {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Error communicating with Supabase Edge Function", e);
        }
    }
}
