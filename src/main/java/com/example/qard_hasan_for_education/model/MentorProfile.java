package com.example.qard_hasan_for_education.model;

import java.time.LocalDateTime;
import java.util.List;

public class MentorProfile {
    private String mentorId;
    private String studentId;
    private String name;
    private String email;
    private List<HelpType> availableHelpTypes;
    private String bio;
    private String contactPreference;
    private Integer maxMentees;
    private Integer currentMentees;
    private boolean active;
    private LocalDateTime joinedAt;
    private LocalDateTime lastUpdated;
    private String university;
    private String program;
    private String country;
    private List<String> preferredTimeSlots;

    // Default constructor
    public MentorProfile() {}

    // Getters and setters
    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<HelpType> getAvailableHelpTypes() { return availableHelpTypes; }
    public void setAvailableHelpTypes(List<HelpType> availableHelpTypes) { this.availableHelpTypes = availableHelpTypes; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getContactPreference() { return contactPreference; }
    public void setContactPreference(String contactPreference) { this.contactPreference = contactPreference; }
    public Integer getMaxMentees() { return maxMentees; }
    public void setMaxMentees(Integer maxMentees) { this.maxMentees = maxMentees; }
    public Integer getCurrentMentees() { return currentMentees; }
    public void setCurrentMentees(Integer currentMentees) { this.currentMentees = currentMentees; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }
    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public List<String> getPreferredTimeSlots() { return preferredTimeSlots; }
    public void setPreferredTimeSlots(List<String> preferredTimeSlots) { this.preferredTimeSlots = preferredTimeSlots; }
}