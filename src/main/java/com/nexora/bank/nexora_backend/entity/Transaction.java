package com.nexora.bank.nexora_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // DEPOSIT, WITHDRAW, TRANSFER

    private double amount;

    private String senderEmail;

    private String receiverEmail;

    private LocalDateTime timestamp;

    private String senderAccountNumber;
    private String recieverAccountNumber;
    private String senderName;
    private String receiverName;

    public Transaction() {}

    public Transaction(String type, double amount, String senderEmail, String receiverEmail, String senderAccountNumber, String recieverAccountNumber, String senderName, String receiverName) {
        this.type = type;
        this.amount = amount;
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.timestamp = LocalDateTime.now();
        this.senderAccountNumber = senderAccountNumber;
        this.recieverAccountNumber = recieverAccountNumber;
        this.senderName = senderName;
        this.receiverName = receiverName;
    }

   public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getReceiverEmail() {
        return receiverEmail;
    }

    public void setReceiverEmail(String receiverEmail) {
        this.receiverEmail = receiverEmail;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderAccountNumber(){
        return senderAccountNumber;
    }
    public String getRecieverAccountNumber(){
        return recieverAccountNumber;
    }

    public String getSenderName(){
        return senderName;
    }

    public String getReceiverName(){
        return receiverName;
    }

}