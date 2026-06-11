package com.nexora.bank.nexora_backend.controller;

import com.nexora.bank.nexora_backend.dto.AccountResponseDTO;
import com.nexora.bank.nexora_backend.dto.TransactionResponseDTO;
import com.nexora.bank.nexora_backend.dto.TransferRequestDTO;
import com.nexora.bank.nexora_backend.entity.Account;
import com.nexora.bank.nexora_backend.service.AccountService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/my-account")
    public AccountResponseDTO getMyAccount(Authentication authentication) {
        Account account = accountService.getAccountByEmail(authentication.getName());
        return new AccountResponseDTO(account.getAccountNumber(), account.getBalance());
    }

    @GetMapping("/lookup/{accountNumber}")
    public String lookup(@PathVariable("accountNumber") String accountNumber) { // Explicitly name it
        return accountService.lookupRecipientName(accountNumber);
    }
    // Updated Backend Code
    @PostMapping("/deposit")
    public AccountResponseDTO deposit(Authentication authentication, @RequestBody Map<String, Double> request) {
        // Frontend se "amount" key nikaal rahe hain
        double amount = request.get("amount");

        Account account = accountService.deposit(authentication.getName(), amount);
        return new AccountResponseDTO(account.getAccountNumber(), account.getBalance());
    }

    @PostMapping("/withdraw")
    public AccountResponseDTO withdraw(Authentication authentication, @RequestBody Map<String, Double> request) {
        // Frontend se "amount" key nikaal rahe hain
        double amount = request.get("amount");

        Account account = accountService.withdraw(authentication.getName(), amount);
        return new AccountResponseDTO(account.getAccountNumber(), account.getBalance());
    }



    @PostMapping("/transfer")
    public String transfer(Authentication authentication, @RequestBody TransferRequestDTO request) {
        return accountService.transfer(authentication.getName(), request.getTargetAccountNumber(), request.getAmount());
    }

    @GetMapping("/transactions")
    public List<TransactionResponseDTO> getTransactions(
            Authentication authentication,
            @RequestParam int page,
            @RequestParam int size) {
        return accountService.getTransactionHistory(authentication.getName(), page, size);
    }
}