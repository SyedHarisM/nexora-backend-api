package com.nexora.bank.nexora_backend.dto;

public class TransferRequestDTO {
    private String targetAccountNumber;
    private double amount;

    public TransferRequestDTO(String targetAccountNumber,  double amount){
        this.targetAccountNumber = targetAccountNumber;
        this.amount = amount;
    }

    public String getTargetAccountNumber(){
        return targetAccountNumber;
    }

    public double getAmount(){
        return amount;
    }

}
