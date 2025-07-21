package com.login.model;

/**
 * Enum to define user roles in the application
 */
public enum UserRole {
    ADMIN("Admin", "Full access including network management"),
    USER("User", "Standard access - view and manage bills only");
    
    private final String displayName;
    private final String description;
    
    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
