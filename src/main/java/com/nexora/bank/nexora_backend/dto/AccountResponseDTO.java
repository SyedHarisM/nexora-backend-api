package com.nexora.bank.nexora_backend.dto;

public class AccountResponseDTO {

    private String accountNumber;
    private double balance;

    public AccountResponseDTO(String accountNumber, double balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public String getAccountNumber(){
        return accountNumber;
    }

    public double getBalance(){
        return balance;
    }
}