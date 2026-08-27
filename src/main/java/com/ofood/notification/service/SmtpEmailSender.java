package com.ofood.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailSender implements EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailSender.class);
    
    private final JavaMailSender javaMailSender;

    public SmtpEmailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            // message.setFrom("..."); // Optional: if spring.mail.username is not used as default
            javaMailSender.send(message);
            logger.info("Successfully sent email to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
            // We log the error but don't rethrow to avoid rolling back transactions or breaking flows
        }
    }
}
