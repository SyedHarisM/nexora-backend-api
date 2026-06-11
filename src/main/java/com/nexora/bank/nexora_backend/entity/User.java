package com.nexora.bank.nexora_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Account account;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private VirtualCard virtualCard; 

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    @Size(min = 3, message = "SYSTEM ERROR: Identity alias too short. (Min 3 chars)")
    private String name;

    @Column(nullable = false, unique = true)
    @Email(message = "PROTOCOL BREACH: Digital coordinate format invalid.")
    @Pattern(
        regexp = "^[A-Za-z0-9+_.-]+@(gmail\\.com|yahoo\\.com|hotmail\\.com|outlook\\.com|icloud\\.com)$",
        message = "ACCESS DENIED: Unverified email provider."
    )
    private String email;

    @Column(nullable = false)
    @Size(min = 6, message = "SYSTEM ERROR: Key must be at least 6 digits.")
    private String password;

    @Size(min = 11, max = 11, message = "COMMUNICATION ERROR: Frequency must be exactly 11 digits.")
    private String phoneNumber;

    @Column(name = "otp")
    private String otp;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    // CORE INJECT: Avatar track system field
    @Column(name = "avatar_url")
    private String avatarUrl;

    public User() {}
    
    // --- GETTERS & SETTERS CORE INTERFACES ---
    
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber){
        this.phoneNumber = phoneNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole(){return role;}

    public void setRole(Role role){this.role = role;}

    public Account getAccount() {
        return account;
    }
}