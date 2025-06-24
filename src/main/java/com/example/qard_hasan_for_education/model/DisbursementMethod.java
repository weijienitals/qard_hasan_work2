package com.example.qard_hasan_for_education.model;

public enum DisbursementMethod {
    BANK_TRANSFER("Direct bank transfer"),
    WIRE_TRANSFER("International wire transfer"),
    DIGITAL_WALLET("Digital wallet transfer"),
    CHECK("Physical check");

    private final String description;

    DisbursementMethod(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
