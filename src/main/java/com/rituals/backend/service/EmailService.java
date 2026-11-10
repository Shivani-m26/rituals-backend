package com.rituals.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from:Rituals <onboarding@resend.dev>}")
    private String resendFrom;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.sender-name:Rituals ✨}")
    private String senderName;

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Dispatch email using HTTPS REST API (Resend / Brevo) first to bypass cloud SMTP blocks.
     * Falls back to standard JavaMail SMTP if no API keys are provided.
     */
    private void dispatchEmail(String toEmail, String subject, String bodyText) {
        // Priority 1: Resend HTTPS API (Port 443 - Never blocked)
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            boolean success = sendViaResend(toEmail, subject, bodyText);
            if (success) return;
        }

        // Priority 2: Brevo HTTPS API (Port 443 - Never blocked)
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            boolean success = sendViaBrevo(toEmail, subject, bodyText);
            if (success) return;
        }

        // Priority 3: Fall back to SMTP (port 587)
        sendViaSmtp(toEmail, subject, bodyText);
    }

    private boolean sendViaResend(String toEmail, String subject, String bodyText) {
        try {
            String json = "{"
                    + "\"from\":\"" + escapeJson(resendFrom) + "\","
                    + "\"to\":[\"" + escapeJson(toEmail) + "\"],"
                    + "\"subject\":\"" + escapeJson(subject) + "\","
                    + "\"text\":\"" + escapeJson(bodyText) + "\""
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[EMAIL-RESEND] Successfully dispatched '{}' to {}", subject, toEmail);
                return true;
            } else {
                log.error("[EMAIL-RESEND] API error {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("[EMAIL-RESEND] Failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean sendViaBrevo(String toEmail, String subject, String bodyText) {
        try {
            String senderEmail = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "contact@rituals.app";
            String json = "{"
                    + "\"sender\":{\"name\":\"" + escapeJson(senderName) + "\",\"email\":\"" + escapeJson(senderEmail) + "\"},"
                    + "\"to\":[{\"email\":\"" + escapeJson(toEmail) + "\"}],"
                    + "\"subject\":\"" + escapeJson(subject) + "\","
                    + "\"textContent\":\"" + escapeJson(bodyText) + "\""
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[EMAIL-BREVO] Successfully dispatched '{}' to {}", subject, toEmail);
                return true;
            } else {
                log.error("[EMAIL-BREVO] API error {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("[EMAIL-BREVO] Failed: {}", e.getMessage());
            return false;
        }
    }

    private void sendViaSmtp(String toEmail, String subject, String bodyText) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("[EMAIL-SMTP] Skipped: No mail configuration found.");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(fromEmail, senderName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(bodyText, false);

            mailSender.send(mimeMessage);
            log.info("[EMAIL-SMTP] Successfully sent '{}' to {}", subject, toEmail);
        } catch (Exception e) {
            log.error("[EMAIL-SMTP] Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String username) {
        String subject = "Welcome to Rituals! ✨ Begin Your Journey";
        String body = "Welcome to Rituals, " + username + "! 🌟\n\n" +
                "We're thrilled to have you here. Rituals is your mindful sanctuary for building transformative habits, daily journaling, and tracking personal growth.\n\n" +
                "Here's what you can explore today:\n" +
                "🌱 Habits Tracker: Build streaks and track daily routines\n" +
                "📖 Journal Sanctuary: Reflect and capture thoughts with custom styled sheets\n" +
                "🎯 Interactive Todos: Stay focused on meaningful goals\n" +
                "🏆 Community Leaderboard: Earn badges and celebrate milestones\n\n" +
                "Your journey to a more intentional life starts now.\n\n" +
                "With warmth,\n" +
                "— The Rituals Team ✨";

        dispatchEmail(toEmail, subject, body);
    }

    @Async
    public void sendLoginAlertEmail(String toEmail, String username, LocalDateTime previousLogin) {
        String subject = "Rituals - Welcome Back & Login Notice 🔔";
        String previousTimeStr = (previousLogin != null)
                ? previousLogin.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"))
                : "over 7 days ago";

        String body = "Hey " + username + "! 👋\n\n" +
                "We noticed a new login to your Rituals account.\n\n" +
                "It has been a while since your last visit (last active: " + previousTimeStr + "). We're so glad you're back to nurture your rituals and reflections!\n\n" +
                "🛡️ Security Note:\n" +
                "If this wasn't you, please reset your password immediately via the 'Lost Your Key?' option on the login page to keep your account safe.\n\n" +
                "— The Rituals Team ✨";

        dispatchEmail(toEmail, subject, body);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String subject = "Rituals - Reset Your Password 🔑";
        String body = "Hello Ritualist! ✨\n\n" +
                "You requested a password reset for your Rituals account.\n" +
                "Use the following token to reset your password:\n\n" +
                "🔑 Reset Token: " + resetToken + "\n\n" +
                "⏳ This token expires in 1 hour.\n\n" +
                "Steps to reset:\n" +
                "1. Go to the Rituals app\n" +
                "2. Click 'Lost Your Key?' on the login page\n" +
                "3. Enter your email, the token above, and your new password\n\n" +
                "If you did not request this reset, please ignore this email.\n\n" +
                "— The Rituals Team ✨";

        dispatchEmail(toEmail, subject, body);
    }

    @Async
    public void sendHabitReminder(String toEmail, String username, String habitName, String time) {
        String subject = "Rituals Reminder - Time for " + habitName + " 🌟";
        String body = "Hey " + username + "! 👋\n\n" +
                "It's time for your ritual: " + habitName + "\n" +
                "Scheduled time: " + time + "\n\n" +
                "Remember, small daily improvements lead to stunning results!\n\n" +
                "— Your Rituals Companion ✨";

        dispatchEmail(toEmail, subject, body);
    }

    @Async
    public void sendContactFormEmail(String fromName, String senderEmail, String fromPhone, String comments) {
        String subject = "Rituals - New Contact from " + fromName;
        String body = "📬 New contact form submission received:\n\n" +
                "Name: " + fromName + "\n" +
                "Email: " + senderEmail + "\n" +
                "Phone: " + fromPhone + "\n\n" +
                "Message:\n" + comments + "\n\n" +
                "— Rituals Contact System";

        String targetAdmin = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "contact@rituals.app";
        dispatchEmail(targetAdmin, subject, body);
    }
}
