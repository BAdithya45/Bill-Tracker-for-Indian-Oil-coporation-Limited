package com.login.service;

import com.login.model.User;
import com.login.model.UserRole;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class to manage user accounts and role-based access
 */
public class UserService {
    private static final String DATA_DIR = "data";
    private static final String USERS_FILE = DATA_DIR + File.separator + "users_with_roles.dat";
    private static UserService instance;
    private List<User> users;
    
    private UserService() {
        users = new ArrayList<>();
        ensureDataDirectory();
        loadUsers();
        ensureDefaultAdmin();
    }
    
    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }
    
    /**
     * Ensure data directory exists
     */
    private void ensureDataDirectory() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
    
    /**
     * Load users from file
     */
    @SuppressWarnings("unchecked")
    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                users = (List<User>) ois.readObject();
                System.out.println("DEBUG: Loaded " + users.size() + " users from file");
            } catch (Exception e) {
                System.err.println("Error loading users: " + e.getMessage());
                users = new ArrayList<>();
            }
        } else {
            System.out.println("DEBUG: Users file does not exist, starting with empty list");
            users = new ArrayList<>();
        }
    }
    
    /**
     * Save users to file
     */
    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(users);
            System.out.println("DEBUG: Saved " + users.size() + " users to file");
        } catch (Exception e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }
    
    /**
     * Ensure there's at least one admin user
     */
    private void ensureDefaultAdmin() {
        // Check if any admin exists
        boolean hasAdmin = users.stream().anyMatch(User::isAdmin);
        
        if (!hasAdmin) {
            // Create default admin user
            User defaultAdmin = new User("admin", "admin123", UserRole.ADMIN, 
                                       "System Administrator", "admin@company.com");
            users.add(defaultAdmin);
            saveUsers();
            System.out.println("DEBUG: Created default admin user - Username: admin, Password: admin123");
        }
    }
    
    /**
     * Authenticate user and return user object if valid
     */
    public User authenticateUser(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        
        return users.stream()
                .filter(user -> user.isActive())
                .filter(user -> username.equals(user.getUsername()))
                .filter(user -> password.equals(user.getPassword()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Register a new user (only admins can create users)
     */
    public boolean registerUser(String username, String password, UserRole role, 
                              String fullName, String email, User requestingUser) {
        // Only admins can create new users
        if (requestingUser == null || !requestingUser.isAdmin()) {
            System.out.println("DEBUG: Non-admin user attempted to create account");
            return false;
        }
        
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return false;
        }
        
        // Check if username already exists
        if (users.stream().anyMatch(user -> username.equals(user.getUsername()))) {
            return false;
        }
        
        User newUser = new User(username.trim(), password, role, fullName, email);
        users.add(newUser);
        saveUsers();
        
        System.out.println("DEBUG: Admin " + requestingUser.getUsername() + 
                         " created new user: " + username + " with role: " + role);
        return true;
    }
    
    /**
     * Check if a username exists
     */
    public boolean userExists(String username) {
        if (username == null) {
            return false;
        }
        return users.stream().anyMatch(user -> username.equals(user.getUsername()));
    }
    
    /**
     * Register a new user during initial setup (when no admin approval is required)
     * This is used for the registration form when users can self-register
     */
    public boolean registerNewUser(String username, String password, UserRole role) {
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return false;
        }
        
        // Check if username already exists
        if (userExists(username)) {
            return false;
        }
        
        // For security, default new registrations to USER role unless explicitly admin
        UserRole assignedRole = (role == UserRole.ADMIN) ? UserRole.USER : role;
        
        User newUser = new User(username.trim(), password, assignedRole, 
                               username.trim(), ""); // Use username as fullName, empty email
        users.add(newUser);
        saveUsers();
        
        System.out.println("DEBUG: New user registered: " + username + " with role: " + assignedRole);
        return true;
    }
    
    /**
     * Update user role (only admins can do this)
     */
    public boolean updateUserRole(String username, UserRole newRole, User requestingUser) {
        if (requestingUser == null || !requestingUser.isAdmin()) {
            return false;
        }
        
        User user = findUserByUsername(username);
        if (user != null && !user.equals(requestingUser)) { // Can't change own role
            user.setRole(newRole);
            saveUsers();
            return true;
        }
        return false;
    }
    
    /**
     * Get all users (only admins can see all users)
     */
    public List<User> getAllUsers(User requestingUser) {
        if (requestingUser == null || !requestingUser.isAdmin()) {
            return new ArrayList<>(); // Return empty list for non-admins
        }
        return new ArrayList<>(users);
    }
    
    /**
     * Find user by username
     */
    public User findUserByUsername(String username) {
        return users.stream()
                .filter(user -> username.equals(user.getUsername()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Change user password
     */
    public boolean changeUserPassword(String username, String currentPassword, String newPassword) {
        if (username == null || currentPassword == null || newPassword == null) {
            return false;
        }
        
        // First authenticate with current password
        User user = authenticateUser(username, currentPassword);
        if (user == null) {
            return false;
        }
        
        // Update password
        user.setPassword(newPassword);
        saveUsers();
        
        System.out.println("DEBUG: Password changed for user: " + username);
        return true;
    }
    
    /**
     * Change username (only the user themselves can change their username)
     */
    public boolean changeUsername(String oldUsername, String newUsername, User requestingUser) {
        if (oldUsername == null || newUsername == null || requestingUser == null) {
            return false;
        }
        
        // Only allow users to change their own username
        if (!oldUsername.equals(requestingUser.getUsername())) {
            return false;
        }
        
        // Check if new username already exists
        if (userExists(newUsername)) {
            return false;
        }
        
        // Find and update the user
        User user = findUserByUsername(oldUsername);
        if (user != null) {
            user.setUsername(newUsername);
            saveUsers();
            
            System.out.println("DEBUG: Username changed from " + oldUsername + " to " + newUsername);
            return true;
        }
        
        return false;
    }

    /**
     * Deactivate user (only admins, can't deactivate themselves)
     */
    public boolean deactivateUser(String username, User requestingUser) {
        if (requestingUser == null || !requestingUser.isAdmin()) {
            return false;
        }
        
        User user = findUserByUsername(username);
        if (user != null && !user.equals(requestingUser)) { // Can't deactivate self
            user.setActive(false);
            saveUsers();
            return true;
        }
        return false;
    }
    
    /**
     * Delete a user (only admins can delete users)
     */
    public boolean deleteUser(String username, User requestingUser) {
        if (requestingUser == null || !requestingUser.isAdmin()) {
            return false;
        }
        
        User userToDelete = findUserByUsername(username);
        if (userToDelete == null) {
            return false;
        }
        
        // Prevent deleting admin users to maintain system integrity
        if (userToDelete.isAdmin()) {
            return false;
        }
        
        users.remove(userToDelete);
        saveUsers();
        
        System.out.println("DEBUG: Admin " + requestingUser.getUsername() + 
                         " deleted user: " + username);
        return true;
    }

    /**
     * Get count of admin users
     */
    public long getAdminCount() {
        return users.stream()
                .filter(User::isActive)
                .filter(User::isAdmin)
                .count();
    }
    
    /**
     * Get active users count
     */
    public long getActiveUserCount() {
        return users.stream()
                .filter(User::isActive)
                .count();
    }
    
    /**
     * Reset to default admin (emergency function)
     */
    public void resetToDefaultAdmin() {
        users.clear();
        ensureDefaultAdmin();
        System.out.println("DEBUG: Reset to default admin user");
    }

    /**
     * Check if there are any admin users in the system
     */
    public boolean hasAdminUser() {
        return users.stream().anyMatch(User::isAdmin);
    }
    
    /**
     * Add a user directly (used for system setup)
     */
    public boolean addUser(User user) {
        if (user == null || userExists(user.getUsername())) {
            return false;
        }
        
        users.add(user);
        saveUsers();
        return true;
    }

    /**
     * Reset user password by admin (admin only)
     */
    public boolean resetUserPassword(String username, String newPassword, User requestingUser) {
        if (requestingUser == null || !requestingUser.isAdmin()) {
            return false;
        }
        
        if (username == null || newPassword == null || newPassword.trim().isEmpty()) {
            return false;
        }
        
        User user = findUserByUsername(username);
        if (user == null) {
            return false;
        }
        
        // Update password
        user.setPassword(newPassword);
        saveUsers();
        
        System.out.println("DEBUG: Admin " + requestingUser.getUsername() + 
                         " reset password for user: " + username);
        return true;
    }
}
