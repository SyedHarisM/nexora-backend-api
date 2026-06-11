package com.nexora.bank.nexora_backend.controller;

import com.nexora.bank.nexora_backend.entity.VirtualCard;
import com.nexora.bank.nexora_backend.service.VirtualCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cards")
@CrossOrigin(origins = "*")
public class VirtualCardController {

    @Autowired
    private VirtualCardService cardService;

    @GetMapping("/create")
    public ResponseEntity<VirtualCard> createCard(@RequestParam String holderName) {
        return ResponseEntity.ok(cardService.createCardForUser(holderName));
    }

    @GetMapping("/details/{holderName}")
    public ResponseEntity<VirtualCard> getCardDetails(@PathVariable String holderName) {
        VirtualCard card = cardService.getCardByHolder(holderName);
        if (card != null) {
            return ResponseEntity.ok(card);
        }
        return ResponseEntity.notFound().build();
    }
}