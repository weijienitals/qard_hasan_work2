package com.example.qard_hasan_for_education.model;

public enum MentorshipMatchStatus {
    PENDING("Pending Acceptance"),
    ACTIVE("Active Mentorship"),
    PAUSED("Temporarily Paused"),
    COMPLETED("Successfully Completed"),
    TERMINATED("Terminated Early"),
    CANCELLED("Cancelled Before Start");

    private final String description;

    MentorshipMatchStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}