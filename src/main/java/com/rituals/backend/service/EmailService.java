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
            "— The Rituals Team ✨"
        );
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
            "— Rituals Contact System"
        );
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
            "— Your Rituals Companion ✨"
        );
        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("[EMAIL] Failed to send habit reminder to " + toEmail + ": " + e.getMessage());
        }
    }
}
