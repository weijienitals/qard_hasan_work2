package com.example.qard_hasan_for_education.model;

import java.time.LocalDateTime;
import java.util.List;

public class MentorshipMatch {
    private String matchId;
    private String mentorId;
    private String menteeId;
    private MentorshipMatchStatus status;
    private LocalDateTime matchedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastSessionAt;
    private Integer sessionCount;
    private Double menteeRating; // 1.0 - 5.0
    private Double mentorRating; // 1.0 - 5.0
    private String menteeFeedback;
    private String mentorFeedback;
    private List<HelpType> focusAreas;
    private String notes;
    private String completionReason;

    // Default constructor
    public MentorshipMatch() {}

    // Getters and setters
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }
    public String getMenteeId() { return menteeId; }
    public void setMenteeId(String menteeId) { this.menteeId = menteeId; }
    public MentorshipMatchStatus getStatus() { return status; }
    public void setStatus(MentorshipMatchStatus status) { this.status = status; }
    public LocalDateTime getMatchedAt() { return matchedAt; }
    public void setMatchedAt(LocalDateTime matchedAt) { this.matchedAt = matchedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getLastSessionAt() { return lastSessionAt; }
    public void setLastSessionAt(LocalDateTime lastSessionAt) { this.lastSessionAt = lastSessionAt; }
    public Integer getSessionCount() { return sessionCount; }
    public void setSessionCount(Integer sessionCount) { this.sessionCount = sessionCount; }
    public Double getMenteeRating() { return menteeRating; }
    public void setMenteeRating(Double menteeRating) { this.menteeRating = menteeRating; }
    public Double getMentorRating() { return mentorRating; }
    public void setMentorRating(Double mentorRating) { this.mentorRating = mentorRating; }
    public String getMenteeFeedback() { return menteeFeedback; }
    public void setMenteeFeedback(String menteeFeedback) { this.menteeFeedback = menteeFeedback; }
    public String getMentorFeedback() { return mentorFeedback; }
    public void setMentorFeedback(String mentorFeedback) { this.mentorFeedback = mentorFeedback; }
    public List<HelpType> getFocusAreas() { return focusAreas; }
    public void setFocusAreas(List<HelpType> focusAreas) { this.focusAreas = focusAreas; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCompletionReason() { return completionReason; }
    public void setCompletionReason(String completionReason) { this.completionReason = completionReason; }
}
