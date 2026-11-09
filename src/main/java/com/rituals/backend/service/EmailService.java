package com.rituals.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendWelcomeEmail(String toEmail, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Welcome to Rituals! ✨ Begin Your Journey");
        message.setText(
                "Welcome to Rituals, " + username + "! 🌟\n\n" +
                        "We're thrilled to have you here. Rituals is your mindful sanctuary for building transformative habits, daily journaling, and tracking personal growth.\n\n"
                        +
                        "Here's what you can explore today:\n" +
                        "🌱 Habits Tracker: Build streaks and track daily routines\n" +
                        "📖 Journal Sanctuary: Reflect and capture thoughts with custom styled sheets\n" +
                        "🎯 Interactive Todos: Stay focused on meaningful goals\n" +
                        "🏆 Community Leaderboard: Earn badges and celebrate milestones\n\n" +
                        "Your journey to a more intentional life starts now.\n\n" +
                        "With warmth,\n" +
                        "— The Rituals Team ✨");
        try {
            mailSender.send(message);
            System.out.println("[EMAIL] Welcome email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.out.println(
                    "[EMAIL] Notice: Welcome email could not be sent to " + toEmail + " (" + e.getMessage() + ")");
        }
    }

    @Async
    public void sendLoginAlertEmail(String toEmail, String username, java.time.LocalDateTime previousLogin) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Rituals - Welcome Back & Login Notice 🔔");

        String previousTimeStr = (previousLogin != null)
                ? previousLogin.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"))
                : "over 7 days ago";

        message.setText(
                "Hey " + username + "! 👋\n\n" +
                        "We noticed a new login to your Rituals account.\n\n" +
                        "It has been a while since your last visit (last active: " + previousTimeStr
                        + "). We're so glad you're back to nurture your rituals and reflections!\n\n" +
                        "🛡️ Security Note:\n" +
                        "If this wasn't you, please reset your password immediately via the 'Lost Your Key?' option on the login page to keep your account safe.\n\n"
                        +
                        "— The Rituals Team ✨");
        try {
            mailSender.send(message);
            System.out.println("[EMAIL] Login notice email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.out.println(
                    "[EMAIL] Notice: Login alert email could not be sent to " + toEmail + " (" + e.getMessage() + ")");
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Rituals - Reset Your Password 🔑");
        message.setText(
                "Hello Ritualist! ✨\n\n" +
                        "You requested a password reset for your Rituals account.\n" +
                        "Use the following token to reset your password:\n\n" +
                        "🔑 Reset Token: " + resetToken + "\n\n" +
                        "⏳ This token expires in 1 hour.\n\n" +
                        "Steps to reset:\n" +
                        "1. Go to the Rituals app\n" +
                        "2. Click 'Lost Your Key?' on the login page\n" +
                        "3. Enter your email, the token above, and your new password\n\n" +
                        "If you did not request this reset, please ignore this email.\n\n" +
                        "— The Rituals Team ✨");
        try {
            mailSender.send(message);
            System.out.println("[EMAIL] Password reset email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.out.println("[EMAIL] Failed to send password reset email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendContactFormEmail(String fromName, String senderEmail, String fromPhone, String comments) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(fromEmail); // Send to the app owner's Gmail
        message.setReplyTo(senderEmail);
        message.setSubject("Rituals - New Contact from " + fromName);
        message.setText(
                "📬 New contact form submission received:\n\n" +
                        "Name: " + fromName + "\n" +
                        "Email: " + senderEmail + "\n" +
                        "Phone: " + fromPhone + "\n\n" +
                        "Message:\n" + comments + "\n\n" +
                        "— Rituals Contact System");
        try {
            mailSender.send(message);
            System.out.println("[EMAIL] Contact form email sent from " + senderEmail);
        } catch (Exception e) {
            System.out.println("[EMAIL] Failed to send contact form email: " + e.getMessage());
        }
    }

    @Async
    public void sendHabitReminder(String toEmail, String username, String habitName, String time) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Rituals Reminder - Time for " + habitName + " 🌟");
        message.setText(
                "Hey " + username + "! 👋\n\n" +
                        "It's time for your ritual: " + habitName + "\n" +
                        "Scheduled time: " + time + "\n\n" +
                        "Remember, small daily improvements lead to stunning results!\n\n" +
                        "— Your Rituals Companion ✨");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("[EMAIL] Failed to send habit reminder to " + toEmail + ": " + e.getMessage());
        }
    }
}
