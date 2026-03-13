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
    private final String frontendUrl;
    private final RestTemplate restTemplate;

    public SupabaseAuthService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.key}") String supabaseKey,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.supabaseUrl = supabaseUrl;
        this.supabaseKey = supabaseKey;
        this.frontendUrl = frontendUrl;
        this.restTemplate = new RestTemplate();
    }

    public void sendRecoveryEmail(String email) {
        if (supabaseUrl == null || supabaseUrl.isEmpty() || supabaseKey == null || supabaseKey.isEmpty()) {
            log.warn("Supabase configuration is missing. Cannot send recovery email to {}", email);
            return;
        }

        String url = supabaseUrl + "/auth/v1/recover";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseKey);
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.set("Redirect-To", frontendUrl + "/reset-password");

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Recovery email requested successfully from Supabase for {}", email);
            } else {
                log.error("Failed to request recovery email from Supabase: {}", response.getBody());
                throw new RuntimeException("Failed to send recovery email via Supabase");
            }
        } catch (Exception e) {
            log.error("Exception while calling Supabase recover endpoint for {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Error communicating with Supabase", e);
        }
    }
}
