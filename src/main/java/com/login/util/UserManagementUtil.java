package com.login.util;

import com.login.model.User;
import com.login.model.UserRole;
import com.login.service.UserService;

import java.util.List;
import java.util.Scanner;

/**
 * Utility to manage users - show existing users, reset passwords, create users
 */
public class UserManagementUtil {
    
    public static void main(String[] args) {
        System.out.println("=== BSNL Bill Tracker - User Management ===");
        System.out.println();
        
        UserService userService = UserService.getInstance();
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("Options:");
            System.out.println("1. Show all users");
            System.out.println("2. Test login credentials");
            System.out.println("3. Create admin user");
            System.out.println("4. Reset user password");
            System.out.println("5. Exit");
            System.out.print("Choose option (1-5): ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    showAllUsers(userService);
                    break;
                case "2":
                    testLogin(userService, scanner);
                    break;
                case "3":
                    createAdminUser(userService, scanner);
                    break;
                case "4":
                    resetPassword(userService, scanner);
                    break;
                case "5":
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
            System.out.println();
        }
    }
    
    private static void showAllUsers(UserService userService) {
        System.out.println("=== All Users in System ===");
        
        // We need to access the users directly since getAllUsers requires admin user
        // Let's use reflection to get the users list
        try {
            java.lang.reflect.Field usersField = UserService.class.getDeclaredField("users");
            usersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<User> users = (List<User>) usersField.get(userService);
            
            if (users.isEmpty()) {
                System.out.println("No users found in system.");
            } else {
                System.out.printf("%-15s %-15s %-10s %-20s %-6s%n", 
                    "Username", "Password", "Role", "Full Name", "Active");
                System.out.println("-".repeat(70));
                
                for (User user : users) {
                    System.out.printf("%-15s %-15s %-10s %-20s %-6s%n",
                        user.getUsername(),
                        user.getPassword(),
                        user.getRole(),
                        user.getFullName() != null ? user.getFullName() : "",
                        user.isActive() ? "Yes" : "No"
                    );
                }
            }
        } catch (Exception e) {
            System.out.println("Error accessing user data: " + e.getMessage());
        }
    }
    
    private static void testLogin(UserService userService, Scanner scanner) {
        System.out.println("=== Test Login Credentials ===");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        
        User user = userService.authenticateUser(username, password);
        if (user != null) {
            System.out.println("✅ LOGIN SUCCESS!");
            System.out.println("User: " + user.getUsername());
            System.out.println("Role: " + user.getRole());
            System.out.println("Full Name: " + user.getFullName());
            System.out.println("Active: " + user.isActive());
        } else {
            System.out.println("❌ LOGIN FAILED!");
            System.out.println("Invalid username or password.");
        }
    }
    
    private static void createAdminUser(UserService userService, Scanner scanner) {
        System.out.println("=== Create Admin User ===");
        System.out.print("New admin username: ");
        String username = scanner.nextLine().trim();
        
        if (userService.userExists(username)) {
            System.out.println("❌ Username already exists!");
            return;
        }
        
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        System.out.print("Full name: ");
        String fullName = scanner.nextLine().trim();
        
        // Create admin user directly using reflection since we need to bypass admin-only restriction
        try {
            java.lang.reflect.Field usersField = UserService.class.getDeclaredField("users");
            usersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<User> users = (List<User>) usersField.get(userService);
            
            User newAdmin = new User(username, password, UserRole.ADMIN, fullName, "");
            users.add(newAdmin);
            
            // Save users using reflection
            java.lang.reflect.Method saveUsersMethod = UserService.class.getDeclaredMethod("saveUsers");
            saveUsersMethod.setAccessible(true);
            saveUsersMethod.invoke(userService);
            
            System.out.println("✅ Admin user created successfully!");
            System.out.println("Username: " + username);
            System.out.println("Password: " + password);
            
        } catch (Exception e) {
            System.out.println("❌ Error creating admin user: " + e.getMessage());
        }
    }
    
    private static void resetPassword(UserService userService, Scanner scanner) {
        System.out.println("=== Reset User Password ===");
        System.out.print("Username to reset: ");
        String username = scanner.nextLine().trim();
        
        try {
            java.lang.reflect.Field usersField = UserService.class.getDeclaredField("users");
            usersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<User> users = (List<User>) usersField.get(userService);
            
            User user = users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
                
            if (user == null) {
                System.out.println("❌ User not found!");
                return;
            }
            
            System.out.print("New password: ");
            String newPassword = scanner.nextLine().trim();
            
            user.setPassword(newPassword);
            
            // Save users using reflection
            java.lang.reflect.Method saveUsersMethod = UserService.class.getDeclaredMethod("saveUsers");
            saveUsersMethod.setAccessible(true);
            saveUsersMethod.invoke(userService);
            
            System.out.println("✅ Password reset successfully!");
            System.out.println("Username: " + username);
            System.out.println("New Password: " + newPassword);
            
        } catch (Exception e) {
            System.out.println("❌ Error resetting password: " + e.getMessage());
        }
    }
}
