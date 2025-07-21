package com.login.ui;

import com.login.service.BillDataService;
import com.login.service.NetworkVendorManager;
import com.login.service.UserService;
import com.login.model.BillRecord;
import com.login.model.User;
import com.login.model.UserRole;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;
import java.util.Date;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import com.toedter.calendar.JDateChooser;
import java.awt.Desktop;

/**
 * Indian Oil Bill Tracker Dashboard - Complete Implementation
 * Login-protected dashboard with View, Update, and Analytics tabs
 */
public class BillTrackerDashboard extends JFrame implements NetworkVendorManager.NetworkVendorChangeListener {
    private BillDataService billDataService;
    private NetworkVendorManager networkManager;
    private String currentUser;
    
    // User and role management
    private User currentUserObject;
    private UserService userService;
    
    // UI Components
    private JTabbedPane tabbedPane;
    private JPanel viewPanel;
    private JPanel updatePanel;
    private JPanel analyticsPanel;
    
    // View Tab Components
    private JComboBox<String> yearComboBox;
    private JComboBox<String> quarterComboBox;
    private JComboBox<String> networkComboBox;
    private JComboBox<String> vendorComboBox;
    private JTextField searchField;
    private JComboBox<String> sortComboBox;
    // New filter components for View tab
    private JComboBox<String> viewCostCenterComboBox;
    private JComboBox<String> viewGlCodeComboBox;
    private JComboBox<String> viewCommitItemComboBox;
    private JTable billTable;
    private DefaultTableModel tableModel;
    private JTable analyticsTable; // Analytics tab table
      // Update Tab Components
    private JComboBox<String> updateNetworkComboBox;
    private JComboBox<String> updateVendorComboBox;
    private JComboBox<String> locationComboBox;
    private JTextField invoiceNumberField;
    private JTextField billWithTaxField;
    private JTextField billWithoutTaxField;
    private boolean isCalculatingBillAmount = false; // Flag to prevent recursive calculation
    private JTextField ses1Field;
    private JTextField ses2Field;    private JComboBox<String> billingPeriodComboBox;
    private JDateChooser fromDateChooser;
    private JDateChooser toDateChooser;    private JComboBox<String> statusComboBox;
    private JTextArea remarksTextArea;  // JTextArea
    private JScrollPane remarksScrollPane; // Added for scrolling
    private final int REMARKS_CHAR_LIMIT = 250; // Character limit for remarks
    
    // New required fields
    private JComboBox<String> glCodeComboBox;  // GL Code dropdown with predefined values
    private JComboBox<String> commitItemComboBox;  // Commit Item dropdown
    private JComboBox<String> costCenterComboBox;  // Cost Center dropdown
    
    private JLabel pdfFileLabel;
    private JLabel fileSizeLabel;  // Added for PDF file size display
    private JButton submitButton;
    private JButton clearButton;
    private JButton clearPdfButton;  // Added for PDF clear functionality
    private String selectedPdfPath;
    private BillRecord editingRecord;    // Analytics filter components
    private JComboBox<String> analyticsNetworkComboBox;
    private JComboBox<String> analyticsVendorComboBox;
    private JComboBox<String> analyticsQuarterComboBox;
    private JComboBox<String> analyticsYearComboBox;
    private JComboBox<String> analyticsCostCenterComboBox;
    private JComboBox<String> analyticsCommitItemComboBox;

    // Location Options as per requirements
    private static final String[] LOCATIONS = {
        "Sankari Depot (Sankari TOP)", "Arakkonam AFS", "Trichy DO", "Trichy TOP", "Trichy BP",
        "Salem DO", "Mannargudi BP", "Tuticorin Marketing Terminal", "Irugur Marketing Terminal",
        "Coimbatore AFS", "Coimbatore DO", "Sulur AFS", "Coimbatore BP (Pollachi BP)", "Salem BP",
        "Pondicherry BP", "Erode BP", "Coimbatore Packed Bitumen Depot", "Trichy AFS",
        "Mayiladuthurai BP", "Southern RO and TamilNadu SO", "Chennai (CPCL) Refinery Coord. Office",
        "Chennai DO", "Chennai AFS", "Tambaram AFS", "Tuticorin AFS", "Ramnad AFS",
        "Ennore OMC Hospitality (HPCL)", "Tondiarpet TOP", "Chengalpet BP",
        "Chennai Indian Oil Tanking Limited", "Chennai (Tondiarpet) LBP",
        "Chennai (FST) Marketing Terminal", "Madurai TOP", "Madurai BP",
        "Korukkupet Marketing Terminal", "Madurai AFS", "Ilayangudi BP",
        "Chennai (LMT/CIP) Lube Marketing Terminal",
        "ETTPL Ennore Tank Marketing Terminals Private Limited", "Madurai DO", "RIL Ennore",
        "Tirunelveli BP", "Asaanr TOP", "Ennore BP LPG", "Erode CFA", "IPPL Ennore",
        "Hosour ASF", "Coimbatore LMW-COLD"
    };

    public BillTrackerDashboard(User user) {
        this.currentUserObject = user;
        this.currentUser = user.getUsername();
        this.userService = UserService.getInstance();
        this.billDataService = new BillDataService(currentUser);
        this.networkManager = NetworkVendorManager.getInstance();
        
        // Register as listener for network/vendor changes
        networkManager.addChangeListener(this);
        
        // Generate sample data if no records exist
        billDataService.generateSampleData();
          initializeUI();
        setupViewTab();
        setupUpdateTab();
        // Don't setup analytics tab immediately - defer until first access
        
        setTitle("Indian Oil Bill Tracker - Dashboard - " + 
                currentUser + " (" + currentUserObject.getRole() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        // Only show User Management menu for admin users
        if (currentUserObject != null && currentUserObject.isAdmin()) {
            JMenuBar menuBar = new JMenuBar();
            JMenu adminMenu = new JMenu("Admin");
            
            JMenuItem userManagementItem = new JMenuItem("Manage Users");
            userManagementItem.addActionListener(e -> showUserManagementDialog());
            adminMenu.add(userManagementItem);
            
            adminMenu.addSeparator(); // Add separator
            
            JMenuItem changePasswordItem = new JMenuItem("Change My Password");
            changePasswordItem.addActionListener(e -> showChangePasswordDialog());
            adminMenu.add(changePasswordItem);
            
            JMenuItem changeUsernameItem = new JMenuItem("Change My Username");
            changeUsernameItem.addActionListener(e -> showChangeUsernameDialog());
            adminMenu.add(changeUsernameItem);
            
            JMenuItem adminManagementItem = new JMenuItem("Manage Admin Accounts");
            adminManagementItem.addActionListener(e -> showAdminManagementDialog());
            adminMenu.add(adminManagementItem);
            
            menuBar.add(adminMenu);
            setJMenuBar(menuBar);
        }
    }

    /**
     * Initialize the main UI structure
     */
    private void initializeUI() {
        setLayout(new BorderLayout());
          // Create tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 16));
          viewPanel = new JPanel(new BorderLayout());
        updatePanel = new JPanel(new BorderLayout());
        analyticsPanel = new JPanel(new BorderLayout());
          // Add tabs as per requirements
        tabbedPane.addTab("View", viewPanel);
        tabbedPane.addTab("Update", updatePanel);
        tabbedPane.addTab("Analytics", analyticsPanel);
        
        // Only show Update Network tab for admin users
        if (currentUserObject != null && currentUserObject.isAdmin()) {
            tabbedPane.addTab("Update Network", new UpdateNetworkTab());
        }
        
        // Add tab change listener to handle lazy initialization and refreshing
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            
            if (selectedIndex == 0) { // View tab is selected
                populateYearComboBox(); // Refresh year dropdown
            } else if (selectedIndex == 2) { // Analytics tab is selected
                // Setup full analytics tab with filters and table
                if (analyticsPanel.getComponentCount() == 0) {
                    setupAnalyticsTab();
                }
            }
            
            // Clear any table selections when switching tabs
            if (billTable != null) {
                billTable.clearSelection();
            }
            if (analyticsTable != null) {
                analyticsTable.clearSelection();
            }
        });
        
        // Add click listener to tabbed pane to clear selections
        tabbedPane.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (billTable != null) {
                    billTable.clearSelection();
                }
                if (analyticsTable != null) {
                    analyticsTable.clearSelection();
                }
            }
        });
          add(tabbedPane, BorderLayout.CENTER);
        
        // Add header panel with title and logout button
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
    }
    
    /**
     * Create header panel with title and logout button
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title label
        JLabel headerLabel = new JLabel("Indian Oil Bill Tracker Dashboard", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        headerLabel.setForeground(new Color(0, 102, 204));
        
        // Logout button
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logoutButton.setBackground(new Color(220, 53, 69)); // Bootstrap danger red
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setBorderPainted(false);
        logoutButton.setPreferredSize(new Dimension(100, 35));
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        logoutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                logoutButton.setBackground(new Color(200, 35, 51)); // Darker red on hover
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                logoutButton.setBackground(new Color(220, 53, 69)); // Original red
            }
        });
        
        // Logout action
        logoutButton.addActionListener(e -> performLogout());
        
        // Add components to header panel
        headerPanel.add(headerLabel, BorderLayout.CENTER);
        headerPanel.add(logoutButton, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    /**
     * Perform logout and return to login screen
     */
    private void performLogout() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to logout?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            // Clear any unsaved data or session information
            currentUser = null;
            editingRecord = null;
            selectedPdfPath = null;
            
            // Close dashboard and return to login
            this.setVisible(false);
            this.dispose();
              // Show login window again
            SwingUtilities.invokeLater(() -> {
                try {
                    // Create new instance of login app
                    com.login.LoginApp_Fixed loginApp = new com.login.LoginApp_Fixed();
                    loginApp.show();
                } catch (Exception ex) {
                    System.err.println("Error returning to login: " + ex.getMessage());
                    ex.printStackTrace();
                    System.exit(0);
                }
            });
        }
    }
    
    /**
     * Configure smooth scrolling for a JScrollPane component
     * This improves the user experience by making scrolling more responsive and smooth
     */
    private void configureSmoothScrolling(JScrollPane scrollPane) {
        // Set scroll unit increments for smoother scrolling
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Smooth vertical scrolling
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16); // Smooth horizontal scrolling
        
        // Set block increments for page up/down scrolling
        scrollPane.getVerticalScrollBar().setBlockIncrement(50);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(50);
        
        // Make scrollbars more responsive and easier to handle
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
        
        // Increase scrollbar width for easier handling
        verticalScrollBar.setPreferredSize(new Dimension(16, Integer.MAX_VALUE));
        horizontalScrollBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 16));
        
        // Add custom mouse wheel listener for ultra-smooth scrolling
        scrollPane.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Get the appropriate scroll bar based on modifier keys
                JScrollBar scrollBar;
                if (e.isShiftDown()) {
                    // Shift + wheel = horizontal scrolling
                    scrollBar = scrollPane.getHorizontalScrollBar();
                } else {
                    // Normal wheel = vertical scrolling
                    scrollBar = scrollPane.getVerticalScrollBar();
                }
                
                // Calculate smooth scroll amount
                int scrollAmount = e.getUnitsToScroll() * 16; // Multiply by unit increment
                
                // Apply smooth scrolling
                int newValue = scrollBar.getValue() + scrollAmount;
                
                // Ensure we stay within bounds
                newValue = Math.max(scrollBar.getMinimum(), 
                          Math.min(scrollBar.getMaximum() - scrollBar.getVisibleAmount(), newValue));
                
                // Set the new value smoothly
                scrollBar.setValue(newValue);
                
                // Consume the event to prevent default scrolling
                e.consume();
            }
        });
        
        // Enable smooth scrolling on the viewport as well
        scrollPane.getViewport().addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Let the scroll pane handle it
                scrollPane.dispatchEvent(e);
            }
        });
        
        // Set smoother scrolling policy
        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
    }

    /**
     * Setup View Tab - Year/Quarter filtering with table display
     */
    private void setupViewTab() {        // Create filter panel
        JPanel filterPanel = createFilterPanel();
        viewPanel.add(filterPanel, BorderLayout.NORTH);
          // Create table
        createBillTable();
        JScrollPane scrollPane = new JScrollPane(billTable);
        
        // Enhanced scroll pane configuration for maximum header visibility and horizontal scrolling
        scrollPane.setColumnHeaderView(billTable.getTableHeader());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // Configure smooth scrolling for better user experience
        configureSmoothScrolling(scrollPane);
        
        // Force horizontal scrolling when needed
        billTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        // Force header visibility in scroll pane
        scrollPane.getColumnHeader().setOpaque(true);
        scrollPane.getColumnHeader().setBackground(new Color(0, 102, 204));
        scrollPane.setColumnHeader(scrollPane.getColumnHeader());
        
        // Ensure proper viewport configuration
        scrollPane.getViewport().setOpaque(true);
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        
        // Additional header visibility enforcement
        if (billTable.getTableHeader() != null) {
            billTable.getTableHeader().setVisible(true);
            billTable.getTableHeader().setOpaque(true);
            billTable.getTableHeader().repaint();
        }
        
        // Enhanced titled border for "Billing Records" with better visibility
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204), 2),
            "Billing Records",
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 16),
            new Color(0, 102, 204)
        );
        scrollPane.setBorder(titledBorder);
        
        // Add click listeners to clear table selection when clicking elsewhere
        viewPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                billTable.clearSelection();
            }
        });
        
        scrollPane.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Only clear selection if click is outside the table area
                if (!billTable.getBounds().contains(e.getPoint())) {
                    billTable.clearSelection();
                }
            }
        });
        
        // Add click listener to the main frame to clear selection
        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                billTable.clearSelection();
            }
        });
        
        viewPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Load initial data
        loadTableData();
    }    /**
     * Create comprehensive filter panel with Year/Quarter filters, Search, and Sorting
     */
    private JPanel createFilterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204), 2),
            "Filter, Search & Sort Options",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(0, 102, 204)
        ));
        mainPanel.setBackground(new Color(248, 249, 250));
        mainPanel.setPreferredSize(new Dimension(0, 200));
        
        // Create a container for all three rows
        JPanel allRowsPanel = new JPanel();
        allRowsPanel.setLayout(new BoxLayout(allRowsPanel, BoxLayout.Y_AXIS));
        allRowsPanel.setBackground(new Color(248, 249, 250));
        
        // First row - Filter options (Year & Quarter)
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        filterRow.setBackground(new Color(248, 249, 250));
        
        // Year dropdown
        JLabel yearLabel = new JLabel("Year:");
        yearLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        filterRow.add(yearLabel);
        
        yearComboBox = new JComboBox<>();
        yearComboBox.setPreferredSize(new Dimension(120, 30));
        yearComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        populateYearComboBox();
        yearComboBox.addActionListener(e -> loadTableData());
        filterRow.add(yearComboBox);
          // Quarter dropdown
        JLabel quarterLabel = new JLabel("Quarter:");
        quarterLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        filterRow.add(quarterLabel);        quarterComboBox = new JComboBox<>();
        quarterComboBox.setPreferredSize(new Dimension(180, 30));
        quarterComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        populateViewQuarterComboBox(); // Initialize with all quarters
        quarterComboBox.addActionListener(e -> loadTableData());
        filterRow.add(quarterComboBox);
        
        // Network dropdown
        JLabel networkLabel = new JLabel("Network:");
        networkLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        filterRow.add(networkLabel);
          networkComboBox = new JComboBox<>();
        networkComboBox.setPreferredSize(new Dimension(180, 30));
        networkComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        populateNetworkComboBox();
        networkComboBox.addActionListener(e -> {
            populateViewQuarterComboBox(); // Update quarters based on network selection
            populateVendorComboBox();
            loadTableData();
        });
        filterRow.add(networkComboBox);
        
        // Vendor dropdown
        JLabel vendorLabel = new JLabel("Vendor:");
        vendorLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        filterRow.add(vendorLabel);
        
        vendorComboBox = new JComboBox<>();
        vendorComboBox.setPreferredSize(new Dimension(200, 30));
        vendorComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));        populateVendorComboBox();
        vendorComboBox.addActionListener(e -> loadTableData());
        filterRow.add(vendorComboBox);
        
        // Refresh button
        JButton refreshButton = new JButton("Refresh Data");
        refreshButton.setPreferredSize(new Dimension(140, 35));
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        refreshButton.setBackground(new Color(0, 120, 215));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setOpaque(true);
        refreshButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        refreshButton.setToolTipText("Refresh and reload all bill records");
        refreshButton.addActionListener(e -> {
            populateYearComboBox();
            populateViewCostCenterComboBox();
            populateViewGlCodeComboBox();
            populateViewCommitItemComboBox();
            loadTableData();
        });
        filterRow.add(refreshButton);
        
        // Second row - Additional filters (Cost Center, GL Code, Commit Item)
        JPanel additionalFilterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        additionalFilterRow.setBackground(new Color(248, 249, 250));
        
        // Cost Center dropdown
        JLabel costCenterLabel = new JLabel("Cost Center:");
        costCenterLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        additionalFilterRow.add(costCenterLabel);
        
        viewCostCenterComboBox = new JComboBox<>();
        viewCostCenterComboBox.setPreferredSize(new Dimension(150, 30));
        viewCostCenterComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        populateViewCostCenterComboBox();
        viewCostCenterComboBox.addActionListener(e -> loadTableData());
        additionalFilterRow.add(viewCostCenterComboBox);
        
        // GL Code dropdown
        JLabel glCodeLabel = new JLabel("GL Code:");
        glCodeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        additionalFilterRow.add(glCodeLabel);
        
        viewGlCodeComboBox = new JComboBox<>();
        viewGlCodeComboBox.setPreferredSize(new Dimension(150, 30));
        viewGlCodeComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        populateViewGlCodeComboBox();
        viewGlCodeComboBox.addActionListener(e -> loadTableData());
        additionalFilterRow.add(viewGlCodeComboBox);
        
        // Commit Item dropdown
        JLabel commitItemLabel = new JLabel("Commit Item:");
        commitItemLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        additionalFilterRow.add(commitItemLabel);
        
        viewCommitItemComboBox = new JComboBox<>();
        viewCommitItemComboBox.setPreferredSize(new Dimension(150, 30));
        viewCommitItemComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        populateViewCommitItemComboBox();
        viewCommitItemComboBox.addActionListener(e -> loadTableData());
        additionalFilterRow.add(viewCommitItemComboBox);

        // Third row - Search options
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        searchRow.setBackground(new Color(248, 249, 250));
        
        // Search field
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchRow.add(searchLabel);
        
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(300, 30));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        searchField.setToolTipText("Search by location, invoice number, status, or remarks");
        
        // Add real-time search functionality
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                loadTableData(); // Refresh table data when user types
            }
        });
        searchRow.add(searchField);
          // Clear search button
        JButton clearSearchButton = new JButton("Clear");
        clearSearchButton.setPreferredSize(new Dimension(90, 30));
        clearSearchButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        clearSearchButton.setBackground(new Color(244, 67, 54));
        clearSearchButton.setForeground(Color.WHITE);
        clearSearchButton.setFocusPainted(false);
        clearSearchButton.setBorderPainted(false);
        clearSearchButton.setOpaque(true);
        clearSearchButton.setToolTipText("Clear search field and show all records");
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            loadTableData();
        });
        searchRow.add(clearSearchButton);
        
        // Fourth row - Sort options
        JPanel sortRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        sortRow.setBackground(new Color(248, 249, 250));
        
        // Sort by dropdown
        JLabel sortLabel = new JLabel("Sort by:");
        sortLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sortRow.add(sortLabel);
        
        sortComboBox = new JComboBox<>(new String[]{
            "Default (Serial No.)",
            "Location (A-Z)",
            "Location (Z-A)",
            "Bill Amount (Low to High)",
            "Bill Amount (High to Low)",
            "Status (Completed First)",
            "Status (Pending First)",
            "Date (Newest First)",
            "Date (Oldest First)"
        });
        sortComboBox.setPreferredSize(new Dimension(200, 30));
        sortComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sortComboBox.addActionListener(e -> loadTableData());
        sortRow.add(sortComboBox);
        
        // Add info label
        JLabel infoLabel = new JLabel("Tip: Use filters, search, and sorting to find and organize your bill records");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(new Color(100, 100, 100));
        sortRow.add(infoLabel);
        
        // Add all rows to the container
        allRowsPanel.add(filterRow);
        allRowsPanel.add(Box.createVerticalStrut(5)); // Small spacing
        allRowsPanel.add(additionalFilterRow);
        allRowsPanel.add(Box.createVerticalStrut(5)); // Small spacing
        allRowsPanel.add(searchRow);
        allRowsPanel.add(Box.createVerticalStrut(5)); // Small spacing
        allRowsPanel.add(sortRow);
        
        mainPanel.add(allRowsPanel, BorderLayout.CENTER);
        
        return mainPanel;
    }
      /**
     * Populate year dropdown with available years from database
     */
    private void populateYearComboBox() {
        yearComboBox.removeAllItems();
        yearComboBox.addItem("All Years");
        
        List<Integer> years = billDataService.getAvailableYears();
        if (years.isEmpty()) {
            yearComboBox.addItem(String.valueOf(LocalDate.now().getYear()));
        } else {
            for (Integer year : years) {
                yearComboBox.addItem(year.toString());
            }
        }
    }    /**
     * Populate network dropdown with available networks
     */
    private void populateNetworkComboBox() {
        networkComboBox.removeAllItems();
        networkComboBox.addItem("All Networks");
        
        // Get networks from NetworkVendorManager
        List<String> networks = networkManager.getAllNetworks();
        for (String network : networks) {
            networkComboBox.addItem(network);
        }
        
        // Add action listener to update quarter and vendor dropdowns when network changes
        networkComboBox.addActionListener(e -> {
            populateViewQuarterComboBox();
            populateVendorComboBox();
            loadTableData();
        });
    }    /**
     * Populate vendor dropdown based on selected network
     */
    private void populateVendorComboBox() {
        if (vendorComboBox == null) return; // Safety check
        
        vendorComboBox.removeAllItems();
        vendorComboBox.addItem("All Vendors");
        
        String selectedNetwork = (networkComboBox != null) ? (String) networkComboBox.getSelectedItem() : null;
        if (selectedNetwork != null && !"All Networks".equals(selectedNetwork)) {
            // Get vendors for the selected network from NetworkVendorManager
            List<String> vendors = networkManager.getVendorsForNetwork(selectedNetwork);
            if (vendors != null) {
                for (String vendor : vendors) {
                    vendorComboBox.addItem(vendor);
                }
            }
            vendorComboBox.setToolTipText("Filter by " + selectedNetwork + " vendors");
        } else {
            vendorComboBox.setToolTipText("Select a network first to filter by vendor");
        }
    }
    
    /**
     * Populate View tab quarter dropdown - shows quarters based on network selection
     * Uses dynamic quarter configurations from NetworkVendorManager
     */
    private void populateViewQuarterComboBox() {
        if (quarterComboBox == null) return; // Safety check
        
        quarterComboBox.removeAllItems();
        quarterComboBox.addItem("All Quarters");
        
        String selectedNetwork = (networkComboBox != null) ? (String) networkComboBox.getSelectedItem() : null;
        if (selectedNetwork != null && !"All Networks".equals(selectedNetwork)) {
            // Get formatted quarters for the selected network from NetworkVendorManager
            List<String> quarters = networkManager.getFormattedQuarters(selectedNetwork);
            if (quarters != null && !quarters.isEmpty()) {
                for (String quarter : quarters) {
                    quarterComboBox.addItem(quarter);
                }
                System.out.println("Loaded " + quarters.size() + " quarters for network: " + selectedNetwork); // Debug
            } else {
                // Fallback to default quarters if no configuration found
                quarterComboBox.addItem("Quarter 1");
                quarterComboBox.addItem("Quarter 2");
                quarterComboBox.addItem("Quarter 3");
                quarterComboBox.addItem("Quarter 4");
                System.out.println("No quarter configuration found for " + selectedNetwork + ", using default quarters"); // Debug
            }
        } else {
            // When "All Networks" is selected, show generic quarters
            quarterComboBox.addItem("Quarter 1");
            quarterComboBox.addItem("Quarter 2");
            quarterComboBox.addItem("Quarter 3");
            quarterComboBox.addItem("Quarter 4");
        }
    }
    
    /**
     * Populate update form network dropdown
     */
    private void populateUpdateNetworkComboBox() {
        updateNetworkComboBox.removeAllItems();
        updateNetworkComboBox.addItem("Select Network");
        
        // Get networks from NetworkVendorManager
        List<String> networks = networkManager.getAllNetworks();
        for (String network : networks) {
            updateNetworkComboBox.addItem(network);
        }
    }
      /**
     * Populate update form vendor dropdown based on selected network
     * Uses the correct vendor mappings from NetworkVendorManager
     * Auto-selects vendor if only one vendor is available
     */
    private void populateUpdateVendorComboBox() {
        System.out.println("populateUpdateVendorComboBox called"); // Debug output
        updateVendorComboBox.removeAllItems();
        
        String selectedNetwork = (String) updateNetworkComboBox.getSelectedItem();
        System.out.println("Selected network: " + selectedNetwork); // Debug output
        if (selectedNetwork == null || selectedNetwork.equals("Select Network")) {
            updateVendorComboBox.addItem("Select Vendor");
            updateVendorComboBox.setEnabled(false);
            updateVendorComboBox.setToolTipText("Select a network first");
            return;
        }
        
        // Get vendors for the selected network from NetworkVendorManager
        List<String> vendors = networkManager.getVendorsForNetwork(selectedNetwork);
        System.out.println("Vendors for " + selectedNetwork + ": " + vendors); // Debug output
        if (vendors != null && !vendors.isEmpty()) {
            updateVendorComboBox.addItem("Select Vendor");
            for (String vendor : vendors) {
                updateVendorComboBox.addItem(vendor);
            }
            updateVendorComboBox.setEnabled(true);
            updateVendorComboBox.setToolTipText("Select from available " + selectedNetwork + " vendors");
            
            // Auto-select vendor if only one vendor is available
            if (vendors.size() == 1) {
                updateVendorComboBox.setSelectedItem(vendors.get(0));
                System.out.println("Auto-selected vendor: " + vendors.get(0)); // Debug output
            }
        } else {
            updateVendorComboBox.addItem("No vendors available");
            updateVendorComboBox.setEnabled(false);
            updateVendorComboBox.setToolTipText("No vendors configured for " + selectedNetwork);
        }
        
        // Update submit button state after vendor population
        updateSubmitButtonState();
    }

    /**
     * Populate View tab Cost Center dropdown
     */
    private void populateViewCostCenterComboBox() {
        if (viewCostCenterComboBox == null) return;
        
        viewCostCenterComboBox.removeAllItems();
        viewCostCenterComboBox.addItem("All Cost Centers");
        
        // Get unique cost centers from database
        List<BillRecord> allRecords = billDataService.getAllBillRecords();
        allRecords.stream()
            .map(BillRecord::getCostCenter)
            .filter(cc -> cc != null && !cc.trim().isEmpty())
            .distinct()
            .sorted()
            .forEach(viewCostCenterComboBox::addItem);
    }
    
    /**
     * Populate View tab GL Code dropdown
     */
    private void populateViewGlCodeComboBox() {
        if (viewGlCodeComboBox == null) return;
        
        viewGlCodeComboBox.removeAllItems();
        viewGlCodeComboBox.addItem("All GL Codes");
        
        // Add predefined GL codes
        String[] predefinedGlCodes = {
            "5261300020",
            "5281525560", 
            "5290700080",
            "5290700020",
            "5290700160"
        };
        
        for (String glCode : predefinedGlCodes) {
            viewGlCodeComboBox.addItem(glCode);
        }
    }
    
    /**
     * Populate View tab Commit Item dropdown
     */
    private void populateViewCommitItemComboBox() {
        if (viewCommitItemComboBox == null) return;
        
        viewCommitItemComboBox.removeAllItems();
        viewCommitItemComboBox.addItem("All Commit Items");
        
        // Get unique commit items from database
        List<BillRecord> allRecords = billDataService.getAllBillRecords();
        allRecords.stream()
            .map(BillRecord::getCommitItem)
            .filter(ci -> ci != null && !ci.trim().isEmpty())
            .distinct()
            .sorted()
            .forEach(viewCommitItemComboBox::addItem);
    }

    /**
     * Update billing period options based on selected network using dynamic quarter configurations
     */
    private void updateBillingPeriodOptions() {
        System.out.println("updateBillingPeriodOptions called"); // Debug output
        if (billingPeriodComboBox == null || updateNetworkComboBox == null) return; // Safety check
        
        billingPeriodComboBox.removeAllItems();
        
        String selectedNetwork = (String) updateNetworkComboBox.getSelectedItem();
        System.out.println("Billing period update for network: " + selectedNetwork); // Debug output
        if (selectedNetwork == null || selectedNetwork.equals("Select Network")) {
            billingPeriodComboBox.addItem("Select Period");
            return;
        }
        
        // Get formatted quarters from NetworkVendorManager
        List<String> quarters = networkManager.getFormattedQuarters(selectedNetwork);
        if (quarters != null && !quarters.isEmpty()) {
            for (String quarter : quarters) {
                billingPeriodComboBox.addItem(quarter);
            }
            System.out.println("Added " + quarters.size() + " quarters for " + selectedNetwork + ": " + quarters);
        } else {
            // Fallback to default quarters if no configuration found
            billingPeriodComboBox.addItem("Q1 (April-June)");
            billingPeriodComboBox.addItem("Q2 (July-September)");
            billingPeriodComboBox.addItem("Q3 (October-December)");
            billingPeriodComboBox.addItem("Q4 (January-March)");
            System.out.println("Added default quarters for " + selectedNetwork);
        }
    }    /**
     * Auto-fill date fields based on selected billing period using dynamic quarter configurations
     */
    private void autoFillDates() {
        if (billingPeriodComboBox == null || updateNetworkComboBox == null || 
            fromDateChooser == null || toDateChooser == null) return; // Safety check
            
        String selectedBillingPeriod = (String) billingPeriodComboBox.getSelectedItem();
        String selectedNetwork = (String) updateNetworkComboBox.getSelectedItem();
        
        if (selectedBillingPeriod == null || selectedNetwork == null) return;
        
        LocalDate currentDate = LocalDate.now();
        int year = currentDate.getYear();
        
        // Extract quarter number from the billing period string
        int quarterNumber = extractQuarterNumber(selectedBillingPeriod);
        if (quarterNumber == -1) return;
        
        // Get quarter configuration from NetworkVendorManager
        NetworkVendorManager.QuarterPeriod quarterPeriod = networkManager.getQuarterPeriod(selectedNetwork, quarterNumber);
        
        if (quarterPeriod != null) {
            try {
                int startMonth = quarterPeriod.getStartMonth();
                int endMonth = quarterPeriod.getEndMonth();
                
                // Handle cross-year quarters (e.g., Oct-Mar)
                if (startMonth > endMonth) {
                    // Cross-year quarter
                    if (currentDate.getMonthValue() >= startMonth) {
                        // Current year start to next year end
                        fromDateChooser.setDate(java.sql.Date.valueOf(LocalDate.of(year, startMonth, 1)));
                        toDateChooser.setDate(java.sql.Date.valueOf(LocalDate.of(year + 1, endMonth, getLastDayOfMonth(endMonth, year + 1))));
                    } else {
                        // Previous year start to current year end
                        fromDateChooser.setDate(java.sql.Date.valueOf(LocalDate.of(year - 1, startMonth, 1)));
                        toDateChooser.setDate(java.sql.Date.valueOf(LocalDate.of(year, endMonth, getLastDayOfMonth(endMonth, year))));
                    }
                } else {
                    // Same-year quarter
                    fromDateChooser.setDate(java.sql.Date.valueOf(LocalDate.of(year, startMonth, 1)));
                    toDateChooser.setDate(java.sql.Date.valueOf(LocalDate.of(year, endMonth, getLastDayOfMonth(endMonth, year))));
                }
                
                System.out.println("Auto-filled dates for " + selectedNetwork + " " + selectedBillingPeriod + 
                                 ": " + quarterPeriod.getMonthRange());
                
            } catch (Exception e) {
                System.err.println("Error auto-filling dates: " + e.getMessage());
            }
        } else {
            System.err.println("No quarter configuration found for " + selectedNetwork + " quarter " + quarterNumber);
        }
    }
    
    /**
     * Extract quarter number from billing period string (e.g., "Q1 (October-March)" -> 1)
     */
    private int extractQuarterNumber(String billingPeriod) {
        if (billingPeriod == null) return -1;
        
        // Look for "Q" followed by a digit
        for (int i = 0; i < billingPeriod.length() - 1; i++) {
            if (billingPeriod.charAt(i) == 'Q' && Character.isDigit(billingPeriod.charAt(i + 1))) {
                return Character.getNumericValue(billingPeriod.charAt(i + 1));
            }
        }
        return -1;
    }
    
    /**
     * Get last day of month considering leap years
     */
    private int getLastDayOfMonth(int month, int year) {
        switch (month) {
            case 2: // February
                return (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28;
            case 4: case 6: case 9: case 11: // April, June, September, November
                return 30;
            default: // January, March, May, July, August, October, December
                return 31;
        }
    }
    
    /**
     * Clear date fields
     */
    private void clearDateFields() {
        fromDateChooser.setDate(null);
        toDateChooser.setDate(null);
    }

    /**
     * Create bill table with all required columns
     */    private void createBillTable() {        String[] columnNames = {
            "S.No", "Network", "Vendor", "Location", "Invoice #", "Bill w/Tax", "Bill w/o Tax",
            "SES 1", "SES 2", "Billing Period", "Status", "GL Code", "Commit Item", "Cost Center", 
            "Remarks", "Download PDF", "Edit", "Delete"
        };tableModel = new DefaultTableModel(columnNames, 0) {            @Override            public boolean isCellEditable(int row, int column) {
                // Only action button columns (15-17) are editable for button clicks
                return column >= 15 && column <= 17;
            }
              @Override
            public Class<?> getColumnClass(int columnIndex) {
                // Set Integer class for SES columns (now columns 7 and 8)
                if (columnIndex == 7 || columnIndex == 8) {
                    return Integer.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };// Create a table with automatic row height adjustment for better remarks display
        billTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component comp = super.prepareRenderer(renderer, row, column);
                  // For remarks column (now column 11), adjust row height as needed
                if (column == 11) {
                    int preferredHeight = comp.getPreferredSize().height;
                    
                    // Ensure minimum height of 100 pixels, or more if content requires it
                    if (preferredHeight > getRowHeight(row)) {
                        setRowHeight(row, Math.max(100, preferredHeight));
                    }
                }
                
                return comp;
            }
        };        // Reduce base row height for better overall table display
        billTable.setRowHeight(50);
        billTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        billTable.setGridColor(new Color(220, 220, 220));
        billTable.setSelectionBackground(new Color(184, 207, 229));
          // Enhanced table header styling - Maximum visibility
        JTableHeader header = billTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12)); // Smaller font for better text visibility // Slightly smaller font
        header.setBackground(new Color(0, 102, 204));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 40)); // Reduced height for more compact display
        header.setOpaque(true);
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
          // Enhanced custom header renderer with maximum visibility properties
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {                String text = value != null ? value.toString() : "";
                
                // Use HTML to enable text wrapping for column headers with better spacing
                String htmlText = "<html><div style='text-align:center;width:100%;padding:1px'>" + text + "</div></html>"; // Minimal padding
                JLabel label = new JLabel(htmlText);
                
                // Set all visibility properties
                label.setBackground(new Color(0, 102, 204));
                label.setForeground(Color.WHITE);
                label.setFont(new Font("Segoe UI", Font.BOLD, 12)); // Smaller font for better text visibility
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setVerticalAlignment(SwingConstants.CENTER);
                label.setOpaque(true);
                // Note: setBorderPainted() is not available for JLabel, using setBorder() instead
                
                // Enhanced border for better visibility
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.WHITE, 1),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2) // Minimal border padding
                ));
                
                // Force component properties
                label.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
                label.setEnabled(true);
                label.setVisible(true);
                label.setDisplayedMnemonic(0);
                
                return label;
            }
        });
          // Enhanced cell renderer with status text coloring only
        billTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                JLabel label = (JLabel)c;
                
                // Set default alignment and padding for all columns
                label.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
                
                // Adjust font size for better visibility
                label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                
                // Keep all cell backgrounds white (no background coloring)
                if (!isSelected) {
                    label.setBackground(Color.WHITE);
                    label.setForeground(Color.BLACK); // Default text color
                }
                
                // Specific column handling
                if (column == 0) { // S.No column
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    label.setFont(new Font("Segoe UI", Font.BOLD, 12)); // Make S.No bold
                } else if (column == 7 || column == 8) { // SES1 and SES2 columns (updated positions)
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    
                    // Ensure SES values are displayed as integers
                    if (value != null) {
                        // Convert to String to remove decimal point
                        if (value instanceof Number) {
                            int intValue = ((Number) value).intValue();
                            label.setText(String.valueOf(intValue));
                        }
                        if (value instanceof Double) {
                            label.setText(String.valueOf((int)((Double)value).doubleValue()));
                        } else if (value instanceof Integer) {
                            label.setText(String.valueOf(value));
                        }
                    }
                } else if (column == 10 && value != null) { // Status column (updated position)
                    label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    
                    // Apply text color based on status (only if not selected)
                    if (!isSelected) {
                        String status = value.toString();
                        if ("Completed".equalsIgnoreCase(status)) {
                            label.setForeground(new Color(0, 128, 0)); // Green text for Completed
                        } else if ("Pending".equalsIgnoreCase(status)) {
                            label.setForeground(new Color(220, 20, 60)); // Red text for Pending
                        } else {
                            label.setForeground(Color.BLACK); // Default text color
                        }
                    }
                } else {
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                }
                
                return c;
            }
        });        // Set column widths - OPTIMIZED FOR 20-DIGIT NUMERIC FIELDS:
        // - SES 1, SES 2, and GL Code increased for 20-digit support
        // - Other columns remain optimally sized for their content
        // - Action button columns sized to fit text exactly
        // - Optimized for complete text visibility in all columns
        int[] columnWidths = {35, 70, 180, 200, 110, 140, 140, 160, 160, 200, 130, 150, 120, 120, 600, 80, 60, 60};
        for (int i = 0; i < columnWidths.length && i < billTable.getColumnCount(); i++) {
            billTable.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
        }        // Setup action button columns with individual renderers to fix visibility issues
        if (billTable.getColumnModel().getColumnCount() > 10) {
            // Create separate renderer instances for each action column to prevent conflicts
            
            // Download PDF column - specific renderer and editor
            TableColumn downloadColumn = billTable.getColumn("Download PDF");
            downloadColumn.setCellRenderer(new ActionButtonRenderer());
            downloadColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
            downloadColumn.setPreferredWidth(80);
            downloadColumn.setMinWidth(70);
            downloadColumn.setMaxWidth(90);
            
            // Edit column - specific renderer and editor
            TableColumn editColumn = billTable.getColumn("Edit");
            editColumn.setCellRenderer(new ActionButtonRenderer());
            editColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
            editColumn.setPreferredWidth(60);
            editColumn.setMinWidth(50);
            editColumn.setMaxWidth(70);
            
            // Delete column - specific renderer and editor
            TableColumn deleteColumn = billTable.getColumn("Delete");
            deleteColumn.setCellRenderer(new ActionButtonRenderer());
            deleteColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
            deleteColumn.setPreferredWidth(60);
            deleteColumn.setMinWidth(50);
            deleteColumn.setMaxWidth(70);
            
            // Billing Period column - custom renderer for multiline display
            TableColumn billingPeriodColumn = billTable.getColumn("Billing Period");
            billingPeriodColumn.setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, 
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    
                    JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                    
                    // Center-align text
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    
                    // Set appropriate row height to accommodate two lines
                    if (table.getRowHeight(row) < 60) {
                        table.setRowHeight(row, 60);
                    }
                    
                    return label;
                }
            });
        }// COMPLETELY REWRITTEN: Simple and reliable mouse click handler for all action buttons
        billTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                handleTableClick(evt);
            }
        });// IMPROVED SOLUTION: Custom renderer for remarks that ensures ALL text is visible
            billTable.getColumn("Remarks").setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    
                    // Use a more HTML-focused approach for better text display
                    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    
                    if (value != null && !value.toString().trim().isEmpty()) {
                        String fullText = value.toString();
                        
                        // Create enhanced HTML formatted text that wraps and shows EVERYTHING
                        String htmlText = "<html><div style='width:580px; padding:5px; font-family:Segoe UI; font-size:13px;'>" + 
                                         fullText.replace("\n", "<br>") + 
                                         "</div></html>";
                        label.setText(htmlText);
                        
                        // Set tooltip for hover viewing
                        label.setToolTipText("<html><div style='width:500px; padding:10px; font-size:13px;'>" +
                                           fullText.replace("\n", "<br>") + 
                                           "</div></html>");
                    } else {
                        label.setText("");
                        label.setToolTipText(null);
                    }
                    
                    // Set vertical alignment to top
                    label.setVerticalAlignment(SwingConstants.TOP);
                    
                    return label;
                }
            });
        }
    /**
     * Load and display table data based on filter, search, and sort selection     */    private void loadTableData() {
        // Safety check to prevent null pointer exceptions
        if (tableModel == null) {
            System.err.println("Warning: tableModel is null in loadTableData()");
            return;
        }
        
        // Clear existing rows
        tableModel.setRowCount(0);
        
        String selectedYear = (String) yearComboBox.getSelectedItem();
        String selectedQuarter = (String) quarterComboBox.getSelectedItem();
        String selectedNetwork = (String) networkComboBox.getSelectedItem();
        String selectedVendor = (String) vendorComboBox.getSelectedItem();
        String selectedSort = (String) sortComboBox.getSelectedItem();
        String selectedCostCenter = (String) viewCostCenterComboBox.getSelectedItem();
        String selectedGlCode = (String) viewGlCodeComboBox.getSelectedItem();
        String selectedCommitItem = (String) viewCommitItemComboBox.getSelectedItem();
          List<BillRecord> records;
          // Get year and quarter filters - with null safety
        Integer year = null;
        if (selectedYear != null && !selectedYear.equals("All Years")) {
            try {
                year = Integer.parseInt(selectedYear);
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid year format: " + selectedYear);
                year = null;
            }
        }
        
        Integer quarter = null;
        if (selectedQuarter != null && !selectedQuarter.equals("All Quarters")) {
            quarter = getQuarterNumber(selectedQuarter);
        }
        String network = "All Networks".equals(selectedNetwork) ? null : selectedNetwork;
        String vendor = "All Vendors".equals(selectedVendor) ? null : selectedVendor;
        
        // Use the enhanced filtering method
        records = billDataService.getFilteredBillRecords(year, quarter, network, vendor);
        
        // Apply search filtering if search text is provided
        String searchText = searchField.getText().trim().toLowerCase();
        if (!searchText.isEmpty()) {
            records = records.stream()
                .filter(record -> {
                    return record.getLocation().toLowerCase().contains(searchText) ||
                           record.getInvoiceNumber().toLowerCase().contains(searchText) ||
                           record.getStatus().toLowerCase().contains(searchText) ||
                           (record.getRemarks() != null && record.getRemarks().toLowerCase().contains(searchText)) ||
                           (record.getNetwork() != null && record.getNetwork().toLowerCase().contains(searchText)) ||
                           (record.getVendor() != null && record.getVendor().toLowerCase().contains(searchText)) ||
                           String.format("%.2f", record.getBillWithTax()).contains(searchText) ||
                           String.format("%.2f", record.getBillWithoutTax()).contains(searchText);
                })
                .collect(Collectors.toList());
        }
        
        // Apply additional filtering for Cost Center, GL Code, and Commit Item
        if (selectedCostCenter != null && !selectedCostCenter.equals("All Cost Centers")) {
            records = records.stream()
                .filter(record -> selectedCostCenter.equals(record.getCostCenter()))
                .collect(Collectors.toList());
        }
        
        if (selectedGlCode != null && !selectedGlCode.equals("All GL Codes")) {
            records = records.stream()
                .filter(record -> selectedGlCode.equals(record.getGlCode()))
                .collect(Collectors.toList());
        }
        
        if (selectedCommitItem != null && !selectedCommitItem.equals("All Commit Items")) {
            records = records.stream()
                .filter(record -> selectedCommitItem.equals(record.getCommitItem()))
                .collect(Collectors.toList());
        }
        
        // Apply sorting based on selected sort option
        records = applySorting(records, selectedSort);
        
        // Check if no data found and show appropriate message
        if (records.isEmpty()) {
            tableModel.addRow(new Object[]{"No data found for the selected filters", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""});
            return;
        }
        
        // Populate table with filtered and sorted records
        // Use the actual serial number from the BillRecord object
        for (BillRecord record : records) {
            Object[] row = {
                record.getSerialNo(), // Use actual serial number from the record
                record.getNetwork() != null ? record.getNetwork() : "N/A",
                record.getVendor() != null ? record.getVendor() : "N/A",
                record.getLocation(),
                record.getInvoiceNumber(),
                record.getBillWithTaxFormatted(), // Use new rupee formatting method
                record.getBillWithoutTaxFormatted(), // Use new rupee formatting method
                record.getSes1(), // Direct string value, no casting needed
                record.getSes2(), // Direct string value, no casting needed
                formatBillingPeriod(record),
                record.getStatus(),
                record.getGlCode(), // New GL Code field (string)
                record.getCommitItem() != null ? record.getCommitItem() : "N/A", // New Commit Item field
                record.getCostCenter() != null ? record.getCostCenter() : "N/A", // New Cost Center field
                record.getRemarks(),
                "Download", 
                "Edit",
                "Delete"
            };
            tableModel.addRow(row);
        }
        
        // Completely refresh the table
        SwingUtilities.invokeLater(() -> {
            // Force complete repaint for both table and header
            if (billTable.getTableHeader() != null) {
                billTable.getTableHeader().repaint();
            }
            
            // Validate and repaint all components
            billTable.revalidate();
            billTable.repaint();
              // Ensure scroll pane is updated if present
            Container parent = billTable.getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
        });
    }
    
    /**
     * Get quarter number from string - enhanced to handle network-specific quarters
     */
    private int getQuarterNumber(String quarterString) {
        if (quarterString == null) return -1;
        if (quarterString.contains("Quarter 1")) return 1;
        if (quarterString.contains("Quarter 2")) return 2;
        if (quarterString.contains("Quarter 3")) return 3;
        if (quarterString.contains("Quarter 4")) return 4;
        return -1; // All quarters
    }/**
     * Format billing period with dates on separate lines for better readability
     * e.g. "Quarter 1 (April-Sep)
     *       14-06-2025 to 15-06-2025"
     */
    private String formatBillingPeriod(BillRecord record) {
        if (record.getFromDate() != null && record.getToDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            return "<html>" + record.getBillingPeriod() + 
                   "<br>" + record.getFromDate().format(formatter) + 
                   " to " + record.getToDate().format(formatter) + "</html>";
        }
        return record.getBillingPeriod();
    }
    
    /**
     * Apply sorting to the bill records based on selected sort option
     */
    private List<BillRecord> applySorting(List<BillRecord> records, String sortOption) {
        if (sortOption == null || sortOption.contains("Default (Serial No.)")) {
            // Default sorting by serial number (ascending)
            return records.stream()
                .sorted((r1, r2) -> Integer.compare(r1.getSerialNo(), r2.getSerialNo()))
                .collect(Collectors.toList());
        }
        
        switch (sortOption) {
            case "Date (Newest First)":
                return records.stream()
                    .sorted((r1, r2) -> {
                        LocalDate date1 = r1.getFromDate() != null ? r1.getFromDate() : LocalDate.MIN;
                        LocalDate date2 = r2.getFromDate() != null ? r2.getFromDate() : LocalDate.MIN;
                        return date2.compareTo(date1); // Descending (newest first)
                    })
                    .collect(Collectors.toList());
                    
            case "Date (Oldest First)":
                return records.stream()
                    .sorted((r1, r2) -> {
                        LocalDate date1 = r1.getFromDate() != null ? r1.getFromDate() : LocalDate.MAX;
                        LocalDate date2 = r2.getFromDate() != null ? r2.getFromDate() : LocalDate.MAX;
                        return date1.compareTo(date2); // Ascending (oldest first)
                    })
                    .collect(Collectors.toList());
                    
            case "Location (A-Z)":
                return records.stream()
                    .sorted((r1, r2) -> {
                        String loc1 = r1.getLocation() != null ? r1.getLocation() : "";
                        String loc2 = r2.getLocation() != null ? r2.getLocation() : "";
                        return loc1.compareToIgnoreCase(loc2); // Ascending A-Z
                    })
                    .collect(Collectors.toList());
                    
            case "Location (Z-A)":
                return records.stream()
                    .sorted((r1, r2) -> {
                        String loc1 = r1.getLocation() != null ? r1.getLocation() : "";
                        String loc2 = r2.getLocation() != null ? r2.getLocation() : "";
                        return loc2.compareToIgnoreCase(loc1); // Descending Z-A
                    })
                    .collect(Collectors.toList());
                    
            case "Bill Amount (Low to High)":
                return records.stream()
                    .sorted((r1, r2) -> Double.compare(r1.getBillWithTax(), r2.getBillWithTax())) // Ascending
                    .collect(Collectors.toList());
                    
            case "Bill Amount (High to Low)":
                return records.stream()
                    .sorted((r1, r2) -> Double.compare(r2.getBillWithTax(), r1.getBillWithTax())) // Descending
                    .collect(Collectors.toList());
                    
            case "Status (Completed First)":
                return records.stream()
                    .sorted((r1, r2) -> {
                        String status1 = r1.getStatus() != null ? r1.getStatus() : "";
                        String status2 = r2.getStatus() != null ? r2.getStatus() : "";
                        
                        // Completed comes before Pending
                        if (status1.equals("Completed") && status2.equals("Pending")) return -1;
                        if (status1.equals("Pending") && status2.equals("Completed")) return 1;
                        return status1.compareToIgnoreCase(status2);
                    })
                    .collect(Collectors.toList());
                    
            case "Status (Pending First)":
                return records.stream()
                    .sorted((r1, r2) -> {
                        String status1 = r1.getStatus() != null ? r1.getStatus() : "";
                        String status2 = r2.getStatus() != null ? r2.getStatus() : "";
                        
                        // Pending comes before Completed
                        if (status1.equals("Pending") && status2.equals("Completed")) return -1;
                        if (status1.equals("Completed") && status2.equals("Pending")) return 1;
                        return status1.compareToIgnoreCase(status2);
                    })
                    .collect(Collectors.toList());
                    
            default:
                return records; // Return as-is for unknown sort options
        }
    }
    /**
     * Setup Update Tab - Form for submitting/updating billing entries
     */
    private void setupUpdateTab() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Create form panel
        JPanel formPanel = createUpdateForm();
        JScrollPane formScrollPane = new JScrollPane(formPanel);
        formScrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Configure smooth scrolling for form
        configureSmoothScrolling(formScrollPane);
        mainPanel.add(formScrollPane, BorderLayout.CENTER);
        
        // Create button panel
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        updatePanel.add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * Create the update form with all form fields
     */
    private JPanel createUpdateForm() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Network dropdown
        gbc.gridx = 0; gbc.gridy = row;
        JLabel networkLabel = new JLabel("Network:");
        networkLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(networkLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        updateNetworkComboBox = new JComboBox<>();
        updateNetworkComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        updateNetworkComboBox.addActionListener(e -> {
            populateUpdateVendorComboBox();
            updateBillingPeriodOptions();
            updateSubmitButtonState();
        });
        formPanel.add(updateNetworkComboBox, gbc);
        
        row++;
        
        // Vendor dropdown
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel vendorLabel = new JLabel("Vendor:");
        vendorLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(vendorLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        updateVendorComboBox = new JComboBox<>();
        updateVendorComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        updateVendorComboBox.addActionListener(e -> updateSubmitButtonState());
        formPanel.add(updateVendorComboBox, gbc);
        
        row++;
        
        // Location dropdown
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel locationLabel = new JLabel("Location:");
        locationLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(locationLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        locationComboBox = new JComboBox<>(LOCATIONS);
        locationComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(locationComboBox, gbc);
        
        row++;
        
        // Invoice Number
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel invoiceLabel = new JLabel("Invoice Number:");
        invoiceLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(invoiceLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        invoiceNumberField = new JTextField();
        invoiceNumberField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(invoiceNumberField, gbc);
        
        row++;
        
        // Bill with Tax
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel billWithTaxLabel = new JLabel("Bill Amount with Tax (18% GST):");
        billWithTaxLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(billWithTaxLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        billWithTaxField = new JTextField();
        billWithTaxField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(billWithTaxField, gbc);
        
        // Add document listener for automatic calculation
        billWithTaxField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                BillTrackerDashboard.this.calculateBillWithoutTax();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                BillTrackerDashboard.this.calculateBillWithoutTax();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                BillTrackerDashboard.this.calculateBillWithoutTax();
            }
        });
        
        row++;
        
        // Bill without Tax
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel billWithoutTaxLabel = new JLabel("Bill Amount without Tax:");
        billWithoutTaxLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(billWithoutTaxLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        billWithoutTaxField = new JTextField();
        billWithoutTaxField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(billWithoutTaxField, gbc);
        
        // Add document listener for automatic calculation
        billWithoutTaxField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                BillTrackerDashboard.this.calculateBillWithTax();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                BillTrackerDashboard.this.calculateBillWithTax();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                BillTrackerDashboard.this.calculateBillWithTax();
            }
        });
        
        row++;
        
        // SES1
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel ses1Label = new JLabel("SES 1:");
        ses1Label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(ses1Label, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        ses1Field = new JTextField();
        ses1Field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(ses1Field, gbc);
        
        row++;
        
        // SES2
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel ses2Label = new JLabel("SES 2:");
        ses2Label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(ses2Label, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        ses2Field = new JTextField();
        ses2Field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(ses2Field, gbc);
        
        row++;
        
        // Billing Period
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel billingPeriodLabel = new JLabel("Billing Period:");
        billingPeriodLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(billingPeriodLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        billingPeriodComboBox = new JComboBox<>();
        billingPeriodComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        billingPeriodComboBox.addActionListener(e -> autoFillDates());
        formPanel.add(billingPeriodComboBox, gbc);
        
        row++;
        
        // From Date
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel fromDateLabel = new JLabel("From Date:");
        fromDateLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(fromDateLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        fromDateChooser = new JDateChooser();
        fromDateChooser.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fromDateChooser.setDateFormatString("dd-MM-yyyy");
        formPanel.add(fromDateChooser, gbc);
        
        row++;
        
        // To Date
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel toDateLabel = new JLabel("To Date:");
        toDateLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(toDateLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        toDateChooser = new JDateChooser();
        toDateChooser.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        toDateChooser.setDateFormatString("dd-MM-yyyy");
        formPanel.add(toDateChooser, gbc);
        
        row++;
        
        // Status
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(statusLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        statusComboBox = new JComboBox<>(new String[]{"Pending", "Completed"});
        statusComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(statusComboBox, gbc);
        
        row++;
        
        // GL Code
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel glCodeLabel = new JLabel("GL Code:");
        glCodeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(glCodeLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        // Create GL Code dropdown with predefined values
        String[] glCodes = {"5261300020", "5281525560", "5290700080", "5290700020", "5290700160"};
        glCodeComboBox = new JComboBox<>(glCodes);
        glCodeComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        glCodeComboBox.setEditable(false);
        formPanel.add(glCodeComboBox, gbc);
        
        row++;
        
        // Commit Item
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel commitItemLabel = new JLabel("Commit Item:");
        commitItemLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(commitItemLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        commitItemComboBox = new JComboBox<>(new String[]{"C_COMMEXP", "C_R&MEQPC"});
        commitItemComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(commitItemComboBox, gbc);
        
        row++;
        
        // Cost Center
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel costCenterLabel = new JLabel("Cost Center:");
        costCenterLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(costCenterLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        costCenterComboBox = new JComboBox<>(new String[]{"M75010-SRO", "M78010-TNSO"});
        costCenterComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(costCenterComboBox, gbc);
        
        row++;
        
        // Remarks
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel remarksLabel = new JLabel("Remarks:");
        remarksLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(remarksLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 0.3;
        remarksTextArea = new JTextArea(4, 20);
        remarksTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        remarksTextArea.setLineWrap(true);
        remarksTextArea.setWrapStyleWord(true);
        remarksScrollPane = new JScrollPane(remarksTextArea);
        remarksScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        formPanel.add(remarksScrollPane, gbc);
        
        row++;
        
        // PDF File selection - Enhanced UI
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.weighty = 0;
        JLabel pdfLabel = new JLabel("PDF File:");
        pdfLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(pdfLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JPanel pdfPanel = new JPanel(new BorderLayout(8, 0));
        pdfPanel.setBackground(Color.WHITE);
        pdfPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        // File info panel
        JPanel fileInfoPanel = new JPanel(new BorderLayout());
        fileInfoPanel.setBackground(Color.WHITE);
        
        pdfFileLabel = new JLabel("No PDF file selected");
        pdfFileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pdfFileLabel.setForeground(new Color(120, 120, 120));
        fileInfoPanel.add(pdfFileLabel, BorderLayout.NORTH);
        
        // File size and status label
        JLabel fileSizeLabel = new JLabel("");
        fileSizeLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        fileSizeLabel.setForeground(new Color(100, 100, 100));
        fileInfoPanel.add(fileSizeLabel, BorderLayout.SOUTH);
        
        pdfPanel.add(fileInfoPanel, BorderLayout.CENTER);
        
        // Button panel for PDF actions
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton selectPdfButton = new JButton("Browse...");
        selectPdfButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        selectPdfButton.setBackground(new Color(0, 123, 255));
        selectPdfButton.setForeground(Color.WHITE);
        selectPdfButton.setFocusPainted(false);
        selectPdfButton.setBorderPainted(false);
        selectPdfButton.setPreferredSize(new Dimension(80, 30));
        selectPdfButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        selectPdfButton.setToolTipText("Select a PDF file to upload");
        selectPdfButton.addActionListener(this::selectPdfFile);
        buttonPanel.add(selectPdfButton);
        
        JButton clearPdfButton = new JButton("Clear");
        clearPdfButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        clearPdfButton.setBackground(new Color(220, 53, 69));
        clearPdfButton.setForeground(Color.WHITE);
        clearPdfButton.setFocusPainted(false);
        clearPdfButton.setBorderPainted(false);
        clearPdfButton.setPreferredSize(new Dimension(60, 30));
        clearPdfButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearPdfButton.setToolTipText("Clear selected PDF file");
        clearPdfButton.setEnabled(false);
        clearPdfButton.addActionListener(e -> clearSelectedPdf());
        buttonPanel.add(clearPdfButton);
        
        pdfPanel.add(buttonPanel, BorderLayout.EAST);
        
        formPanel.add(pdfPanel, gbc);
        
        // Store references to update them later
        this.fileSizeLabel = fileSizeLabel;
        this.clearPdfButton = clearPdfButton;
        
        // Initialize dropdowns
        populateUpdateNetworkComboBox();
        
        // Initialize submit button state
        updateSubmitButtonState();
        
        return formPanel;
    }
    
    /**
     * Create the button panel with Submit and Clear buttons
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Submit button
        submitButton = new JButton("Submit Record");
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        submitButton.setBackground(new Color(40, 167, 69));
        submitButton.setForeground(Color.WHITE);
        submitButton.setPreferredSize(new Dimension(160, 45));
        submitButton.setBorderPainted(false);
        submitButton.setFocusPainted(false);
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitButton.addActionListener(this::submitRecord);
        buttonPanel.add(submitButton);
        
        // Clear button
        clearButton = new JButton("Clear Form");
        clearButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        clearButton.setBackground(new Color(108, 117, 125));
        clearButton.setForeground(Color.WHITE);
        clearButton.setPreferredSize(new Dimension(160, 45));
        clearButton.setBorderPainted(false);
        clearButton.setFocusPainted(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(e -> clearForm());
        buttonPanel.add(clearButton);
        
        return buttonPanel;
    }
    

    
    private JPanel createFinancialAnalyticsPanel() {
        JPanel analyticsPanel = new JPanel(new BorderLayout());
        analyticsPanel.setBackground(Color.WHITE);
        
        // Create main content panel
        JPanel contentPanel = new JPanel(new BorderLayout(0, 15));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header with title
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("Financial Analytics Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 102, 204));
        headerPanel.add(titleLabel);
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Quick stats cards panel - Changed to 4 cards layout
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        // Calculate quick stats
        List<BillRecord> allRecords = getFilteredAnalyticsRecords();
        double totalAmountWithTax = allRecords.stream()
            .mapToDouble(r -> r.getBillWithTax())
            .sum();
        double totalAmountWithoutTax = allRecords.stream()
            .mapToDouble(r -> r.getBillWithoutTax())
            .sum();
        
        // Create stat cards with separate tax amounts
        statsPanel.add(createQuickStatCard("Total Bills", 
            String.valueOf(allRecords.size()), new Color(23, 162, 184)));
        statsPanel.add(createQuickStatCard("Total Amount With Tax", 
            String.format("₹%.2f", totalAmountWithTax), new Color(40, 167, 69)));
        statsPanel.add(createQuickStatCard("Total Amount Without Tax", 
            String.format("₹%.2f", totalAmountWithoutTax), new Color(255, 193, 7)));
        statsPanel.add(createQuickStatCard("Total Tax Amount", 
            String.format("₹%.2f", totalAmountWithTax - totalAmountWithoutTax), new Color(220, 53, 69)));
        
        contentPanel.add(statsPanel, BorderLayout.CENTER);
        
        // Network/Vendor breakdown panel
        JPanel breakdownPanel = new JPanel(new BorderLayout());
        breakdownPanel.setBackground(Color.WHITE);
        breakdownPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204), 1),
            "Network & Vendor Breakdown",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(0, 102, 204)
        ));
        
        // Create breakdown table with enhanced columns
        String[] columns = {"Network", "Vendor", "Count", "Total Amount With Tax", "Total Amount Without Tax", "GL Code", "Cost Center", "Commit Item"};
        java.util.Map<String, java.util.Map<String, java.util.List<BillRecord>>> networkVendorMap = 
            new java.util.HashMap<>();
        
        // Group records by network and vendor
        for (BillRecord record : allRecords) {
            networkVendorMap.computeIfAbsent(record.getNetwork(), k -> new java.util.HashMap<>())
                .computeIfAbsent(record.getVendor(), k -> new java.util.ArrayList<>())
                .add(record);
        }
        
        // Create table data
        java.util.List<Object[]> tableData = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, java.util.Map<String, java.util.List<BillRecord>>> networkEntry : 
             networkVendorMap.entrySet()) {
            for (java.util.Map.Entry<String, java.util.List<BillRecord>> vendorEntry : 
                 networkEntry.getValue().entrySet()) {
                java.util.List<BillRecord> records = vendorEntry.getValue();
                double sumWithTax = records.stream().mapToDouble(BillRecord::getBillWithTax).sum();
                double sumWithoutTax = records.stream().mapToDouble(BillRecord::getBillWithoutTax).sum();
                
                // Collect unique GL codes, cost centers, and commit items for this group
                java.util.Set<String> glCodes = records.stream()
                    .map(BillRecord::getGlCode)
                    .filter(gl -> gl != null && !gl.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toSet());
                java.util.Set<String> costCenters = records.stream()
                    .map(BillRecord::getCostCenter)
                    .filter(cc -> cc != null && !cc.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toSet());
                java.util.Set<String> commitItems = records.stream()
                    .map(BillRecord::getCommitItem)
                    .filter(ci -> ci != null && !ci.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toSet());
                
                tableData.add(new Object[]{
                    networkEntry.getKey(),
                    vendorEntry.getKey(),
                    records.size(),
                    String.format("₹%.2f", sumWithTax),
                    String.format("₹%.2f", sumWithoutTax),
                    glCodes.isEmpty() ? "N/A" : String.join(", ", glCodes),
                    costCenters.isEmpty() ? "N/A" : String.join(", ", costCenters),
                    commitItems.isEmpty() ? "N/A" : String.join(", ", commitItems)
                });
            }
        }
        
        Object[][] data = tableData.toArray(new Object[0][]);
        
        JTable breakdownTable = new JTable(data, columns);
        breakdownTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        // Force table header visibility with custom renderer
        JTableHeader header = breakdownTable.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setBackground(new Color(0, 102, 204));
                setForeground(Color.WHITE);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 1, 1, 1, Color.WHITE),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
                setOpaque(true);
                return this;
            }
        });
        
        // Additional header styling
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(0, 102, 204));
        header.setForeground(Color.WHITE);
        header.setOpaque(true);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 35));
        header.setReorderingAllowed(false);
        
        breakdownTable.setRowHeight(25);
        breakdownTable.setGridColor(new Color(230, 230, 230));
        breakdownTable.setSelectionBackground(new Color(184, 207, 229));
        breakdownTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // Set preferred column widths for better display
        if (breakdownTable.getColumnCount() >= 8) {
            breakdownTable.getColumnModel().getColumn(0).setPreferredWidth(100); // Network
            breakdownTable.getColumnModel().getColumn(1).setPreferredWidth(120); // Vendor
            breakdownTable.getColumnModel().getColumn(2).setPreferredWidth(60);  // Count
            breakdownTable.getColumnModel().getColumn(3).setPreferredWidth(140); // Amount With Tax
            breakdownTable.getColumnModel().getColumn(4).setPreferredWidth(140); // Amount Without Tax
            breakdownTable.getColumnModel().getColumn(5).setPreferredWidth(100); // GL Code
            breakdownTable.getColumnModel().getColumn(6).setPreferredWidth(100); // Cost Center
            breakdownTable.getColumnModel().getColumn(7).setPreferredWidth(120); // Commit Item
        }
        
        JScrollPane scrollPane = new JScrollPane(breakdownTable);
        scrollPane.setPreferredSize(new Dimension(1000, 250));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        breakdownPanel.add(scrollPane, BorderLayout.CENTER);
        
        contentPanel.add(breakdownPanel, BorderLayout.SOUTH);
        
        analyticsPanel.add(contentPanel, BorderLayout.CENTER);
        
        return analyticsPanel;
    }
    
    private List<BillRecord> getFilteredAnalyticsRecords() {
        // Get filter values
        String selectedNetwork = (analyticsNetworkComboBox != null) ? 
            (String) analyticsNetworkComboBox.getSelectedItem() : null;
        String selectedVendor = (analyticsVendorComboBox != null) ? 
            (String) analyticsVendorComboBox.getSelectedItem() : null;
        String selectedQuarter = (analyticsQuarterComboBox != null) ? 
            (String) analyticsQuarterComboBox.getSelectedItem() : null;
        String selectedYear = (analyticsYearComboBox != null) ? 
            (String) analyticsYearComboBox.getSelectedItem() : null;
        String selectedCostCenter = (analyticsCostCenterComboBox != null) ? 
            (String) analyticsCostCenterComboBox.getSelectedItem() : null;
        String selectedCommitItem = (analyticsCommitItemComboBox != null) ? 
            (String) analyticsCommitItemComboBox.getSelectedItem() : null;
        
        // Convert selections to filter parameters
        String networkFilter = ("All Networks".equals(selectedNetwork)) ? null : selectedNetwork;
        String vendorFilter = ("All Vendors".equals(selectedVendor)) ? null : selectedVendor;
        String costCenterFilter = ("All Cost Centers".equals(selectedCostCenter)) ? null : selectedCostCenter;
        String commitItemFilter = ("All Commit Items".equals(selectedCommitItem)) ? null : selectedCommitItem;
        
        Integer quarterFilter = null;
        if (selectedQuarter != null && !selectedQuarter.equals("All Quarters")) {
            quarterFilter = getQuarterNumber(selectedQuarter);
        }
        Integer yearFilter = null;
        if (selectedYear != null && !selectedYear.equals("All Years")) {
            try {
                yearFilter = Integer.parseInt(selectedYear);
            } catch (NumberFormatException e) {
                yearFilter = null;
            }
        }
        
        // Get basic filtered records first
        List<BillRecord> records = billDataService.getFilteredBillRecords(yearFilter, quarterFilter, networkFilter, vendorFilter);
        
        // Apply additional filtering for cost center and commit item
        return records.stream()
            .filter(record -> costCenterFilter == null || 
                (record.getCostCenter() != null && record.getCostCenter().equals(costCenterFilter)))
            .filter(record -> commitItemFilter == null || 
                (record.getCommitItem() != null && record.getCommitItem().equals(commitItemFilter)))
            .collect(Collectors.toList());
    }
    
    private JPanel createQuickStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(color);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(15, 10, 15, 10)
        ));
        
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(Color.WHITE);
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        
        return card;
    }
    
    private void populateAnalyticsYearComboBox() {
        if (analyticsYearComboBox == null || billDataService == null) return;
        
        try {
            analyticsYearComboBox.removeAllItems();
            analyticsYearComboBox.addItem("All Years");
            
            // Get available years from the database
            List<Integer> years = billDataService.getAvailableYears();
            if (years.isEmpty()) {
                // Add current year as fallback
                analyticsYearComboBox.addItem(String.valueOf(LocalDate.now().getYear()));
            } else {
                for (Integer year : years) {
                    analyticsYearComboBox.addItem(String.valueOf(year));
                }
            }
        } catch (Exception e) {
            System.err.println("Error populating analytics year dropdown: " + e.getMessage());
            // Add current year as fallback
            if (analyticsYearComboBox.getItemCount() <= 1) {
                analyticsYearComboBox.addItem(String.valueOf(LocalDate.now().getYear()));
            }
        }
    }
    
    private void refreshAnalytics() {
        // Just skip the refresh to avoid circular dependency for now
        // The analytics will be populated when the tab is properly accessed
    }
    
    /**
     * Calculate bill amount without tax from bill amount with tax (18% GST)
     * Formula: Bill without tax = Bill with tax / 1.18
     */
    private void calculateBillWithoutTax() {
        if (billWithTaxField == null || billWithoutTaxField == null || isCalculatingBillAmount) return;
        
        String withTaxText = billWithTaxField.getText().trim();
        if (withTaxText.isEmpty()) {
            if (!isCalculatingBillAmount) {
                isCalculatingBillAmount = true;
                billWithoutTaxField.setText("");
                isCalculatingBillAmount = false;
            }
            return;
        }
        
        try {
            double withTax = Double.parseDouble(withTaxText);
            double withoutTax = withTax / 1.18; // Remove 18% GST
            isCalculatingBillAmount = true;
            billWithoutTaxField.setText(String.format("%.2f", withoutTax));
            isCalculatingBillAmount = false;
        } catch (NumberFormatException e) {
            // Invalid number format, clear the without tax field
            if (!isCalculatingBillAmount) {
                isCalculatingBillAmount = true;
                billWithoutTaxField.setText("");
                isCalculatingBillAmount = false;
            }
        }
    }
    
    /**
     * Calculate bill amount with tax from bill amount without tax (18% GST)
     * Formula: Bill with tax = Bill without tax * 1.18
     */
    private void calculateBillWithTax() {
        if (billWithTaxField == null || billWithoutTaxField == null || isCalculatingBillAmount) return;
        
        String withoutTaxText = billWithoutTaxField.getText().trim();
        if (withoutTaxText.isEmpty()) {
            if (!isCalculatingBillAmount) {
                isCalculatingBillAmount = true;
                billWithTaxField.setText("");
                isCalculatingBillAmount = false;
            }
            return;
        }
        
        try {
            double withoutTax = Double.parseDouble(withoutTaxText);
            double withTax = withoutTax * 1.18; // Add 18% GST
            isCalculatingBillAmount = true;
            billWithTaxField.setText(String.format("%.2f", withTax));
            isCalculatingBillAmount = false;
        } catch (NumberFormatException e) {
            // Invalid number format, clear the with tax field
            if (!isCalculatingBillAmount) {
                isCalculatingBillAmount = true;
                billWithTaxField.setText("");
                isCalculatingBillAmount = false;
            }
        }
    }
    
    /**
     * Check if the minimum required fields are filled for form submission
     */
    private boolean isFormReadyForSubmission() {
        String selectedNetwork = (String) updateNetworkComboBox.getSelectedItem();
        String selectedVendor = (String) updateVendorComboBox.getSelectedItem();
        
        return selectedNetwork != null && !selectedNetwork.equals("Select Network") &&
               selectedVendor != null && !selectedVendor.equals("Select Vendor") && 
               !selectedVendor.equals("No vendors available");
    }
    
    /**
     * Update submit button state based on form validation
     */
    private void updateSubmitButtonState() {
        if (submitButton != null) {
            boolean isReady = isFormReadyForSubmission();
            submitButton.setEnabled(isReady);
            
            if (isReady) {
                submitButton.setBackground(new Color(40, 167, 69)); // Green when ready
                submitButton.setToolTipText("Click to submit the record");
            } else {
                submitButton.setBackground(new Color(108, 117, 125)); // Gray when not ready
                submitButton.setToolTipText("Please select both Network and Vendor to enable submission");
            }
        }
    }
    
    private boolean validateDatesWithBillingPeriod() {
        return true; // Placeholder validation
    }
    
    private int calculateQuarterFromBillingPeriod(String billingPeriod, String network) {
        if (billingPeriod == null) return 1;
        if (billingPeriod.contains("Quarter 1")) return 1;
        if (billingPeriod.contains("Quarter 2")) return 2;
        if (billingPeriod.contains("Quarter 3")) return 3;
        if (billingPeriod.contains("Quarter 4")) return 4;
        return 1;
    }

    /**
     * Submit or update billing record
     */    private void submitRecord(ActionEvent e) {
        try {
            // Enhanced validation with specific error messages
            String selectedNetwork = (String) updateNetworkComboBox.getSelectedItem();
            if (selectedNetwork == null || selectedNetwork.equals("Select Network")) {
                JOptionPane.showMessageDialog(this, 
                    "Please select a Network from the dropdown", 
                    "Missing Network", JOptionPane.WARNING_MESSAGE);
                updateNetworkComboBox.requestFocus();
                return;
            }
            
            String selectedVendor = (String) updateVendorComboBox.getSelectedItem();
            if (selectedVendor == null || selectedVendor.equals("Select Vendor") || selectedVendor.equals("No vendors available")) {
                JOptionPane.showMessageDialog(this, 
                    "Please select a Vendor from the dropdown", 
                    "Missing Vendor", JOptionPane.WARNING_MESSAGE);
                updateVendorComboBox.requestFocus();
                return;
            }
            
            if (invoiceNumberField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter an Invoice Number", 
                    "Missing Invoice Number", JOptionPane.WARNING_MESSAGE);
                invoiceNumberField.requestFocus();
                return;
            }
            
            if (billWithTaxField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter the Bill amount with Tax", 
                    "Missing Bill Amount", JOptionPane.WARNING_MESSAGE);
                billWithTaxField.requestFocus();
                return;
            }
            
            if (fromDateChooser.getDate() == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select a From Date", 
                    "Missing From Date", JOptionPane.WARNING_MESSAGE);
                return;
            }            if (toDateChooser.getDate() == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select a To Date", 
                    "Missing To Date", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Validate new required fields
            if (glCodeComboBox.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select a GL Code", 
                    "Missing GL Code", JOptionPane.WARNING_MESSAGE);
                glCodeComboBox.requestFocus();
                return;
            }
            
            if (commitItemComboBox.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select a Commit Item", 
                    "Missing Commit Item", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (costCenterComboBox.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select a Cost Center", 
                    "Missing Cost Center", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Validate that dates match the selected billing period
            if (!validateDatesWithBillingPeriod()) {
                return; // User chose not to proceed with invalid dates
            }
            
            // Validate that selected dates match the billing period according to network rules
            if (!validateDatesWithBillingPeriod()) {
                return; // Validation failed, do not proceed
            }
            
            BillRecord record = editingRecord != null ? editingRecord : new BillRecord();
            
            // Set all form values
            record.setNetwork((String) updateNetworkComboBox.getSelectedItem());
            record.setVendor((String) updateVendorComboBox.getSelectedItem());
            record.setLocation((String) locationComboBox.getSelectedItem());
            record.setInvoiceNumber(invoiceNumberField.getText().trim());
            record.setBillWithTax(Double.parseDouble(billWithTaxField.getText().trim()));
            record.setBillWithoutTax(Double.parseDouble(billWithoutTaxField.getText().trim()));            // Handle SES fields (string values for 20+ digit support)
            record.setSes1(ses1Field.getText().trim().isEmpty() ? "0" : 
                          ses1Field.getText().trim());
            record.setSes2(ses2Field.getText().trim().isEmpty() ? "0" : 
                          ses2Field.getText().trim());
              record.setBillingPeriod((String) billingPeriodComboBox.getSelectedItem());
            
            // Convert dates
            Date fromDate = fromDateChooser.getDate();
            Date toDate = toDateChooser.getDate();
            record.setFromDate(fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            record.setToDate(toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            
            // Set year and quarter based on the from date
            LocalDate fromLocalDate = record.getFromDate();
            record.setYear(fromLocalDate.getYear());
            
            // Calculate quarter based on billing period and network
            String billingPeriod = record.getBillingPeriod();
            String network = record.getNetwork();
            int quarter = calculateQuarterFromBillingPeriod(billingPeriod, network);            record.setQuarter(quarter);
            
            // Set new required fields (string values for 20+ digit support)
            record.setGlCode((String) glCodeComboBox.getSelectedItem());
            record.setCommitItem((String) commitItemComboBox.getSelectedItem());
            record.setCostCenter((String) costCenterComboBox.getSelectedItem());
            
            record.setStatus((String) statusComboBox.getSelectedItem());
            record.setRemarks(remarksTextArea.getText().trim());
            
            // Handle PDF upload
            if (selectedPdfPath != null) {
                String pdfFileName = copyPdfToStorage(selectedPdfPath, record.getInvoiceNumber());
                record.setPdfFilePath(pdfFileName);
            }
            
            // Save record
            if (editingRecord != null) {
                billDataService.updateBillRecord(record);
                JOptionPane.showMessageDialog(this, "Bill record updated successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                editingRecord = null;
                submitButton.setText("Submit Record");
            } else {
                billDataService.addBillRecord(record);
                JOptionPane.showMessageDialog(this, "Bill record added successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            }
            
            // Clear form and refresh table
            clearForm();
            loadTableData();
            
            // Refresh year dropdown to include new years
            populateYearComboBox();
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for bill amounts and SES fields.", 
                "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving record: " + ex.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Select PDF file for upload - Enhanced with validation and user feedback
     */
    private void selectPdfFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select PDF Bill Document");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "PDF Documents (*.pdf)", "pdf"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setMultiSelectionEnabled(false);
        
        // Set file chooser properties for better UX
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Validate file
            if (!selectedFile.exists()) {
                JOptionPane.showMessageDialog(this, 
                    "Selected file does not exist. Please choose a valid PDF file.",
                    "File Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) {
                JOptionPane.showMessageDialog(this, 
                    "Please select a valid PDF file.",
                    "Invalid File Type", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check file size (limit to 10MB)
            long fileSizeBytes = selectedFile.length();
            double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);
            
            if (fileSizeMB > 10) {
                JOptionPane.showMessageDialog(this, 
                    String.format("File size (%.2f MB) exceeds the maximum limit of 10 MB.\nPlease select a smaller PDF file.", fileSizeMB),
                    "File Too Large", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Update UI with selected file info
            selectedPdfPath = selectedFile.getAbsolutePath();
            String fileName = selectedFile.getName();
            
            // Update file label with name
            pdfFileLabel.setText(fileName);
            pdfFileLabel.setForeground(new Color(0, 123, 255));
            pdfFileLabel.setToolTipText("Selected: " + selectedPdfPath);
            
            // Update file size label
            String sizeText;
            if (fileSizeMB >= 1) {
                sizeText = String.format("Size: %.2f MB", fileSizeMB);
            } else {
                sizeText = String.format("Size: %.0f KB", fileSizeBytes / 1024.0);
            }
            fileSizeLabel.setText(sizeText);
            fileSizeLabel.setForeground(new Color(0, 150, 0));
            
            // Enable clear button
            if (clearPdfButton != null) {
                clearPdfButton.setEnabled(true);
            }
            
            // Show success message
            JOptionPane.showMessageDialog(this, 
                String.format("PDF file selected successfully!\n\nFile: %s\nSize: %s", fileName, sizeText),
                "PDF Selected", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Clear selected PDF file
     */
    private void clearSelectedPdf() {
        selectedPdfPath = null;
        
        // Reset UI elements
        if (pdfFileLabel != null) {
            pdfFileLabel.setText("No PDF file selected");
            pdfFileLabel.setForeground(new Color(120, 120, 120));
            pdfFileLabel.setToolTipText(null);
        }
        
        if (fileSizeLabel != null) {
            fileSizeLabel.setText("");
        }
        
        if (clearPdfButton != null) {
            clearPdfButton.setEnabled(false);
        }
        
        JOptionPane.showMessageDialog(this, 
            "PDF file selection cleared.",
            "File Cleared", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Copy PDF file to secure storage - Enhanced with validation and progress feedback
     */
    private String copyPdfToStorage(String sourcePath, String invoiceNumber) throws IOException {
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            throw new IOException("Source path is empty or null");
        }
        
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            throw new IOException("Source PDF file does not exist: " + sourcePath);
        }
        
        if (!sourceFile.canRead()) {
            throw new IOException("Cannot read source PDF file: " + sourcePath);
        }
        
        // Create secure filename with timestamp and sanitized invoice number
        String sanitizedInvoiceNumber = invoiceNumber.replaceAll("[^a-zA-Z0-9-_]", "_");
        String timestamp = String.valueOf(System.currentTimeMillis());
        String originalFileName = sourceFile.getName();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        String fileName = timestamp + "_" + sanitizedInvoiceNumber + "_" + 
                         originalFileName.substring(0, originalFileName.lastIndexOf('.')) + fileExtension;
        
        // Create shared PDF directory for all users
        File sharedPdfDir = new File("pdfs", "shared");
        if (!sharedPdfDir.exists()) {
            boolean created = sharedPdfDir.mkdirs();
            if (!created) {
                throw new IOException("Failed to create PDF storage directory: " + sharedPdfDir.getAbsolutePath());
            }
        }
        
        File destFile = new File(sharedPdfDir, fileName);
        
        try {
            // Copy file with progress feedback
            long fileSize = sourceFile.length();
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // Verify copy was successful
            if (!destFile.exists() || destFile.length() != fileSize) {
                throw new IOException("PDF file copy verification failed");
            }
            
            // Return relative path for storage in database
            return "shared/" + fileName;
            
        } catch (IOException e) {
            // Clean up partial copy on failure
            if (destFile.exists()) {
                destFile.delete();
            }
            throw new IOException("Failed to copy PDF file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Clear form - Enhanced to handle new PDF UI elements
     */
    private void clearForm() {
        if (locationComboBox != null) locationComboBox.setSelectedIndex(0);
        if (invoiceNumberField != null) invoiceNumberField.setText("");
        if (billWithTaxField != null) billWithTaxField.setText("");
        if (billWithoutTaxField != null) billWithoutTaxField.setText("");
        if (ses1Field != null) ses1Field.setText("");
        if (ses2Field != null) ses2Field.setText("");
        if (billingPeriodComboBox != null) billingPeriodComboBox.setSelectedIndex(0);
        if (fromDateChooser != null) fromDateChooser.setDate(null);
        if (toDateChooser != null) toDateChooser.setDate(null);
        if (statusComboBox != null) statusComboBox.setSelectedIndex(0);
        
        // Clear new required fields
        if (glCodeComboBox != null) glCodeComboBox.setSelectedIndex(0);
        if (commitItemComboBox != null) commitItemComboBox.setSelectedIndex(0);
        if (costCenterComboBox != null) costCenterComboBox.setSelectedIndex(0);
        
        // Clear remarks and force cursor to start at top
        if (remarksTextArea != null) {
            remarksTextArea.setText("");
            remarksTextArea.setCaretPosition(0);
        }
        if (remarksScrollPane != null) {
            remarksScrollPane.getVerticalScrollBar().setValue(0);
        }
        
        // Clear PDF selection - Enhanced
        selectedPdfPath = null;
        if (pdfFileLabel != null) {
            pdfFileLabel.setText("No PDF file selected");
            pdfFileLabel.setForeground(new Color(120, 120, 120));
            pdfFileLabel.setToolTipText(null);
        }
        if (fileSizeLabel != null) {
            fileSizeLabel.setText("");
        }
        if (clearPdfButton != null) {
            clearPdfButton.setEnabled(false);
        }
        
        editingRecord = null;
        if (submitButton != null) submitButton.setText("Submit Record");
        
        // Update submit button state after clearing form
        updateSubmitButtonState();
    }

    /**
     * Cancel edit mode
     */    private void cancelEdit() {
        editingRecord = null;
        submitButton.setText("Submit Record");
        clearForm();
        JOptionPane.showMessageDialog(this, "Edit mode cancelled. Form cleared.", 
            "Edit Cancelled", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Populate form for editing existing record
     */
    private void populateFormForEdit(BillRecord record) {
        editingRecord = record;
        
        locationComboBox.setSelectedItem(record.getLocation());
        invoiceNumberField.setText(record.getInvoiceNumber());
        billWithTaxField.setText(String.valueOf(record.getBillWithTax()));
        billWithoutTaxField.setText(String.valueOf(record.getBillWithoutTax()));        ses1Field.setText(String.valueOf(record.getSes1()));
        ses2Field.setText(String.valueOf(record.getSes2()));
        billingPeriodComboBox.setSelectedItem(record.getBillingPeriod());
        
        if (record.getFromDate() != null) {
            fromDateChooser.setDate(Date.from(record.getFromDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        if (record.getToDate() != null) {
            toDateChooser.setDate(Date.from(record.getToDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }        statusComboBox.setSelectedItem(record.getStatus());
        
        // Set new required fields
        glCodeComboBox.setSelectedItem(record.getGlCode());
        commitItemComboBox.setSelectedItem(record.getCommitItem());
        costCenterComboBox.setSelectedItem(record.getCostCenter());
        
        // Set remarks and force cursor to start at top
        remarksTextArea.setText(record.getRemarks() != null ? record.getRemarks() : "");
        remarksTextArea.setCaretPosition(0);
        remarksScrollPane.getVerticalScrollBar().setValue(0);
        
        // Handle PDF file display for editing - Enhanced
        if (record.getPdfFilePath() != null && !record.getPdfFilePath().trim().isEmpty()) {
            String pdfPath = record.getPdfFilePath();
            File pdfFile = new File("pdfs", pdfPath);
            
            if (pdfFile.exists()) {
                // Display file name and size
                pdfFileLabel.setText(pdfFile.getName());
                pdfFileLabel.setForeground(new Color(0, 123, 255));
                pdfFileLabel.setToolTipText("Existing PDF: " + pdfFile.getAbsolutePath());
                
                // Show file size
                long fileSizeBytes = pdfFile.length();
                String sizeText;
                if (fileSizeBytes >= 1024 * 1024) {
                    sizeText = String.format("Size: %.2f MB (existing)", fileSizeBytes / (1024.0 * 1024.0));
                } else {
                    sizeText = String.format("Size: %.0f KB (existing)", fileSizeBytes / 1024.0);
                }
                if (fileSizeLabel != null) {
                    fileSizeLabel.setText(sizeText);
                    fileSizeLabel.setForeground(new Color(0, 150, 0));
                }
                
                // Enable clear button for existing files
                if (clearPdfButton != null) {
                    clearPdfButton.setEnabled(true);
                }
            } else {
                pdfFileLabel.setText("PDF file missing: " + pdfPath);
                pdfFileLabel.setForeground(new Color(220, 53, 69));
                pdfFileLabel.setToolTipText("Original PDF file not found");
                
                if (fileSizeLabel != null) {
                    fileSizeLabel.setText("File not found");
                    fileSizeLabel.setForeground(new Color(220, 53, 69));
                }
            }
        } else {
            pdfFileLabel.setText("No PDF file selected");
            pdfFileLabel.setForeground(new Color(120, 120, 120));
            pdfFileLabel.setToolTipText(null);
            
            if (fileSizeLabel != null) {
                fileSizeLabel.setText("");
            }
            if (clearPdfButton != null) {
                clearPdfButton.setEnabled(false);
            }
        }
        
        submitButton.setText("Update Record");
          // Switch to Update tab
        tabbedPane.setSelectedIndex(1);
    }
    
    /**
     * View PDF file
     */
    private void viewPdf(int row) {
        try {
            int actualRow = billTable.convertRowIndexToModel(row);
            String serialNo = tableModel.getValueAt(actualRow, 0).toString();
            
            List<BillRecord> records = billDataService.getAllBillRecords();
            BillRecord record = records.stream()
                .filter(r -> String.valueOf(r.getSerialNo()).equals(serialNo))
                .findFirst()
                .orElse(null);
            
            if (record != null && record.getPdfFilePath() != null) {
                File pdfFile = new File("pdfs", record.getPdfFilePath());
                if (pdfFile.exists()) {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(pdfFile);
                    } else {
                        JOptionPane.showMessageDialog(this, "Desktop operations not supported on this system.", 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "PDF file not found: " + record.getPdfFilePath(), 
                        "File Not Found", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "No PDF file associated with this record.", 
                    "No PDF", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error opening PDF: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Download PDF file
     */
    private void downloadPdf(int row) {
        try {
            int actualRow = billTable.convertRowIndexToModel(row);
            String serialNo = tableModel.getValueAt(actualRow, 0).toString();
            
            List<BillRecord> records = billDataService.getAllBillRecords();
            BillRecord record = records.stream()
                .filter(r -> String.valueOf(r.getSerialNo()).equals(serialNo))
                .findFirst()
                .orElse(null);
            
            if (record != null && record.getPdfFilePath() != null) {
                File pdfFile = new File("pdfs", record.getPdfFilePath());
                if (pdfFile.exists()) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setSelectedFile(new File(record.getInvoiceNumber() + "_invoice.pdf"));
                    
                    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        File destinationFile = fileChooser.getSelectedFile();
                        Files.copy(pdfFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        JOptionPane.showMessageDialog(this, "PDF downloaded successfully to: " + destinationFile.getAbsolutePath(), 
                            "Download Complete", JOptionPane.INFORMATION_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "PDF file not found: " + record.getPdfFilePath(), 
                        "File Not Found", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "No PDF file associated with this record.", 
                    "No PDF", JOptionPane.INFORMATION_MESSAGE);
            }        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error downloading PDF: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Edit a record from the table
     */
    private void editRecord(int row) {
        try {
            // Get the record ID from the table
            String recordId = billTable.getValueAt(row, 0).toString();
              // Find the record to edit
            List<BillRecord> records = billDataService.getAllBillRecords();
            BillRecord record = records.stream()
                .filter(r -> String.valueOf(r.getSerialNo()).equals(recordId))
                .findFirst()
                .orElse(null);
                
            if (record != null) {
                editingRecord = record;
                // Switch to add record tab
                tabbedPane.setSelectedIndex(1);
                // Populate form with record data
                populateFormForEdit(record);
            } else {
                JOptionPane.showMessageDialog(this, "Record not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error editing record: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Delete a record from the table
     */
    private void deleteRecord(int row) {
        try {
            // Get the record ID from the table
            String recordId = billTable.getValueAt(row, 0).toString();
            
            // Confirm deletion
            int result = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete this record?", 
                "Confirm Delete", 
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (result == JOptionPane.YES_OPTION) {                // Find and delete the record
                List<BillRecord> records = billDataService.getAllBillRecords();
                BillRecord record = records.stream()
                    .filter(r -> String.valueOf(r.getSerialNo()).equals(recordId))
                    .findFirst()
                    .orElse(null);
                      if (record != null) {
                    billDataService.deleteBillRecord(record.getSerialNo());
                    loadTableData(); // Refresh the table
                    refreshAnalyticsTable(); // Update analytics if needed
                    JOptionPane.showMessageDialog(this, "Record deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Record not found!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error deleting record: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }    /**
     * ActionButtonRenderer - Renders buttons in table cells with proper visibility
     */
    class ActionButtonRenderer extends JButton implements TableCellRenderer {
        public ActionButtonRenderer() {
            setOpaque(true);
            setFocusPainted(false);
            setBorderPainted(true);
            setContentAreaFilled(true);
            setFont(new Font("Arial", Font.BOLD, 10));
            setMargin(new Insets(1, 2, 1, 2));
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            String buttonText = value != null ? value.toString() : "";
            setText(buttonText);
            
            // Ensure text is always visible by setting explicit colors and properties
            setFont(new Font("Arial", Font.BOLD, 10));
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
              // Set different colors for each button type with YOUR SPECIFIED COLORS
            if (buttonText.equals("Download")) {
                setBackground(Color.GREEN);   // Green background
                setForeground(Color.BLACK);   // Black text
                setToolTipText("Download PDF file");
                setText("Download");
            } else if (buttonText.equals("Edit")) {
                setBackground(Color.BLUE);    // Blue background
                setForeground(Color.BLACK);   // Black text
                setToolTipText("Edit this record");
                setText("Edit");
            } else if (buttonText.equals("Delete")) {
                setBackground(Color.RED);     // Red background
                setForeground(Color.BLACK);   // Black text
                setToolTipText("Delete this record");
                setText("Delete");
            } else {
                // Default styling for any other button text
                setBackground(new Color(108, 117, 125));
                setForeground(Color.WHITE);
                setText(buttonText.isEmpty() ? "Action" : buttonText);
            }
            
            // Force visibility and enabled state
            setEnabled(true);
            setVisible(true);
            
            // Ensure proper border for visibility
            setBorder(BorderFactory.createRaisedBevelBorder());
            
            // Force text to be painted
            setOpaque(true);
            setContentAreaFilled(true);
            
            return this;
        }
    }/**
     * ButtonEditor - Handles button clicks in table cells
     */
    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private int row;        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setFocusPainted(false);
            button.setBorderPainted(true);
            button.setContentAreaFilled(true);
            button.setFont(new Font("Arial", Font.BOLD, 10));
            button.setMargin(new Insets(1, 2, 1, 2));
            button.setHorizontalAlignment(SwingConstants.CENTER);
            button.setVerticalAlignment(SwingConstants.CENTER);
            
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            
            this.row = row;
            this.label = value != null ? value.toString() : "";
            button.setText(label);
            
            // Ensure text is always visible
            button.setFont(new Font("Arial", Font.BOLD, 10));
            button.setHorizontalAlignment(SwingConstants.CENTER);
            button.setVerticalAlignment(SwingConstants.CENTER);
              // Set colors same as renderer with YOUR SPECIFIED COLORS
            if (label.equals("Download")) {
                button.setBackground(Color.GREEN);   // Green background
                button.setForeground(Color.BLACK);   // Black text
                button.setText("Download");
            } else if (label.equals("Edit")) {
                button.setBackground(Color.BLUE);    // Blue background
                button.setForeground(Color.BLACK);   // Black text
                button.setText("Edit");
            } else if (label.equals("Delete")) {
                button.setBackground(Color.RED);     // Red background
                button.setForeground(Color.BLACK);   // Black text
                button.setText("Delete");
            } else {
                // Default styling for any other button text
                button.setBackground(new Color(108, 117, 125));
                button.setForeground(Color.WHITE);
                button.setText(label.isEmpty() ? "Action" : label);
            }
            
            // Force visibility and enabled state
            button.setEnabled(true);
            button.setVisible(true);
            button.setBorder(BorderFactory.createRaisedBevelBorder());
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Handle button action based on label
                if (label.equals("Download")) {
                    BillTrackerDashboard.this.downloadPdf(row);
                } else if (label.equals("Edit")) {
                    BillTrackerDashboard.this.editRecord(row);
                } else if (label.equals("Delete")) {
                    BillTrackerDashboard.this.deleteRecord(row);
                }
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
    
    /**
     * Handle table click events for action buttons
     */
    private void handleTableClick(MouseEvent evt) {
        int row = billTable.rowAtPoint(evt.getPoint());
        int col = billTable.columnAtPoint(evt.getPoint());
        
        if (row >= 0 && col >= 0) {
            // Check if click is on action button columns
            String columnName = billTable.getColumnName(col);
            
            if (columnName.equals("Download PDF")) {
                downloadPdf(row);
            } else if (columnName.equals("Edit")) {
                editRecord(row);
            } else if (columnName.equals("Delete")) {
                deleteRecord(row);
            }
        }
    }
    
    // NetworkVendorChangeListener implementation
    @Override
    public void onNetworksChanged() {
        SwingUtilities.invokeLater(() -> {
            // Refresh all network combo boxes in the UI
            populateNetworkComboBox();
            populateUpdateNetworkComboBox();
            populateAnalyticsNetworkComboBox();
            
            // Also refresh billing period options in Update tab and quarter filters
            updateBillingPeriodOptions();
            populateViewQuarterComboBox();
            populateAnalyticsQuarterComboBox();
        });
    }
    
    @Override
    public void onVendorsChanged(String network) {
        SwingUtilities.invokeLater(() -> {
            // Refresh vendor combo boxes based on the changed network
            populateVendorComboBox();
            populateUpdateVendorComboBox();
            populateAnalyticsVendorComboBox();
        });
    }
    
    /**
     * Setup the Analytics tab with filters and table
     */
    private void setupAnalyticsTab() {
        // Remove any existing components
        analyticsPanel.removeAll();
        
        // Create comprehensive analytics filter panel
        JPanel filterPanel = createComprehensiveAnalyticsFilterPanel();
        analyticsPanel.add(filterPanel, BorderLayout.NORTH);
        
        // Create the comprehensive financial analytics content
        JPanel financialAnalyticsContent = createFinancialAnalyticsPanel();
        analyticsPanel.add(financialAnalyticsContent, BorderLayout.CENTER);
        
        // Populate all filter combo boxes
        populateAnalyticsNetworkComboBox();
        populateAnalyticsYearComboBox();
        populateAnalyticsQuarterComboBox();
        populateAnalyticsCostCenterComboBox();
        populateAnalyticsCommitItemComboBox();
        
        // Refresh the panel
        analyticsPanel.revalidate();
        analyticsPanel.repaint();
    }
    
    /**
     * Create comprehensive analytics filter panel with all filters
     */
    private JPanel createComprehensiveAnalyticsFilterPanel() {
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204), 2),
            "Analytics Filters",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(0, 102, 204)
        ));
        filterPanel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // First row - Network and Vendor
        gbc.gridx = 0; gbc.gridy = 0;
        filterPanel.add(new JLabel("Network:"), gbc);
        gbc.gridx = 1;
        analyticsNetworkComboBox = new JComboBox<>();
        analyticsNetworkComboBox.setPreferredSize(new Dimension(150, 25));
        analyticsNetworkComboBox.addActionListener(e -> {
            populateAnalyticsVendorComboBox();
            populateAnalyticsQuarterComboBox(); // Update quarters when network changes
        });
        filterPanel.add(analyticsNetworkComboBox, gbc);
        
        gbc.gridx = 2;
        filterPanel.add(new JLabel("Vendor:"), gbc);
        gbc.gridx = 3;
        analyticsVendorComboBox = new JComboBox<>();
        analyticsVendorComboBox.setPreferredSize(new Dimension(150, 25));
        analyticsVendorComboBox.addActionListener(e -> refreshFinancialAnalytics());
        filterPanel.add(analyticsVendorComboBox, gbc);
        
        // Second row - Year and Quarter
        gbc.gridx = 0; gbc.gridy = 1;
        filterPanel.add(new JLabel("Year:"), gbc);
        gbc.gridx = 1;
        analyticsYearComboBox = new JComboBox<>();
        analyticsYearComboBox.setPreferredSize(new Dimension(150, 25));
        analyticsYearComboBox.addActionListener(e -> refreshFinancialAnalytics());
        filterPanel.add(analyticsYearComboBox, gbc);
        
        gbc.gridx = 2;
        filterPanel.add(new JLabel("Quarter:"), gbc);
        gbc.gridx = 3;
        analyticsQuarterComboBox = new JComboBox<>();
        analyticsQuarterComboBox.setPreferredSize(new Dimension(150, 25));
        analyticsQuarterComboBox.addActionListener(e -> refreshFinancialAnalytics());
        filterPanel.add(analyticsQuarterComboBox, gbc);
        
        // Third row - Cost Center and Commit Item
        gbc.gridx = 0; gbc.gridy = 2;
        filterPanel.add(new JLabel("Cost Center:"), gbc);
        gbc.gridx = 1;
        analyticsCostCenterComboBox = new JComboBox<>();
        analyticsCostCenterComboBox.setPreferredSize(new Dimension(150, 25));
        analyticsCostCenterComboBox.addActionListener(e -> refreshFinancialAnalytics());
        filterPanel.add(analyticsCostCenterComboBox, gbc);
        
        gbc.gridx = 2;
        filterPanel.add(new JLabel("Commit Item:"), gbc);
        gbc.gridx = 3;
        analyticsCommitItemComboBox = new JComboBox<>();
        analyticsCommitItemComboBox.setPreferredSize(new Dimension(150, 25));
        analyticsCommitItemComboBox.addActionListener(e -> refreshFinancialAnalytics());
        filterPanel.add(analyticsCommitItemComboBox, gbc);
        
        // Fourth row - Apply, Clear, and Refresh buttons with custom rendering for maximum visibility
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        
        // Create Apply button with custom rendering
        JButton applyButton = new JButton("Apply Filters") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Force text rendering with black text
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(Color.BLACK);
                FontMetrics fm = g2.getFontMetrics();
                String text = "Apply Filters";
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, x, y);
                g2.dispose();
            }
        };
        applyButton.setBackground(new Color(0, 102, 204));
        applyButton.setForeground(Color.BLACK);
        applyButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        applyButton.setFocusPainted(false);
        applyButton.setBorderPainted(true);
        applyButton.setContentAreaFilled(true);
        applyButton.setOpaque(true);
        applyButton.setPreferredSize(new Dimension(120, 30));
        applyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        applyButton.addActionListener(e -> refreshFinancialAnalytics());
        filterPanel.add(applyButton, gbc);
        
        gbc.gridx = 1;
        
        // Create Clear button with custom rendering
        JButton clearButton = new JButton("Clear Filters") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Force text rendering with black text
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(Color.BLACK);
                FontMetrics fm = g2.getFontMetrics();
                String text = "Clear Filters";
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, x, y);
                g2.dispose();
            }
        };
        clearButton.setBackground(new Color(108, 117, 125));
        clearButton.setForeground(Color.BLACK);
        clearButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        clearButton.setFocusPainted(false);
        clearButton.setBorderPainted(true);
        clearButton.setContentAreaFilled(true);
        clearButton.setOpaque(true);
        clearButton.setPreferredSize(new Dimension(120, 30));
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        clearButton.addActionListener(e -> clearAnalyticsFilters());
        filterPanel.add(clearButton, gbc);
        
        gbc.gridx = 2;
        
        // Create Refresh button with custom rendering
        JButton refreshButton = new JButton("Refresh Data") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Force text rendering with black text
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(Color.BLACK);
                FontMetrics fm = g2.getFontMetrics();
                String text = "Refresh Data";
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, x, y);
                g2.dispose();
            }
        };
        refreshButton.setBackground(new Color(40, 167, 69));
        refreshButton.setForeground(Color.BLACK);
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(true);
        refreshButton.setContentAreaFilled(true);
        refreshButton.setOpaque(true);
        refreshButton.setPreferredSize(new Dimension(120, 30));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        refreshButton.setToolTipText("Refresh analytics data from database");
        refreshButton.addActionListener(e -> {
            // Force refresh all data and components
            populateAnalyticsNetworkComboBox();
            populateAnalyticsYearComboBox();
            populateAnalyticsQuarterComboBox();
            populateAnalyticsCostCenterComboBox();
            populateAnalyticsCommitItemComboBox();
            refreshFinancialAnalytics();
            JOptionPane.showMessageDialog(this, 
                "Analytics data refreshed successfully!", 
                "Refresh Complete", 
                JOptionPane.INFORMATION_MESSAGE);
        });
        filterPanel.add(refreshButton, gbc);
        
        return filterPanel;
    }

    /**
     * Create the analytics content panel with filters and table
     */
    private JPanel createAnalyticsContent() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        
        // Create filter panel
        JPanel filterPanel = createAnalyticsFilterPanel();
        contentPanel.add(filterPanel, BorderLayout.NORTH);
        
        // Create analytics table
        createAnalyticsTable();
        JScrollPane scrollPane = new JScrollPane(analyticsTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Analytics Results"));
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        return contentPanel;
    }
    
    /**
     * Create the analytics filter panel
     */
    private JPanel createAnalyticsFilterPanel() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Analytics Filters"));
        
        // Network filter
        filterPanel.add(new JLabel("Network:"));
        analyticsNetworkComboBox = new JComboBox<>();
        analyticsNetworkComboBox.addActionListener(e -> populateAnalyticsVendorComboBox());
        filterPanel.add(analyticsNetworkComboBox);
        
        // Vendor filter
        filterPanel.add(new JLabel("Vendor:"));
        analyticsVendorComboBox = new JComboBox<>();
        filterPanel.add(analyticsVendorComboBox);
        
        // Year filter
        filterPanel.add(new JLabel("Year:"));
        analyticsYearComboBox = new JComboBox<>();
        filterPanel.add(analyticsYearComboBox);
        
        // Quarter filter
        filterPanel.add(new JLabel("Quarter:"));
        analyticsQuarterComboBox = new JComboBox<>();
        filterPanel.add(analyticsQuarterComboBox);
        
        // Apply button
        JButton applyButton = new JButton("Apply Filters");
        applyButton.addActionListener(e -> refreshAnalyticsTable());
        filterPanel.add(applyButton);
        
        return filterPanel;
    }
    
    /**
     * Create analytics table
     */
    private void createAnalyticsTable() {
        String[] columns = {"Network", "Vendor", "Count", "Total Amount", "Avg Amount"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        analyticsTable = new JTable(model);
        analyticsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        analyticsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
    }
    
    /**
     * Refresh analytics table data
     */
    private void refreshAnalyticsTable() {
        if (analyticsTable == null) return;
        
        DefaultTableModel model = (DefaultTableModel) analyticsTable.getModel();
        model.setRowCount(0);
        
        // Add some placeholder data for now
        model.addRow(new Object[]{"BSNL", "Vendor1", "5", "₹50,000", "₹10,000"});
        model.addRow(new Object[]{"Airtel", "Vendor2", "3", "₹30,000", "₹10,000"});
    }
    
    /**
     * Populate the analytics network combo box
     */
    private void populateAnalyticsNetworkComboBox() {
        if (analyticsNetworkComboBox != null) {
            analyticsNetworkComboBox.removeAllItems();
            analyticsNetworkComboBox.addItem("All Networks");
            for (String network : networkManager.getAllNetworks()) {
                analyticsNetworkComboBox.addItem(network);
            }
        }
    }
    
    /**
     * Populate the analytics vendor combo box based on selected network
     */
    private void populateAnalyticsVendorComboBox() {
        if (analyticsVendorComboBox != null) {
            analyticsVendorComboBox.removeAllItems();
            analyticsVendorComboBox.addItem("All Vendors");
            
            String selectedNetwork = (String) analyticsNetworkComboBox.getSelectedItem();
            if (selectedNetwork != null && !selectedNetwork.equals("All Networks")) {
                List<String> vendors = networkManager.getVendorsForNetwork(selectedNetwork);
                for (String vendor : vendors) {
                    analyticsVendorComboBox.addItem(vendor);
                }
            } else {
                // Show all vendors from all networks
                for (String network : networkManager.getAllNetworks()) {
                    List<String> vendors = networkManager.getVendorsForNetwork(network);
                    for (String vendor : vendors) {
                        if (analyticsVendorComboBox.getItemCount() == 1 || 
                            !containsItem(analyticsVendorComboBox, vendor)) {
                            analyticsVendorComboBox.addItem(vendor);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Helper method to check if combo box contains item
     */
    private boolean containsItem(JComboBox<String> comboBox, String item) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (item.equals(comboBox.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Populate the analytics quarter combo box
     */
    private void populateAnalyticsQuarterComboBox() {
        if (analyticsQuarterComboBox != null) {
            analyticsQuarterComboBox.removeAllItems();
            analyticsQuarterComboBox.addItem("All Quarters");
            
            String selectedNetwork = (String) analyticsNetworkComboBox.getSelectedItem();
            
            if (selectedNetwork != null && !selectedNetwork.equals("All Networks")) {
                // Get network-specific quarters from NetworkVendorManager
                List<String> quarters = networkManager.getFormattedQuarters(selectedNetwork);
                if (quarters != null && !quarters.isEmpty()) {
                    for (String quarter : quarters) {
                        analyticsQuarterComboBox.addItem(quarter);
                    }
                } else {
                    // Fallback to standard quarters if network quarters not available
                    analyticsQuarterComboBox.addItem("Q1 (Jan-Mar)");
                    analyticsQuarterComboBox.addItem("Q2 (Apr-Jun)");
                    analyticsQuarterComboBox.addItem("Q3 (Jul-Sep)");
                    analyticsQuarterComboBox.addItem("Q4 (Oct-Dec)");
                }
            } else {
                // Show all possible quarters when "All Networks" is selected
                analyticsQuarterComboBox.addItem("Q1 (Jan-Mar)");
                analyticsQuarterComboBox.addItem("Q2 (Apr-Jun)");  
                analyticsQuarterComboBox.addItem("Q3 (Jul-Sep)");
                analyticsQuarterComboBox.addItem("Q4 (Oct-Dec)");
            }
        }
    }
    
    /**
     * Populate the analytics cost center combo box
     */
    private void populateAnalyticsCostCenterComboBox() {
        if (analyticsCostCenterComboBox != null) {
            analyticsCostCenterComboBox.removeAllItems();
            analyticsCostCenterComboBox.addItem("All Cost Centers");
            
            // Add the user-supplied cost center options
            String[] costCenters = {"M75010-SRO", "M78010-TNSO"};
            
            for (String costCenter : costCenters) {
                analyticsCostCenterComboBox.addItem(costCenter);
            }
        }
    }
    
    /**
     * Populate the analytics commit item combo box
     */
    private void populateAnalyticsCommitItemComboBox() {
        if (analyticsCommitItemComboBox != null) {
            analyticsCommitItemComboBox.removeAllItems();
            analyticsCommitItemComboBox.addItem("All Commit Items");
            
            // Add the user-supplied commit item options
            String[] commitItems = {"C_COMMEXP", "C_R&MEQPC"};
            
            for (String commitItem : commitItems) {
                analyticsCommitItemComboBox.addItem(commitItem);
            }
        }
    }
    
    /**
     * Refresh the financial analytics display
     */
    private void refreshFinancialAnalytics() {
        // Remove existing financial analytics content and recreate it
        Component[] components = analyticsPanel.getComponents();
        for (Component comp : components) {
            if (comp != analyticsPanel.getComponent(0)) { // Keep the filter panel
                analyticsPanel.remove(comp);
            }
        }
        
        // Recreate and add the financial analytics content
        JPanel financialAnalyticsContent = createFinancialAnalyticsPanel();
        analyticsPanel.add(financialAnalyticsContent, BorderLayout.CENTER);
        
        // Refresh the panel
        analyticsPanel.revalidate();
        analyticsPanel.repaint();
    }
    
    /**
     * Clear all analytics filters
     */
    private void clearAnalyticsFilters() {
        if (analyticsNetworkComboBox != null) {
            analyticsNetworkComboBox.setSelectedIndex(0); // "All Networks"
        }
        if (analyticsVendorComboBox != null) {
            analyticsVendorComboBox.setSelectedIndex(0); // "All Vendors"
        }
        if (analyticsYearComboBox != null) {
            analyticsYearComboBox.setSelectedIndex(0); // "All Years"
        }
        if (analyticsQuarterComboBox != null) {
            analyticsQuarterComboBox.setSelectedIndex(0); // "All Quarters"
        }
        if (analyticsCostCenterComboBox != null) {
            analyticsCostCenterComboBox.setSelectedIndex(0); // "All Cost Centers"
        }
        if (analyticsCommitItemComboBox != null) {
            analyticsCommitItemComboBox.setSelectedIndex(0); // "All Commit Items"
        }
        
        // Refresh the analytics display
        refreshFinancialAnalytics();
    }
    
    /**
     * Show user management dialog for admins
     */
    private void showUserManagementDialog() {
        JDialog userDialog = new JDialog(this, "User Management", true);
        userDialog.setSize(800, 600);
        userDialog.setLocationRelativeTo(this);
        userDialog.setLayout(new BorderLayout());
        
        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("User Management - Admin Panel");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(0, 102, 204));
        titlePanel.add(titleLabel);
        contentPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Create tabbed pane for different user management functions
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // User List Tab
        JPanel userListPanel = createUserListPanel();
        tabbedPane.addTab("User List", userListPanel);
        
        // Add User Tab
        JPanel addUserPanel = createAddUserPanel();
        tabbedPane.addTab("Add User", addUserPanel);
        
        contentPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Bottom button panel
        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.setBackground(Color.WHITE);
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshButton.setBackground(new Color(0, 123, 255));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBorderPainted(false);
        refreshButton.setFocusPainted(false);
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> {
            // Refresh both panels
            tabbedPane.removeAll();
            tabbedPane.addTab("User List", createUserListPanel());
            tabbedPane.addTab("Add User", createAddUserPanel());
        });
        bottomPanel.add(refreshButton);
        
        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeButton.setBackground(new Color(108, 117, 125));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> userDialog.dispose());
        bottomPanel.add(closeButton);
        
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
        userDialog.add(contentPanel);
        userDialog.setVisible(true);
    }
    
    /**
     * Create user list panel for viewing and managing existing users
     */
    private JPanel createUserListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Get current admin user for authorization
        User currentAdmin = userService.findUserByUsername(currentUser);
        if (currentAdmin == null || !currentAdmin.isAdmin()) {
            JLabel errorLabel = new JLabel("Access Denied: Admin privileges required");
            errorLabel.setForeground(Color.RED);
            panel.add(errorLabel, BorderLayout.CENTER);
            return panel;
        }
        
        // Get all users
        List<User> allUsers = userService.getAllUsers(currentAdmin);
        
        // Create table columns
        String[] columnNames = {"Username", "Role", "Actions"};
        Object[][] data = new Object[allUsers.size()][3];
        
        for (int i = 0; i < allUsers.size(); i++) {
            User user = allUsers.get(i);
            data[i][0] = user.getUsername();
            data[i][1] = user.getRole().toString();
            data[i][2] = "Actions";
        }
        
        // Create table
        JTable userTable = new JTable(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Only actions column is editable
            }
        };
        
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userTable.setRowHeight(30);
        userTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        userTable.getTableHeader().setBackground(new Color(230, 240, 250));
        
        // Set column widths
        userTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        
        // Add action button functionality
        userTable.getColumn("Actions").setCellRenderer(new UserActionButtonRenderer());
        userTable.getColumn("Actions").setCellEditor(new UserActionButtonEditor(new JCheckBox(), allUsers, this));
        
        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setPreferredSize(new Dimension(700, 400));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add info label
        JLabel infoLabel = new JLabel("Total Users: " + allUsers.size() + " | Admin Users: " + 
            allUsers.stream().mapToInt(u -> u.isAdmin() ? 1 : 0).sum());
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoLabel.setForeground(new Color(108, 117, 125));
        panel.add(infoLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Create add user panel for adding new users
     */
    private JPanel createAddUserPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Title
        JLabel titleLabel = new JLabel("Add New User");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(0, 102, 204));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        
        // Username
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(userLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField usernameField = new JTextField(20);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(usernameField, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(passLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(passwordField, gbc);
        
        // Confirm Password
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(confirmLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(confirmPasswordField, gbc);
        
        // Role
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        JLabel roleLabel = new JLabel("Role:");
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(roleLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JComboBox<UserRole> roleComboBox = new JComboBox<>(UserRole.values());
        roleComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(roleComboBox, gbc);
        
        // Add button
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        JButton addButton = new JButton("Add User");
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        addButton.setBackground(new Color(40, 167, 69));
        addButton.setForeground(Color.WHITE);
        addButton.setPreferredSize(new Dimension(150, 40));
        addButton.setBorderPainted(false);
        addButton.setFocusPainted(false);
        addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        addButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            UserRole role = (UserRole) roleComboBox.getSelectedItem();
            
            // Validation
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Username and password are required!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(panel, "Passwords do not match!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (password.length() < 3) {
                JOptionPane.showMessageDialog(panel, "Password must be at least 3 characters long!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (userService.userExists(username)) {
                JOptionPane.showMessageDialog(panel, "Username already exists!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create and add user (without full name and email)
            User newUser = new User(username, password, role, null, null);
            
            boolean success = userService.addUser(newUser);
            
            if (success) {
                JOptionPane.showMessageDialog(panel, 
                    "User '" + username + "' added successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                
                // Clear form
                usernameField.setText("");
                passwordField.setText("");
                confirmPasswordField.setText("");
                roleComboBox.setSelectedIndex(0);
            } else {
                JOptionPane.showMessageDialog(panel, "Failed to add user. Please try again.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        panel.add(addButton, gbc);
        
        return panel;
    }
    
    /**
     * Show role management dialog for admins  
     */
    
    /**
     * Show change password dialog for current admin user
     */
    private void showChangePasswordDialog() {
        JDialog passwordDialog = new JDialog(this, "Change Password", true);
        passwordDialog.setSize(400, 250);
        passwordDialog.setLocationRelativeTo(this);
        passwordDialog.setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Title
        JLabel titleLabel = new JLabel("Change Password for: " + currentUser);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(0, 102, 204));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        contentPanel.add(titleLabel, gbc);
        
        // Current Password
        gbc.gridwidth = 1; gbc.gridy = 1;
        JLabel currentPassLabel = new JLabel("Current Password:");
        currentPassLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(currentPassLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField currentPassField = new JPasswordField(15);
        contentPanel.add(currentPassField, gbc);
        
        // New Password
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        JLabel newPassLabel = new JLabel("New Password:");
        newPassLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(newPassLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField newPassField = new JPasswordField(15);
        contentPanel.add(newPassField, gbc);
        
        // Confirm New Password
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        JLabel confirmPassLabel = new JLabel("Confirm New Password:");
        confirmPassLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(confirmPassLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField confirmPassField = new JPasswordField(15);
        contentPanel.add(confirmPassField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(Color.WHITE);
        
        JButton changeButton = new JButton("Change Password");
        changeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        changeButton.setBackground(new Color(40, 167, 69));
        changeButton.setForeground(Color.WHITE);
        changeButton.setBorderPainted(false);
        changeButton.setFocusPainted(false);
        changeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        changeButton.addActionListener(e -> {
            String currentPassword = new String(currentPassField.getPassword());
            String newPassword = new String(newPassField.getPassword());
            String confirmPassword = new String(confirmPassField.getPassword());
            
            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(passwordDialog, "All fields are required!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(passwordDialog, "New passwords do not match!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (newPassword.length() < 3) {
                JOptionPane.showMessageDialog(passwordDialog, "New password must be at least 3 characters long!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Verify current password and change to new password
            boolean success = userService.changeUserPassword(currentUser, currentPassword, newPassword);
            
            if (success) {
                JOptionPane.showMessageDialog(passwordDialog, 
                    "Password changed successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                passwordDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(passwordDialog, 
                    "Failed to change password. Please check your current password.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(changeButton);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelButton.setBackground(new Color(108, 117, 125));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setBorderPainted(false);
        cancelButton.setFocusPainted(false);
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> passwordDialog.dispose());
        buttonPanel.add(cancelButton);
        
        passwordDialog.add(contentPanel, BorderLayout.CENTER);
        passwordDialog.add(buttonPanel, BorderLayout.SOUTH);
        passwordDialog.setVisible(true);
    }
    
    /**
     * Show change username dialog for current admin user
     */
    private void showChangeUsernameDialog() {
        JDialog usernameDialog = new JDialog(this, "Change Username", true);
        usernameDialog.setSize(400, 200);
        usernameDialog.setLocationRelativeTo(this);
        usernameDialog.setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Title
        JLabel titleLabel = new JLabel("Change Username");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(0, 102, 204));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        contentPanel.add(titleLabel, gbc);
        
        // Current Username (display only)
        gbc.gridwidth = 1; gbc.gridy = 1;
        JLabel currentUserLabel = new JLabel("Current Username:");
        currentUserLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(currentUserLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel currentUserDisplay = new JLabel(currentUser);
        currentUserDisplay.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        currentUserDisplay.setForeground(new Color(100, 100, 100));
        contentPanel.add(currentUserDisplay, gbc);
        
        // New Username
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        JLabel newUserLabel = new JLabel("New Username:");
        newUserLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(newUserLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField newUserField = new JTextField(15);
        contentPanel.add(newUserField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(Color.WHITE);
        
        JButton changeButton = new JButton("Change Username");
        changeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        changeButton.setBackground(new Color(40, 167, 69));
        changeButton.setForeground(Color.WHITE);
        changeButton.setBorderPainted(false);
        changeButton.setFocusPainted(false);
        changeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        changeButton.addActionListener(e -> {
            String newUsername = newUserField.getText().trim();
            
            if (newUsername.isEmpty()) {
                JOptionPane.showMessageDialog(usernameDialog, "New username cannot be empty!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (newUsername.equals(currentUser)) {
                JOptionPane.showMessageDialog(usernameDialog, "New username must be different from current username!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (userService.userExists(newUsername)) {
                JOptionPane.showMessageDialog(usernameDialog, "Username already exists!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Change username
            boolean success = userService.changeUsername(currentUser, newUsername, currentUserObject);
            
            if (success) {
                JOptionPane.showMessageDialog(usernameDialog, 
                    "Username changed successfully!\nYou will be logged out. Please login with your new username.", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                usernameDialog.dispose();
                
                // Logout user since username changed
                performLogout();
            } else {
                JOptionPane.showMessageDialog(usernameDialog, 
                    "Failed to change username. Please try again.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(changeButton);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelButton.setBackground(new Color(108, 117, 125));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setBorderPainted(false);
        cancelButton.setFocusPainted(false);
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> usernameDialog.dispose());
        buttonPanel.add(cancelButton);
        
        usernameDialog.add(contentPanel, BorderLayout.CENTER);
        usernameDialog.add(buttonPanel, BorderLayout.SOUTH);
        usernameDialog.setVisible(true);
    }
    
    /**
     * Show admin management dialog for managing admin accounts
     */
    private void showAdminManagementDialog() {
        JDialog adminDialog = new JDialog(this, "Admin Account Management", true);
        adminDialog.setSize(600, 500);
        adminDialog.setLocationRelativeTo(this);
        adminDialog.setLayout(new BorderLayout());
        
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Title
        JLabel titleLabel = new JLabel("Admin Account Management", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(0, 102, 204));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Admin list
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(BorderFactory.createTitledBorder("Current Admin Users"));
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> adminList = new JList<>(listModel);
        adminList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        adminList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Populate admin list
        refreshAdminList(listModel);
        
        JScrollPane listScrollPane = new JScrollPane(adminList);
        listScrollPane.setPreferredSize(new Dimension(0, 200));
        listPanel.add(listScrollPane, BorderLayout.CENTER);
        
        mainPanel.add(listPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton addAdminButton = new JButton("Add New Admin");
        addAdminButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addAdminButton.setBackground(new Color(40, 167, 69));
        addAdminButton.setForeground(Color.WHITE);
        addAdminButton.setBorderPainted(false);
        addAdminButton.setFocusPainted(false);
        addAdminButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addAdminButton.addActionListener(e -> {
            showAddAdminDialog(listModel);
        });
        buttonPanel.add(addAdminButton);
        
        JButton removeAdminButton = new JButton("Remove Admin");
        removeAdminButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        removeAdminButton.setBackground(new Color(220, 53, 69));
        removeAdminButton.setForeground(Color.WHITE);
        removeAdminButton.setBorderPainted(false);
        removeAdminButton.setFocusPainted(false);
        removeAdminButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeAdminButton.addActionListener(e -> {
            String selectedAdmin = adminList.getSelectedValue();
            if (selectedAdmin != null) {
                removeAdminUser(selectedAdmin, listModel);
            } else {
                JOptionPane.showMessageDialog(adminDialog, "Please select an admin to remove.", 
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        });
        buttonPanel.add(removeAdminButton);
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshButton.setBackground(new Color(0, 123, 255));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBorderPainted(false);
        refreshButton.setFocusPainted(false);
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> refreshAdminList(listModel));
        buttonPanel.add(refreshButton);
        
        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeButton.setBackground(new Color(108, 117, 125));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> adminDialog.dispose());
        buttonPanel.add(closeButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        adminDialog.add(mainPanel);
        adminDialog.setVisible(true);
    }
    
    /**
     * Refresh the admin list
     */
    private void refreshAdminList(DefaultListModel<String> listModel) {
        listModel.clear();
        java.util.List<User> allUsers = userService.getAllUsers(currentUserObject);
        for (User user : allUsers) {
            if (user.isAdmin()) {
                String displayText = user.getUsername();
                if (user.getUsername().equals(currentUser)) {
                    displayText += " (You)";
                }
                listModel.addElement(displayText);
            }
        }
    }
    
    /**
     * Show dialog to add new admin user
     */
    private void showAddAdminDialog(DefaultListModel<String> listModel) {
        JDialog addDialog = new JDialog(this, "Add New Admin", true);
        addDialog.setSize(400, 300);
        addDialog.setLocationRelativeTo(this);
        addDialog.setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Title
        JLabel titleLabel = new JLabel("Create New Admin Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
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
        contentPanel.add(adminUserField, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(passLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField adminPassField = new JPasswordField(15);
        contentPanel.add(adminPassField, gbc);
        
        // Confirm Password
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(confirmLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField confirmPassField = new JPasswordField(15);
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
                JOptionPane.showMessageDialog(addDialog, "Username and password are required!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(addDialog, "Passwords do not match!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (userService.userExists(username)) {
                JOptionPane.showMessageDialog(addDialog, "Username already exists!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create admin user
            boolean success = userService.registerUser(username, password, UserRole.ADMIN, 
                                                     "Administrator", "", currentUserObject);
            
            if (success) {
                JOptionPane.showMessageDialog(addDialog, 
                    "Admin account created successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                addDialog.dispose();
                refreshAdminList(listModel);
            } else {
                JOptionPane.showMessageDialog(addDialog, "Failed to create admin account!", 
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
        cancelButton.addActionListener(e -> addDialog.dispose());
        buttonPanel.add(cancelButton);
        
        addDialog.add(contentPanel, BorderLayout.CENTER);
        addDialog.add(buttonPanel, BorderLayout.SOUTH);
        addDialog.setVisible(true);
    }
    
    /**
     * Remove admin user
     */
    private void removeAdminUser(String adminDisplayText, DefaultListModel<String> listModel) {
        // Extract username from display text (remove " (You)" if present)
        String username = adminDisplayText.replace(" (You)", "");
        
        if (username.equals(currentUser)) {
            JOptionPane.showMessageDialog(this, "You cannot remove your own admin account!", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Check if this is the last admin
        java.util.List<User> allUsers = userService.getAllUsers(currentUserObject);
        long adminCount = allUsers.stream().filter(User::isAdmin).count();
        
        if (adminCount <= 1) {
            JOptionPane.showMessageDialog(this, "Cannot remove the last admin user!\nAt least one admin must exist.", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to remove admin privileges from '" + username + "'?\n" +
            "This will demote them to a regular user.",
            "Remove Admin",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = userService.updateUserRole(username, UserRole.USER, currentUserObject);
            if (success) {
                JOptionPane.showMessageDialog(this, "Admin privileges removed successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshAdminList(listModel);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to remove admin privileges!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Button renderer for user management table actions
     */
    class UserActionButtonRenderer extends JButton implements TableCellRenderer {
        public UserActionButtonRenderer() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.BOLD, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Delete");
            setBackground(Color.RED);
            setForeground(Color.WHITE);
            setOpaque(true);
            setBorderPainted(false);
            return this;
        }
    }

    /**
     * Button editor for user management table actions
     */
    class UserActionButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private List<User> userList;
        private BillTrackerDashboard parentDialog;

        public UserActionButtonEditor(JCheckBox checkBox, List<User> users, BillTrackerDashboard parent) {
            super(checkBox);
            this.userList = users;
            this.parentDialog = parent;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = "Delete";
            button.setText(label);
            button.setBackground(Color.RED);
            button.setForeground(Color.WHITE);
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setFont(new Font("Segoe UI", Font.BOLD, 10));
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed && userList != null) {
                int row = ((JTable) button.getParent()).getSelectedRow();
                if (row >= 0 && row < userList.size()) {
                    User userToDelete = userList.get(row);
                    String username = userToDelete.getUsername();
                    
                    // Get current admin user for authorization
                    User currentAdmin = userService.findUserByUsername(currentUser);
                    
                    // Prevent deleting admin users or self
                    if (userToDelete.isAdmin()) {
                        if (username.equals(currentUser)) {
                            JOptionPane.showMessageDialog(button, "Cannot delete your own account!", 
                                "Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(button, "Cannot delete admin users from this interface!\n" + 
                                "Use 'Manage Admin Accounts' option instead.", 
                                "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        int confirm = JOptionPane.showConfirmDialog(button, 
                            "Are you sure you want to delete user '" + username + "'?\n" +
                            "This action cannot be undone.", 
                            "Delete User", 
                            JOptionPane.YES_NO_OPTION, 
                            JOptionPane.QUESTION_MESSAGE);
                            
                        if (confirm == JOptionPane.YES_OPTION) {
                            boolean success = userService.deleteUser(username, currentAdmin);
                            if (success) {
                                JOptionPane.showMessageDialog(button, 
                                    "User '" + username + "' deleted successfully!", 
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                                // Note: In a real application, you'd refresh the dialog here
                            } else {
                                JOptionPane.showMessageDialog(button, 
                                    "Failed to delete user '" + username + "'!", 
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}
