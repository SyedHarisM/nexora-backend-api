package com.nexora.bank.nexora_backend.controller;

import com.nexora.bank.nexora_backend.dto.AuthResponseDTO;
import com.nexora.bank.nexora_backend.dto.LoginRequest;
import com.nexora.bank.nexora_backend.entity.User;
import com.nexora.bank.nexora_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    // Core directory reference mapping where avatars are safely stored on device storage disk
    private static final String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/uploads/avatars/";

    @PostMapping("/register")
    public User registerUser(@Valid @RequestBody User user){
        return userService.registerUser(user);
    }

    @PostMapping("/login")
    public AuthResponseDTO loginUser(@Valid @RequestBody LoginRequest request) {
        return userService.login(request.getEmail(), request.getPassword());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Email coordinate is mandatory.");
        }
        return userService.processForgotPassword(email); 
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        if (email == null || otp == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Incomplete coordinates for password reset.");
        }
        return userService.verifyAndResetPassword(email, otp, newPassword);
    }

    @GetMapping("/profile/{email}")
    public ResponseEntity<?> getProfileDetails(@PathVariable String email) {
        Map<String, Object> response = new HashMap<>();
        User user = userService.findByEmail(email); 
        
        if (user == null) {
            response.put("error", "OPERATOR_NOT_FOUND // Data pipeline tracking void");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("id", user.getId());
        profileData.put("username", user.getName()); // Aligned with 'name' property
        profileData.put("fullName", user.getName()); // Aligned with 'name' property
        profileData.put("email", user.getEmail());
        profileData.put("isVerified", user.getIsVerified());
        profileData.put("avatarUrl", user.getAvatarUrl());

        profileData.put("phoneNumber", user.getPhoneNumber()); // Jo bhi aapka getter method ho
        if (user.getAccount() != null) {
        profileData.put("accountNumber", user.getAccount().getAccountNumber());
    } else {
        profileData.put("accountNumber", "Not Linked"); // Fallback agar account na bana ho
    }

        return ResponseEntity.ok(profileData);
    }


    @PostMapping("/upload-avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("email") String email) {
        
        Map<String, String> response = new HashMap<>();
        User user = userService.findByEmail(email);

        if (user == null) {
            response.put("error", "INVALID_OPERATOR // Session terminated");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        try {
            if (file.isEmpty()) {
                response.put("error", "EMPTY_TRANSMISSION // Data block void");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Create target folders if they do not exist physically
            Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalName = file.getOriginalFilename();
            String fileExtension = originalName != null && originalName.contains(".") 
                    ? originalName.substring(originalName.lastIndexOf(".")) : ".jpg";
            
            String fileName = "avatar_" + user.getId() + "_" + System.currentTimeMillis() + fileExtension;
            Path filePath = uploadPath.resolve(fileName);

            // Stream write physical copy to device backend structures
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Absolute IP Mapping URL for Mobile Devices fetch routing
            String hostUrl = "http://192.168.18.139:8080/uploads/avatars/" + fileName;
            user.setAvatarUrl(hostUrl);
            userService.updateUser(user); 

            response.put("status", "SUCCESS");
            response.put("avatarUrl", hostUrl);
            response.put("message", "Nexora Core Cluster Array Sync Success.");
            
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("error", "STORAGE_BLOCK_CRASH // File write violation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}