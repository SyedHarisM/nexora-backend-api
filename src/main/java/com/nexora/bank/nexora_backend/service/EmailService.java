package com.nexora.bank.nexora_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import org.springframework.mail.javamail.JavaMailSender;

@Service
public class EmailService {

    /**
     * One-time codes are an authentication factor, so they must not come from a
     * predictable generator. java.util.Random is seeded from the clock and its
     * sequence can be reconstructed from a few observed outputs.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private JavaMailSender mailSender;

    // 1. Method: 6-digit OTP generate karne ke liye
    public String generateOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    // 2. Method: Email bhejne ke liye
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        // No explicit From: the configured SMTP account is the sender. The old
        // placeholder address was never a real mailbox.
        message.setTo(toEmail);
        message.setSubject("NEXORA SECURITY FREQUENCY");
        message.setText("Your access code for Nexora Terminal is: " + otp + "\n\nThis code will expire shortly.");
        mailSender.send(message);
    }
}
