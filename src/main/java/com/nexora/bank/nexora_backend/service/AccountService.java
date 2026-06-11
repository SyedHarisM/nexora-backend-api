package com.nexora.bank.nexora_backend.service;

import com.nexora.bank.nexora_backend.dto.TransactionResponseDTO;
import com.nexora.bank.nexora_backend.entity.Account;
import com.nexora.bank.nexora_backend.entity.Transaction;
import com.nexora.bank.nexora_backend.entity.User;
import com.nexora.bank.nexora_backend.repository.AccountRepository;
import com.nexora.bank.nexora_backend.repository.TransactionRepository;
import com.nexora.bank.nexora_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Objects;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                          UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public Account getAccountByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public String lookupRecipientName(String accountNumber) {
        System.out.println("🔍 Looking for Account: [" + accountNumber + "]"); // Yeh console mein dikhega
        return accountRepository.findByAccountNumber(accountNumber)
                .map(acc -> acc.getUser().getName())
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public Account deposit(String email, double amount) {
        if (amount <= 0) throw new RuntimeException("Invalid deposit amount");

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        Account account = accountRepository.findByUserId(user.getId()).orElseThrow(() -> new RuntimeException("Account not found"));

        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account);

        // Naye fields ke saath Deposit log
        transactionRepository.save(new Transaction(
                "DEPOSIT", amount, email, email,
                account.getAccountNumber(), account.getAccountNumber(),
                user.getName(), user.getName()
        ));

        return account;
    }

    public Account withdraw(String email, double amount) {
        if (amount <= 0) throw new RuntimeException("Invalid withdraw amount");

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        Account account = accountRepository.findByUserId(user.getId()).orElseThrow(() -> new RuntimeException("Account not found"));

        if (account.getBalance() < amount) throw new RuntimeException("Insufficient balance");

        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account);

        // Naye fields ke saath Withdraw log
        transactionRepository.save(new Transaction(
                "WITHDRAW", amount, email, email,
                account.getAccountNumber(), account.getAccountNumber(),
                user.getName(), user.getName()
        ));

        return account;
    }

    public String transfer(String senderEmail, String targetAccountNumber, double amount) {
        if (amount <= 0) throw new RuntimeException("Invalid amount");

        User sender = userRepository.findByEmail(senderEmail).orElseThrow(() -> new RuntimeException("Sender not found"));
        Account senderAcc = accountRepository.findByUserId(sender.getId()).orElseThrow(() -> new RuntimeException("Sender account error"));
        Account receiverAcc = accountRepository.findByAccountNumber(targetAccountNumber).orElseThrow(() -> new RuntimeException("Receiver account not found"));

        if (senderAcc.getAccountNumber().equals(targetAccountNumber)) throw new RuntimeException("Cannot transfer to self");
        if (senderAcc.getBalance() < amount) throw new RuntimeException("Insufficient balance");

        senderAcc.setBalance(senderAcc.getBalance() - amount);
        receiverAcc.setBalance(receiverAcc.getBalance() + amount);
        accountRepository.save(senderAcc);
        accountRepository.save(receiverAcc);

        // SENT Record for Sender[cite: 4]
        transactionRepository.save(new Transaction(
                "SENT", amount, senderEmail, receiverAcc.getUser().getEmail(),
                senderAcc.getAccountNumber(), targetAccountNumber,
                sender.getName(), receiverAcc.getUser().getName()
        ));

        // RECEIVED Record for Receiver[cite: 4]
        transactionRepository.save(new Transaction(
                "RECEIVED", amount, senderEmail, receiverAcc.getUser().getEmail(),
                senderAcc.getAccountNumber(), targetAccountNumber,
                sender.getName(), receiverAcc.getUser().getName()
        ));

        return "Transfer successful";
    }

    public List<TransactionResponseDTO> getTransactionHistory(String email, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Yahan OrderByTimestampDesc wala method use karein
        return transactionRepository.findBySenderEmailOrReceiverEmailOrderByTimestampDesc(email, email, pageable)
                .map(txn -> {
                    boolean isSender = txn.getSenderEmail().equals(email);

                    // Duplicate prevention logic[cite: 4]
                    if (txn.getType().equals("SENT") && !isSender) return null;
                    if (txn.getType().equals("RECEIVED") && isSender) return null;

                    String displayType = txn.getType();
                    return new TransactionResponseDTO(
                            displayType,
                            txn.getAmount(),
                            txn.getSenderName(),
                            txn.getReceiverName(),
                            txn.getTimestamp()
                    );
                })
                .getContent().stream().filter(Objects::nonNull).toList();
    }
}