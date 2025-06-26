// File: src/main/java/com/example/qard_hasan_for_education/model/HelpType.java
package com.example.qard_hasan_for_education.model;

public enum HelpType {
    ACADEMIC_GUIDANCE("Academic Guidance & Study Tips"),
    MENTAL_HEALTH_SUPPORT("Mental Health & Emotional Support"),
    CULTURAL_ADAPTATION("Cultural Adaptation & Living Abroad"),
    CAREER_GUIDANCE("Career Guidance & Networking"),
    FINANCIAL_MANAGEMENT("Financial Management & Budgeting"),
    GENERAL_FRIENDSHIP("General Friendship & Social Support"),
    LANGUAGE_SUPPORT("Language Learning & Practice"),
    RESEARCH_GUIDANCE("Research & Thesis Support"),
    INTERNSHIP_GUIDANCE("Internship & Job Search Help"),
    UNIVERSITY_NAVIGATION("University System Navigation");

    private final String description;

    HelpType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}