package com.rituals.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Rituals - Reset Your Password");
        message.setText(
            "Hello Ritualist!\n\n" +
            "You requested a password reset. Use the following token to reset your password:\n\n" +
            "Reset Token: " + resetToken + "\n\n" +
            "This token expires in 1 hour.\n\n" +
            "If you did not request this reset, please ignore this email.\n\n" +
            "— The Rituals Team ✨"
        );
        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("[EMAIL] Failed to send password reset email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendContactFormEmail(String fromName, String fromEmail, String fromPhone, String comments) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("shivanimanivannan1@gmail.com");
        message.setSubject("Rituals - New Contact Form Submission");
        message.setText(
            "New contact form submission received:\n\n" +
            "Name: " + fromName + "\n" +
            "Email: " + fromEmail + "\n" +
            "Phone: " + fromPhone + "\n\n" +
            "Message:\n" + comments + "\n\n" +
            "— Rituals Contact System"
        );
        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("[EMAIL] Failed to send contact form email: " + e.getMessage());
        }
    }

    @Async
    public void sendHabitReminder(String toEmail, String username, String habitName, String time) {
        SimpleMailMessage message = new SimpleMailMessage();
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
