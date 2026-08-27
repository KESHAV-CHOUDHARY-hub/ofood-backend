package com.ofood.notification.service;

import com.ofood.notification.event.PasswordResetRequestedEvent;
import com.ofood.notification.event.UserRegisteredEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class NotificationListener {

    private final EmailSender emailSender;
    
    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public NotificationListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        String subject = "Welcome to Eatomics 🎉";
        String body = "Welcome to Eatomics 🎉 Your account has been created successfully.";
        
        emailSender.sendEmail(event.email(), subject, body);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordResetRequestedEvent(PasswordResetRequestedEvent event) {
        String subject = "Password Reset Request - Eatomics";
        
        // Ensure frontendBaseUrl doesn't end with a slash for clean URL construction
        String baseUrl = frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) : frontendBaseUrl;
        String resetUrl = baseUrl + "/reset-password?token=" + event.resetToken();
        
        String body = "You have requested to reset your password.\n\n" +
                "Click the link below to set a new password:\n" +
                resetUrl + "\n\n" +
                "If you did not request this, please ignore this email.";
                
        emailSender.sendEmail(event.email(), subject, body);
    }
}
