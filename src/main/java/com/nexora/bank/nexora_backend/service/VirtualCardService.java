package com.nexora.bank.nexora_backend.service;

import com.nexora.bank.nexora_backend.entity.VirtualCard;
import com.nexora.bank.nexora_backend.repository.VirtualCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Random;

@Service
public class VirtualCardService {

    @Autowired
    private VirtualCardRepository cardRepository;

public VirtualCard createCardForUser(String holderName) {
    VirtualCard card = new VirtualCard();
    card.setHolderName(holderName);
    card.setCardNumber(generateRandomDigits(16));
    card.setCvv(generateRandomDigits(3));
    card.setExpiryDate(LocalDate.now().plusYears(5));
    card.setFrozen(false);
    card.setDailyLimit(5000.0);

    return cardRepository.save(card);
}

    private String generateRandomDigits(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public VirtualCard getCardByHolder(String holderName) {
        return cardRepository.findByHolderName(holderName).orElse(null);
    }
}