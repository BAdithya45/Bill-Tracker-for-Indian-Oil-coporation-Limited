package com.login.controller;

import com.login.model.BillRecord;
import com.login.model.User;
import com.login.service.BillDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bills")
public class BillController {
    
    @GetMapping
    public ResponseEntity<?> getAllBills(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        try {
            BillDataService billService = new BillDataService(user.getUsername());
            List<BillRecord> bills = billService.getAllBillRecords();
            return ResponseEntity.ok(bills);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> addBill(@RequestBody BillRecord billRecord, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            System.out.println("DEBUG: User not authenticated");
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        System.out.println("DEBUG: Adding bill for user: " + user.getUsername());
        System.out.println("DEBUG: Bill data: " + billRecord.toString());
        
        try {
            BillDataService billService = new BillDataService(user.getUsername());
            billService.addBillRecord(billRecord);
            
            System.out.println("DEBUG: Bill added successfully with serial no: " + billRecord.getSerialNo());
            
            // Return the serial number for PDF upload and other operations
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Bill added successfully",
                "serialNo", billRecord.getSerialNo()
            ));
        } catch (Exception e) {
            System.out.println("DEBUG: Error adding bill: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{serialNo}")
    public ResponseEntity<?> updateBill(@PathVariable int serialNo, @RequestBody BillRecord billRecord, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        try {
            BillDataService billService = new BillDataService(user.getUsername());
            billRecord.setSerialNo(serialNo);
            boolean success = billService.updateBillRecord(billRecord);
            
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Bill updated successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failed to update bill"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{serialNo}")
    public ResponseEntity<?> deleteBill(@PathVariable int serialNo, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        try {
            BillDataService billService = new BillDataService(user.getUsername());
            boolean success = billService.deleteBillRecord(serialNo);
            
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Bill deleted successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failed to delete bill"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{serialNo}/upload-pdf")
    public ResponseEntity<?> uploadPdf(@PathVariable int serialNo, @RequestParam("file") MultipartFile file, HttpSession session) {
        System.out.println("DEBUG: PDF Upload request received");
        System.out.println("DEBUG: Serial No: " + serialNo);
        System.out.println("DEBUG: File name: " + (file != null ? file.getOriginalFilename() : "null"));
        System.out.println("DEBUG: File size: " + (file != null ? file.getSize() : "null"));
        System.out.println("DEBUG: Content type: " + (file != null ? file.getContentType() : "null"));
        System.out.println("DEBUG: Session ID: " + session.getId());
        System.out.println("DEBUG: Session is new: " + session.isNew());
        
        User user = (User) session.getAttribute("user");
        System.out.println("DEBUG: User from session: " + (user != null ? user.getUsername() : "null"));
        
        if (user == null) {
            System.out.println("DEBUG: User not authenticated for PDF upload");
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        try {
            if (file == null || file.isEmpty() || file.getSize() == 0) {
                System.out.println("DEBUG: File is empty or null");
                return ResponseEntity.badRequest().body(Map.of("error", "No file selected or file is empty"));
            }
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                String filename = file.getOriginalFilename();
                if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                    System.out.println("DEBUG: Invalid file type: " + contentType);
                    return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files are allowed"));
                }
            }
            
            // Create directories if they don't exist
            String uploadDir = "pdfs" + File.separator + "shared";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) originalFilename = "unnamed.pdf";
            String filename = System.currentTimeMillis() + "_" + serialNo + "_" + originalFilename;
            Path filePath = uploadPath.resolve(filename);
            
            // Save file
            Files.copy(file.getInputStream(), filePath);
            
            // Update bill record with PDF path
            BillDataService billService = new BillDataService(user.getUsername());
            BillRecord bill = billService.getBillRecordBySerialNo(serialNo);
            if (bill != null) {
                // Store relative path instead of full system path
                String relativePath = "shared" + File.separator + filename;
                bill.setPdfFilePath(relativePath);
                billService.updateBillRecord(bill);
                
                System.out.println("DEBUG: PDF uploaded and saved with path: " + relativePath);
                
                return ResponseEntity.ok(Map.of(
                    "success", true, 
                    "message", "PDF uploaded successfully",
                    "filePath", relativePath
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Bill not found"));
            }
            
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/locations")
    public ResponseEntity<?> getLocations() {
        return ResponseEntity.ok(BillDataService.LOCATIONS);
    }
    
    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        try {
            BillDataService billService = new BillDataService(user.getUsername());
            List<BillRecord> bills = billService.getAllBillRecords();
            
            Map<String, Object> analytics = new HashMap<>();
            
            // Total bills
            analytics.put("totalBills", bills.size());
            
            // Total amount
            double totalAmount = bills.stream().mapToDouble(BillRecord::getBillWithTax).sum();
            analytics.put("totalAmount", totalAmount);
            
            // Bills by network
            Map<String, Long> billsByNetwork = new HashMap<>();
            bills.stream().collect(
                java.util.stream.Collectors.groupingBy(
                    bill -> bill.getNetwork() != null ? bill.getNetwork() : "Unknown",
                    java.util.stream.Collectors.counting()
                )
            ).forEach(billsByNetwork::put);
            analytics.put("billsByNetwork", billsByNetwork);
            
            // Bills by quarter using quarterString
            Map<String, Long> billsByQuarter = new HashMap<>();
            bills.stream().collect(
                java.util.stream.Collectors.groupingBy(
                    bill -> bill.getQuarterString() != null ? bill.getQuarterString() : "Unknown",
                    java.util.stream.Collectors.counting()
                )
            ).forEach(billsByQuarter::put);
            analytics.put("billsByQuarter", billsByQuarter);
            
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/pdf/{filename:.+}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String filename) {
        try {
            // Decode the filename (it might contain URL encoding)
            filename = java.net.URLDecoder.decode(filename, "UTF-8");
            System.out.println("DEBUG: Original filename parameter: " + filename);
            
            // Clean up malformed paths
            String cleanFilename = filename;
            
            // Fix common malformed patterns
            if (cleanFilename.contains("pdfsshared}")) {
                // Extract the filename after the }
                cleanFilename = cleanFilename.substring(cleanFilename.indexOf("}") + 1);
                System.out.println("DEBUG: Fixed pdfsshared} pattern, new filename: " + cleanFilename);
            } else if (cleanFilename.contains("pdfs\\shared}")) {
                cleanFilename = cleanFilename.substring(cleanFilename.indexOf("}") + 1);
                System.out.println("DEBUG: Fixed pdfs\\shared} pattern, new filename: " + cleanFilename);
            } else if (cleanFilename.startsWith("shared}")) {
                // Handle the specific case of shared}filename
                cleanFilename = cleanFilename.substring(7); // Remove "shared}" 
                System.out.println("DEBUG: Fixed shared} pattern, new filename: " + cleanFilename);
            } else if (cleanFilename.startsWith("pdfs\\") || cleanFilename.startsWith("pdfs/")) {
                // Remove the "pdfs/" prefix
                cleanFilename = cleanFilename.substring(5);
                System.out.println("DEBUG: Removed pdfs prefix, new filename: " + cleanFilename);
            }
            
            Path filePath;
            
            // Handle different path formats
            if (cleanFilename.contains("/") || cleanFilename.contains("\\")) {
                // If it contains path separators, it's a relative path from pdfs directory
                filePath = Paths.get("pdfs", cleanFilename);
            } else {
                // If it's just a filename, look in the shared directory
                filePath = Paths.get("pdfs", "shared", cleanFilename);
            }
            
            System.out.println("DEBUG: Looking for PDF at: " + filePath.toString());
            
            if (!Files.exists(filePath)) {
                System.out.println("DEBUG: PDF file not found at: " + filePath.toString());
                
                // Try alternative locations
                Path alternativeSharedPath = Paths.get("pdfs", "shared", cleanFilename);
                Path alternativeRootPath = Paths.get("pdfs", cleanFilename);
                
                if (Files.exists(alternativeSharedPath)) {
                    filePath = alternativeSharedPath;
                    System.out.println("DEBUG: Found PDF at alternative shared location: " + filePath.toString());
                } else if (Files.exists(alternativeRootPath)) {
                    filePath = alternativeRootPath;
                    System.out.println("DEBUG: Found PDF at alternative root location: " + filePath.toString());
                } else {
                    // Try pattern matching in the shared directory
                    Path sharedDir = Paths.get("pdfs", "shared");
                    if (Files.exists(sharedDir)) {
                        try {
                            // Extract the base filename without timestamp prefix
                            String baseFilename = cleanFilename;
                            String searchPattern = null;
                            
                            if (baseFilename.matches("\\d+_.*")) {
                                // Remove the timestamp prefix (digits followed by underscore)
                                searchPattern = baseFilename.replaceFirst("\\d+_", "");
                                System.out.println("DEBUG: Extracted pattern without timestamp: " + searchPattern);
                            } else {
                                // Use the full filename as search pattern
                                searchPattern = baseFilename;
                            }
                            
                            final String finalSearchPattern = searchPattern;
                            System.out.println("DEBUG: Searching for files matching pattern: " + finalSearchPattern);
                            
                            java.util.Optional<Path> matchingFile = Files.list(sharedDir)
                                .filter(Files::isRegularFile)
                                .filter(file -> {
                                    String fileName = file.getFileName().toString();
                                    System.out.println("DEBUG: Checking file: " + fileName);
                                    
                                    // Check if the file ends with the same pattern (ignoring timestamp prefix)
                                    if (fileName.endsWith(finalSearchPattern)) {
                                        System.out.println("DEBUG: File matches by ending: " + fileName);
                                        return true;
                                    }
                                    
                                    // Check if the file contains the pattern
                                    if (fileName.contains(finalSearchPattern)) {
                                        System.out.println("DEBUG: File matches by containing: " + fileName);
                                        return true;
                                    }
                                    
                                    // Try to match by removing timestamp from both filenames
                                    if (fileName.matches("\\d+_.*")) {
                                        String fileWithoutTimestamp = fileName.replaceFirst("\\d+_", "");
                                        if (fileWithoutTimestamp.equals(finalSearchPattern)) {
                                            System.out.println("DEBUG: File matches by pattern without timestamp: " + fileName);
                                            return true;
                                        }
                                    }
                                    
                                    return false;
                                })
                                .findFirst();
                            
                            if (matchingFile.isPresent()) {
                                filePath = matchingFile.get();
                                System.out.println("DEBUG: Found matching PDF file: " + filePath.toString());
                            } else {
                                System.out.println("DEBUG: No matching PDF files found");
                                return ResponseEntity.notFound().build();
                            }
                        } catch (IOException e) {
                            System.out.println("DEBUG: Error while searching for matching files: " + e.getMessage());
                            return ResponseEntity.notFound().build();
                        }
                    } else {
                        System.out.println("DEBUG: PDF shared directory does not exist");
                        return ResponseEntity.notFound().build();
                    }
                }
            }
            
            byte[] fileContent = Files.readAllBytes(filePath);
            
            // Get the actual filename for the download
            String actualFilename = filePath.getFileName().toString();
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"" + actualFilename + "\"")
                .header("Content-Type", "application/pdf")
                .body(fileContent);
                
        } catch (IOException e) {
            System.out.println("DEBUG: IOException while serving PDF: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            System.out.println("DEBUG: Exception while serving PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/config")
    public ResponseEntity<?> getConfiguration(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        try {
            BillDataService billService = new BillDataService(user.getUsername());
            Map<String, Object> config = billService.getConfiguration();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/config")
    public ResponseEntity<?> saveConfiguration(@RequestBody Map<String, Object> config, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        try {
            BillDataService billService = new BillDataService(user.getUsername());
            billService.saveConfiguration(config);
            return ResponseEntity.ok(Map.of("success", true, "message", "Configuration saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/vendors/{network}")
    public ResponseEntity<?> getVendorsByNetwork(@PathVariable String network, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            System.out.println("DEBUG: User not authenticated for vendors request");
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        System.out.println("DEBUG: Getting vendors for network: " + network);
        
        try {
            BillDataService billService = new BillDataService(user.getUsername());
            List<String> vendors = billService.getVendorsByNetwork(network);
            
            System.out.println("DEBUG: Found vendors: " + vendors);
            
            return ResponseEntity.ok(vendors);
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting vendors: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/quarters/{network}")
    public ResponseEntity<?> getQuartersByNetwork(@PathVariable String network, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        try {
            BillDataService billService = new BillDataService(user.getUsername());
            List<String> quarters = billService.getQuartersByNetwork(network);
            return ResponseEntity.ok(quarters);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/debug/pdf-paths")
    public ResponseEntity<?> debugPdfPaths(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        try {
            BillDataService billService = new BillDataService(user.getUsername());
            List<BillRecord> bills = billService.getAllBillRecords();
            
            List<Map<String, Object>> debugInfo = new ArrayList<>();
            for (BillRecord bill : bills) {
                if (bill.getPdfFilePath() != null && !bill.getPdfFilePath().isEmpty()) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("serialNo", bill.getSerialNo());
                    info.put("originalPath", bill.getPdfFilePath());
                    
                    // Check if path is malformed and fix it
                    String path = bill.getPdfFilePath();
                    String fixedPath = null;
                    
                    // Check for common malformed patterns
                    if (path.contains("pdfsshared}")) {
                        // Extract the filename after the }
                        String filename = path.substring(path.indexOf("}") + 1);
                        fixedPath = "shared" + File.separator + filename;
                    } else if (path.contains("pdfs\\shared}")) {
                        String filename = path.substring(path.indexOf("}") + 1);
                        fixedPath = "shared" + File.separator + filename;
                    } else if (path.startsWith("pdfs\\") || path.startsWith("pdfs/")) {
                        // Remove the "pdfs/" prefix
                        fixedPath = path.substring(5);
                    }
                    
                    info.put("fixedPath", fixedPath);
                    
                    // If we have a fixed path, update the bill
                    if (fixedPath != null && !fixedPath.equals(path)) {
                        bill.setPdfFilePath(fixedPath);
                        billService.updateBillRecord(bill);
                        info.put("updated", true);
                    } else {
                        info.put("updated", false);
                    }
                    
                    debugInfo.add(info);
                }
            }
            
            return ResponseEntity.ok(Map.of("debugInfo", debugInfo));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
