package com.example.qard_hasan_for_education.model;

public enum DisbursementStatus {
    PENDING("Disbursement initiated, awaiting processing"),
    PROCESSING("Funds being processed by bank"),
    SENT("Funds sent to student account"),
    COMPLETED("Funds successfully received by student"),
    FAILED("Disbursement failed"),
    CANCELLED("Disbursement cancelled");

    private final String description;

    DisbursementStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}