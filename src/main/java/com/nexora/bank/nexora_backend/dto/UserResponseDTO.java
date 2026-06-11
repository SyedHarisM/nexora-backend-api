package com.nexora.bank.nexora_backend.dto;

public class UserResponseDTO {
    private long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String accountNumber;

    public UserResponseDTO(long id, String name, String email, String phoneNumber){
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public long getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public String getEmail(){
        return email;
    }

    public String getPhoneNumber(){
        return phoneNumber;
    }

   public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

}
