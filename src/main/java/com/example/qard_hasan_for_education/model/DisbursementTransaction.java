package com.example.qard_hasan_for_education.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DisbursementTransaction {
    @JsonProperty("disbursementId")
    private String disbursementId;

    @JsonProperty("loanId")
    private String loanId;

    @JsonProperty("studentId")
    private String studentId;

    @JsonProperty("applicationId")
    private String applicationId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("disbursementMethod")
    private DisbursementMethod disbursementMethod;

    @JsonProperty("status")
    private DisbursementStatus status;

    @JsonProperty("initiatedAt")
    private LocalDateTime initiatedAt;

    @JsonProperty("processedAt")
    private LocalDateTime processedAt;

    @JsonProperty("completedAt")
    private LocalDateTime completedAt;

    @JsonProperty("studentBankAccount")
    private String studentBankAccount;

    @JsonProperty("studentBankName")
    private String studentBankName;

    @JsonProperty("exchangeRate")
    private BigDecimal exchangeRate;

    @JsonProperty("fees")
    private BigDecimal fees;

    @JsonProperty("netAmount")
    private BigDecimal netAmount;

    @JsonProperty("referenceNumber")
    private String referenceNumber;

    @JsonProperty("trackingNumber")
    private String trackingNumber;

    @JsonProperty("failureReason")
    private String failureReason;

    @JsonProperty("estimatedDelivery")
    private LocalDateTime estimatedDelivery;

    @JsonProperty("notes")
    private String notes;

    // Constructors
    public DisbursementTransaction() {}

    public DisbursementTransaction(String loanId, String studentId, String applicationId,
                                   BigDecimal amount, DisbursementMethod method,
                                   String bankAccount, String bankName) {
        this.disbursementId = generateDisbursementId();
        this.loanId = loanId;
        this.studentId = studentId;
        this.applicationId = applicationId;
        this.amount = amount;
        this.disbursementMethod = method;
        this.studentBankAccount = bankAccount;
        this.studentBankName = bankName;
        this.status = DisbursementStatus.PENDING;
        this.initiatedAt = LocalDateTime.now();
        this.referenceNumber = generateReferenceNumber();
        this.fees = calculateFees(amount, method);
        this.netAmount = amount.subtract(this.fees);
        this.estimatedDelivery = calculateEstimatedDelivery(method);
    }

    // Getters and Setters
    public String getDisbursementId() { return disbursementId; }
    public void setDisbursementId(String disbursementId) { this.disbursementId = disbursementId; }

    public String getLoanId() { return loanId; }
    public void setLoanId(String loanId) { this.loanId = loanId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public DisbursementMethod getDisbursementMethod() { return disbursementMethod; }
    public void setDisbursementMethod(DisbursementMethod disbursementMethod) { this.disbursementMethod = disbursementMethod; }

    public DisbursementStatus getStatus() { return status; }
    public void setStatus(DisbursementStatus status) { this.status = status; }

    public LocalDateTime getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(LocalDateTime initiatedAt) { this.initiatedAt = initiatedAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getStudentBankAccount() { return studentBankAccount; }
    public void setStudentBankAccount(String studentBankAccount) { this.studentBankAccount = studentBankAccount; }

    public String getStudentBankName() { return studentBankName; }
    public void setStudentBankName(String studentBankName) { this.studentBankName = studentBankName; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public BigDecimal getFees() { return fees; }
    public void setFees(BigDecimal fees) { this.fees = fees; }

    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // Utility methods
    private String generateDisbursementId() {
        return "DISB_" + System.currentTimeMillis() + "_" +
                java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateReferenceNumber() {
        return "REF" + System.currentTimeMillis();
    }

    private BigDecimal calculateFees(BigDecimal amount, DisbursementMethod method) {
        switch (method) {
            case BANK_TRANSFER:
                return amount.multiply(new BigDecimal("0.01")); // 1% fee
            case WIRE_TRANSFER:
                return new BigDecimal("25.00"); // Fixed $25 fee
            case DIGITAL_WALLET:
                return amount.multiply(new BigDecimal("0.005")); // 0.5% fee
            case CHECK:
                return new BigDecimal("10.00"); // Fixed $10 fee
            default:
                return BigDecimal.ZERO;
        }
    }

    private LocalDateTime calculateEstimatedDelivery(DisbursementMethod method) {
        LocalDateTime now = LocalDateTime.now();
        switch (method) {
            case BANK_TRANSFER:
                return now.plusDays(1); // 1 business day
            case WIRE_TRANSFER:
                return now.plusDays(3); // 3 business days
            case DIGITAL_WALLET:
                return now.plusHours(1); // 1 hour
            case CHECK:
                return now.plusDays(7); // 7 business days
            default:
                return now.plusDays(2);
        }
    }

    public boolean isCompleted() {
        return status == DisbursementStatus.COMPLETED;
    }

    public boolean hasFailed() {
        return status == DisbursementStatus.FAILED || status == DisbursementStatus.CANCELLED;
    }

    @Override
    public String toString() {
        return "DisbursementTransaction{" +
                "disbursementId='" + disbursementId + '\'' +
                ", loanId='" + loanId + '\'' +
                ", amount=" + amount +
                ", status=" + status +
                ", method=" + disbursementMethod +
                '}';
    }
}