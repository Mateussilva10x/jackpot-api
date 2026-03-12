package com.worldJackpot.api.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ResendEmailService implements EmailService {

    private final Resend resend;

    @Value("${resend.api.key}")
    private String resendApiKey;

    public ResendEmailService(@Value("${resend.api.key}") String resendApiKey) {
        this.resend = new Resend(resendApiKey);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        if (resendApiKey == null || resendApiKey.isEmpty()) {
            log.warn("RESEND_API_KEY is not configured. Email will not be sent to {}", toEmail);
            return;
        }

        String htmlContent = buildEmailTemplate(resetLink);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("onboarding@resend.dev")
                .to(toEmail)
                .subject("World Jackpot - Redefinição de Senha")
                .html(htmlContent)
                .build();

        try {
            CreateEmailResponse data = resend.emails().send(params);
            log.info("Password reset email sent successfully to {}. Resend ID: {}", toEmail, data.getId());
        } catch (ResendException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildEmailTemplate(String resetLink) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; color: #333;\">"
                + "<h2>Redefinição de Senha</h2>"
                + "<p>Você solicitou a redefinição de sua senha no World Jackpot.</p>"
                + "<p>Clique no botão abaixo para criar uma nova senha:</p>"
                + "<a href=\"" + resetLink + "\" style=\"display: inline-block; padding: 10px 20px; color: white; background-color: #007bff; text-decoration: none; border-radius: 5px; margin-top: 15px;\">Redefinir Senha</a>"
                + "<p style=\"margin-top: 30px; font-size: 14px; color: #777;\">"
                + "Se você não solicitou esta alteração, ignore este e-mail."
                + "</p>"
                + "</div>";
    }
}
