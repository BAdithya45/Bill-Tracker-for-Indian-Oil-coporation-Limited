package com.login.model;

import java.io.Serializable;

/**
 * User model to store user information with role-based access
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String username;
    private String password;
    private UserRole role;
    private String fullName;
    private String email;
    private boolean active;
    
    public User() {
        this.role = UserRole.USER; // Default role
        this.active = true;
    }
    
    public User(String username, String password, UserRole role) {
        this.username = username;
        this.password = password;
        this.role = role != null ? role : UserRole.USER;
        this.active = true;
    }
    
    public User(String username, String password, UserRole role, String fullName, String email) {
        this.username = username;
        this.password = password;
        this.role = role != null ? role : UserRole.USER;
        this.fullName = fullName;
        this.email = email;
        this.active = true;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role != null ? role : UserRole.USER;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    // Convenience methods
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
    
    public boolean isUser() {
        return role == UserRole.USER;
    }
    
    public boolean canManageNetworks() {
        return isAdmin();
    }
    
    @Override
    public String toString() {
        return String.format("User{username='%s', role=%s, fullName='%s', active=%s}", 
                           username, role, fullName, active);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return username != null ? username.equals(user.username) : user.username == null;
    }
    
    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : 0;
    }
}
