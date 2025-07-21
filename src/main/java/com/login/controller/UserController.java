package com.login.controller;

import com.login.model.User;
import com.login.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    
    private UserService userService = UserService.getInstance();
    
    /**
     * Get current user info
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        // Return user info without password
        return ResponseEntity.ok(Map.of(
            "username", currentUser.getUsername(),
            "fullName", currentUser.getFullName() != null ? currentUser.getFullName() : "",
            "email", currentUser.getEmail() != null ? currentUser.getEmail() : "",
            "role", currentUser.getRole().name(),
            "isAdmin", currentUser.isAdmin()
        ));
    }
    
    /**
     * Change current user's password
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        try {
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");
            
            if (currentPassword == null || newPassword == null || 
                currentPassword.trim().isEmpty() || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Current password and new password are required"));
            }
            
            boolean success = userService.changeUserPassword(currentUser.getUsername(), currentPassword, newPassword);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to change password. Current password may be incorrect."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
