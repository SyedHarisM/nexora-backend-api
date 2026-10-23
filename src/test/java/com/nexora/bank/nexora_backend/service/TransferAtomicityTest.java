package com.nexora.bank.nexora_backend.service;

import com.nexora.bank.nexora_backend.entity.Account;
import com.nexora.bank.nexora_backend.entity.Role;
import com.nexora.bank.nexora_backend.entity.User;
import com.nexora.bank.nexora_backend.repository.AccountRepository;
import com.nexora.bank.nexora_backend.repository.TransactionRepository;
import com.nexora.bank.nexora_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that money movement is now bounded and serialized.
 *
 * Before the change nothing in AccountService was transactional: a transfer ran
 * four separate writes with no boundary, so a failure between the debit and the
 * credit destroyed money, and two concurrent transfers could both read the same
 * balance and overwrite each other.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransferAtomicityTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockitoBean
    private EmailService emailService;

    private String senderEmail;
    private String receiverAccountNumber;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        senderEmail = "sender@gmail.com";
        createUserWithAccount("Sender", senderEmail, "03001111111", "NEX-SEND01", 1000.0);
        receiverAccountNumber = "NEX-RECV01";
        createUserWithAccount("Receiver", "receiver@gmail.com", "03002222222",
                receiverAccountNumber, 0.0);
    }

    private void createUserWithAccount(String name, String email, String phone,
                                       String accountNumber, double balance) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("hashed");
        user.setPhoneNumber(phone);
        user.setRole(Role.USER);
        user.setIsVerified(true);
        user = userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setAccountNumber(accountNumber);
        account.setBalance(balance);
        accountRepository.save(account);
    }

    private double balanceOf(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).orElseThrow().getBalance();
    }

    @Test
    @DisplayName("a successful transfer moves the money and writes both ledger rows")
    void successfulTransferMovesMoney() {
        accountService.transfer(senderEmail, receiverAccountNumber, 250.0);

        assertThat(balanceOf("NEX-SEND01")).isEqualTo(750.0);
        assertThat(balanceOf(receiverAccountNumber)).isEqualTo(250.0);
        assertThat(transactionRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("a rejected transfer leaves both balances untouched")
    void rejectedTransferChangesNothing() {
        assertThatThrownBy(() ->
                accountService.transfer(senderEmail, receiverAccountNumber, 5000.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient balance");

        // The debit must not have landed on its own.
        assertThat(balanceOf("NEX-SEND01")).isEqualTo(1000.0);
        assertThat(balanceOf(receiverAccountNumber)).isEqualTo(0.0);
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("an unknown destination leaves the sender untouched")
    void unknownDestinationChangesNothing() {
        assertThatThrownBy(() ->
                accountService.transfer(senderEmail, "NEX-NOPE99", 100.0))
                .isInstanceOf(RuntimeException.class);

        assertThat(balanceOf("NEX-SEND01")).isEqualTo(1000.0);
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("concurrent transfers never overdraw the sender")
    void concurrentTransfersDoNotOverdraw() throws Exception {
        // Twenty simultaneous attempts of 100 against a balance of 1000: at
        // most ten can succeed. Without locking, several would read the same
        // balance and the account would end up negative.
        int attempts = 20;
        double amount = 100.0;

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();

        List<Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            futures.add(pool.submit(() -> {
                startLine.await();
                try {
                    accountService.transfer(senderEmail, receiverAccountNumber, amount);
                    succeeded.incrementAndGet();
                } catch (Exception e) {
                    refused.incrementAndGet();
                }
                return null;
            }));
        }

        startLine.countDown();
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        double senderBalance = balanceOf("NEX-SEND01");
        double receiverBalance = balanceOf(receiverAccountNumber);

        assertThat(succeeded.get() + refused.get()).isEqualTo(attempts);
        // The invariant that matters: money is conserved and never negative.
        assertThat(senderBalance).isGreaterThanOrEqualTo(0.0);
        assertThat(senderBalance + receiverBalance).isEqualTo(1000.0);
        assertThat(receiverBalance).isEqualTo(succeeded.get() * amount);
        assertThat(succeeded.get()).isLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("withdrawing more than the balance is refused and changes nothing")
    void withdrawBeyondBalanceIsRefused() {
        assertThatThrownBy(() -> accountService.withdraw(senderEmail, 5000.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient balance");

        assertThat(balanceOf("NEX-SEND01")).isEqualTo(1000.0);
    }

    @Test
    @DisplayName("a transfer to the sender's own account is refused")
    void selfTransferIsRefused() {
        assertThatThrownBy(() ->
                accountService.transfer(senderEmail, "NEX-SEND01", 10.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot transfer to self");

        assertThat(balanceOf("NEX-SEND01")).isEqualTo(1000.0);
    }
}
