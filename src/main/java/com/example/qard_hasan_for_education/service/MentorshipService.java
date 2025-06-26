package com.example.qard_hasan_for_education.service;

import com.example.qard_hasan_for_education.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MentorshipService {

    private static final Logger logger = LoggerFactory.getLogger(MentorshipService.class);

    // Simple in-memory storage
    private final Map<String, MentorProfile> mentorProfiles = new ConcurrentHashMap<>();
    private final Map<String, MenteeProfile> menteeProfiles = new ConcurrentHashMap<>();
    private final Map<String, MentorshipMatch> mentorshipMatches = new ConcurrentHashMap<>();

    @Autowired
    private NotificationService notificationService;

    /**
     * Create a mentee profile for students seeking help
     */
    public MenteeProfile createMenteeProfile(String studentId, String name, String university,
                                             String program, Integer currentYear, String currentCountry,
                                             List<HelpType> needsHelpWith, String urgencyLevel,
                                             String description) {
        logger.info("Creating mentee profile for student: {}", studentId);

        MenteeProfile menteeProfile = new MenteeProfile();
        menteeProfile.setMenteeId("MENTEE_" + System.currentTimeMillis());
        menteeProfile.setStudentId(studentId);
        menteeProfile.setName(name);
        menteeProfile.setUniversity(university);
        menteeProfile.setProgram(program);
        menteeProfile.setCountry(currentCountry);  // Fixed: setCurrentCountry() -> setCountry()
        menteeProfile.setAcademicYear(currentYear != null ? currentYear.toString() : "Unknown");  // Map currentYear to academicYear
        menteeProfile.setNeededHelpTypes(needsHelpWith);  // Fixed: setNeedsHelpWith() -> setNeededHelpTypes()
        // Handle urgency level conversion with fallback
        try {
            menteeProfile.setUrgencyLevel(urgencyLevel != null ? Integer.parseInt(urgencyLevel) : 3);
        } catch (NumberFormatException e) {
            menteeProfile.setUrgencyLevel(3); // Default to medium urgency
        }
        menteeProfile.setDescription(description);
        menteeProfile.setNeedsMentor(true);
        menteeProfile.setCreatedAt(LocalDateTime.now());

        // Store in memory
        menteeProfiles.put(menteeProfile.getMenteeId(), menteeProfile);

        logger.info("Mentee profile created: {} for student: {}", menteeProfile.getMenteeId(), studentId);
        return menteeProfile;
    }

    /**
     * Match mentor with mentee based on preferences and availability
     */
    public MentorshipMatch createMentorshipMatch(String mentorId, String menteeId, HelpType helpType) throws Exception {
        logger.info("Creating mentorship match: mentor={}, mentee={}, helpType={}", mentorId, menteeId, helpType);

        // Validate mentor
        MentorProfile mentor = mentorProfiles.get(mentorId);
        if (mentor == null) {
            throw new Exception("Mentor not found: " + mentorId);
        }

        Integer currentMentees = mentor.getCurrentMentees() != null ? mentor.getCurrentMentees() : 0;
        Integer maxMentees = mentor.getMaxMentees() != null ? mentor.getMaxMentees() : 5;

        if (currentMentees >= maxMentees) {
            throw new Exception("Mentor has reached maximum mentee capacity");
        }

        List<HelpType> availableHelpTypes = mentor.getAvailableHelpTypes();
        if (availableHelpTypes == null || !availableHelpTypes.contains(helpType)) {
            throw new Exception("Mentor does not offer this type of help: " + helpType);
        }

        // Validate mentee
        MenteeProfile mentee = menteeProfiles.get(menteeId);
        if (mentee == null) {
            throw new Exception("Mentee not found: " + menteeId);
        }

        if (!mentee.isNeedsMentor()) {
            throw new Exception("Mentee is not actively seeking a mentor");
        }

        // Check for existing active match
        if (hasActiveMatch(mentorId, menteeId)) {
            throw new Exception("Active mentorship already exists between these users");
        }

        // Create the match
        MentorshipMatch match = new MentorshipMatch();
        match.setMatchId("MATCH_" + System.currentTimeMillis());
        match.setMentorId(mentorId);
        match.setMenteeId(menteeId);
        match.setStatus(MentorshipMatchStatus.ACTIVE);
        match.setMatchedAt(LocalDateTime.now());
        match.setFocusAreas(Arrays.asList(helpType));
        match.setSessionCount(0);
        mentorshipMatches.put(match.getMatchId(), match);

        // Update mentor and mentee statistics
        updateMentorStats(mentor, 1, 0);
        mentee.setLastUpdated(LocalDateTime.now());

        // Send notifications
        List<NotificationService.NotificationData> notifications = notificationService.generateMentorshipMatchNotifications(mentorId, menteeId, match.getMatchId());
        logger.info("Generated {} mentorship match notifications", notifications.size());

        logger.info("Mentorship match created: {}", match.getMatchId());
        return match;
    }

    /**
     * Get all matches for a mentor
     */
    public List<MentorshipMatch> getMentorMatches(String mentorId) {
        return mentorshipMatches.values().stream()
                .filter(match -> mentorId.equals(match.getMentorId()))
                .sorted((m1, m2) -> m2.getMatchedAt().compareTo(m1.getMatchedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Get all matches for a mentee
     */
    public List<MentorshipMatch> getMenteeMatches(String menteeId) {
        return mentorshipMatches.values().stream()
                .filter(match -> menteeId.equals(match.getMenteeId()))
                .sorted((m1, m2) -> m2.getMatchedAt().compareTo(m1.getMatchedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Record a mentoring session
     */
    public MentorshipMatch recordSession(String matchId, String sessionNotes) throws Exception {
        MentorshipMatch match = mentorshipMatches.get(matchId);
        if (match == null) {
            throw new Exception("Mentorship match not found: " + matchId);
        }

        if (match.getStatus() != MentorshipMatchStatus.ACTIVE) {
            throw new Exception("Cannot record session for inactive mentorship");
        }

        // Update match
        Integer currentSessions = match.getSessionCount() != null ? match.getSessionCount() : 0;
        match.setSessionCount(currentSessions + 1);
        match.setLastSessionAt(LocalDateTime.now());
        if (sessionNotes != null && !sessionNotes.trim().isEmpty()) {
            String existingNotes = match.getNotes() != null ? match.getNotes() : "";
            match.setNotes(existingNotes + "\n[" + LocalDateTime.now() + "] " + sessionNotes);
        }

        // Update mentor statistics
        MentorProfile mentor = mentorProfiles.get(match.getMentorId());
        if (mentor != null) {
            updateMentorStats(mentor, 0, 1);
        }

        logger.info("Session recorded for match: {}, total sessions: {}", matchId, match.getSessionCount());
        return match;
    }

    /**
     * Rate a mentorship (by mentee rating mentor, or mentor rating mentee)
     */
    public MentorshipMatch rateMentorship(String matchId, int rating, boolean isMenteeRating) throws Exception {
        if (rating < 1 || rating > 5) {
            throw new Exception("Rating must be between 1 and 5");
        }

        MentorshipMatch match = mentorshipMatches.get(matchId);
        if (match == null) {
            throw new Exception("Mentorship match not found: " + matchId);
        }

        if (isMenteeRating) {
            match.setMenteeRating((double) rating);
            logger.info("Mentee rated mentorship: {} - Rating: {}", matchId, rating);
        } else {
            match.setMentorRating((double) rating);
            logger.info("Mentor rated mentorship: {} - Rating: {}", matchId, rating);
        }

        // Update mentor's average rating if mentee rated
        if (isMenteeRating) {
            updateMentorAverageRating(match.getMentorId());
        }

        return match;
    }

    /**
     * Complete a mentorship
     */
    public MentorshipMatch completeMentorship(String matchId, String completionReason) throws Exception {
        MentorshipMatch match = mentorshipMatches.get(matchId);
        if (match == null) {
            throw new Exception("Mentorship match not found: " + matchId);
        }

        match.setStatus(MentorshipMatchStatus.COMPLETED);
        match.setCompletedAt(LocalDateTime.now());
        match.setLastSessionAt(LocalDateTime.now());

        if (completionReason != null) {
            String existingNotes = match.getNotes() != null ? match.getNotes() : "";
            match.setNotes(existingNotes + "\n[COMPLETED] " + completionReason);
        }

        // Update mentor's current mentee count
        MentorProfile mentor = mentorProfiles.get(match.getMentorId());
        if (mentor != null) {
            updateMentorStats(mentor, -1, 0);
        }

        // Send completion notification
        List<NotificationService.NotificationData> notifications = notificationService.generateMentorshipCompletionNotifications(match.getMentorId(), match.getMenteeId(), matchId);
        logger.info("Generated {} mentorship completion notifications", notifications.size());

        logger.info("Mentorship completed: {}", matchId);
        return match;
    }

    /**
     * Pause a mentorship temporarily
     */
    public MentorshipMatch pauseMentorship(String matchId, String reason) throws Exception {
        MentorshipMatch match = mentorshipMatches.get(matchId);
        if (match == null) {
            throw new Exception("Mentorship match not found: " + matchId);
        }

        match.setStatus(MentorshipMatchStatus.PAUSED);
        match.setLastSessionAt(LocalDateTime.now());

        if (reason != null) {
            String existingNotes = match.getNotes() != null ? match.getNotes() : "";
            match.setNotes(existingNotes + "\n[PAUSED] " + reason);
        }

        logger.info("Mentorship paused: {} - Reason: {}", matchId, reason);
        return match;
    }

    /**
     * Resume a paused mentorship
     */
    public MentorshipMatch resumeMentorship(String matchId) throws Exception {
        MentorshipMatch match = mentorshipMatches.get(matchId);
        if (match == null) {
            throw new Exception("Mentorship match not found: " + matchId);
        }

        if (match.getStatus() != MentorshipMatchStatus.PAUSED) {
            throw new Exception("Can only resume paused mentorships");
        }

        match.setStatus(MentorshipMatchStatus.ACTIVE);
        match.setLastSessionAt(LocalDateTime.now());

        String existingNotes = match.getNotes() != null ? match.getNotes() : "";
        match.setNotes(existingNotes + "\n[RESUMED] " + LocalDateTime.now());

        logger.info("Mentorship resumed: {}", matchId);
        return match;
    }

    /**
     * Get mentorship statistics for dashboard
     */
    public Map<String, Object> getMentorshipStatistics() {
        Collection<MentorshipMatch> allMatches = mentorshipMatches.values();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMentors", mentorProfiles.size());
        stats.put("totalMentees", menteeProfiles.size());
        stats.put("totalMatches", allMatches.size());
        stats.put("activeMatches", allMatches.stream().filter(m -> m.getStatus() == MentorshipMatchStatus.ACTIVE).count());
        stats.put("completedMatches", allMatches.stream().filter(m -> m.getStatus() == MentorshipMatchStatus.COMPLETED).count());
        stats.put("totalSessions", allMatches.stream().mapToInt(m -> m.getSessionCount() != null ? m.getSessionCount() : 0).sum());

        return stats;
    }

    /**
     * Check if there's an active match between mentor and mentee
     */
    public boolean hasActiveMatch(String mentorId, String menteeId) {
        return mentorshipMatches.values().stream()
                .anyMatch(match -> mentorId.equals(match.getMentorId()) &&
                        menteeId.equals(match.getMenteeId()) &&
                        match.getStatus() == MentorshipMatchStatus.ACTIVE);
    }

    // Private helper methods

    private void updateMentorStats(MentorProfile mentor, int menteeCountChange, int sessionCountChange) {
        Integer currentMentees = mentor.getCurrentMentees() != null ? mentor.getCurrentMentees() : 0;
        mentor.setCurrentMentees(Math.max(0, currentMentees + menteeCountChange));
        mentor.setLastUpdated(LocalDateTime.now());
    }

    private void updateMentorAverageRating(String mentorId) {
        List<MentorshipMatch> mentorMatches = getMentorMatches(mentorId);

        List<Double> ratings = mentorMatches.stream()
                .map(MentorshipMatch::getMenteeRating)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!ratings.isEmpty()) {
            double averageRating = ratings.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            // Note: MentorProfile doesn't have setAverageRating() method
            // Average rating calculation is done but not stored
            logger.info("Mentor {} average rating calculated: {}", mentorId, Math.round(averageRating * 100.0) / 100.0);
        }
    }
}