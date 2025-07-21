import java.io.*;
import java.util.*;
import java.time.LocalDate;

// Simplified BillRecord class for data creation
class SimpleBill implements Serializable {
    private static final long serialVersionUID = 1L;
    private int serialNo;
    private String network;
    private String vendor;
    private String location;
    private String invoiceNumber;
    private double billWithoutTax;
    private double billWithTax;
    private String ses1;
    private String ses2;
    private String billingPeriod;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String status;
    private String remarks;
    private int year;
    private int quarter;
    private String quarterString;
    private String glCode;
    private String commitItem;
    private String costCenter;

    // Constructor
    public SimpleBill(int serialNo, String network, String vendor, String location, 
                     String invoiceNumber, double billWithoutTax, String quarterString) {
        this.serialNo = serialNo;
        this.network = network;
        this.vendor = vendor;
        this.location = location;
        this.invoiceNumber = invoiceNumber;
        this.billWithoutTax = billWithoutTax;
        this.billWithTax = billWithoutTax * 1.18; // 18% tax
        this.quarterString = quarterString;
        this.year = 2024;
        this.quarter = 1;
        this.status = "Pending";
        this.ses1 = "50";
        this.ses2 = "25";
        this.billingPeriod = "Quarter 1 (Oct-Mar)";
        this.fromDate = LocalDate.of(2024, 10, 1);
        this.toDate = LocalDate.of(2024, 12, 31);
        this.glCode = "1001";
        this.commitItem = "C_COMMEXP";
        this.costCenter = "M75010-SRO";
        this.remarks = "Sample bill entry";
    }

    // Getters (simplified)
    public int getSerialNo() { return serialNo; }
    public String getNetwork() { return network; }
    public String getVendor() { return vendor; }
    public String getLocation() { return location; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public double getBillWithoutTax() { return billWithoutTax; }
    public double getBillWithTax() { return billWithTax; }
    public String getSes1() { return ses1; }
    public String getSes2() { return ses2; }
    public String getBillingPeriod() { return billingPeriod; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
    public String getStatus() { return status; }
    public String getRemarks() { return remarks; }
    public int getYear() { return year; }
    public int getQuarter() { return quarter; }
    public String getQuarterString() { return quarterString; }
    public String getGlCode() { return glCode; }
    public String getCommitItem() { return commitItem; }
    public String getCostCenter() { return costCenter; }
}

public class CreateSampleData {
    public static void main(String[] args) {
        try {
            // Create data directory if it doesn't exist
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            // Create sample bills
            List<SimpleBill> bills = new ArrayList<>();
            
            bills.add(new SimpleBill(1, "BSNL", "BSNL Vendor", "Sankari Depot (Sankari TOP)", 
                                   "INV-2024-001", 100000, "Q1-FY2024"));
            
            bills.add(new SimpleBill(2, "P2P", "RAILTEL CORPORATION OF INDIA LTD", "Arakkonam AFS", 
                                   "INV-2024-002", 150000, "Q1-2024"));
            
            bills.add(new SimpleBill(3, "ILL", "RELIANCE JIO INFOCOMM LTD", "Trichy DO", 
                                   "INV-2024-003", 120000, "Q2-2024"));

            // Save to file
            File billsFile = new File("data", "bills.dat");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(billsFile))) {
                oos.writeObject(bills);
                System.out.println("✅ Sample data created successfully!");
                System.out.println("📁 File: " + billsFile.getAbsolutePath());
                System.out.println("📊 Created " + bills.size() + " sample bills");
            }

        } catch (Exception e) {
            System.err.println("❌ Error creating sample data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
