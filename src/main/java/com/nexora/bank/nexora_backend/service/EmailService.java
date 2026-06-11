package com.nexora.bank.nexora_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import java.util.Random;
import org.springframework.mail.javamail.JavaMailSender;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // 1. Method: 6-digit Random OTP generate karne ke liye
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    // 2. Method: Email bhejne ke liye
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("your-email@gmail.com"); // SMTP wali email
        message.setTo(toEmail);
        message.setSubject("NEXORA SECURITY FREQUENCY");
        message.setText("Your access code for Nexora Terminal is: " + otp + "\n\nThis code will expire shortly.");
        mailSender.send(message);
    }
}
