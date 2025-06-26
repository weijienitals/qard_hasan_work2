package com.example.qard_hasan_for_education.model;

public enum MentoringOfferStatus {
    PENDING("Pending Response"),
    ACCEPTED("Accepted - Mentor Profile Created"),
    DECLINED("Declined"),
    EXPIRED("Expired - No Response"),
    CANCELLED("Cancelled");

    private final String description;

    MentoringOfferStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}