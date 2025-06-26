package com.example.qard_hasan_for_education.model;

import java.time.LocalDateTime;
import java.util.List;

public class MenteeProfile {
    private String menteeId;
    private String studentId;
    private String name;
    private String email;
    private List<HelpType> neededHelpTypes;
    private String description;
    private boolean needsMentor;
    private Integer urgencyLevel; // 1-5, 5 being most urgent
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String university;
    private String program;
    private String country;
    private String academicYear;
    private List<String> availableTimeSlots;
    private String preferredContactMethod;

    // Default constructor
    public MenteeProfile() {}

    // Getters and setters
    public String getMenteeId() { return menteeId; }
    public void setMenteeId(String menteeId) { this.menteeId = menteeId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<HelpType> getNeededHelpTypes() { return neededHelpTypes; }
    public void setNeededHelpTypes(List<HelpType> neededHelpTypes) { this.neededHelpTypes = neededHelpTypes; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isNeedsMentor() { return needsMentor; }
    public void setNeedsMentor(boolean needsMentor) { this.needsMentor = needsMentor; }
    public Integer getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(Integer urgencyLevel) { this.urgencyLevel = urgencyLevel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }
    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public List<String> getAvailableTimeSlots() { return availableTimeSlots; }
    public void setAvailableTimeSlots(List<String> availableTimeSlots) { this.availableTimeSlots = availableTimeSlots; }
    public String getPreferredContactMethod() { return preferredContactMethod; }
    public void setPreferredContactMethod(String preferredContactMethod) { this.preferredContactMethod = preferredContactMethod; }
}