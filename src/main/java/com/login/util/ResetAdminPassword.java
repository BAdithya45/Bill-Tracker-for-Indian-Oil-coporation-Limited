package com.login.util;

import com.login.model.User;
import com.login.model.UserRole;
import com.login.service.UserService;

/**
 * Utility to reset the admin password
 */
public class ResetAdminPassword {
    
    public static void main(String[] args) {
        System.out.println("Starting admin password reset utility...");
        
        try {
            UserService userService = UserService.getInstance();
            
            // Create a default admin user with admin/1234
            User defaultAdmin = new User("admin", "1234", UserRole.ADMIN, 
                                      "System Administrator", "admin@company.com");
            
            // Force recreate the user file with fresh admin
            System.out.println("Creating fresh users file with admin account...");
            
            // Delete existing user file to start fresh
            java.io.File usersFile = new java.io.File("data/users_with_roles.dat");
            if (usersFile.exists()) {
                usersFile.delete();
                System.out.println("Deleted existing users file");
            }
            
            // Get fresh instance
            userService = UserService.getInstance();
            
            // Force add admin user
            userService.addUser(defaultAdmin);
            System.out.println("Added fresh admin user with username 'admin' and password '1234'");
            
            // Verify the user exists
            User verifyAdmin = userService.findUserByUsername("admin");
            if (verifyAdmin != null) {
                System.out.println("Verified admin user exists with password: " + verifyAdmin.getPassword());
            } else {
                System.out.println("WARNING: Failed to verify admin user exists!");
            }
            
            System.out.println("Password reset completed. You can now login with admin/1234");
            
            // Test the login manually
            User authTest = userService.authenticateUser("admin", "1234");
            if (authTest != null) {
                System.out.println("LOGIN TEST SUCCESS: Authentication succeeded with admin/1234");
            } else {
                System.out.println("LOGIN TEST FAILED: Authentication failed with admin/1234");
            }
            
        } catch (Exception e) {
            System.err.println("Error resetting admin password: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
