package com.login.controller;

import com.login.model.User;
import com.login.model.UserRole;
import com.login.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private UserService userService = UserService.getInstance();
    
    /**
     * Get all users (admin only)
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        if (!currentUser.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        try {
            List<User> users = userService.getAllUsers(currentUser);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Add a new user (admin only)
     */
    @PostMapping("/users")
    public ResponseEntity<?> addUser(@RequestBody Map<String, String> request, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        if (!currentUser.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        try {
            String username = request.get("username");
            String password = request.get("password");
            String roleStr = request.get("role");
            String fullName = request.get("fullName");
            String email = request.get("email");
            
            if (username == null || username.trim().isEmpty() || 
                password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
            }
            
            UserRole role = UserRole.USER; // Default to USER
            if (roleStr != null && "ADMIN".equalsIgnoreCase(roleStr)) {
                role = UserRole.ADMIN;
            }
            
            boolean success = userService.registerUser(username, password, role, fullName, email, currentUser);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "User created successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to create user. Username may already exist."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Delete a user (admin only)
     */
    @DeleteMapping("/users/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        if (!currentUser.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        try {
            boolean success = userService.deleteUser(username, currentUser);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete user. User may not exist or cannot be deleted."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Reset a user's password (admin only)
     */
    @PostMapping("/users/{username}/reset-password")
    public ResponseEntity<?> resetUserPassword(@PathVariable String username, 
                                             @RequestBody Map<String, String> request, 
                                             HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        if (!currentUser.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        try {
            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "New password is required"));
            }
            
            boolean success = userService.resetUserPassword(username, newPassword, currentUser);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to reset password. User may not exist."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
