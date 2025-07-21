package com.login.service;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JOptionPane;

/**
 * Network and Vendor Management Service with live synchronization
 */
public class NetworkVendorManager {
    
    /**
     * Quarter Configuration class to store custom quarter settings for each network
     */
    public static class QuarterConfiguration implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private int numberOfQuarters;
        private Map<Integer, QuarterPeriod> quarterPeriods;
        
        public QuarterConfiguration() {
            quarterPeriods = new HashMap<>();
        }
        
        public QuarterConfiguration(int numberOfQuarters) {
            this.numberOfQuarters = numberOfQuarters;
            this.quarterPeriods = new HashMap<>();
        }
        
        public int getNumberOfQuarters() { return numberOfQuarters; }
        public void setNumberOfQuarters(int numberOfQuarters) { this.numberOfQuarters = numberOfQuarters; }
        
        public Map<Integer, QuarterPeriod> getQuarterPeriods() { return quarterPeriods; }
        public void setQuarterPeriods(Map<Integer, QuarterPeriod> quarterPeriods) { this.quarterPeriods = quarterPeriods; }
        
        public void addQuarterPeriod(int quarterNumber, String quarterName, int startMonth, int endMonth) {
            quarterPeriods.put(quarterNumber, new QuarterPeriod(quarterName, startMonth, endMonth));
        }
        
        public QuarterPeriod getQuarterPeriod(int quarterNumber) {
            return quarterPeriods.get(quarterNumber);
        }
    }
    
    /**
     * Quarter Period class to store individual quarter details
     */
    public static class QuarterPeriod implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String quarterName;
        private int startMonth;
        private int endMonth;
        
        public QuarterPeriod() {}
        
        public QuarterPeriod(String quarterName, int startMonth, int endMonth) {
            this.quarterName = quarterName;
            this.startMonth = startMonth;
            this.endMonth = endMonth;
        }
        
        public String getQuarterName() { return quarterName; }
        public void setQuarterName(String quarterName) { this.quarterName = quarterName; }
        
        public int getStartMonth() { return startMonth; }
        public void setStartMonth(int startMonth) { this.startMonth = startMonth; }
        
        public int getEndMonth() { return endMonth; }
        public void setEndMonth(int endMonth) { this.endMonth = endMonth; }
        
        public String getMonthRange() {
            String[] monthNames = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                 "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            return monthNames[startMonth] + "-" + monthNames[endMonth];
        }
    }
    
    /**
     * Network Data Container to store both vendors and quarter configurations
     */
    public static class NetworkDataContainer implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Map<String, Set<String>> networkVendorMap;
        private Map<String, QuarterConfiguration> networkQuarterMap;
        
        public NetworkDataContainer() {
            networkVendorMap = new HashMap<>();
            networkQuarterMap = new HashMap<>();
        }
        
        public Map<String, Set<String>> getNetworkVendorMap() { return networkVendorMap; }
        public void setNetworkVendorMap(Map<String, Set<String>> networkVendorMap) { this.networkVendorMap = networkVendorMap; }
        
        public Map<String, QuarterConfiguration> getNetworkQuarterMap() { return networkQuarterMap; }
        public void setNetworkQuarterMap(Map<String, QuarterConfiguration> networkQuarterMap) { this.networkQuarterMap = networkQuarterMap; }
    }
    
    private static final String DATA_DIR = "data";
    private static final String NETWORKS_FILE = DATA_DIR + File.separator + "networks.dat";
    
    // Thread-safe collections for live updates
    private Map<String, Set<String>> networkVendorMap;
    private Map<String, QuarterConfiguration> networkQuarterMap;
    private List<NetworkVendorChangeListener> listeners;
    
    // Singleton instance for application-wide access
    private static NetworkVendorManager instance;
    
    private NetworkVendorManager() {
        networkVendorMap = new HashMap<>();
        networkQuarterMap = new HashMap<>();
        listeners = new CopyOnWriteArrayList<>();
        loadNetworkVendorData();
    }
    
    public static synchronized NetworkVendorManager getInstance() {
        if (instance == null) {
            instance = new NetworkVendorManager();
        }
        return instance;
    }
    
    /**
     * Interface for components that need to be notified of network/vendor changes
     */
    public interface NetworkVendorChangeListener {
        void onNetworksChanged();
        void onVendorsChanged(String network);
    }
    
    /**
     * Register a component to receive network/vendor change notifications
     */
    public void addChangeListener(NetworkVendorChangeListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a change listener
     */
    public void removeChangeListener(NetworkVendorChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify all listeners of network changes
     */
    private void notifyNetworkChanges() {
        for (NetworkVendorChangeListener listener : listeners) {
            try {
                listener.onNetworksChanged();
            } catch (Exception e) {
                System.err.println("Error notifying listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notify all listeners of vendor changes for a specific network
     */
    private void notifyVendorChanges(String network) {
        for (NetworkVendorChangeListener listener : listeners) {
            try {
                listener.onVendorsChanged(network);
            } catch (Exception e) {
                System.err.println("Error notifying listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Load network-vendor mappings from file
     */
    private void loadNetworkVendorData() {
        File file = new File(NETWORKS_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                // Try to load new format first
                Object loaded = ois.readObject();
                if (loaded instanceof NetworkDataContainer) {
                    // New format with quarter configurations
                    NetworkDataContainer container = (NetworkDataContainer) loaded;
                    networkVendorMap = container.getNetworkVendorMap() != null ? 
                                     container.getNetworkVendorMap() : new HashMap<>();
                    networkQuarterMap = container.getNetworkQuarterMap() != null ? 
                                      container.getNetworkQuarterMap() : new HashMap<>();
                    System.out.println("Loaded " + networkVendorMap.size() + " networks with quarter configurations");
                } else {
                    // Old format - just vendor data
                    @SuppressWarnings("unchecked")
                    Map<String, Set<String>> oldData = (Map<String, Set<String>>) loaded;
                    networkVendorMap = oldData != null ? oldData : new HashMap<>();
                    networkQuarterMap = new HashMap<>();
                    System.out.println("Loaded " + networkVendorMap.size() + " networks (old format, creating default quarters)");
                    createDefaultQuarterConfigurations();
                }
            } catch (Exception e) {
                e.printStackTrace();
                networkVendorMap = new HashMap<>();
                networkQuarterMap = new HashMap<>();
                createDefaultNetworkVendorData();
            }
        } else {
            networkVendorMap = new HashMap<>();
            networkQuarterMap = new HashMap<>();
            createDefaultNetworkVendorData();
        }
    }
    
    /**
     * Save network-vendor mappings to file
     */
    private void saveNetworkVendorData() {
        try {
            File dataDir = new File(DATA_DIR);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            // Create container with both vendor and quarter data
            NetworkDataContainer container = new NetworkDataContainer();
            container.setNetworkVendorMap(networkVendorMap);
            container.setNetworkQuarterMap(networkQuarterMap);
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(NETWORKS_FILE))) {
                oos.writeObject(container);
                System.out.println("Saved network-vendor-quarter data to: " + NETWORKS_FILE);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Error saving network-vendor data: " + e.getMessage(), 
                "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Create default network-vendor mappings
     */
    private void createDefaultNetworkVendorData() {
        // BSNL vendors
        Set<String> bsnlVendors = new HashSet<>();
        bsnlVendors.add("RAITEL INFRA PRIVATE LIMITED");
        bsnlVendors.add("ALSTONIA ENERGY SOLUTIONS PVT LTD");
        bsnlVendors.add("VODAFONE INDIA LTD");
        networkVendorMap.put("BSNL", bsnlVendors);
        
        // P2P vendors
        Set<String> p2pVendors = new HashSet<>();
        p2pVendors.add("POWER GRID CORPORATION OF INDIA LTD");
        p2pVendors.add("VELOCIS SYSTEMS PRIVATE LIMITED");
        p2pVendors.add("TATA COMMUNICATIONS LTD");
        networkVendorMap.put("P2P", p2pVendors);
        
        // ILL vendors
        Set<String> illVendors = new HashSet<>();
        illVendors.add("BHARTI AIRTEL LIMITED");
        illVendors.add("RELIANCE JIO INFOCOMM LIMITED");
        illVendors.add("VODAFONE IDEA LIMITED");
        networkVendorMap.put("ILL", illVendors);
        
        // AWS vendors
        Set<String> awsVendors = new HashSet<>();
        awsVendors.add("AMAZON WEB SERVICES");
        awsVendors.add("MICROSOFT AZURE");
        awsVendors.add("GOOGLE CLOUD PLATFORM");
        networkVendorMap.put("AWS", awsVendors);
        
        // Switches vendors
        Set<String> switchesVendors = new HashSet<>();
        switchesVendors.add("CISCO SYSTEMS");
        switchesVendors.add("JUNIPER NETWORKS");
        switchesVendors.add("HUAWEI TECHNOLOGIES");
        networkVendorMap.put("Switches", switchesVendors);
        
        // F5 vendors
        Set<String> f5Vendors = new HashSet<>();
        f5Vendors.add("F5 NETWORKS INC");
        f5Vendors.add("CITRIX SYSTEMS");
        f5Vendors.add("A10 NETWORKS");
        networkVendorMap.put("F5", f5Vendors);
        
        // Create default quarter configurations
        createDefaultQuarterConfigurations();
        
        saveNetworkVendorData();
    }
    
    /**
     * Create default quarter configurations for all networks
     */
    private void createDefaultQuarterConfigurations() {
        // BSNL - 2 quarters with specific periods
        QuarterConfiguration bsnlQuarters = new QuarterConfiguration(2);
        bsnlQuarters.addQuarterPeriod(1, "Q1 (October-March)", 10, 3);
        bsnlQuarters.addQuarterPeriod(2, "Q2 (April-September)", 4, 9);
        networkQuarterMap.put("BSNL", bsnlQuarters);
        
        // Standard networks - 4 quarters
        String[] standardNetworks = {"P2P", "ILL", "AWS", "Switches", "F5"};
        for (String network : standardNetworks) {
            QuarterConfiguration standardQuarters = new QuarterConfiguration(4);
            standardQuarters.addQuarterPeriod(1, "Q1 (April-June)", 4, 6);
            standardQuarters.addQuarterPeriod(2, "Q2 (July-September)", 7, 9);
            standardQuarters.addQuarterPeriod(3, "Q3 (October-December)", 10, 12);
            standardQuarters.addQuarterPeriod(4, "Q4 (January-March)", 1, 3);
            networkQuarterMap.put(network, standardQuarters);
        }
        
        System.out.println("Created default quarter configurations for all networks");
    }
    
    // CRUD Operations
    
    /**
     * Add a new network with vendors
     */
    public boolean addNetwork(String networkName, Set<String> vendors) {
        if (networkName == null || networkName.trim().isEmpty()) {
            return false;
        }
        
        networkName = networkName.trim();
        if (networkVendorMap.containsKey(networkName)) {
            return false; // Network already exists
        }
        
        networkVendorMap.put(networkName, new HashSet<>(vendors));
        saveNetworkVendorData();
        notifyNetworkChanges();
        return true;
    }
    
    /**
     * Update network name
     */
    public boolean updateNetworkName(String oldName, String newName) {
        if (oldName == null || newName == null || oldName.equals(newName)) {
            return false;
        }
        
        newName = newName.trim();
        if (networkVendorMap.containsKey(newName)) {
            return false; // New name already exists
        }
        
        Set<String> vendors = networkVendorMap.remove(oldName);
        if (vendors != null) {
            networkVendorMap.put(newName, vendors);
            saveNetworkVendorData();
            notifyNetworkChanges();
            return true;
        }
        return false;
    }
    
    /**
     * Delete a network and all its vendors
     */
    public boolean deleteNetwork(String networkName) {
        if (networkVendorMap.remove(networkName) != null) {
            saveNetworkVendorData();
            notifyNetworkChanges();
            return true;
        }
        return false;
    }
    
    /**
     * Add vendor to existing network
     */
    public boolean addVendorToNetwork(String networkName, String vendorName) {
        if (networkName == null || vendorName == null) {
            return false;
        }
        
        Set<String> vendors = networkVendorMap.get(networkName);
        if (vendors != null) {
            if (vendors.add(vendorName.trim())) {
                saveNetworkVendorData();
                notifyVendorChanges(networkName);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Update vendor name within a network
     */
    public boolean updateVendorName(String networkName, String oldVendorName, String newVendorName) {
        Set<String> vendors = networkVendorMap.get(networkName);
        if (vendors != null && vendors.remove(oldVendorName)) {
            vendors.add(newVendorName.trim());
            saveNetworkVendorData();
            notifyVendorChanges(networkName);
            return true;
        }
        return false;
    }
    
    /**
     * Delete vendor from network
     */
    public boolean deleteVendorFromNetwork(String networkName, String vendorName) {
        Set<String> vendors = networkVendorMap.get(networkName);
        if (vendors != null && vendors.remove(vendorName)) {
            saveNetworkVendorData();
            notifyVendorChanges(networkName);
            return true;
        }
        return false;
    }
    
    // Query Operations
    
    /**
     * Get all network names
     */
    public List<String> getAllNetworks() {
        return new ArrayList<>(networkVendorMap.keySet());
    }
    
    /**
     * Get all vendors for a specific network
     */
    public List<String> getVendorsForNetwork(String networkName) {
        Set<String> vendors = networkVendorMap.get(networkName);
        return vendors != null ? new ArrayList<>(vendors) : new ArrayList<>();
    }
    
    /**
     * Get all vendors across all networks
     */
    public List<String> getAllVendors() {
        Set<String> allVendors = new HashSet<>();
        for (Set<String> vendors : networkVendorMap.values()) {
            allVendors.addAll(vendors);
        }
        return new ArrayList<>(allVendors);
    }
    
    /**
     * Get complete network-vendor mapping
     */
    public Map<String, List<String>> getNetworkVendorMapping() {
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : networkVendorMap.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
    
    /**
     * Check if network exists
     */
    public boolean networkExists(String networkName) {
        return networkVendorMap.containsKey(networkName);
    }
    
    /**
     * Check if vendor exists in a specific network
     */
    public boolean vendorExistsInNetwork(String networkName, String vendorName) {
        Set<String> vendors = networkVendorMap.get(networkName);
        return vendors != null && vendors.contains(vendorName);
    }
    
    /**
     * Get network for a given vendor (first match)
     */
    public String getNetworkForVendor(String vendorName) {
        for (Map.Entry<String, Set<String>> entry : networkVendorMap.entrySet()) {
            if (entry.getValue().contains(vendorName)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Validate network-vendor combination
     */
    public boolean isValidNetworkVendorCombination(String networkName, String vendorName) {
        return vendorExistsInNetwork(networkName, vendorName);
    }
    
    // Quarter Configuration Methods
    
    /**
     * Get quarter configuration for a network
     */
    public QuarterConfiguration getQuarterConfiguration(String networkName) {
        return networkQuarterMap.get(networkName);
    }
    
    /**
     * Set quarter configuration for a network
     */
    public void setQuarterConfiguration(String networkName, QuarterConfiguration quarterConfig) {
        networkQuarterMap.put(networkName, quarterConfig);
        saveNetworkVendorData();
        notifyVendorChanges(networkName); // Notify of changes
    }
    
    /**
     * Get number of quarters for a network
     */
    public int getNumberOfQuarters(String networkName) {
        QuarterConfiguration config = networkQuarterMap.get(networkName);
        return config != null ? config.getNumberOfQuarters() : 4; // Default to 4 quarters
    }
    
    /**
     * Get quarter periods for a network
     */
    public Map<Integer, QuarterPeriod> getQuarterPeriods(String networkName) {
        QuarterConfiguration config = networkQuarterMap.get(networkName);
        return config != null ? config.getQuarterPeriods() : new HashMap<>();
    }
    
    /**
     * Get quarter period for a specific quarter number
     */
    public QuarterPeriod getQuarterPeriod(String networkName, int quarterNumber) {
        QuarterConfiguration config = networkQuarterMap.get(networkName);
        return config != null ? config.getQuarterPeriod(quarterNumber) : null;
    }
    
    /**
     * Get formatted quarter list for a network (for dropdowns)
     */
    public List<String> getFormattedQuarters(String networkName) {
        List<String> quarters = new ArrayList<>();
        QuarterConfiguration config = networkQuarterMap.get(networkName);
        
        if (config != null) {
            for (int i = 1; i <= config.getNumberOfQuarters(); i++) {
                QuarterPeriod period = config.getQuarterPeriod(i);
                if (period != null) {
                    quarters.add(period.getQuarterName());
                }
            }
        }
        
        return quarters;
    }
}
