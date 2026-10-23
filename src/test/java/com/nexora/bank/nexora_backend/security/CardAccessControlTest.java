package com.nexora.bank.nexora_backend.security;

import com.nexora.bank.nexora_backend.entity.Role;
import com.nexora.bank.nexora_backend.entity.User;
import com.nexora.bank.nexora_backend.entity.VirtualCard;
import com.nexora.bank.nexora_backend.repository.UserRepository;
import com.nexora.bank.nexora_backend.repository.VirtualCardRepository;
import com.nexora.bank.nexora_backend.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Verifies the virtual-card access-control fix against the real filter chain,
 * rather than by reading the configuration and assuming.
 *
 * Before the change {@code /api/v1/cards/**} was permitAll and the endpoints
 * were addressed by holder name, so an anonymous caller could read any card —
 * full number and verification code included — by guessing a name. Each test
 * pins one half of the fix: anonymous callers are refused, and an authenticated
 * caller receives only their own card with the sensitive fields absent.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardAccessControlTest {

    private static final String FULL_PAN = "4111111111117823";
    private static final String CVV = "321";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VirtualCardRepository cardRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /** Nothing in these tests should attempt to send mail. */
    @MockitoBean
    private EmailService emailService;

    private User owner;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        userRepository.deleteAll();

        owner = newUser("Card Owner", "owner@gmail.com", "03001234567");

        VirtualCard card = new VirtualCard();
        card.setUser(owner);
        card.setHolderName(owner.getName());
        card.setCardNumber(FULL_PAN);
        card.setCvv(CVV);
        card.setExpiryDate(LocalDate.now().plusYears(3));
        cardRepository.save(card);
    }

    private User newUser(String name, String email, String phone) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("hashed");
        user.setPhoneNumber(phone);
        user.setRole(Role.USER);
        user.setIsVerified(true);
        return userRepository.save(user);
    }

    private String bearerFor(String email) {
        return "Bearer " + jwtUtil.generateToken(email, Role.USER);
    }

    private static void assertNoCardSecrets(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(FULL_PAN);
        assertThat(body).doesNotContain(CVV);
    }

    @Test
    @DisplayName("anonymous caller cannot read a card")
    void anonymousCannotReadCard() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/cards/me")).andReturn();

        assertThat(result.getResponse().getStatus()).isIn(401, 403);
        assertNoCardSecrets(result);
    }

    @Test
    @DisplayName("anonymous caller cannot reveal a card")
    void anonymousCannotRevealCard() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/cards/me/reveal")).andReturn();

        assertThat(result.getResponse().getStatus()).isIn(401, 403);
        assertNoCardSecrets(result);
    }

    @Test
    @DisplayName("anonymous caller cannot issue a card")
    void anonymousCannotIssueCard() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/cards")).andReturn();

        assertThat(result.getResponse().getStatus()).isIn(401, 403);
    }

    @Test
    @DisplayName("the old name-addressed route no longer serves card data")
    void legacyHolderNameRouteIsGone() throws Exception {
        MvcResult result =
                mockMvc.perform(get("/api/v1/cards/details/{name}", "Card Owner")).andReturn();

        assertThat(result.getResponse().getStatus()).isNotEqualTo(200);
        assertNoCardSecrets(result);
    }

    @Test
    @DisplayName("a forged token does not open the card endpoints")
    void forgedTokenIsRefused() throws Exception {
        // Signed with a key the application does not hold, claiming the owner's
        // identity and an administrator role. The previously hardcoded key is
        // deliberately not reproduced here — it is already published, and
        // copying it into the tree would outlive any history cleanup.
        String forged = io.jsonwebtoken.Jwts.builder()
                .setSubject(owner.getEmail())
                .claim("role", Role.ADMIN.name())
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(
                        io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                                "any-key-that-is-not-the-configured-one-1234567890"
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        MvcResult result = mockMvc.perform(
                get("/api/v1/cards/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(401, 403);
        assertNoCardSecrets(result);
    }

    @Test
    @DisplayName("owner reads their own card without number or verification code")
    void ownerGetsMaskedCardOnly() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/api/v1/cards/me").header(HttpHeaders.AUTHORIZATION, bearerFor(owner.getEmail())))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("7823");          // last four stay visible
        assertThat(body).doesNotContain(FULL_PAN);  // the rest does not
        assertThat(body).doesNotContain(CVV);
        assertThat(body).doesNotContain("cvv");
    }

    @Test
    @DisplayName("a different signed-in user sharing the holder name gets nothing")
    void otherUserCannotReachOwnersCard() throws Exception {
        User intruder = newUser("Card Owner", "intruder@gmail.com", "03007654321");

        MvcResult result = mockMvc.perform(
                get("/api/v1/cards/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(intruder.getEmail())))
                .andReturn();

        // Sharing a display name must not share a card: resolution is by owner.
        assertNoCardSecrets(result);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("7823");
    }

    @Test
    @DisplayName("owner can still reveal their own full card details")
    void ownerCanRevealOwnCard() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/api/v1/cards/me/reveal")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(owner.getEmail())))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains(FULL_PAN);
    }

    @Test
    @DisplayName("account lookup is no longer anonymous")
    void accountLookupRequiresAuthentication() throws Exception {
        MvcResult result =
                mockMvc.perform(get("/api/accounts/lookup/{n}", "NEX-ABC123")).andReturn();

        assertThat(result.getResponse().getStatus()).isIn(401, 403);
    }
}
