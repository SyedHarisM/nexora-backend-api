package com.nexora.bank.nexora_backend.service;

import com.nexora.bank.nexora_backend.dto.AuthResponseDTO;
import com.nexora.bank.nexora_backend.entity.Account;
import com.nexora.bank.nexora_backend.entity.RefreshToken;
import com.nexora.bank.nexora_backend.entity.Role;
import com.nexora.bank.nexora_backend.entity.User;
import com.nexora.bank.nexora_backend.repository.AccountRepository;
import com.nexora.bank.nexora_backend.repository.UserRepository;
import com.nexora.bank.nexora_backend.security.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.Random;
import java.security.SecureRandom;


@Service
public class UserService {
    
    @Autowired
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final RefreshTokenService refreshTokenService;
    @Autowired
    private VirtualCardService cardService;
    @Autowired private EmailService emailService;
    

    // Custom ID Generation Settings
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();

    public UserService(UserRepository userRepository, AccountRepository accountRepository,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
        this.refreshTokenService = refreshTokenService;
    }

    // Helper to generate professional NEXO-ID
    private String generateSecureNexoId() {
        StringBuilder sb = new StringBuilder("NEX-");
        for (int i = 0; i < 6; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

@Transactional
    public User registerUser(User user) {
        // 1. Email check: Agar verified email maujood hai toh error do
        Optional<User> existingByEmail = userRepository.findByEmail(user.getEmail());
        if (existingByEmail.isPresent() && existingByEmail.get().getIsVerified()) {
            throw new RuntimeException("Email already exists and is verified");
        }

        // 2. Phone check: Agar phone hai par unverified hai, toh purana saaf karo
        Optional<User> existingByPhone = userRepository.findByPhoneNumber(user.getPhoneNumber());
        if (existingByPhone.isPresent()) {
            User existingUser = existingByPhone.get();
            if (existingUser.getIsVerified()) {
                throw new RuntimeException("Phone Number already exists");
            } else {
                // Orphaned account cleanup: Purana unverified record delete karein
                userRepository.delete(existingUser);
                userRepository.flush(); // Force delete taake naya record save ho sake
            }
        }

        // 3. Setup User Profile
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);
        user.setIsVerified(false); // Default false set karein safety ke liye
        
        User savedUser = userRepository.save(user);

        // 4. Card Service
        cardService.createCardForUser(savedUser.getName());

        // 5. Setup Elite Account
        Account account = new Account();
        account.setUser(savedUser);
        account.setBalance(0.0);

        // Setting the new professional NEXO-ID format
        account.setAccountNumber(generateSecureNexoId());
        accountRepository.save(account);

        // 6. OTP Handling
        String otp = generateOtp(); 
        savedUser.setOtp(otp);
        userRepository.save(savedUser); 

        // 7. Mail bhejein
        emailService.sendOtpEmail(savedUser.getEmail(), otp);

        return savedUser;
    }

    public AuthResponseDTO login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

 if (!user.getIsVerified()) {
    // 1. Naya OTP generate aur save karo
    String newOtp = generateOtp(); 
    user.setOtp(newOtp);
    userRepository.save(user);

    // 2. Email bhej do (Aapka existing email service)
    emailService.sendOtpEmail(user.getEmail(), newOtp);

    // 3. Error throw karo taaki frontend ko pata chale
    throw new RuntimeException("IDENTITY_UNVERIFIED");
}

        // generate access token
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());

        // generate refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        return new AuthResponseDTO(
                accessToken,
                refreshToken.getToken(),
                user.getName()
        );
    }

    public ResponseEntity<?> processForgotPassword(String email) {
    // 1. User find karein
    Optional<User> userOptional = userRepository.findByEmail(email);

    if (userOptional.isPresent()) {
        User user = userOptional.get();
        
        // 2. Generate 6-digit OTP using our service
        String otp = emailService.generateOtp();
        
        // 3. Save OTP to database (Temporarily in User entity)
        user.setOtp(otp);
        userRepository.save(user);
        
        // 4. Send Email via EmailService
        try {
            emailService.sendOtpEmail(email, otp);
            return ResponseEntity.ok("Security frequency (OTP) dispatched to " + email);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("SMTP Communication Failure. Please check server logs.");
        }
    }

    // Agar user nahi mila
    return ResponseEntity.status(404).body("Identity coordinate not found in Nexora records.");
}

public ResponseEntity<?> verifyAndResetPassword(String email, String otp, String newPassword) {
    Optional<User> userOptional = userRepository.findByEmail(email);
    

    if (userOptional.isPresent()) {
        User user = userOptional.get();

        // 1. Check if OTP matches
        if (user.getOtp() != null && user.getOtp().equals(otp)) {
            
            // 2. Update Password (Bhai, BCrypt use kar rahe ho toh encode lazmi karna)
            // user.setPassword(passwordEncoder.encode(newPassword)); 
            // Abhi ke liye direct set, baad mein security badhayenge
            
           user.setIsVerified(true);

           if (newPassword != null && !newPassword.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

            // 3. Clear OTP after successful reset
            user.setOtp(null); 
            userRepository.save(user);

            return ResponseEntity.ok("Vault access restored. Password updated successfully.");
        } else {
            return ResponseEntity.status(401).body("Invalid Security Frequency (OTP).");
        }
    }

    return ResponseEntity.status(404).body("Identity not found.");
}

    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }


    // INJECT THIS CODE INSIDE YOUR UserService.java CLASS (AT THE BOTTOM LAYER)

    /**
     * CORE PIPELINE: FIND OPERATOR RECORDS VIA EMAIL MATRIX
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * CORE PIPELINE: UPDATE USER INSTANCE DATA NODES
     */
    public User updateUser(User user) {
        return userRepository.save(user);
    }

}