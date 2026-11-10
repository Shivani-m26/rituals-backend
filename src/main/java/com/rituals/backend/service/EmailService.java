package com.rituals.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.sender-name:Rituals ✨}")
    private String senderName;

    private boolean isMailConfigured() {
        return fromEmail != null && !fromEmail.isBlank();
    }

    private void sendMimeEmail(String toEmail, String subject, String bodyText, boolean isHtml) {
        if (!isMailConfigured()) {
            log.warn("[EMAIL] Outgoing email skipped for {}: MAIL_USERNAME is not configured.", toEmail);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(fromEmail, senderName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(bodyText, isHtml);

            mailSender.send(mimeMessage);
            log.info("[EMAIL] Successfully sent '{}' to {}", subject, toEmail);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send email to {}: {}", toEmail, e.getMessage());
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

        sendMimeEmail(toEmail, subject, body, false);
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

        sendMimeEmail(toEmail, subject, body, false);
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

        sendMimeEmail(toEmail, subject, body, false);
    }

    @Async
    public void sendHabitReminder(String toEmail, String username, String habitName, String time) {
        String subject = "Rituals Reminder - Time for " + habitName + " 🌟";
        String body = "Hey " + username + "! 👋\n\n" +
                "It's time for your ritual: " + habitName + "\n" +
                "Scheduled time: " + time + "\n\n" +
                "Remember, small daily improvements lead to stunning results!\n\n" +
                "— Your Rituals Companion ✨";

        sendMimeEmail(toEmail, subject, body, false);
    }

    @Async
    public void sendContactFormEmail(String fromName, String senderEmail, String fromPhone, String comments) {
        if (!isMailConfigured()) {
            log.warn("[EMAIL] Contact form submission could not be sent: MAIL_USERNAME is not configured.");
            return;
        }

        String subject = "Rituals - New Contact from " + fromName;
        String body = "📬 New contact form submission received:\n\n" +
                "Name: " + fromName + "\n" +
                "Email: " + senderEmail + "\n" +
                "Phone: " + fromPhone + "\n\n" +
                "Message:\n" + comments + "\n\n" +
                "— Rituals Contact System";

        sendMimeEmail(fromEmail, subject, body, false);
    }
}
