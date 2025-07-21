package com.login;
import com.login.ui.BillTrackerDashboard;
import com.login.service.UserService;
import com.login.model.User;
import com.login.model.UserRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class LoginApp_Fixed {
    private JFrame frame;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JPanel loginPanel;
    private JPanel registerPanel;

    private UserService userService;
    private boolean loginInProgress = false;

    public LoginApp_Fixed() {
        userService = UserService.getInstance();
        
        // Ensure data directory exists
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        frame = new JFrame("Indian Oil Bill Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 900);
        frame.setMinimumSize(new Dimension(850, 850));
        frame.setLocationRelativeTo(null);
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        createLoginPanel();
        createRegisterPanel();
        
        mainPanel.add(loginPanel, "login");
        mainPanel.add(registerPanel, "register");
        
        cardLayout.show(mainPanel, "login");
        
        frame.add(mainPanel);
        
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                updateComponentSizes();
            }
        });
    }

    private void createLoginPanel() {
        loginPanel = new JPanel(null);
        loginPanel.setBackground(new Color(235, 242, 248));

        // Logo
        final JLabel logoLabel = new JLabel();
        try {
            ImageIcon originalIcon = new ImageIcon("IOCLLOGO.png");
            Image scaledImage = originalIcon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            logoLabel.setIcon(scaledIcon);
        } catch (Exception ex) {
            System.err.println("Error loading logo: " + ex.getMessage());
            logoLabel.setText("IOC");
            logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
            logoLabel.setForeground(new Color(0, 102, 204));
        }
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loginPanel.add(logoLabel);

        // Title
        final JLabel titleLabel = new JLabel("INDIAN OIL BILL TRACKER", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(new Color(0, 102, 204));
        loginPanel.add(titleLabel);

        // Username
        final JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        userLabel.setForeground(new Color(0, 51, 102));
        loginPanel.add(userLabel);

        final JTextField userField = new JTextField();
        userField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        loginPanel.add(userField);

        // Password
        final JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        passLabel.setForeground(new Color(0, 51, 102));
        loginPanel.add(passLabel);

        final JPasswordField passField = new JPasswordField();
        passField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        loginPanel.add(passField);

        // Show password checkbox
        final JCheckBox showPasswordCheckbox = new JCheckBox("Show Password");
        showPasswordCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showPasswordCheckbox.setBackground(new Color(235, 242, 248));
        showPasswordCheckbox.setForeground(new Color(0, 102, 204));
        showPasswordCheckbox.addActionListener(e -> {
            if (showPasswordCheckbox.isSelected()) {
                passField.setEchoChar((char) 0);
            } else {
                passField.setEchoChar('•');
            }
        });
        loginPanel.add(showPasswordCheckbox);

        // Login button
        final JButton loginButton = new JButton("Login") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(0, 120, 215), 0, getHeight(), new Color(0, 80, 170)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        loginButton.setOpaque(false);
        loginButton.setContentAreaFilled(false);
        loginButton.setBorderPainted(false);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(e -> performLogin(userField, passField));
        loginPanel.add(loginButton);

        // Register link
        final JButton registerLink = new JButton("New User? Register Here");
        registerLink.setForeground(new Color(0, 102, 204));
        registerLink.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        registerLink.setBorderPainted(false);
        registerLink.setContentAreaFilled(false);
        registerLink.setFocusPainted(false);
        registerLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerLink.addActionListener(e -> cardLayout.show(mainPanel, "register"));
        loginPanel.add(registerLink);

        // Admin Setup link (only show if no admin exists)
        if (!userService.hasAdminUser()) {
            final JButton adminSetupLink = new JButton("Setup Admin Account");
            adminSetupLink.setForeground(new Color(220, 53, 69));
            adminSetupLink.setBorderPainted(false);
            adminSetupLink.setContentAreaFilled(false);
            adminSetupLink.setFocusPainted(false);
            adminSetupLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
            adminSetupLink.addActionListener(e -> showAdminSetupDialog());
            loginPanel.add(adminSetupLink);
        }

        // Set login button as default for Enter key
        SwingUtilities.invokeLater(() -> {
            JRootPane rootPane = frame.getRootPane();
            if (rootPane != null) {
                rootPane.setDefaultButton(loginButton);
            }
        });

        // Simple layout - position components with fixed layout
        loginPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutLoginComponents(loginPanel, logoLabel, titleLabel, userLabel, userField, 
                                    passLabel, passField, showPasswordCheckbox, loginButton, 
                                    registerLink);
            }
        });
    }

    private void layoutLoginComponents(JPanel panel, JLabel logoLabel, JLabel titleLabel,
                                     JLabel userLabel, JTextField userField,
                                     JLabel passLabel, JPasswordField passField,
                                     JCheckBox showPasswordCheckbox, JButton loginButton,
                                     JButton registerLink) {
        
        int panelWidth = panel.getWidth();
        int panelHeight = panel.getHeight();
        
        if (panelWidth <= 0 || panelHeight <= 0) return;
        
        int centerX = panelWidth / 2;
        int fieldWidth = Math.min(300, panelWidth - 100);
        
        // Logo
        int logoSize = 60;
        int logoY = 30;
        logoLabel.setBounds(centerX - logoSize / 2, logoY, logoSize, logoSize);
        
        // Title
        int titleY = logoY + logoSize + 15;
        int titleWidth = Math.min(titleLabel.getPreferredSize().width, panelWidth - 40);
        titleLabel.setBounds(centerX - titleWidth / 2, titleY, titleWidth, 40);
        
        // Form start
        int formY = titleY + 60;
        
        // Username
        userLabel.setBounds(centerX - fieldWidth / 2, formY, fieldWidth, 20);
        userField.setBounds(centerX - fieldWidth / 2, formY + 25, fieldWidth, 35);
        
        // Password
        passLabel.setBounds(centerX - fieldWidth / 2, formY + 70, fieldWidth, 20);
        passField.setBounds(centerX - fieldWidth / 2, formY + 95, fieldWidth, 35);
        
        // Show password checkbox
        showPasswordCheckbox.setBounds(centerX - fieldWidth / 2, formY + 140, fieldWidth, 25);
        
        // Login button
        int buttonWidth = 150;
        loginButton.setBounds(centerX - buttonWidth / 2, formY + 175, buttonWidth, 40);
        
        // Register link
        int linkWidth = 200;
        registerLink.setBounds(centerX - linkWidth / 2, formY + 230, linkWidth, 25);
        
        // Admin Setup link
        if (!userService.hasAdminUser()) {
            JButton adminSetupLink = (JButton) Arrays.stream(panel.getComponents())
                .filter(comp -> comp instanceof JButton && ((JButton) comp).getText().equals("Setup Admin Account"))
                .findFirst()
                .orElse(null);
            
            if (adminSetupLink != null) {
                adminSetupLink.setBounds(centerX - linkWidth / 2, formY + 260, linkWidth, 25);
            }
        }
        
        panel.revalidate();
        panel.repaint();
    }

    private void performLogin(JTextField userField, JPasswordField passField) {
        if (loginInProgress) {
            System.out.println("DEBUG: Login already in progress");
            return;
        }
        
        loginInProgress = true;
        System.out.println("DEBUG: Starting login process");
        
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        
        System.out.println("DEBUG: Username: " + username);
        System.out.println("DEBUG: Password length: " + password.length());
        
        if (validateLogin(username, password)) {
            System.out.println("DEBUG: Login successful - opening dashboard");
            
            SwingUtilities.invokeLater(() -> {
                try {
                    // Get the authenticated user object
                    User authenticatedUser = userService.authenticateUser(username, password);
                    
                    BillTrackerDashboard dashboard = new BillTrackerDashboard(authenticatedUser);
                    dashboard.setExtendedState(JFrame.NORMAL);
                    dashboard.setVisible(true);
                    dashboard.toFront();
                    dashboard.requestFocus();
                    
                    frame.setVisible(false);
                    dashboard.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosed(WindowEvent e) {
                            // Return to login instead of exiting completely
                            SwingUtilities.invokeLater(() -> {
                                frame.setVisible(true);
                                frame.toFront();
                            });
                        }
                    });
                    
                    SwingUtilities.invokeLater(() -> {
                        dashboard.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    });
                    
                } catch (Exception ex) {
                    System.err.println("ERROR creating dashboard: " + ex.getMessage());
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error opening dashboard: " + ex.getMessage());
                } finally {
                    loginInProgress = false;
                }
            });
        } else {
            System.out.println("DEBUG: Login failed");
            JOptionPane.showMessageDialog(frame, "Invalid username or password", "Login Failed", JOptionPane.ERROR_MESSAGE);
            passField.setText("");
            passField.requestFocus();
            loginInProgress = false;
        }
    }

    private boolean validateLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("DEBUG: Empty username or password");
            return false;
        }
        
        User user = userService.authenticateUser(username, password);
        boolean isValid = user != null;
        System.out.println("DEBUG: Authentication for " + username + ": " + isValid);
        return isValid;
    }

    // Simplified register panel
    private void createRegisterPanel() {
        registerPanel = new JPanel(null);
        registerPanel.setBackground(new Color(240, 248, 255));
        
        JLabel headerLabel = new JLabel("Create New Account", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerLabel.setForeground(new Color(0, 102, 204));
        registerPanel.add(headerLabel);
        
        JLabel userLabelReg = new JLabel("Username:");
        userLabelReg.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerPanel.add(userLabelReg);
        
        JTextField userFieldReg = new JTextField();
        userFieldReg.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        registerPanel.add(userFieldReg);
        
        JLabel passLabelReg = new JLabel("Password:");
        passLabelReg.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerPanel.add(passLabelReg);
        
        JPasswordField passFieldReg = new JPasswordField();
        passFieldReg.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        registerPanel.add(passFieldReg);
        
        JLabel confirmPassLabelReg = new JLabel("Confirm:");
        confirmPassLabelReg.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerPanel.add(confirmPassLabelReg);
        
        JPasswordField confirmPassFieldReg = new JPasswordField();
        confirmPassFieldReg.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        registerPanel.add(confirmPassFieldReg);
        
        JButton registerButtonReg = new JButton("Register");
        registerButtonReg.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerButtonReg.addActionListener(e -> {
            String username = userFieldReg.getText().trim();
            String password = new String(passFieldReg.getPassword()).trim();
            String confirmPassword = new String(confirmPassFieldReg.getPassword()).trim();
            
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "All fields are required", "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(frame, "Passwords do not match", "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (userService.userExists(username)) {
                JOptionPane.showMessageDialog(frame, "Username already exists", "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Register new user with USER role by default
            boolean success = userService.registerNewUser(username, password, UserRole.USER);
            
            if (success) {
                JOptionPane.showMessageDialog(frame, "Registration successful! You can now login.", "Success", JOptionPane.INFORMATION_MESSAGE);
                
                userFieldReg.setText("");
                passFieldReg.setText("");
                confirmPassFieldReg.setText("");
                cardLayout.show(mainPanel, "login");
            } else {
                JOptionPane.showMessageDialog(frame, "Registration failed. Please try again.", "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
            
            userFieldReg.setText("");
            passFieldReg.setText("");
            confirmPassFieldReg.setText("");
            cardLayout.show(mainPanel, "login");
        });
        registerPanel.add(registerButtonReg);
        
        JButton loginLinkReg = new JButton("Already have an account? Login");
        loginLinkReg.setForeground(new Color(0, 102, 204));
        loginLinkReg.setBorderPainted(false);
        loginLinkReg.setContentAreaFilled(false);
        loginLinkReg.setFocusPainted(false);
        loginLinkReg.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLinkReg.addActionListener(e -> cardLayout.show(mainPanel, "login"));
        registerPanel.add(loginLinkReg);

        // Simple layout for register panel
        registerPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = registerPanel.getWidth();
                int centerX = width / 2;
                int fieldWidth = Math.min(300, width - 100);
                
                headerLabel.setBounds(centerX - 150, 50, 300, 40);
                
                int formY = 120;
                userLabelReg.setBounds(centerX - fieldWidth / 2, formY, fieldWidth, 20);
                userFieldReg.setBounds(centerX - fieldWidth / 2, formY + 25, fieldWidth, 30);
                
                passLabelReg.setBounds(centerX - fieldWidth / 2, formY + 70, fieldWidth, 20);
                passFieldReg.setBounds(centerX - fieldWidth / 2, formY + 95, fieldWidth, 30);
                
                confirmPassLabelReg.setBounds(centerX - fieldWidth / 2, formY + 140, fieldWidth, 20);
                confirmPassFieldReg.setBounds(centerX - fieldWidth / 2, formY + 165, fieldWidth, 30);
                
                registerButtonReg.setBounds(centerX - 100, formY + 210, 200, 40);
                loginLinkReg.setBounds(centerX - 125, formY + 260, 250, 30);
                
                registerPanel.revalidate();
                registerPanel.repaint();
            }
        });
    }

    // Complete user management panel
    private void updateComponentSizes() {
        if (mainPanel != null) {
            mainPanel.revalidate();
            mainPanel.repaint();
        }
        if (frame != null) {
            frame.revalidate();
            frame.repaint();
        }
    }

    public void show() {
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            LoginApp_Fixed app = new LoginApp_Fixed();
            app.show();
        });
    }

    /**
     * Show dialog for setting up the first admin user
     */
    private void showAdminSetupDialog() {
        JDialog adminDialog = new JDialog(frame, "Setup Admin Account", true);
        adminDialog.setSize(400, 300);
        adminDialog.setLocationRelativeTo(frame);
        adminDialog.setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Title
        JLabel titleLabel = new JLabel("Create Administrator Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(0, 102, 204));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        contentPanel.add(titleLabel, gbc);
        
        // Username
        gbc.gridwidth = 1; gbc.gridy = 1;
        JLabel userLabel = new JLabel("Admin Username:");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(userLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField adminUserField = new JTextField(15);
        adminUserField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentPanel.add(adminUserField, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(passLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField adminPassField = new JPasswordField(15);
        adminPassField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentPanel.add(adminPassField, gbc);
        
        // Confirm Password
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(confirmLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField confirmPassField = new JPasswordField(15);
        confirmPassField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentPanel.add(confirmPassField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(Color.WHITE);
        
        JButton createButton = new JButton("Create Admin");
        createButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        createButton.setBackground(new Color(40, 167, 69));
        createButton.setForeground(Color.WHITE);
        createButton.setBorderPainted(false);
        createButton.setFocusPainted(false);
        createButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createButton.addActionListener(e -> {
            String username = adminUserField.getText().trim();
            String password = new String(adminPassField.getPassword());
            String confirmPassword = new String(confirmPassField.getPassword());
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(adminDialog, "Username and password are required!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(adminDialog, "Passwords do not match!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (userService.userExists(username)) {
                JOptionPane.showMessageDialog(adminDialog, "Username already exists!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create admin user directly with admin role
            User adminUser = new User(username, password, UserRole.ADMIN, 
                                    "System Administrator", "");
            boolean success = userService.addUser(adminUser);
            
            if (success) {
                JOptionPane.showMessageDialog(adminDialog, 
                    "Admin account created successfully!\nYou can now login with these credentials.", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                adminDialog.dispose();
                
                // Refresh the login panel to hide the admin setup link
                frame.remove(mainPanel);
                mainPanel = new JPanel(cardLayout);
                createLoginPanel();
                createRegisterPanel();
                mainPanel.add(loginPanel, "login");
                mainPanel.add(registerPanel, "register");
                frame.add(mainPanel);
                frame.revalidate();
                frame.repaint();
            } else {
                JOptionPane.showMessageDialog(adminDialog, "Failed to create admin account!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(createButton);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelButton.setBackground(new Color(108, 117, 125));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setBorderPainted(false);
        cancelButton.setFocusPainted(false);
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> adminDialog.dispose());
        buttonPanel.add(cancelButton);
        
        adminDialog.add(contentPanel, BorderLayout.CENTER);
        adminDialog.add(buttonPanel, BorderLayout.SOUTH);
        adminDialog.setVisible(true);
    }
}
