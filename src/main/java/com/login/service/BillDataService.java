package com.login.service;
import com.login.model.BillRecord;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class to manage bill records storage and retrieval
 * Uses file-based storage for simplicity (can be upgraded to database later)
 */
public class BillDataService {
    private static final String PDF_STORAGE_DIR = "pdfs";
    private static final String DATA_DIR = "data";
    private final String billsDataFile;
    private final String sharedPdfDir;
    private List<BillRecord> billRecords;
    private int nextSerialNo;
    
    public BillDataService(String username) {
        // Ensure data directory exists
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // Use shared data file for all users to access the same data
        this.billsDataFile = DATA_DIR + File.separator + "bills.dat";
        this.sharedPdfDir = PDF_STORAGE_DIR + File.separator + "shared";
        billRecords = new ArrayList<>();
        loadBills();
        ensurePdfDirectory();
    }
      /**
     * Predefined list of locations
     */
    public static final String[] LOCATIONS = {
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
      /**
     * Predefined list of networks - cleaned up to remove regional BSNL variants
     */
    public static final String[] NETWORKS = {
        "BSNL", "P2P", "ILL", "AWS", "Switches", "F5"
    };
    
    /**
     * Network-specific vendor mappings
     */
    public static final Map<String, String[]> NETWORK_VENDORS = new HashMap<String, String[]>() {{
        put("BSNL", new String[]{"BSNL Vendor"});
        put("P2P", new String[]{"RAILTEL CORPORATION OF INDIA LTD", "POWER GRID CORPORATION OF INDIA LTD"});
        put("ILL", new String[]{"RELIANCE JIO INFOCOMM LTD", "ALSTONIA CONSULTING LLP"});
        put("AWS", new String[]{"TATA COMMUNICATIONS LTD", "AMAZON WEB SERVICES INDIA PVT LTD"});
        put("Switches", new String[]{"SWITCHES VENDOR"});
        put("F5", new String[]{"ALSTONIA CONSULTING LLP"});
    }};
    
    /**
     * Network-specific quarter mappings
     */
    public static final Map<String, String[]> NETWORK_QUARTERS = new HashMap<String, String[]>() {{
        put("BSNL", new String[]{"Q1-FY2024", "Q2-FY2024", "Q3-FY2024", "Q4-FY2024", "Q1-FY2025", "Q2-FY2025"});
        put("P2P", new String[]{"Q1-2024", "Q2-2024", "Q3-2024", "Q4-2024", "Q1-2025", "Q2-2025"});
        put("ILL", new String[]{"Q1-2024", "Q2-2024", "Q3-2024", "Q4-2024", "Q1-2025", "Q2-2025"});
        put("AWS", new String[]{"Q1-2024", "Q2-2024", "Q3-2024", "Q4-2024", "Q1-2025", "Q2-2025"});
        put("Switches", new String[]{"Q1-2024", "Q2-2024", "Q3-2024", "Q4-2024", "Q1-2025", "Q2-2025"});
        put("F5", new String[]{"Q1-2024", "Q2-2024", "Q3-2024", "Q4-2024", "Q1-2025", "Q2-2025"});
    }};
      
    /**
     * Predefined list of vendors - updated with actual vendor names
     */
    public static final String[] VENDORS = {
        "BSNL Vendor", 
        "RAILTEL CORPORATION OF INDIA LTD", "POWER GRID CORPORATION OF INDIA LTD",
        "RELIANCE JIO INFOCOMM LTD", "ALSTONIA CONSULTING LLP", 
        "TATA COMMUNICATIONS LTD", "AMAZON WEB SERVICES INDIA PVT LTD",
        "SWITCHES VENDOR"
    };
    
    /**
     * Predefined list of Commit Items
     */
    public static final String[] COMMIT_ITEMS = {
        "C_COMMEXP",
        "C_R&MEQPC"
    };
    
    /**
     * Predefined list of Cost Centers
     */
    public static final String[] COST_CENTERS = {
        "M75010-SRO",
        "M78010-TNSO"
    };
    
    /**
     * Load bills from file storage
     */
    private void loadBills() {
        File file = new File(billsDataFile);
        System.out.println("DEBUG: Attempting to load bills from: " + file.getAbsolutePath());
        System.out.println("DEBUG: File exists: " + file.exists() + ", File size: " + file.length());
        
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                @SuppressWarnings("unchecked")
                List<BillRecord> loadedBills = (List<BillRecord>) ois.readObject();
                billRecords = loadedBills;
                System.out.println("DEBUG: Successfully loaded " + billRecords.size() + " bills");
                
                  // Calculate next serial number
                nextSerialNo = billRecords.stream()
                    .mapToInt(BillRecord::getSerialNo)
                    .max()
                    .orElse(0) + 1;
                
                // Fix any records with missing year/quarter data
                fixMissingYearQuarterData();
                    
            } catch (Exception e) {
                System.err.println("ERROR: Failed to load bills from " + billsDataFile);
                e.printStackTrace();
                System.out.println("DEBUG: Attempting to recover by creating new empty list");
                billRecords = new ArrayList<>();
                nextSerialNo = 1;
            }        } else {
            System.out.println("DEBUG: Bills file does not exist, starting with empty list");
            nextSerialNo = 1;
            // Generate some sample data to help user get started
            generateSampleData();
        }
    }
    
    /**
     * Fix existing records that might have missing year/quarter data
     */
    private void fixMissingYearQuarterData() {
        boolean needsSave = false;
        for (BillRecord record : billRecords) {
            if (record.getYear() == 0 && record.getFromDate() != null) {
                // Set year from fromDate
                record.setYear(record.getFromDate().getYear());
                needsSave = true;
            }
            if (record.getQuarter() == 0 && record.getBillingPeriod() != null) {
                // Set quarter from billing period
                String billingPeriod = record.getBillingPeriod();
                int quarter = 1; // default
                if (billingPeriod.contains("Quarter 1")) quarter = 1;
                else if (billingPeriod.contains("Quarter 2")) quarter = 2;
                else if (billingPeriod.contains("Quarter 3")) quarter = 3;
                else if (billingPeriod.contains("Quarter 4")) quarter = 4;
                
                record.setQuarter(quarter);
                needsSave = true;
            }
        }
        if (needsSave) {
            saveBills();
            System.out.println("Fixed year/quarter data for existing records");
        }
    }
    
    /**
     * Save bills to file storage
     */
    private void saveBills() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(billsDataFile))) {
            oos.writeObject(billRecords);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error saving bill data: " + e.getMessage());
        }
    }
    
    /**
     * Ensure PDF storage directory exists
     */
    private void ensurePdfDirectory() {
        File pdfDir = new File(sharedPdfDir);
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }
    }
    
    /**
     * Add a new bill record
     */
    public void addBillRecord(BillRecord record) {
        record.setSerialNo(nextSerialNo++);
        billRecords.add(record);
        saveBills();
    }
    
    /**
     * Update an existing bill record
     */
    public boolean updateBillRecord(BillRecord updatedRecord) {
        for (int i = 0; i < billRecords.size(); i++) {
            if (billRecords.get(i).getSerialNo() == updatedRecord.getSerialNo()) {
                billRecords.set(i, updatedRecord);
                saveBills();
                return true;
            }
        }
        return false;
    }      /**
     * Delete a bill record and reindex remaining records
     */
    public boolean deleteBillRecord(int serialNo) {
        boolean removed = billRecords.removeIf(record -> record.getSerialNo() == serialNo);
        if (removed) {
            // Reindex all remaining records to ensure serial numbers are consecutive
            reindexSerialNumbers();
            saveBills();
        }
        return removed;
    }
    
    /**
     * Improved reindexing to ensure serial numbers are consecutive starting from 1
     */
    private void reindexSerialNumbers() {
        // Sort records by their existing serial numbers to maintain relative order
        Collections.sort(billRecords, Comparator.comparingInt(BillRecord::getSerialNo));
        
        // Reassign serial numbers starting from 1
        for (int i = 0; i < billRecords.size(); i++) {
            billRecords.get(i).setSerialNo(i + 1);
        }
        
        System.out.println("Reindexed serial numbers. New sequence:");
        for (BillRecord record : billRecords) {
            System.out.println("Serial #" + record.getSerialNo() + " - " + record.getInvoiceNumber());
        }
        
        // Update the next serial number for new records
        nextSerialNo = billRecords.isEmpty() ? 1 : billRecords.size() + 1;
    }
    
    /**
     * Get all bill records
     */
    public List<BillRecord> getAllBillRecords() {
        return new ArrayList<>(billRecords);
    }
      /**
     * Get bill records filtered by year and quarter
     */
    public List<BillRecord> getBillRecordsByYearAndQuarter(int year, int quarter) {
        return billRecords.stream()
            .filter(record -> record.getYear() == year && record.getQuarter() == quarter)
            .collect(Collectors.toList());
    }
    
    /**
     * Get bill records filtered by multiple criteria
     */
    public List<BillRecord> getFilteredBillRecords(Integer year, Integer quarter, String network, String vendor) {
        return billRecords.stream()
            .filter(record -> year == null || record.getYear() == year)
            .filter(record -> quarter == null || record.getQuarter() == quarter)
            .filter(record -> network == null || "All Networks".equals(network) || network.equals(record.getNetwork()))
            .filter(record -> vendor == null || "All Vendors".equals(vendor) || vendor.equals(record.getVendor()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get bill record by serial number
     */
    public BillRecord getBillRecordBySerialNo(int serialNo) {
        return billRecords.stream()
            .filter(record -> record.getSerialNo() == serialNo)
            .findFirst()
            .orElse(null);
    }
      /**
     * Get available years from records
     */
    public List<Integer> getAvailableYears() {
        return billRecords.stream()
            .map(BillRecord::getYear)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get available networks from records
     */
    public List<String> getAvailableNetworks() {
        List<String> networks = billRecords.stream()
            .map(BillRecord::getNetwork)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        return networks.isEmpty() ? Arrays.asList(NETWORKS) : networks;
    }
    
    /**
     * Get available vendors from records
     */
    public List<String> getAvailableVendors() {
        List<String> vendors = billRecords.stream()
            .map(BillRecord::getVendor)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        return vendors.isEmpty() ? Arrays.asList(VENDORS) : vendors;
    }
    
    /**
     * Get vendors filtered by network
     */
    public List<String> getVendorsByNetwork(String network) {
        if (network == null || network.equals("All Networks")) {
            return getAvailableVendors();
        }
        
        // First check predefined network-vendor mappings
        String[] networkVendors = NETWORK_VENDORS.get(network);
        if (networkVendors != null) {
            return Arrays.asList(networkVendors);
        }
        
        // Check custom configuration from file
        Map<String, Object> config = getConfiguration();
        if (config.containsKey("networkConfig")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> networkConfig = (Map<String, Object>) config.get("networkConfig");
            if (networkConfig.containsKey(network)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> networkData = (Map<String, Object>) networkConfig.get(network);
                if (networkData.containsKey("vendors")) {
                    @SuppressWarnings("unchecked")
                    List<String> vendors = (List<String>) networkData.get("vendors");
                    return vendors;
                }
            }
        }
        
        // Fallback to filtering from existing records
        List<String> recordVendors = billRecords.stream()
            .filter(record -> network.equals(record.getNetwork()))
            .map(BillRecord::getVendor)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
            
        // If no vendors found from records, return empty list for custom networks
        if (recordVendors.isEmpty()) {
            System.out.println("DEBUG: No vendors found for network: " + network + " - returning empty list");
            return new ArrayList<>();
        }
        
        return recordVendors;
    }
    
    /**
     * Get quarters filtered by network using predefined mappings
     */
    public List<String> getQuartersByNetwork(String network) {
        if (network == null || network.equals("All Networks")) {
            return getAvailableQuarters();
        }
        
        // First check predefined network-quarter mappings
        String[] networkQuarters = NETWORK_QUARTERS.get(network);
        if (networkQuarters != null) {
            return Arrays.asList(networkQuarters);
        }
        
        // Check custom configuration from file
        Map<String, Object> config = getConfiguration();
        if (config.containsKey("networkConfig")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> networkConfig = (Map<String, Object>) config.get("networkConfig");
            if (networkConfig.containsKey(network)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> networkData = (Map<String, Object>) networkConfig.get(network);
                if (networkData.containsKey("quarters")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> quarters = (List<Map<String, String>>) networkData.get("quarters");
                    return quarters.stream()
                        .map(q -> q.get("name"))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                }
            }
        }
        
        // Fallback to filtering from existing records
        List<String> recordQuarters = billRecords.stream()
            .filter(record -> network.equals(record.getNetwork()))
            .map(BillRecord::getQuarterString)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
            
        // If no quarters found from records, return simple default quarters
        if (recordQuarters.isEmpty()) {
            // Return simple quarter names
            return Arrays.asList("Q1", "Q2", "Q3", "Q4");
        }
        
        return recordQuarters;
    }
    
    /**
     * Store PDF file and return the file path
     */
    public String storePdfFile(byte[] pdfData, String originalFileName) {
        try {
            String fileName = System.currentTimeMillis() + "_" + originalFileName;
            File pdfFile = new File(sharedPdfDir, fileName);
            
            try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                fos.write(pdfData);
            }
            
            return pdfFile.getPath();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error storing PDF file: " + e.getMessage());
        }
    }
    
    /**
     * Get PDF file data
     */
    public byte[] getPdfFileData(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    return fis.readAllBytes();
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generate sample data for testing
     */
    public void generateSampleData() {
        if (billRecords.isEmpty()) {
            // Add some sample records
            LocalDate date1 = LocalDate.of(2024, 10, 1);
            LocalDate date2 = LocalDate.of(2024, 12, 31);
              BillRecord sample1 = new BillRecord();
            sample1.setNetwork(NETWORKS[0]);
            sample1.setVendor(VENDORS[0]);
            sample1.setLocation(LOCATIONS[0]);
            sample1.setInvoiceNumber("INV-2024-001");
            sample1.setBillWithoutTax(100000);
            sample1.calculateBillWithTax();
            sample1.setSes1("50");
            sample1.setSes2("25");
            sample1.setBillingPeriod("Quarter 1 (Oct-Mar)");
            sample1.setFromDate(date1);
            sample1.setToDate(date2);
            sample1.setStatus("Completed");            sample1.setRemarks("Sample record 1");
            sample1.setYear(2024);
            sample1.setQuarter(1);
            sample1.setQuarterString("Q1-2024");
            sample1.setGlCode("1001");
            sample1.setCommitItem(COMMIT_ITEMS[0]);
            sample1.setCostCenter(COST_CENTERS[0]);
            
            BillRecord sample2 = new BillRecord();
            sample2.setNetwork(NETWORKS[1]);
            sample2.setVendor(VENDORS[1]);
            sample2.setLocation(LOCATIONS[1]);
            sample2.setInvoiceNumber("INV-2024-002");
            sample2.setBillWithoutTax(150000);
            sample2.calculateBillWithTax();
            sample2.setSes1("75");
            sample2.setSes2("35");
            sample2.setBillingPeriod("Quarter 1 (Oct-Mar)");
            sample2.setFromDate(date1);
            sample2.setToDate(date2);
            sample2.setStatus("Pending");            sample2.setRemarks("Sample record 2");            sample2.setYear(2024);
            sample2.setQuarter(1);
            sample2.setQuarterString("Q1-2024");
            sample2.setGlCode("2002");
            sample2.setCommitItem(COMMIT_ITEMS[1]);
            sample2.setCostCenter(COST_CENTERS[1]);
            
            // Add 2025 sample records for testing year filtering
            LocalDate date3 = LocalDate.of(2025, 4, 1);
            LocalDate date4 = LocalDate.of(2025, 6, 30);
            
            BillRecord sample3 = new BillRecord();
            sample3.setNetwork(NETWORKS[2]);
            sample3.setVendor(VENDORS[4]);
            sample3.setLocation(LOCATIONS[2]);
            sample3.setInvoiceNumber("INV-2025-001");
            sample3.setBillWithoutTax(120000);
            sample3.calculateBillWithTax();
            sample3.setSes1("60");
            sample3.setSes2("30");
            sample3.setBillingPeriod("Quarter 2 (Apr-Jun)");
            sample3.setFromDate(date3);
            sample3.setToDate(date4);
            sample3.setStatus("Completed");            sample3.setRemarks("Sample record 2025");
            sample3.setYear(2025);
            sample3.setQuarter(2);
            sample3.setQuarterString("Q2-2025");
            sample3.setGlCode("3003");
            sample3.setCommitItem(COMMIT_ITEMS[0]);
            sample3.setCostCenter(COST_CENTERS[0]);
            
            addBillRecord(sample1);
            addBillRecord(sample2);
            addBillRecord(sample3);
        }
    }
    
    /**
     * Get configuration for networks, vendors, and quarters
     */
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        // Get default networks, vendors, and quarters
        List<String> networks = new ArrayList<>(Arrays.asList(NETWORKS));
        List<String> vendors = new ArrayList<>(Arrays.asList(VENDORS));
        List<String> quarters = getAvailableQuarters();
        
        // Load additional configuration from file if exists
        File configFile = new File(DATA_DIR + File.separator + "config.dat");
        if (configFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(configFile))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> savedConfig = (Map<String, Object>) ois.readObject();
                
                System.out.println("DEBUG: Loaded config from file: " + savedConfig.keySet());
                
                if (savedConfig.containsKey("networks")) {
                    networks = (List<String>) savedConfig.get("networks");
                }
                if (savedConfig.containsKey("vendors")) {
                    vendors = (List<String>) savedConfig.get("vendors");
                }
                if (savedConfig.containsKey("quarters")) {
                    quarters = (List<String>) savedConfig.get("quarters");
                }
                
                // Include networkConfig if it exists
                if (savedConfig.containsKey("networkConfig")) {
                    config.put("networkConfig", savedConfig.get("networkConfig"));
                    System.out.println("DEBUG: Found networkConfig in saved file");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        config.put("networks", networks);
        config.put("vendors", vendors);
        config.put("quarters", quarters);
        
        return config;
    }

    /**
     * Save configuration for networks, vendors, and quarters
     */
    public void saveConfiguration(Map<String, Object> config) {
        System.out.println("DEBUG: Saving configuration with keys: " + config.keySet());
        if (config.containsKey("networkConfig")) {
            System.out.println("DEBUG: Configuration contains networkConfig");
        }
        
        File configFile = new File(DATA_DIR + File.separator + "config.dat");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(configFile))) {
            oos.writeObject(config);
            System.out.println("DEBUG: Configuration saved successfully to " + configFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error saving configuration: " + e.getMessage());
        }
    }
    
    /**
     * Get available quarters from records
     */
    public List<String> getAvailableQuarters() {
        List<String> quarters = billRecords.stream()
            .map(BillRecord::getQuarterString)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        
        // If no quarters in records, return default quarters
        if (quarters.isEmpty()) {
            quarters = Arrays.asList("Q1-2024", "Q2-2024", "Q3-2024", "Q4-2024", "Q1-2025");
        }
        
        return quarters;
    }
}
