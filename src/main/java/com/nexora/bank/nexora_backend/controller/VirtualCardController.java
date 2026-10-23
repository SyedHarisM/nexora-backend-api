package com.nexora.bank.nexora_backend.controller;

import com.nexora.bank.nexora_backend.dto.VirtualCardResponseDTO;
import com.nexora.bank.nexora_backend.dto.VirtualCardRevealDTO;
import com.nexora.bank.nexora_backend.service.VirtualCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Virtual card endpoints.
 *
 * Three things changed here, each closing a separate hole:
 *
 *  - The caller is taken from the authenticated principal. Previously a holder
 *    name arrived as a query or path parameter, so any caller could read any
 *    card by guessing a name.
 *  - Responses are DTOs. Returning the entity published the full card number
 *    and the verification code on every read.
 *  - Issuing a card is a POST. It creates state, and a GET that creates state
 *    can be triggered by anything that follows a link.
 *
 * The open CORS policy is gone too; these routes are not for arbitrary origins.
 */
@RestController
@RequestMapping("/api/v1/cards")
public class VirtualCardController {

    private final VirtualCardService cardService;

    public VirtualCardController(VirtualCardService cardService) {
        this.cardService = cardService;
    }

    /** Issues a card to the authenticated caller, or returns the existing one. */
    @PostMapping
    public ResponseEntity<VirtualCardResponseDTO> issueCard(Authentication authentication) {
        return ResponseEntity.ok(cardService.issueCardForEmail(authentication.getName()));
    }

    /** The caller's own card. Masked number, no verification code. */
    @GetMapping("/me")
    public ResponseEntity<VirtualCardResponseDTO> getMyCard(Authentication authentication) {
        return ResponseEntity.ok(cardService.getCardForEmail(authentication.getName()));
    }

    /**
     * Full details for the caller's own card. Deliberately a POST: it is an
     * explicit, auditable action rather than something a page can fetch twice
     * by accident.
     */
    @PostMapping("/me/reveal")
    public ResponseEntity<VirtualCardRevealDTO> revealMyCard(Authentication authentication) {
        return ResponseEntity.ok(cardService.revealCardForEmail(authentication.getName()));
    }
}
