package com.login.controller;

import com.login.model.LoginRequest;
import com.login.model.RegisterRequest;
import com.login.model.User;
import com.login.model.UserRole;
import com.login.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private UserService userService = UserService.getInstance();
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpSession session) {
        try {
             
            System.out.println("DEBUG: Login attempt - Username: " + 
                (loginRequest != null ? loginRequest.getUsername() : "null") + 
                ", Password length: " + 
                (loginRequest != null && loginRequest.getPassword() != null ? loginRequest.getPassword().length() : "null"));
            
              
            if (loginRequest == null) {
                System.out.println("DEBUG: LoginRequest is null - JSON deserialization failed");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid request format - ensure Content-Type is application/json"
                ));
            }
             System.out.println("username: " + loginRequest.getUsername()+"**password: " + loginRequest.getPassword());
            if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
                System.out.println("DEBUG: Username or password is null");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Username and password are required"
                ));
            }
            
            User user = userService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
            
            if (user != null) {
                // Store user in session
                session.setAttribute("user", user);
                
                // Debug session information
                System.out.println("DEBUG: Login successful for user: " + user.getUsername());
                System.out.println("DEBUG: Session ID: " + session.getId());
                System.out.println("DEBUG: Session creation time: " + new java.util.Date(session.getCreationTime()));
                System.out.println("DEBUG: Session max inactive interval: " + session.getMaxInactiveInterval() + " seconds");
                System.out.println("DEBUG: Session is new: " + session.isNew());
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Login successful");
                response.put("user", Map.of(
                    "username", user.getUsername(),
                    "role", user.getRole().toString(),
                    "fullName", user.getFullName() != null ? user.getFullName() : "",
                    "isAdmin", user.isAdmin()
                ));
                
                return ResponseEntity.ok(response);
            } else {
                System.out.println("DEBUG: Authentication failed for username: " + loginRequest.getUsername());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid username or password"
                ));
            }
        } catch (Exception e) {
            System.err.println("DEBUG: Login error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Login failed: " + e.getMessage()
            ));
        }
    }
    
    // Alternative login endpoint for external Tomcat compatibility
    @PostMapping(value = "/login-alt", consumes = "application/json")
    public ResponseEntity<?> loginAlternative(@RequestBody Map<String, String> requestBody, HttpSession session) {
        try {
            System.out.println("DEBUG: Alternative login attempt - Raw request: " + requestBody);
            
            String username = requestBody.get("username");
            String password = requestBody.get("password");
            
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Username and password are required"
                ));
            }
            
            User user = userService.authenticateUser(username, password);
            
            if (user != null) {
                session.setAttribute("user", user);
                
                System.out.println("DEBUG: Alternative login successful for user: " + user.getUsername());
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Login successful");
                response.put("user", Map.of(
                    "username", user.getUsername(),
                    "role", user.getRole().toString(),
                    "fullName", user.getFullName() != null ? user.getFullName() : "",
                    "isAdmin", user.isAdmin()
                ));
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid username or password"
                ));
            }
        } catch (Exception e) {
            System.err.println("DEBUG: Alternative login error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Login failed: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            if (userService.userExists(registerRequest.getUsername())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Username already exists"
                ));
            }
            
            boolean success = userService.registerNewUser(
                registerRequest.getUsername(), 
                registerRequest.getPassword(), 
                UserRole.USER
            );
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Registration successful"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Registration failed"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Registration failed: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Logged out successfully"
        ));
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        System.out.println("DEBUG: Checking current user - Session ID: " + session.getId());
        System.out.println("DEBUG: Session is new: " + session.isNew());
        System.out.println("DEBUG: Session creation time: " + new java.util.Date(session.getCreationTime()));
        System.out.println("DEBUG: Session last accessed time: " + new java.util.Date(session.getLastAccessedTime()));
        
        User user = (User) session.getAttribute("user");
        if (user != null) {
            System.out.println("DEBUG: Found user in session: " + user.getUsername());
            return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "role", user.getRole().toString(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "isAdmin", user.isAdmin()
            ));
        } else {
            System.out.println("DEBUG: No user found in session - returning 401");
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Not authenticated"
            ));
        }
    }
    
    @GetMapping("/check")
    public ResponseEntity<?> checkAuth(HttpSession session) {
        System.out.println("DEBUG: Auth check endpoint called - Session ID: " + session.getId());
        
        User user = (User) session.getAttribute("user");
        if (user != null) {
            System.out.println("DEBUG: Auth check - Found user in session: " + user.getUsername());
            return ResponseEntity.ok(Map.of(
                "authenticated", true
            ));
        } else {
            System.out.println("DEBUG: Auth check - No user found in session");
            return ResponseEntity.status(401).body(Map.of(
                "authenticated", false,
                "message", "Not authenticated"
            ));
        }
    }
  
    
}
