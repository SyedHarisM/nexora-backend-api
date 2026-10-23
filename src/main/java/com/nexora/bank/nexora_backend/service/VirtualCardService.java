package com.nexora.bank.nexora_backend.service;

import com.nexora.bank.nexora_backend.dto.VirtualCardResponseDTO;
import com.nexora.bank.nexora_backend.dto.VirtualCardRevealDTO;
import com.nexora.bank.nexora_backend.entity.User;
import com.nexora.bank.nexora_backend.entity.VirtualCard;
import com.nexora.bank.nexora_backend.repository.UserRepository;
import com.nexora.bank.nexora_backend.repository.VirtualCardRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;

/**
 * Virtual card issuing and lookup.
 *
 * Every entry point resolves the caller first and works from the card owned by
 * that user. Cards used to be addressed by holder name, which is neither unique
 * nor secret; ownership is now the only way in.
 */
@Service
public class VirtualCardService {

    /** Card material must not come from a predictable generator. */
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VirtualCardRepository cardRepository;
    private final UserRepository userRepository;

    public VirtualCardService(VirtualCardRepository cardRepository,
                              UserRepository userRepository) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
    }

    /**
     * Issues a card to the given user, or returns the one they already hold.
     * The user is attached to the card, which previously never happened — cards
     * were saved with a null owner and could only ever be found by name.
     */
    public VirtualCard createCardForUser(User user) {

        return cardRepository.findByUserId(user.getId()).orElseGet(() -> {
            VirtualCard card = new VirtualCard();
            card.setUser(user);
            card.setHolderName(user.getName());
            card.setCardNumber(generateRandomDigits(16));
            card.setCvv(generateRandomDigits(3));
            card.setExpiryDate(LocalDate.now().plusYears(5));
            card.setFrozen(false);
            card.setDailyLimit(5000.0);
            return cardRepository.save(card);
        });
    }

    /** Card summary for the authenticated caller. Never includes full details. */
    public VirtualCardResponseDTO getCardForEmail(String email) {
        VirtualCard card = requireOwnCard(email);
        return toSummary(card);
    }

    /** Full card details for the authenticated owner, and only on request. */
    public VirtualCardRevealDTO revealCardForEmail(String email) {
        VirtualCard card = requireOwnCard(email);
        return new VirtualCardRevealDTO(
                card.getHolderName(),
                card.getCardNumber(),
                card.getCvv(),
                card.getExpiryDate());
    }

    /** Issues a card for the authenticated caller if they do not yet hold one. */
    public VirtualCardResponseDTO issueCardForEmail(String email) {
        return toSummary(createCardForUser(requireUser(email)));
    }

    private VirtualCardResponseDTO toSummary(VirtualCard card) {
        return new VirtualCardResponseDTO(
                card.getHolderName(),
                maskCardNumber(card.getCardNumber()),
                card.getExpiryDate(),
                card.isFrozen(),
                card.getDailyLimit());
    }

    private VirtualCard requireOwnCard(String email) {
        User user = requireUser(email);
        return cardRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Card not found"));
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /** Leaves the last four digits visible, which is all a card face needs. */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    private String generateRandomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
