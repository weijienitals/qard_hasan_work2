package com.example.qard_hasan_for_education.controller;

import com.example.qard_hasan_for_education.model.*;
import com.example.qard_hasan_for_education.service.VolunteeringService;
import com.example.qard_hasan_for_education.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/volunteering")
@CrossOrigin(origins = "*")
public class VolunteeringController {

    private static final Logger logger = LoggerFactory.getLogger(VolunteeringController.class);

    @Autowired
    private VolunteeringService volunteeringService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Calculate mentoring offer acceptance - STATELESS
     */
    @PostMapping("/calculate-accept-offer")
    public ResponseEntity<?> calculateAcceptMentoringOffer(@RequestBody CalculateAcceptOfferRequest request) {
        try {
            if (request.getOfferId() == null || request.getOfferId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Offer ID is required"));
            }
            if (request.getCurrentOffer() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Current offer data is required"));
            }
            if (request.getLoanAccount() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Loan account data is required"));
            }

            VolunteeringService.MentorProfileResult result = volunteeringService.calculateMentorProfile(
                    request.getOfferId(),
                    request.getAvailableHelpTypes(),
                    request.getBio(),
                    request.getContactPreference(),
                    request.getCurrentOffer(),
                    request.getLoanAccount()
            );

            // Generate welcome notification
            NotificationService.NotificationData welcomeNotification = notificationService.generateMentorWelcomeNotification(
                    result.getMentorProfile().getMentorId(),
                    result.getMentorProfile().getName()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Thank you for joining our mentorship program!");
            response.put("mentorProfile", result.getMentorProfile());
            response.put("updatedOffer", result.getUpdatedOffer());
            response.put("updatedTransaction", result.getUpdatedTransaction());
            response.put("welcomeNotification", welcomeNotification);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating mentoring offer acceptance", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Calculate mentoring offer decline - STATELESS
     */
    @PostMapping("/calculate-decline-offer")
    public ResponseEntity<?> calculateDeclineMentoringOffer(@RequestBody CalculateDeclineOfferRequest request) {
        try {
            if (request.getOfferId() == null || request.getOfferId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Offer ID is required"));
            }
            if (request.getCurrentOffer() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Current offer data is required"));
            }

            VolunteeringService.MentoringDeclineResult result = volunteeringService.calculateMentoringDecline(
                    request.getOfferId(),
                    request.getReason(),
                    request.getCurrentOffer()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Thank you for your response. You can always volunteer later!");
            response.put("updatedOffer", result.getUpdatedOffer());
            response.put("updatedTransaction", result.getUpdatedTransaction());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating mentoring offer decline", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Calculate potential mentees for a mentor - STATELESS
     */
    @PostMapping("/calculate-potential-mentees")
    public ResponseEntity<?> calculatePotentialMentees(@RequestBody CalculatePotentialMenteesRequest request) {
        try {
            if (request.getMentor() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mentor profile is required"));
            }
            if (request.getAllMentees() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "All mentees data is required"));
            }
            if (request.getExistingMatches() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Existing matches data is required"));
            }

            List<MenteeProfile> potentialMentees = volunteeringService.calculatePotentialMentees(
                    request.getMentor(),
                    request.getHelpTypes(),
                    request.getAllMentees(),
                    request.getExistingMatches()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mentees", potentialMentees);
            response.put("totalMentees", potentialMentees.size());
            response.put("message", potentialMentees.isEmpty() ?
                    "No mentees found matching your criteria at the moment. Check back later!" :
                    "Here are students who could benefit from your guidance");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating potential mentees", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error calculating potential mentees"));
        }
    }

    /**
     * Calculate mentor statistics and impact - STATELESS
     */
    @PostMapping("/calculate-mentor-stats")
    public ResponseEntity<?> calculateMentorStats(@RequestBody CalculateMentorStatsRequest request) {
        try {
            if (request.getMentor() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mentor profile is required"));
            }
            if (request.getAllMatches() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "All matches data is required"));
            }

            Map<String, Object> stats = volunteeringService.calculateMentorStats(
                    request.getMentor(),
                    request.getAllMatches()
            );

            if (stats.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Unable to calculate mentor stats"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);
            response.put("message", "Your mentoring impact summary");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating mentor stats", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error calculating mentor statistics"));
        }
    }

    /**
     * Calculate updated mentor preferences - STATELESS
     */
    @PostMapping("/calculate-mentor-preferences")
    public ResponseEntity<?> calculateUpdatedMentorPreferences(@RequestBody CalculateMentorPreferencesRequest request) {
        try {
            if (request.getCurrentProfile() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Current mentor profile is required"));
            }

            MentorProfile updatedProfile = volunteeringService.calculateUpdatedMentorPreferences(
                    request.getCurrentProfile(),
                    request.getHelpTypes(),
                    request.getMaxMentees(),
                    request.getBio(),
                    request.getTimeSlots(),
                    request.isActive()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mentor preferences calculated successfully");
            response.put("updatedMentorProfile", updatedProfile);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating mentor preferences", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Generate mentoring offer data - STATELESS
     */
    @PostMapping("/generate-mentoring-offer")
    public ResponseEntity<?> generateMentoringOffer(@RequestBody GenerateMentoringOfferRequest request) {
        try {
            if (request.getLoanAccount() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Loan account is required"));
            }
            if (request.getTransaction() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Transaction is required"));
            }

            VolunteeringService.MentoringOfferData offerData = volunteeringService.generateMentoringOffer(
                    request.getLoanAccount(),
                    request.getTransaction()
            );

            if (offerData == null) {
                return ResponseEntity.status(500)
                        .body(Map.of("success", false, "message", "Failed to generate mentoring offer data"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mentoring offer data generated successfully");
            response.put("offerData", offerData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating mentoring offer data", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error generating mentoring offer"));
        }
    }

    /**
     * Get available help types - STATELESS
     */
    @GetMapping("/help-types")
    public ResponseEntity<?> getAvailableHelpTypes() {
        try {
            Map<String, Object> helpTypes = new HashMap<>();
            for (HelpType type : HelpType.values()) {
                helpTypes.put(type.name(), type.getDescription());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "helpTypes", helpTypes,
                    "message", "Available types of help you can offer"
            ));

        } catch (Exception e) {
            logger.error("Error retrieving help types", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error retrieving help types"));
        }
    }

    /**
     * Get mentoring program information - STATELESS
     */
    @GetMapping("/program-info")
    public ResponseEntity<?> getMentoringProgramInfo() {
        try {
            Map<String, Object> programInfo = new HashMap<>();
            programInfo.put("title", "Indonesian Student Mentorship Program");
            programInfo.put("description", "Help fellow Indonesian students succeed in their studies abroad by sharing your experience and providing guidance.");
            programInfo.put("benefits", List.of(
                    "Make a meaningful impact on someone's educational journey",
                    "Build your leadership and communication skills",
                    "Expand your professional network",
                    "Give back to the Indonesian student community",
                    "Flexible commitment - mentor at your own pace"
            ));
            programInfo.put("commitment", "Flexible - typically 1-2 hours per month per mentee");
            programInfo.put("helpTypes", List.of(
                    "Academic guidance and study tips",
                    "Mental health and emotional support",
                    "Cultural adaptation advice",
                    "Career guidance and networking",
                    "Financial management tips",
                    "General friendship and support"
            ));

            return ResponseEntity.ok(Map.of("success", true, "programInfo", programInfo));

        } catch (Exception e) {
            logger.error("Error retrieving program info", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error retrieving program information"));
        }
    }

    /**
     * Health check for volunteering service - STATELESS
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        logger.info("=== VOLUNTEERING HEALTH CHECK CALLED ===");

        Map<String, Object> health = new HashMap<>();
        health.put("service", "VolunteeringService");
        health.put("status", "UP");
        health.put("timestamp", java.time.LocalDateTime.now());
        health.put("version", "2.0.0-STATELESS");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Volunteering API is running in stateless mode!");
        response.put("health", health);

        return ResponseEntity.ok(response);
    }

    // Request DTOs for stateless operations
    public static class CalculateAcceptOfferRequest {
        private String offerId;
        private VolunteeringService.MentoringOfferData currentOffer;
        private LoanAccount loanAccount;
        private List<HelpType> availableHelpTypes;
        private String bio;
        private String contactPreference;

        // Getters and setters
        public String getOfferId() { return offerId; }
        public void setOfferId(String offerId) { this.offerId = offerId; }
        public VolunteeringService.MentoringOfferData getCurrentOffer() { return currentOffer; }
        public void setCurrentOffer(VolunteeringService.MentoringOfferData currentOffer) { this.currentOffer = currentOffer; }
        public LoanAccount getLoanAccount() { return loanAccount; }
        public void setLoanAccount(LoanAccount loanAccount) { this.loanAccount = loanAccount; }
        public List<HelpType> getAvailableHelpTypes() { return availableHelpTypes; }
        public void setAvailableHelpTypes(List<HelpType> availableHelpTypes) { this.availableHelpTypes = availableHelpTypes; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getContactPreference() { return contactPreference; }
        public void setContactPreference(String contactPreference) { this.contactPreference = contactPreference; }
    }

    public static class CalculateDeclineOfferRequest {
        private String offerId;
        private VolunteeringService.MentoringOfferData currentOffer;
        private String reason;

        // Getters and setters
        public String getOfferId() { return offerId; }
        public void setOfferId(String offerId) { this.offerId = offerId; }
        public VolunteeringService.MentoringOfferData getCurrentOffer() { return currentOffer; }
        public void setCurrentOffer(VolunteeringService.MentoringOfferData currentOffer) { this.currentOffer = currentOffer; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class CalculatePotentialMenteesRequest {
        private MentorProfile mentor;
        private List<HelpType> helpTypes;
        private List<MenteeProfile> allMentees;
        private List<MentorshipMatch> existingMatches;

        // Getters and setters
        public MentorProfile getMentor() { return mentor; }
        public void setMentor(MentorProfile mentor) { this.mentor = mentor; }
        public List<HelpType> getHelpTypes() { return helpTypes; }
        public void setHelpTypes(List<HelpType> helpTypes) { this.helpTypes = helpTypes; }
        public List<MenteeProfile> getAllMentees() { return allMentees; }
        public void setAllMentees(List<MenteeProfile> allMentees) { this.allMentees = allMentees; }
        public List<MentorshipMatch> getExistingMatches() { return existingMatches; }
        public void setExistingMatches(List<MentorshipMatch> existingMatches) { this.existingMatches = existingMatches; }
    }

    public static class CalculateMentorStatsRequest {
        private MentorProfile mentor;
        private List<MentorshipMatch> allMatches;

        // Getters and setters
        public MentorProfile getMentor() { return mentor; }
        public void setMentor(MentorProfile mentor) { this.mentor = mentor; }
        public List<MentorshipMatch> getAllMatches() { return allMatches; }
        public void setAllMatches(List<MentorshipMatch> allMatches) { this.allMatches = allMatches; }
    }

    public static class CalculateMentorPreferencesRequest {
        private MentorProfile currentProfile;
        private List<HelpType> helpTypes;
        private Integer maxMentees;
        private String bio;
        private List<String> timeSlots;
        private boolean active;

        // Getters and setters
        public MentorProfile getCurrentProfile() { return currentProfile; }
        public void setCurrentProfile(MentorProfile currentProfile) { this.currentProfile = currentProfile; }
        public List<HelpType> getHelpTypes() { return helpTypes; }
        public void setHelpTypes(List<HelpType> helpTypes) { this.helpTypes = helpTypes; }
        public Integer getMaxMentees() { return maxMentees; }
        public void setMaxMentees(Integer maxMentees) { this.maxMentees = maxMentees; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public List<String> getTimeSlots() { return timeSlots; }
        public void setTimeSlots(List<String> timeSlots) { this.timeSlots = timeSlots; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class GenerateMentoringOfferRequest {
        private LoanAccount loanAccount;
        private RepaymentTransaction transaction;

        // Getters and setters
        public LoanAccount getLoanAccount() { return loanAccount; }
        public void setLoanAccount(LoanAccount loanAccount) { this.loanAccount = loanAccount; }
        public RepaymentTransaction getTransaction() { return transaction; }
        public void setTransaction(RepaymentTransaction transaction) { this.transaction = transaction; }
    }
}