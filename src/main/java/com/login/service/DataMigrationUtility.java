package com.login.service;

import com.login.model.BillRecord;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Utility to migrate user-specific data to shared data format
 * This helps transition from user-segregated data to shared data across all users
 */
public class DataMigrationUtility {
    
    private static final String DATA_DIR = "data";
    private static final String SHARED_BILLS_FILE = DATA_DIR + File.separator + "bills.dat";
    
    public static void main(String[] args) {
        System.out.println("Starting data migration from user-specific to shared format...");
        
        try {
            migrateUserDataToShared();
            System.out.println("Data migration completed successfully!");
        } catch (Exception e) {
            System.err.println("Error during data migration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Migrate all user-specific bill data to shared format
     */
    public static void migrateUserDataToShared() throws IOException, ClassNotFoundException {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            System.out.println("Data directory does not exist. Nothing to migrate.");
            return;
        }
        
        List<BillRecord> allBillRecords = new ArrayList<>();
        int nextSerialNo = 1;
        
        // Find all user-specific bill files
        File[] userFiles = dataDir.listFiles((dir, name) -> 
            name.startsWith("bills_") && name.endsWith(".dat") && !name.equals("bills.dat"));
        
        if (userFiles == null || userFiles.length == 0) {
            System.out.println("No user-specific bill files found. Creating empty shared data file.");
            saveSharedBillRecords(allBillRecords);
            return;
        }
        
        System.out.println("Found " + userFiles.length + " user-specific data files:");
        
        // Load data from each user file
        for (File userFile : userFiles) {
            String filename = userFile.getName();
            String username = filename.substring(6, filename.length() - 4); // Remove "bills_" and ".dat"
            System.out.println("  - Processing: " + filename + " (User: " + username + ")");
            
            try {
                List<BillRecord> userBills = loadUserBillRecords(userFile);
                System.out.println("    Loaded " + userBills.size() + " records from " + username);
                
                // Update serial numbers to avoid conflicts
                for (BillRecord record : userBills) {
                    record.setSerialNo(nextSerialNo++);
                    allBillRecords.add(record);
                }
                
            } catch (Exception e) {
                System.err.println("    Error loading data from " + filename + ": " + e.getMessage());
            }
        }
        
        System.out.println("\nTotal records to migrate: " + allBillRecords.size());
        
        // Save all records to shared file
        saveSharedBillRecords(allBillRecords);
        
        // Optionally backup original files
        backupUserFiles(userFiles);
        
        System.out.println("Migration completed. All " + allBillRecords.size() + 
                         " records are now available to all users in shared format.");
    }
    
    /**
     * Load bill records from a user-specific file
     */
    @SuppressWarnings("unchecked")
    private static List<BillRecord> loadUserBillRecords(File userFile) 
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(userFile))) {
            return (List<BillRecord>) ois.readObject();
        }
    }
    
    /**
     * Save bill records to shared file
     */
    private static void saveSharedBillRecords(List<BillRecord> records) throws IOException {
        File sharedFile = new File(SHARED_BILLS_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(sharedFile))) {
            oos.writeObject(records);
        }
        System.out.println("Saved " + records.size() + " records to shared file: " + sharedFile.getAbsolutePath());
    }
    
    /**
     * Backup original user files by renaming them
     */
    private static void backupUserFiles(File[] userFiles) {
        System.out.println("\nBacking up original user files...");
        for (File userFile : userFiles) {
            String backupName = userFile.getName() + ".backup." + System.currentTimeMillis();
            File backupFile = new File(userFile.getParent(), backupName);
            
            if (userFile.renameTo(backupFile)) {
                System.out.println("  - Backed up: " + userFile.getName() + " -> " + backupName);
            } else {
                System.err.println("  - Failed to backup: " + userFile.getName());
            }
        }
    }
    
    /**
     * Migrate PDF files from user-specific directories to shared directory
     */
    public static void migratePdfFilesToShared() {
        System.out.println("\nMigrating PDF files to shared directory...");
        
        File pdfsDir = new File("pdfs");
        if (!pdfsDir.exists()) {
            System.out.println("PDFs directory does not exist. Nothing to migrate.");
            return;
        }
        
        File sharedPdfDir = new File("pdfs", "shared");
        if (!sharedPdfDir.exists()) {
            sharedPdfDir.mkdirs();
        }
        
        // Find all user-specific PDF directories
        File[] userDirs = pdfsDir.listFiles((dir, name) -> 
            new File(dir, name).isDirectory() && !name.equals("shared"));
        
        if (userDirs == null || userDirs.length == 0) {
            System.out.println("No user-specific PDF directories found.");
            return;
        }
        
        int totalFilesMoved = 0;
        
        for (File userDir : userDirs) {
            System.out.println("  - Processing PDFs from user: " + userDir.getName());
            
            File[] pdfFiles = userDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (pdfFiles != null) {
                for (File pdfFile : pdfFiles) {
                    try {
                        File newLocation = new File(sharedPdfDir, pdfFile.getName());
                        
                        // If file already exists, rename with timestamp
                        if (newLocation.exists()) {
                            String nameWithoutExt = pdfFile.getName().substring(0, pdfFile.getName().lastIndexOf('.'));
                            String ext = pdfFile.getName().substring(pdfFile.getName().lastIndexOf('.'));
                            newLocation = new File(sharedPdfDir, nameWithoutExt + "_" + System.currentTimeMillis() + ext);
                        }
                        
                        if (pdfFile.renameTo(newLocation)) {
                            totalFilesMoved++;
                            System.out.println("    Moved: " + pdfFile.getName() + " -> " + newLocation.getName());
                        } else {
                            System.err.println("    Failed to move: " + pdfFile.getName());
                        }
                        
                    } catch (Exception e) {
                        System.err.println("    Error moving " + pdfFile.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        System.out.println("PDF migration completed. Moved " + totalFilesMoved + " files to shared directory.");
    }
}
