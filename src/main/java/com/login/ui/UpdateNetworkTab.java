package com.login.ui;
import com.login.service.NetworkVendorManager;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * Update Network Tab - Manage networks and vendors with CRUD operations
 */
public class UpdateNetworkTab extends JPanel implements NetworkVendorManager.NetworkVendorChangeListener {
    
    private NetworkVendorManager networkManager;
    
    // Add Network Components
    private JTextField networkNameField;
    private JPanel vendorInputPanel;
    private java.util.List<JTextField> vendorFields;
    private JButton addVendorFieldButton;
    private JButton addNetworkButton;
    
    // Quarter Configuration Components
    private JSpinner numberOfQuartersSpinner;
    private JPanel quarterConfigPanel;
    private java.util.List<QuarterInputPanel> quarterInputPanels;
    
    // View/Edit Components
    private JTable networkTable;
    private DefaultTableModel networkTableModel;
    private JTable vendorTable;
    private DefaultTableModel vendorTableModel;
    private JLabel selectedNetworkLabel;
    private String currentSelectedNetwork;
    
    public UpdateNetworkTab() {
        networkManager = NetworkVendorManager.getInstance();
        networkManager.addChangeListener(this);
        vendorFields = new ArrayList<>();
        quarterInputPanels = new ArrayList<>();
        
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        
        initializeComponents();
        refreshNetworkTable();
    }
    
    private void initializeComponents() {
        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);
        
        // Title
        JLabel titleLabel = new JLabel("Network & Vendor Management", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 102, 204));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Create main content with split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.4);
        
        // Left panel - Add new network
        JPanel leftPanel = createAddNetworkPanel();
        splitPane.setLeftComponent(leftPanel);
        
        // Right panel - View/Edit existing networks
        JPanel rightPanel = createViewEditPanel();
        splitPane.setRightComponent(rightPanel);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * Create the "Add Network" panel
     */
    private JPanel createAddNetworkPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(40, 167, 69), 2),
            "Add New Network", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 16), new Color(40, 167, 69)
        ));
        panel.setBackground(Color.WHITE);
        
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Network name input
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel networkLabel = new JLabel("Network Name:");
        networkLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(networkLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        networkNameField = new JTextField(20);
        networkNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        networkNameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        networkNameField.setToolTipText("Enter network name (e.g., BSNL, P2P, ILL)");
        formPanel.add(networkNameField, gbc);
        
        // Vendors section
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 0.5;
        JPanel vendorSection = createVendorInputSection();
        formPanel.add(vendorSection, gbc);
        
        // Quarter Configuration section
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 0.5;
        JPanel quarterSection = createQuarterConfigSection();
        formPanel.add(quarterSection, gbc);
        
        panel.add(formPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        
        addNetworkButton = new JButton("Add Network");
        addNetworkButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addNetworkButton.setBackground(new Color(40, 167, 69));
        addNetworkButton.setForeground(Color.WHITE);
        addNetworkButton.setPreferredSize(new Dimension(140, 35));
        addNetworkButton.setBorderPainted(false);
        addNetworkButton.setFocusPainted(false);
        addNetworkButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addNetworkButton.addActionListener(this::addNetworkAction);
        
        buttonPanel.add(addNetworkButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Create dynamic vendor input section
     */
    private JPanel createVendorInputSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            "Vendors", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12), Color.GRAY
        ));
        
        // Vendor input panel with scroll
        vendorInputPanel = new JPanel();
        vendorInputPanel.setLayout(new BoxLayout(vendorInputPanel, BoxLayout.Y_AXIS));
        vendorInputPanel.setBackground(Color.WHITE);
        
        // Add initial vendor field
        addVendorInputField();
        
        JScrollPane scrollPane = new JScrollPane(vendorInputPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(350, 200));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        section.add(scrollPane, BorderLayout.CENTER);
        
        // Add vendor field button
        JPanel addButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButtonPanel.setBackground(Color.WHITE);
        
        addVendorFieldButton = new JButton("+ Add Vendor Field");
        addVendorFieldButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addVendorFieldButton.setForeground(new Color(0, 102, 204));
        addVendorFieldButton.setBorderPainted(false);
        addVendorFieldButton.setContentAreaFilled(false);
        addVendorFieldButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addVendorFieldButton.addActionListener(e -> addVendorInputField());
        
        addButtonPanel.add(addVendorFieldButton);
        section.add(addButtonPanel, BorderLayout.SOUTH);
        
        return section;
    }
    
    /**
     * Add a new vendor input field
     */
    private void addVendorInputField() {
        JPanel fieldPanel = new JPanel(new BorderLayout(5, 0));
        fieldPanel.setBackground(Color.WHITE);
        fieldPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        JTextField vendorField = new JTextField();
        vendorField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        vendorField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)
        ));
        vendorField.setToolTipText("Enter vendor name");
        
        JButton removeButton = new JButton("×");
        removeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        removeButton.setForeground(Color.RED);
        removeButton.setPreferredSize(new Dimension(25, 25));
        removeButton.setBorderPainted(false);
        removeButton.setContentAreaFilled(false);
        removeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeButton.setToolTipText("Remove this vendor field");
        removeButton.addActionListener(e -> removeVendorInputField(fieldPanel, vendorField));
        
        fieldPanel.add(vendorField, BorderLayout.CENTER);
        fieldPanel.add(removeButton, BorderLayout.EAST);
        
        vendorFields.add(vendorField);
        vendorInputPanel.add(fieldPanel);
        vendorInputPanel.add(Box.createVerticalStrut(5));
        
        vendorInputPanel.revalidate();
        vendorInputPanel.repaint();
    }
    
    /**
     * Remove a vendor input field
     */
    private void removeVendorInputField(JPanel fieldPanel, JTextField vendorField) {
        if (vendorFields.size() > 1) { // Keep at least one field
            vendorFields.remove(vendorField);
            vendorInputPanel.remove(fieldPanel);
            
            // Remove the strut after this field if it exists
            Component[] components = vendorInputPanel.getComponents();
            for (int i = 0; i < components.length - 1; i++) {
                if (components[i] == fieldPanel && components[i + 1] instanceof Box.Filler) {
                    vendorInputPanel.remove(components[i + 1]);
                    break;
                }
            }
            
            vendorInputPanel.revalidate();
            vendorInputPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(this, 
                "At least one vendor field is required.", 
                "Cannot Remove", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * Create the view/edit panel
     */
    private JPanel createViewEditPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204), 2),
            "Manage Existing Networks", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 16), new Color(0, 102, 204)
        ));
        panel.setBackground(Color.WHITE);
        
        // Split into network list (top) and vendor list (bottom)
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        verticalSplit.setDividerLocation(200);
        verticalSplit.setResizeWeight(0.5);
        
        // Networks table
        JPanel networkPanel = createNetworkTablePanel();
        verticalSplit.setTopComponent(networkPanel);
        
        // Vendors table
        JPanel vendorPanel = createVendorTablePanel();
        verticalSplit.setBottomComponent(vendorPanel);
        
        panel.add(verticalSplit, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create network table panel
     */
    private JPanel createNetworkTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Networks"));
        
        // Table model
        String[] networkColumns = {"Network", "Vendor Count", "Quarters", "Actions"};
        networkTableModel = new DefaultTableModel(networkColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; // Only actions column is editable
            }
        };
        
        networkTable = new JTable(networkTableModel);
        networkTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        networkTable.setRowHeight(35);
        networkTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        networkTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        networkTable.getTableHeader().setBackground(new Color(230, 240, 250));
        
        // Column widths
        networkTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        networkTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        networkTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        networkTable.getColumnModel().getColumn(3).setPreferredWidth(160);
        
        // Selection listener
        networkTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = networkTable.getSelectedRow();
                if (selectedRow >= 0) {
                    currentSelectedNetwork = (String) networkTableModel.getValueAt(selectedRow, 0);
                    refreshVendorTable();
                }
            }
        });
        
        // Custom button renderer/editor for actions
        networkTable.getColumn("Actions").setCellRenderer(new NetworkButtonRenderer());
        networkTable.getColumn("Actions").setCellEditor(new NetworkButtonEditor());
        
        JScrollPane scrollPane = new JScrollPane(networkTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create vendor table panel
     */
    private JPanel createVendorTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        selectedNetworkLabel = new JLabel("Select a network to view vendors");
        selectedNetworkLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        selectedNetworkLabel.setForeground(new Color(0, 102, 204));
        selectedNetworkLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(selectedNetworkLabel, BorderLayout.NORTH);
        
        // Table model
        String[] vendorColumns = {"Vendor", "Actions"};
        vendorTableModel = new DefaultTableModel(vendorColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Only actions column is editable
            }
        };
        
        vendorTable = new JTable(vendorTableModel);
        vendorTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        vendorTable.setRowHeight(35);
        vendorTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        vendorTable.getTableHeader().setBackground(new Color(240, 248, 255));
        
        // Column widths
        vendorTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        vendorTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        
        // Custom button renderer/editor for actions
        vendorTable.getColumn("Actions").setCellRenderer(new VendorButtonRenderer());
        vendorTable.getColumn("Actions").setCellEditor(new VendorButtonEditor());
        
        JScrollPane scrollPane = new JScrollPane(vendorTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Add network action
     */
    private void addNetworkAction(ActionEvent e) {
        String networkName = networkNameField.getText().trim();
        if (networkName.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a network name.", 
                "Missing Network Name", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Collect vendors
        Set<String> vendors = new HashSet<>();
        for (JTextField field : vendorFields) {
            String vendor = field.getText().trim();
            if (!vendor.isEmpty()) {
                vendors.add(vendor);
            }
        }
        
        if (vendors.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter at least one vendor.", 
                "Missing Vendors", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Add to network manager
        if (networkManager.addNetwork(networkName, vendors)) {
            
            // Create and set quarter configuration
            NetworkVendorManager.QuarterConfiguration quarterConfig = 
                new NetworkVendorManager.QuarterConfiguration((Integer) numberOfQuartersSpinner.getValue());
            
            for (QuarterInputPanel quarterPanel : quarterInputPanels) {
                quarterConfig.addQuarterPeriod(
                    quarterPanel.getQuarterNumber(),
                    quarterPanel.getQuarterName(),
                    quarterPanel.getStartMonth(),
                    quarterPanel.getEndMonth()
                );
            }
            
            networkManager.setQuarterConfiguration(networkName, quarterConfig);
            
            JOptionPane.showMessageDialog(this, 
                "Network '" + networkName + "' added successfully with " + vendors.size() + " vendors and " + 
                quarterInputPanels.size() + " quarters!", 
                "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // Clear form
            clearAddNetworkForm();
            
            // Refresh table will be handled by the change listener
        } else {
            JOptionPane.showMessageDialog(this, 
                "Failed to add network. Network '" + networkName + "' may already exist.", 
                "Add Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Clear the add network form
     */
    private void clearAddNetworkForm() {
        networkNameField.setText("");
        
        // Clear all vendor fields except the first one
        for (int i = vendorFields.size() - 1; i > 0; i--) {
            JTextField field = vendorFields.get(i);
            Container parent = field.getParent();
            if (parent != null) {
                vendorFields.remove(field);
                vendorInputPanel.remove(parent);
            }
        }
        
        // Clear the remaining field
        if (!vendorFields.isEmpty()) {
            vendorFields.get(0).setText("");
        }
        
        // Reset quarter configuration to default
        numberOfQuartersSpinner.setValue(4);
        updateQuarterInputs();
        
        vendorInputPanel.revalidate();
        vendorInputPanel.repaint();
    }
    
    /**
     * Refresh network table
     */
    private void refreshNetworkTable() {
        networkTableModel.setRowCount(0);
        
        List<String> networks = networkManager.getAllNetworks();
        networks.sort(String::compareToIgnoreCase);
        
        for (String network : networks) {
            int vendorCount = networkManager.getVendorsForNetwork(network).size();
            int quarterCount = networkManager.getNumberOfQuarters(network);
            networkTableModel.addRow(new Object[]{network, vendorCount, quarterCount, "Edit | Quarters | Delete"});
        }
        
        // Clear vendor table if current selection no longer exists
        if (currentSelectedNetwork != null && !networks.contains(currentSelectedNetwork)) {
            currentSelectedNetwork = null;
            refreshVendorTable();
        }
    }
    
    /**
     * Refresh vendor table
     */
    private void refreshVendorTable() {
        vendorTableModel.setRowCount(0);
        
        if (currentSelectedNetwork != null) {
            selectedNetworkLabel.setText("Vendors for: " + currentSelectedNetwork);
            
            List<String> vendors = networkManager.getVendorsForNetwork(currentSelectedNetwork);
            vendors.sort(String::compareToIgnoreCase);
            
            for (String vendor : vendors) {
                vendorTableModel.addRow(new Object[]{vendor, "Edit | Delete"});
            }
        } else {
            selectedNetworkLabel.setText("Select a network to view vendors");
        }
    }
    
    // NetworkVendorChangeListener implementation
    @Override
    public void onNetworksChanged() {
        SwingUtilities.invokeLater(this::refreshNetworkTable);
    }
    
    @Override
    public void onVendorsChanged(String network) {
        SwingUtilities.invokeLater(() -> {
            if (currentSelectedNetwork != null && currentSelectedNetwork.equals(network)) {
                refreshVendorTable();
            }
            refreshNetworkTable(); // Update vendor counts
        });
    }
    
    /**
     * Create quarter configuration section
     */
    private JPanel createQuarterConfigSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            "Quarter Configuration", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12), Color.GRAY
        ));
        
        // Number of quarters selector
        JPanel quarterCountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        quarterCountPanel.setBackground(Color.WHITE);
        
        JLabel quarterCountLabel = new JLabel("Number of Quarters:");
        quarterCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        numberOfQuartersSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 12, 1));
        numberOfQuartersSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        numberOfQuartersSpinner.setPreferredSize(new Dimension(60, 25));
        numberOfQuartersSpinner.addChangeListener(e -> updateQuarterInputs());
        
        quarterCountPanel.add(quarterCountLabel);
        quarterCountPanel.add(Box.createHorizontalStrut(10));
        quarterCountPanel.add(numberOfQuartersSpinner);
        
        section.add(quarterCountPanel, BorderLayout.NORTH);
        
        // Quarter configuration panel
        quarterConfigPanel = new JPanel();
        quarterConfigPanel.setLayout(new BoxLayout(quarterConfigPanel, BoxLayout.Y_AXIS));
        quarterConfigPanel.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(quarterConfigPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(350, 180));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        section.add(scrollPane, BorderLayout.CENTER);
        
        // Initialize with default 4 quarters
        updateQuarterInputs();
        
        return section;
    }
    
    /**
     * Update quarter input fields based on number of quarters selected
     */
    private void updateQuarterInputs() {
        quarterConfigPanel.removeAll();
        quarterInputPanels.clear();
        
        int numberOfQuarters = (Integer) numberOfQuartersSpinner.getValue();
        
        for (int i = 1; i <= numberOfQuarters; i++) {
            QuarterInputPanel quarterPanel = new QuarterInputPanel(i);
            quarterInputPanels.add(quarterPanel);
            quarterConfigPanel.add(quarterPanel);
            if (i < numberOfQuarters) {
                quarterConfigPanel.add(Box.createVerticalStrut(8));
            }
        }
        
        quarterConfigPanel.revalidate();
        quarterConfigPanel.repaint();
    }
    
    /**
     * Inner class for quarter input panel
     */
    private class QuarterInputPanel extends JPanel {
        private int quarterNumber;
        private JTextField quarterNameField;
        private JComboBox<String> startMonthCombo;
        private JComboBox<String> endMonthCombo;
        
        public QuarterInputPanel(int quarterNumber) {
            this.quarterNumber = quarterNumber;
            initializeQuarterPanel();
        }
        
        private void initializeQuarterPanel() {
            setLayout(new GridBagLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 5, 2, 5);
            gbc.anchor = GridBagConstraints.WEST;
            
            // Quarter label
            gbc.gridx = 0; gbc.gridy = 0;
            JLabel quarterLabel = new JLabel("Q" + quarterNumber + " Name:");
            quarterLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            add(quarterLabel, gbc);
            
            // Quarter name field
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            quarterNameField = new JTextField("Q" + quarterNumber);
            quarterNameField.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            quarterNameField.setPreferredSize(new Dimension(100, 25));
            quarterNameField.setEditable(false); // Make read-only since it auto-updates
            quarterNameField.setBackground(new Color(245, 245, 245));
            quarterNameField.setToolTipText("Quarter name updates automatically based on selected months");
            add(quarterNameField, gbc);
            
            // Start month
            gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
            JLabel startLabel = new JLabel("From:");
            startLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            add(startLabel, gbc);
            
            gbc.gridx = 1;
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                             "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            startMonthCombo = new JComboBox<>(months);
            startMonthCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            startMonthCombo.setPreferredSize(new Dimension(70, 25));
            startMonthCombo.addActionListener(e -> updateQuarterName());
            add(startMonthCombo, gbc);
            
            // End month
            gbc.gridx = 2;
            JLabel endLabel = new JLabel("To:");
            endLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            add(endLabel, gbc);
            
            gbc.gridx = 3;
            endMonthCombo = new JComboBox<>(months);
            endMonthCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            endMonthCombo.setPreferredSize(new Dimension(70, 25));
            endMonthCombo.addActionListener(e -> updateQuarterName());
            add(endMonthCombo, gbc);
            
            // Set default values based on quarter number
            setDefaultValues();
        }
        
        private void setDefaultValues() {
            if (quarterNumber <= 4) {
                // Standard quarter defaults
                switch (quarterNumber) {
                    case 1:
                        startMonthCombo.setSelectedIndex(3); // April
                        endMonthCombo.setSelectedIndex(5);   // June
                        break;
                    case 2:
                        startMonthCombo.setSelectedIndex(6); // July
                        endMonthCombo.setSelectedIndex(8);   // September
                        break;
                    case 3:
                        startMonthCombo.setSelectedIndex(9); // October
                        endMonthCombo.setSelectedIndex(11);  // December
                        break;
                    case 4:
                        startMonthCombo.setSelectedIndex(0); // January
                        endMonthCombo.setSelectedIndex(2);   // March
                        break;
                }
            } else {
                // For more than 4 quarters, just set sequential months
                int startMonth = ((quarterNumber - 1) * 3) % 12;
                int endMonth = (startMonth + 2) % 12;
                startMonthCombo.setSelectedIndex(startMonth);
                endMonthCombo.setSelectedIndex(endMonth);
            }
            // Update the quarter name after setting default values
            updateQuarterName();
        }
        
        /**
         * Automatically update quarter name based on selected months
         */
        private void updateQuarterName() {
            String startMonth = (String) startMonthCombo.getSelectedItem();
            String endMonth = (String) endMonthCombo.getSelectedItem();
            
            if (startMonth != null && endMonth != null) {
                String quarterName = "Q" + quarterNumber + " (" + startMonth + "-" + endMonth + ")";
                quarterNameField.setText(quarterName);
            }
        }
        
        public String getQuarterName() {
            return quarterNameField.getText().trim();
        }
        
        public int getStartMonth() {
            return startMonthCombo.getSelectedIndex() + 1;
        }
        
        public int getEndMonth() {
            return endMonthCombo.getSelectedIndex() + 1;
        }
        
        public int getQuarterNumber() {
            return quarterNumber;
        }
    }
    
    /**
     * Network table button renderer
     */
    private class NetworkButtonRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 1));
            
            JButton editButton = new JButton("Edit");
            editButton.setFont(new Font("Segoe UI", Font.BOLD, 10));
            editButton.setPreferredSize(new Dimension(60, 28));
            editButton.setBackground(new Color(0, 123, 255));
            editButton.setForeground(Color.WHITE);
            editButton.setBorderPainted(false);
            editButton.setFocusPainted(false);
            editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            JButton quartersButton = new JButton("Quarters");
            quartersButton.setFont(new Font("Segoe UI", Font.BOLD, 10));
            quartersButton.setPreferredSize(new Dimension(80, 28));
            quartersButton.setBackground(new Color(255, 193, 7));
            quartersButton.setForeground(Color.BLACK);
            quartersButton.setBorderPainted(false);
            quartersButton.setFocusPainted(false);
            quartersButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            JButton deleteButton = new JButton("Delete");
            deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 10));
            deleteButton.setPreferredSize(new Dimension(65, 28));
            deleteButton.setBackground(new Color(220, 53, 69));
            deleteButton.setForeground(Color.WHITE);
            deleteButton.setBorderPainted(false);
            deleteButton.setFocusPainted(false);
            deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            panel.add(editButton);
            panel.add(quartersButton);
            panel.add(deleteButton);
            
            if (isSelected) {
                panel.setBackground(table.getSelectionBackground());
            } else {
                panel.setBackground(table.getBackground());
            }
            
            return panel;
        }
    }
    
    /**
     * Network table button editor
     */
    private class NetworkButtonEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private JPanel panel;
        private String networkName;
        
        public NetworkButtonEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 1));
            
            JButton editButton = new JButton("Edit");
            editButton.setFont(new Font("Segoe UI", Font.BOLD, 10));
            editButton.setPreferredSize(new Dimension(60, 28));
            editButton.setBackground(new Color(0, 123, 255));
            editButton.setForeground(Color.WHITE);
            editButton.setBorderPainted(false);
            editButton.setFocusPainted(false);
            editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            editButton.addActionListener(e -> editNetwork());
            
            JButton quartersButton = new JButton("Quarters");
            quartersButton.setFont(new Font("Segoe UI", Font.BOLD, 10));
            quartersButton.setPreferredSize(new Dimension(80, 28));
            quartersButton.setBackground(new Color(255, 193, 7));
            quartersButton.setForeground(Color.BLACK);
            quartersButton.setBorderPainted(false);
            quartersButton.setFocusPainted(false);
            quartersButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            quartersButton.addActionListener(e -> editQuarters());
            
            JButton deleteButton = new JButton("Delete");
            deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 10));
            deleteButton.setPreferredSize(new Dimension(65, 28));
            deleteButton.setBackground(new Color(220, 53, 69));
            deleteButton.setForeground(Color.WHITE);
            deleteButton.setBorderPainted(false);
            deleteButton.setFocusPainted(false);
            deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            deleteButton.addActionListener(e -> deleteNetwork());
            
            panel.add(editButton);
            panel.add(quartersButton);
            panel.add(deleteButton);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            networkName = (String) table.getValueAt(row, 0);
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "Edit | Quarters | Delete";
        }
        
        private void editNetwork() {
            fireEditingStopped();
            
            String newName = JOptionPane.showInputDialog(UpdateNetworkTab.this, 
                "Enter new name for network:", "Edit Network", 
                JOptionPane.QUESTION_MESSAGE);
            
            if (newName != null && !newName.trim().isEmpty() && !newName.equals(networkName)) {
                if (networkManager.updateNetworkName(networkName, newName.trim())) {
                    JOptionPane.showMessageDialog(UpdateNetworkTab.this, 
                        "Network renamed successfully!", "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(UpdateNetworkTab.this, 
                        "Failed to rename network. Name may already exist.", "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        
        private void editQuarters() {
            fireEditingStopped();
            
            // Create and show quarter configuration dialog
            UpdateNetworkTab.this.showQuarterConfigDialog(networkName);
        }
        
        private void deleteNetwork() {
            fireEditingStopped();
            
            int vendorCount = networkManager.getVendorsForNetwork(networkName).size();
            String message = String.format(
                "Are you sure you want to delete network '%s'?\n" +
                "This will also delete %d associated vendors.\n" +
                "This action cannot be undone.", 
                networkName, vendorCount);
            
            int confirm = JOptionPane.showConfirmDialog(UpdateNetworkTab.this, 
                message, "Confirm Delete", 
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                if (networkManager.deleteNetwork(networkName)) {
                    JOptionPane.showMessageDialog(UpdateNetworkTab.this, 
                        "Network deleted successfully!", "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(UpdateNetworkTab.this, 
                        "Failed to delete network.", "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    /**
     * Vendor table button renderer
     */
    private class VendorButtonRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,  
                boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 1));
            
            JButton editButton = new JButton("Edit");
            editButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            editButton.setPreferredSize(new Dimension(65, 28));
            editButton.setBackground(new Color(0, 123, 255));
            editButton.setForeground(Color.WHITE);
            editButton.setBorderPainted(false);
            editButton.setFocusPainted(false);
            editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            JButton deleteButton = new JButton("Delete");
            deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            deleteButton.setPreferredSize(new Dimension(70, 28));
            deleteButton.setBackground(new Color(220, 53, 69));
            deleteButton.setForeground(Color.WHITE);
            deleteButton.setBorderPainted(false);
            deleteButton.setFocusPainted(false);
            deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            panel.add(editButton);
            panel.add(deleteButton);
            
            if (isSelected) {
                panel.setBackground(table.getSelectionBackground());
            } else {
                panel.setBackground(table.getBackground());
            }
            
            return panel;
        }
    }
    
    /**
     * Vendor table button editor
     */
    private class VendorButtonEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private JPanel panel;
        private String vendorName;
        
        public VendorButtonEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 1));
            
            JButton editButton = new JButton("Edit");
            editButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            editButton.setPreferredSize(new Dimension(65, 28));
            editButton.setBackground(new Color(0, 123, 255));
            editButton.setForeground(Color.WHITE);
            editButton.setBorderPainted(false);
            editButton.setFocusPainted(false);
            editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            editButton.addActionListener(e -> editVendor());
            
            JButton deleteButton = new JButton("Delete");
            deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            deleteButton.setPreferredSize(new Dimension(70, 28));
            deleteButton.setBackground(new Color(220, 53, 69));
            deleteButton.setForeground(Color.WHITE);
            deleteButton.setBorderPainted(false);
            deleteButton.setFocusPainted(false);
            deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            deleteButton.addActionListener(e -> deleteVendor());
            
            panel.add(editButton);
            panel.add(deleteButton);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            vendorName = (String) table.getValueAt(row, 0);
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "Edit | Delete";
        }
        
        private void editVendor() {
            fireEditingStopped();
            
            String newName = JOptionPane.showInputDialog(UpdateNetworkTab.this, 
                "Enter new name for vendor:", "Edit Vendor", 
                JOptionPane.QUESTION_MESSAGE);
            
            if (newName != null && !newName.trim().isEmpty() && !newName.equals(vendorName)) {
                if (networkManager.updateVendorName(currentSelectedNetwork, vendorName, newName.trim())) {
                    JOptionPane.showMessageDialog(UpdateNetworkTab.this, 
                        "Vendor renamed successfully!", "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(UpdateNetworkTab.this, 
                        "Failed to rename vendor.", "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        
        private void deleteVendor() {
            fireEditingStopped();
            
            String message = String.format(
                "Are you sure you want to delete vendor '%s' from network '%s'?\n" +
                "This action cannot be undone.", 
                vendorName, currentSelectedNetwork);
            
            int confirm = JOptionPane.showConfirmDialog(UpdateNetworkTab.this, 
                message, "Confirm Delete", 
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                if (networkManager.deleteVendorFromNetwork(currentSelectedNetwork, vendorName)) {
                    JOptionPane.showMessageDialog(UpdateNetworkTab.this, 
                        "Vendor deleted successfully!", "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(UpdateNetworkTab.this, 
                        "Failed to delete vendor.", "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    /**
     * Show quarter configuration dialog for a network
     */
    private void showQuarterConfigDialog(String networkName) {
        // Get current quarter configuration
        NetworkVendorManager.QuarterConfiguration currentConfig = 
            networkManager.getQuarterConfiguration(networkName);
        
        // Create dialog
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Quarter Configuration for " + networkName, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);
        
        // Title
        JLabel titleLabel = new JLabel("Configure Quarters for " + networkName, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(0, 102, 204));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Quarter configuration form
        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setBackground(Color.WHITE);
        
        // Number of quarters
        JPanel quarterCountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        quarterCountPanel.setBackground(Color.WHITE);
        
        JLabel quarterCountLabel = new JLabel("Number of Quarters:");
        quarterCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JSpinner quarterSpinner = new JSpinner(new SpinnerNumberModel(
            currentConfig != null ? currentConfig.getNumberOfQuarters() : 4, 1, 12, 1));
        quarterSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        quarterSpinner.setPreferredSize(new Dimension(80, 30));
        
        quarterCountPanel.add(quarterCountLabel);
        quarterCountPanel.add(Box.createHorizontalStrut(10));
        quarterCountPanel.add(quarterSpinner);
        
        formPanel.add(quarterCountPanel, BorderLayout.NORTH);
        
        // Quarter details panel
        JPanel quarterDetailsPanel = new JPanel();
        quarterDetailsPanel.setLayout(new BoxLayout(quarterDetailsPanel, BoxLayout.Y_AXIS));
        quarterDetailsPanel.setBackground(Color.WHITE);
        
        java.util.List<QuarterEditPanel> quarterEditPanels = new ArrayList<>();
        
        // Function to update quarter panels
        Runnable updateQuarterPanels = () -> {
            quarterDetailsPanel.removeAll();
            quarterEditPanels.clear();
            
            int numberOfQuarters = (Integer) quarterSpinner.getValue();
            
            for (int i = 1; i <= numberOfQuarters; i++) {
                QuarterEditPanel panel = new QuarterEditPanel(i, currentConfig);
                quarterEditPanels.add(panel);
                quarterDetailsPanel.add(panel);
                if (i < numberOfQuarters) {
                    quarterDetailsPanel.add(Box.createVerticalStrut(10));
                }
            }
            
            quarterDetailsPanel.revalidate();
            quarterDetailsPanel.repaint();
        };
        
        quarterSpinner.addChangeListener(e -> updateQuarterPanels.run());
        
        JScrollPane scrollPane = new JScrollPane(quarterDetailsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(560, 300));
        
        formPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton saveButton = new JButton("Save Configuration");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        saveButton.setBackground(new Color(40, 167, 69));
        saveButton.setForeground(Color.WHITE);
        saveButton.setPreferredSize(new Dimension(160, 35));
        saveButton.setBorderPainted(false);
        saveButton.addActionListener(e -> {
            // Save configuration
            NetworkVendorManager.QuarterConfiguration newConfig = 
                new NetworkVendorManager.QuarterConfiguration((Integer) quarterSpinner.getValue());
            
            for (QuarterEditPanel panel : quarterEditPanels) {
                newConfig.addQuarterPeriod(
                    panel.getQuarterNumber(),
                    panel.getQuarterName(),
                    panel.getStartMonth(),
                    panel.getEndMonth()
                );
            }
            
            networkManager.setQuarterConfiguration(networkName, newConfig);
            
            JOptionPane.showMessageDialog(dialog, 
                "Quarter configuration saved successfully!", 
                "Success", JOptionPane.INFORMATION_MESSAGE);
            
            dialog.dispose();
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelButton.setBackground(new Color(108, 117, 125));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.setBorderPainted(false);
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        
        // Initialize panels
        updateQuarterPanels.run();
        
        dialog.setVisible(true);
    }
    
    /**
     * Panel for editing individual quarter configuration
     */
    private class QuarterEditPanel extends JPanel {
        private int quarterNumber;
        private JTextField quarterNameField;
        private JComboBox<String> startMonthCombo;
        private JComboBox<String> endMonthCombo;
        
        public QuarterEditPanel(int quarterNumber, NetworkVendorManager.QuarterConfiguration config) {
            this.quarterNumber = quarterNumber;
            initializePanel(config);
        }
        
        private void initializePanel(NetworkVendorManager.QuarterConfiguration config) {
            setLayout(new GridBagLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(3, 8, 3, 8);
            gbc.anchor = GridBagConstraints.WEST;
            
            // Quarter label
            gbc.gridx = 0; gbc.gridy = 0;
            JLabel quarterLabel = new JLabel("Quarter " + quarterNumber + " Name:");
            quarterLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            add(quarterLabel, gbc);
            
            // Quarter name field
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            quarterNameField = new JTextField(15);
            quarterNameField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            quarterNameField.setPreferredSize(new Dimension(150, 25));
            quarterNameField.setEditable(false); // Make read-only since it auto-updates
            quarterNameField.setBackground(new Color(245, 245, 245));
            quarterNameField.setToolTipText("Quarter name updates automatically based on selected months");
            add(quarterNameField, gbc);
            
            // Start month
            gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
            JLabel startLabel = new JLabel("From Month:");
            startLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            add(startLabel, gbc);
            
            gbc.gridx = 1;
            String[] months = {"January", "February", "March", "April", "May", "June",
                             "July", "August", "September", "October", "November", "December"};
            startMonthCombo = new JComboBox<>(months);
            startMonthCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            startMonthCombo.setPreferredSize(new Dimension(120, 25));
            startMonthCombo.addActionListener(e -> updateQuarterName());
            add(startMonthCombo, gbc);
            
            // End month
            gbc.gridx = 2;
            JLabel endLabel = new JLabel("To Month:");
            endLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            add(endLabel, gbc);
            
            gbc.gridx = 3;
            endMonthCombo = new JComboBox<>(months);
            endMonthCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            endMonthCombo.setPreferredSize(new Dimension(120, 25));
            endMonthCombo.addActionListener(e -> updateQuarterName());
            add(endMonthCombo, gbc);
            
            // Load existing values or set defaults
            loadValues(config);
        }
        
        private void loadValues(NetworkVendorManager.QuarterConfiguration config) {
            if (config != null) {
                NetworkVendorManager.QuarterPeriod period = config.getQuarterPeriod(quarterNumber);
                if (period != null) {
                    startMonthCombo.setSelectedIndex(period.getStartMonth() - 1);
                    endMonthCombo.setSelectedIndex(period.getEndMonth() - 1);
                    // Update quarter name after setting months
                    updateQuarterName();
                    return;
                }
            }
            
            // Set default values
            setDefaultValues();
        }
        
        private void setDefaultValues() {
            switch (quarterNumber) {
                case 1:
                    startMonthCombo.setSelectedIndex(3); // April
                    endMonthCombo.setSelectedIndex(5);   // June
                    break;
                case 2:
                    startMonthCombo.setSelectedIndex(6); // July
                    endMonthCombo.setSelectedIndex(8);   // September
                    break;
                case 3:
                    startMonthCombo.setSelectedIndex(9); // October
                    endMonthCombo.setSelectedIndex(11);  // December
                    break;
                case 4:
                    startMonthCombo.setSelectedIndex(0); // January
                    endMonthCombo.setSelectedIndex(2);   // March
                    break;
                default:
                    // For additional quarters, distribute evenly
                    int startMonth = ((quarterNumber - 1) * 3) % 12;
                    int endMonth = (startMonth + 2) % 12;
                    startMonthCombo.setSelectedIndex(startMonth);
                    endMonthCombo.setSelectedIndex(endMonth);
                    break;
            }
            // Update quarter name after setting default values
            updateQuarterName();
        }
        
        /**
         * Automatically update quarter name based on selected months
         */
        private void updateQuarterName() {
            String startMonth = (String) startMonthCombo.getSelectedItem();
            String endMonth = (String) endMonthCombo.getSelectedItem();
            
            if (startMonth != null && endMonth != null) {
                // Use abbreviated month names for consistent display
                String startAbbrev = getMonthAbbreviation(startMonth);
                String endAbbrev = getMonthAbbreviation(endMonth);
                String quarterName = "Q" + quarterNumber + " (" + startAbbrev + "-" + endAbbrev + ")";
                quarterNameField.setText(quarterName);
            }
        }
        
        /**
         * Get abbreviated month name
         */
        private String getMonthAbbreviation(String fullMonth) {
            switch (fullMonth) {
                case "January": return "Jan";
                case "February": return "Feb";
                case "March": return "Mar";
                case "April": return "Apr";
                case "May": return "May";
                case "June": return "Jun";
                case "July": return "Jul";
                case "August": return "Aug";
                case "September": return "Sep";
                case "October": return "Oct";
                case "November": return "Nov";
                case "December": return "Dec";
                default: return fullMonth;
            }
        }
        
        public String getQuarterName() {
            return quarterNameField.getText().trim();
        }
        
        public int getStartMonth() {
            return startMonthCombo.getSelectedIndex() + 1;
        }
        
        public int getEndMonth() {
            return endMonthCombo.getSelectedIndex() + 1;
        }
        
        public int getQuarterNumber() {
            return quarterNumber;
        }
    }
}
