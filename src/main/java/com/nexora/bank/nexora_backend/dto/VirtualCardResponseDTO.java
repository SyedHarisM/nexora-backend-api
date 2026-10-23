package com.nexora.bank.nexora_backend.dto;

import java.time.LocalDate;

/**
 * Default card representation returned to clients.
 *
 * Carries only what a card list or card face needs. The full number and the
 * verification code are deliberately absent: returning the entity exposed both
 * on every read, which made an ordinary card fetch a disclosure of the two
 * values that matter most.
 */
public class VirtualCardResponseDTO {

    private final String holderName;
    private final String maskedCardNumber;
    private final LocalDate expiryDate;
    private final boolean frozen;
    private final double dailyLimit;

    public VirtualCardResponseDTO(String holderName,
                                  String maskedCardNumber,
                                  LocalDate expiryDate,
                                  boolean frozen,
                                  double dailyLimit) {
        this.holderName = holderName;
        this.maskedCardNumber = maskedCardNumber;
        this.expiryDate = expiryDate;
        this.frozen = frozen;
        this.dailyLimit = dailyLimit;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public double getDailyLimit() {
        return dailyLimit;
    }
}
