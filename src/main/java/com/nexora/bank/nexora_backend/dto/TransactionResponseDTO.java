package com.nexora.bank.nexora_backend.dto;

import java.time.LocalDateTime;

public class TransactionResponseDTO {

    private String type;
    private double amount;
    private String senderEmail;
    private String receiverEmail;
    private LocalDateTime timestamp;

    public TransactionResponseDTO(String type, double amount,
                                  String senderEmail, String receiverEmail,
                                  LocalDateTime timestamp) {
        this.type = type;
        this.amount = amount;
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.timestamp = timestamp;
    }

    public String getType(){
        return type;
    }

    public double getAmount(){
        return amount;
    }

    public String getSenderEmail(){
        return senderEmail;
    }

    public String getReceiverEmail(){
        return receiverEmail;
    }

    public LocalDateTime getTimestamp(){
        return timestamp;
    }
}