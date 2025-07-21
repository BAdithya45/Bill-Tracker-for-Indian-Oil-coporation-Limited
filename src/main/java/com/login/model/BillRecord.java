package com.login.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Model class representing a bill record
 */
public class BillRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int serialNo;
    private String network;
    private String vendor;
    private String location;
    private String invoiceNumber;
    private double billWithTax;
    private double billWithoutTax;
    private String ses1;
    private String ses2;
    private String billingPeriod;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;
    
    private String status;
    private String remarks;
    private int year;
    private int quarter;
    private String quarterString; // New field for quarter strings like "Q1-2024"
    private String glCode;
    private String commitItem;
    private String costCenter;
    private String pdfFilePath;
    
    public BillRecord() {}
    
    // Getters and Setters
    public int getSerialNo() { return serialNo; }
    public void setSerialNo(int serialNo) { this.serialNo = serialNo; }
    
    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = network; }
    
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    
    public double getBillWithTax() { return billWithTax; }
    public void setBillWithTax(double billWithTax) { this.billWithTax = billWithTax; }
    
    public double getBillWithoutTax() { return billWithoutTax; }
    public void setBillWithoutTax(double billWithoutTax) { this.billWithoutTax = billWithoutTax; }
    
    public String getSes1() { return ses1; }
    public void setSes1(String ses1) { this.ses1 = ses1; }
    
    public String getSes2() { return ses2; }
    public void setSes2(String ses2) { this.ses2 = ses2; }
    
    public String getBillingPeriod() { return billingPeriod; }
    public void setBillingPeriod(String billingPeriod) { this.billingPeriod = billingPeriod; }
    
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    
    public int getQuarter() { return quarter; }
    public void setQuarter(int quarter) { this.quarter = quarter; }
    
    public String getQuarterString() { return quarterString; }
    public void setQuarterString(String quarterString) { this.quarterString = quarterString; }
    
    public String getGlCode() { return glCode; }
    public void setGlCode(String glCode) { this.glCode = glCode; }
    
    public String getCommitItem() { return commitItem; }
    public void setCommitItem(String commitItem) { this.commitItem = commitItem; }
    
    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }
    
    public String getPdfFilePath() { return pdfFilePath; }
    public void setPdfFilePath(String pdfFilePath) { this.pdfFilePath = pdfFilePath; }
    
    // Utility methods
    public void calculateBillWithTax() {
        this.billWithTax = this.billWithoutTax * 1.18; // Add 18% GST
    }
    
    public static String formatAmountWithSymbol(double amount) {
        return String.format("₹%.2f", amount);
    }
    
    public String getFormattedBillWithTax() {
        return formatAmountWithSymbol(billWithTax);
    }
    
    public String getFormattedBillWithoutTax() {
        return formatAmountWithSymbol(billWithoutTax);
    }

    public String getBillWithTaxFormatted() {
        return formatAmountWithSymbol(billWithTax);
    }
    
    public String getBillWithoutTaxFormatted() {
        return formatAmountWithSymbol(billWithoutTax);
    }
}
