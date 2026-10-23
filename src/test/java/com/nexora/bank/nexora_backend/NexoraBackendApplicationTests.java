package com.nexora.bank.nexora_backend;

import com.nexora.bank.nexora_backend.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * This test could not run before: it needed a live MySQL instance and a signing
 * key that no longer has a default. It now runs against the in-memory test
 * profile, so a broken application context fails the build instead of going
 * unnoticed.
 */
@SpringBootTest
@ActiveProfiles("test")
class NexoraBackendApplicationTests {

	@MockitoBean
	private EmailService emailService;

	@Test
	void contextLoads() {
	}

}
