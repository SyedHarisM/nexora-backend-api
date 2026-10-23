package com.nexora.bank.nexora_backend.dto;

import java.time.LocalDate;

/**
 * Full card details, returned only from the explicit reveal endpoint and only
 * to the authenticated owner of the card.
 *
 * Kept as a separate type from {@link VirtualCardResponseDTO} so that revealing
 * a card is always a deliberate call rather than something an ordinary read can
 * drift into returning.
 */
public class VirtualCardRevealDTO {

    private final String holderName;
    private final String cardNumber;
    private final String cvv;
    private final LocalDate expiryDate;

    public VirtualCardRevealDTO(String holderName,
                                String cardNumber,
                                String cvv,
                                LocalDate expiryDate) {
        this.holderName = holderName;
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expiryDate = expiryDate;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getCvv() {
        return cvv;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }
}
