package com.example.qard_hasan_for_education.service;

import com.example.qard_hasan_for_education.model.*;
import com.example.qard_hasan_for_education.model.individual.SimpleBankInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VolunteeringService {

    private static final Logger logger = LoggerFactory.getLogger(VolunteeringService.class);

    /**
     * Calculate mentor profile creation from offer acceptance - STATELESS
     */
    public MentorProfileResult calculateMentorProfile(String offerId,
                                                      List<HelpType> availableHelpTypes,
                                                      String bio,
                                                      String contactPreference,
                                                      MentoringOfferData currentOffer,
                                                      LoanAccount loanAccount) throws Exception {
        logger.info("=== CALCULATING MENTOR PROFILE === Offer: {}", offerId);

        if (currentOffer == null) {
            throw new Exception("Current offer data not found: " + offerId);
        }

        if (currentOffer.getStatus() != MentoringOfferStatus.PENDING) {
            throw new Exception("Offer is not pending. Status: " + currentOffer.getStatus());
        }

        // Create mentor profile from loan account data
        MentorProfile mentorProfile = new MentorProfile();
        mentorProfile.setMentorId("MENTOR_" + System.currentTimeMillis());
        mentorProfile.setStudentId(loanAccount.getStudentId());

        // Use available methods from LoanAccount
        mentorProfile.setName(loanAccount.getStudentName() != null ? loanAccount.getStudentName() : "Student");

        // Generate email from student ID since getStudentEmail() doesn't exist
        String email = generateEmailFromStudentId(loanAccount.getStudentId());
        mentorProfile.setEmail(email);

        mentorProfile.setAvailableHelpTypes(availableHelpTypes != null ? availableHelpTypes : Arrays.asList(HelpType.ACADEMIC_GUIDANCE));
        mentorProfile.setBio(bio != null ? bio : "Ready to help fellow Indonesian students!");
        mentorProfile.setContactPreference(contactPreference != null ? contactPreference : "email");
        mentorProfile.setMaxMentees(3); // Default
        mentorProfile.setCurrentMentees(0);
        mentorProfile.setActive(true);
        mentorProfile.setJoinedAt(LocalDateTime.now());

        // Use default values since getUniversity(), getProgram(), getCountry() don't exist
        mentorProfile.setUniversity("Unknown University");
        mentorProfile.setProgram("Unknown Program");
        mentorProfile.setCountry("Unknown Country");

        // Update offer status
        MentoringOfferData updatedOffer = copyOffer(currentOffer);
        updatedOffer.setStatus(MentoringOfferStatus.ACCEPTED);
        updatedOffer.setRespondedAt(LocalDateTime.now());
        updatedOffer.setMentorId(mentorProfile.getMentorId());

        // Create a new transaction record
        RepaymentTransaction updatedTransaction = null;
        if (currentOffer.getTransactionId() != null) {
            updatedTransaction = new RepaymentTransaction();
            updatedTransaction.setTransactionId(currentOffer.getTransactionId());
        }

        logger.info("Mentor profile calculated: {} for student: {}",
                mentorProfile.getMentorId(), loanAccount.getStudentId());

        return new MentorProfileResult(mentorProfile, updatedOffer, updatedTransaction);
    }

    /**
     * Calculate mentoring offer decline - STATELESS
     */
    public MentoringDeclineResult calculateMentoringDecline(String offerId,
                                                            String reason,
                                                            MentoringOfferData currentOffer) throws Exception {
        logger.info("=== CALCULATING MENTORING DECLINE === Offer: {}", offerId);

        if (currentOffer == null) {
            throw new Exception("Current offer data not found: " + offerId);
        }

        if (currentOffer.getStatus() != MentoringOfferStatus.PENDING) {
            throw new Exception("Offer is not pending. Status: " + currentOffer.getStatus());
        }

        // Update offer status
        MentoringOfferData updatedOffer = copyOffer(currentOffer);
        updatedOffer.setStatus(MentoringOfferStatus.DECLINED);
        updatedOffer.setRespondedAt(LocalDateTime.now());
        updatedOffer.setDeclineReason(reason);

        // Create updated transaction record
        RepaymentTransaction updatedTransaction = null;
        if (currentOffer.getTransactionId() != null) {
            updatedTransaction = new RepaymentTransaction();
            updatedTransaction.setTransactionId(currentOffer.getTransactionId());
        }

        logger.info("Mentoring decline calculated for offer: {}", offerId);

        return new MentoringDeclineResult(updatedOffer, updatedTransaction);
    }

    /**
     * Calculate potential mentees for a mentor - STATELESS
     */
    public List<MenteeProfile> calculatePotentialMentees(MentorProfile mentor,
                                                         List<HelpType> helpTypes,
                                                         List<MenteeProfile> allMentees,
                                                         List<MentorshipMatch> existingMatches) {
        logger.info("=== CALCULATING POTENTIAL MENTEES === Mentor: {}", mentor.getMentorId());

        if (allMentees == null) {
            return Collections.emptyList();
        }

        if (existingMatches == null) {
            existingMatches = Collections.emptyList();
        }

        // Get IDs of already matched mentees for this mentor
        Set<String> alreadyMatchedIds = existingMatches.stream()
                .filter(match -> mentor.getMentorId().equals(match.getMentorId()))
                .filter(match -> match.getStatus() == MentorshipMatchStatus.ACTIVE)
                .map(MentorshipMatch::getMenteeId)
                .collect(Collectors.toSet());

        // Filter potential mentees
        List<MenteeProfile> potentialMentees = allMentees.stream()
                .filter(mentee -> !alreadyMatchedIds.contains(mentee.getMenteeId()))
                .filter(mentee -> mentee.isNeedsMentor())
                .filter(mentee -> isCompatibleMatch(mentor, mentee, helpTypes))
                .sorted(this::prioritizeMentees)
                .limit(10) // Return top 10 matches
                .collect(Collectors.toList());

        logger.info("Found {} potential mentees for mentor: {}", potentialMentees.size(), mentor.getMentorId());

        return potentialMentees;
    }

    /**
     * Calculate mentor statistics - STATELESS
     */
    public Map<String, Object> calculateMentorStats(MentorProfile mentor, List<MentorshipMatch> allMatches) {
        logger.info("=== CALCULATING MENTOR STATS === Mentor: {}", mentor.getMentorId());

        if (mentor == null) {
            return Collections.emptyMap();
        }

        if (allMatches == null) {
            allMatches = Collections.emptyList();
        }

        List<MentorshipMatch> mentorMatches = allMatches.stream()
                .filter(match -> mentor.getMentorId().equals(match.getMentorId()))
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();

        // Basic statistics
        stats.put("mentorId", mentor.getMentorId());
        stats.put("name", mentor.getName());
        stats.put("university", mentor.getUniversity());
        stats.put("program", mentor.getProgram());
        stats.put("country", mentor.getCountry());
        stats.put("joinedAt", mentor.getJoinedAt());
        stats.put("isActive", mentor.isActive());
        stats.put("bio", mentor.getBio());
        stats.put("contactPreference", mentor.getContactPreference());
        stats.put("availableHelpTypes", mentor.getAvailableHelpTypes());

        // Mentorship statistics
        long totalMentees = mentorMatches.size();
        long activeMentees = mentorMatches.stream()
                .filter(match -> match.getStatus() == MentorshipMatchStatus.ACTIVE)
                .count();
        long completedMentorships = mentorMatches.stream()
                .filter(match -> match.getStatus() == MentorshipMatchStatus.COMPLETED)
                .count();

        stats.put("totalMentees", totalMentees);
        stats.put("activeMentees", activeMentees);
        stats.put("completedMentorships", completedMentorships);
        stats.put("maxMentees", mentor.getMaxMentees());
        stats.put("canTakeNewMentees", activeMentees < mentor.getMaxMentees());

        // Impact metrics with null safety
        int totalSessions = mentorMatches.stream()
                .mapToInt(match -> match.getSessionCount() != null ? match.getSessionCount() : 0)
                .sum();
        stats.put("totalSessions", totalSessions);

        double averageRating = mentorMatches.stream()
                .filter(match -> match.getMenteeRating() != null && match.getMenteeRating() > 0)
                .mapToDouble(MentorshipMatch::getMenteeRating)
                .average()
                .orElse(0.0);
        stats.put("averageRating", Math.round(averageRating * 100.0) / 100.0);

        // Recent activity with null safety
        long recentMatches = mentorMatches.stream()
                .filter(match -> match.getMatchedAt() != null && match.getMatchedAt().isAfter(LocalDateTime.now().minusMonths(3)))
                .count();
        stats.put("recentMatches", recentMatches);

        return stats;
    }

    /**
     * Calculate updated mentor preferences - STATELESS
     */
    public MentorProfile calculateUpdatedMentorPreferences(MentorProfile currentProfile,
                                                           List<HelpType> helpTypes,
                                                           Integer maxMentees,
                                                           String bio,
                                                           List<String> timeSlots,
                                                           boolean active) throws Exception {
        logger.info("=== CALCULATING UPDATED MENTOR PREFERENCES === Mentor: {}", currentProfile.getMentorId());

        if (currentProfile == null) {
            throw new Exception("Current mentor profile is required");
        }

        // Create updated profile
        MentorProfile updatedProfile = copyMentorProfile(currentProfile);

        // Update fields if provided
        if (helpTypes != null) {
            updatedProfile.setAvailableHelpTypes(helpTypes);
        }
        if (maxMentees != null && maxMentees > 0 && maxMentees <= 10) {
            updatedProfile.setMaxMentees(maxMentees);
        }
        if (bio != null && !bio.trim().isEmpty()) {
            updatedProfile.setBio(bio.trim());
        }
        if (timeSlots != null) {
            updatedProfile.setPreferredTimeSlots(timeSlots);
        }
        updatedProfile.setActive(active);
        updatedProfile.setLastUpdated(LocalDateTime.now());

        logger.info("Mentor preferences calculated for: {}", currentProfile.getMentorId());

        return updatedProfile;
    }

    /**
     * Generate mentoring offer data from payment - STATELESS
     */
    public MentoringOfferData generateMentoringOffer(LoanAccount loanAccount, RepaymentTransaction transaction) {
        logger.info("=== GENERATING MENTORING OFFER === Student: {}", loanAccount.getStudentId());

        // Check if student is eligible for mentoring offer
        if (!isEligibleForMentoringOffer(loanAccount, transaction)) {
            logger.info("Student not eligible for mentoring offer: {}", loanAccount.getStudentId());
            return null;
        }

        MentoringOfferData offerData = new MentoringOfferData();
        offerData.setOfferId("OFFER_" + System.currentTimeMillis());
        offerData.setStudentId(loanAccount.getStudentId());
        offerData.setLoanId(loanAccount.getLoanId());
        offerData.setTransactionId(transaction.getTransactionId());
        offerData.setOfferAmount(calculateOfferAmount(transaction));
        offerData.setStatus(MentoringOfferStatus.PENDING);
        offerData.setCreatedAt(LocalDateTime.now());
        offerData.setExpiresAt(LocalDateTime.now().plusDays(30)); // 30 days to respond
        offerData.setOfferType(determineOfferType(loanAccount, transaction));
        offerData.setDescription(generateOfferDescription(loanAccount, transaction));

        logger.info("Mentoring offer generated: {} for student: {}",
                offerData.getOfferId(), loanAccount.getStudentId());

        return offerData;
    }

    /**
     * Calculate mentoring match compatibility - STATELESS
     */
    public double calculateMatchCompatibility(MentorProfile mentor, MenteeProfile mentee) {
        double compatibility = 0.0;

        // Null safety checks
        if (mentor == null || mentee == null) {
            return 0.0;
        }

        // University match (30% weight)
        if (mentor.getUniversity() != null && mentee.getUniversity() != null &&
                mentor.getUniversity().equals(mentee.getUniversity())) {
            compatibility += 0.3;
        }

        // Country match (20% weight)
        if (mentor.getCountry() != null && mentee.getCountry() != null &&
                mentor.getCountry().equals(mentee.getCountry())) {
            compatibility += 0.2;
        }

        // Help type overlap (40% weight)
        if (mentor.getAvailableHelpTypes() != null && mentee.getNeededHelpTypes() != null) {
            Set<HelpType> mentorTypes = new HashSet<>(mentor.getAvailableHelpTypes());
            Set<HelpType> menteeNeeds = new HashSet<>(mentee.getNeededHelpTypes());
            mentorTypes.retainAll(menteeNeeds);
            if (!menteeNeeds.isEmpty()) {
                double helpTypeOverlap = (double) mentorTypes.size() / menteeNeeds.size();
                compatibility += helpTypeOverlap * 0.4;
            }
        }

        // Program similarity (10% weight)
        if (mentor.getProgram() != null && mentee.getProgram() != null) {
            if (mentor.getProgram().toLowerCase().contains(mentee.getProgram().toLowerCase()) ||
                    mentee.getProgram().toLowerCase().contains(mentor.getProgram().toLowerCase())) {
                compatibility += 0.1;
            }
        }

        return Math.min(compatibility, 1.0);
    }

    // Private helper methods

    private String generateEmailFromStudentId(String studentId) {
        // Generate a reasonable email from student ID
        if (studentId == null || studentId.isEmpty()) {
            return "student@university.edu";
        }
        return studentId.toLowerCase() + "@university.edu";
    }

    private MentoringOfferData copyOffer(MentoringOfferData original) {
        MentoringOfferData copy = new MentoringOfferData();
        copy.setOfferId(original.getOfferId());
        copy.setStudentId(original.getStudentId());
        copy.setLoanId(original.getLoanId());
        copy.setTransactionId(original.getTransactionId());
        copy.setOfferAmount(original.getOfferAmount());
        copy.setStatus(original.getStatus());
        copy.setCreatedAt(original.getCreatedAt());
        copy.setExpiresAt(original.getExpiresAt());
        copy.setRespondedAt(original.getRespondedAt());
        copy.setOfferType(original.getOfferType());
        copy.setDescription(original.getDescription());
        copy.setMentorId(original.getMentorId());
        copy.setDeclineReason(original.getDeclineReason());
        return copy;
    }

    private MentorProfile copyMentorProfile(MentorProfile original) {
        MentorProfile copy = new MentorProfile();
        copy.setMentorId(original.getMentorId());
        copy.setStudentId(original.getStudentId());
        copy.setName(original.getName());
        copy.setEmail(original.getEmail());
        copy.setAvailableHelpTypes(original.getAvailableHelpTypes() != null ?
                new ArrayList<>(original.getAvailableHelpTypes()) : null);
        copy.setBio(original.getBio());
        copy.setContactPreference(original.getContactPreference());
        copy.setMaxMentees(original.getMaxMentees());
        copy.setCurrentMentees(original.getCurrentMentees());
        copy.setActive(original.isActive());
        copy.setJoinedAt(original.getJoinedAt());
        copy.setLastUpdated(original.getLastUpdated());
        copy.setUniversity(original.getUniversity());
        copy.setProgram(original.getProgram());
        copy.setCountry(original.getCountry());
        copy.setPreferredTimeSlots(original.getPreferredTimeSlots() != null ?
                new ArrayList<>(original.getPreferredTimeSlots()) : null);
        return copy;
    }

    private boolean isCompatibleMatch(MentorProfile mentor, MenteeProfile mentee, List<HelpType> helpTypes) {
        // Check if mentor can provide help types that mentee needs
        List<HelpType> effectiveHelpTypes = helpTypes != null ? helpTypes : mentor.getAvailableHelpTypes();
        if (effectiveHelpTypes == null || mentee.getNeededHelpTypes() == null) {
            return false;
        }
        return mentee.getNeededHelpTypes().stream()
                .anyMatch(effectiveHelpTypes::contains);
    }

    private int prioritizeMentees(MenteeProfile a, MenteeProfile b) {
        // Prioritize by urgency, then by creation date
        if (a.getUrgencyLevel() != null && b.getUrgencyLevel() != null) {
            int urgencyCompare = Integer.compare(b.getUrgencyLevel(), a.getUrgencyLevel());
            if (urgencyCompare != 0) return urgencyCompare;
        }

        if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        }

        return 0;
    }

    private boolean isEligibleForMentoringOffer(LoanAccount loanAccount, RepaymentTransaction transaction) {
        // Student is eligible if they've made a payment and are in good standing
        // Fixed: Use correct enum value - COMPLETED for successful payments
        return transaction != null &&
                transaction.getStatus() == PaymentStatus.COMPLETED &&
                loanAccount.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0 &&
                loanAccount.getLoanStatus() == LoanStatus.ACTIVE;
    }

    private BigDecimal calculateOfferAmount(RepaymentTransaction transaction) {
        // Offer 5% of payment amount as volunteer incentive
        return transaction.getAmount().multiply(new BigDecimal("0.05"));
    }

    private String determineOfferType(LoanAccount loanAccount, RepaymentTransaction transaction) {
        if (transaction.getAmount().compareTo(new BigDecimal("5000")) >= 0) {
            return "PREMIUM_MENTORING";
        } else {
            return "STANDARD_MENTORING";
        }
    }

    private String generateOfferDescription(LoanAccount loanAccount, RepaymentTransaction transaction) {
        return String.format(
                "Thank you for your payment of %s! You're invited to become a mentor and help fellow Indonesian students. " +
                        "Share your experience and make a positive impact while earning volunteer credits.",
                transaction.getAmount()
        );
    }

    // Result classes for stateless operations

    public static class MentorProfileResult {
        private final MentorProfile mentorProfile;
        private final MentoringOfferData updatedOffer;
        private final RepaymentTransaction updatedTransaction;

        public MentorProfileResult(MentorProfile mentorProfile, MentoringOfferData updatedOffer, RepaymentTransaction updatedTransaction) {
            this.mentorProfile = mentorProfile;
            this.updatedOffer = updatedOffer;
            this.updatedTransaction = updatedTransaction;
        }

        public MentorProfile getMentorProfile() { return mentorProfile; }
        public MentoringOfferData getUpdatedOffer() { return updatedOffer; }
        public RepaymentTransaction getUpdatedTransaction() { return updatedTransaction; }
    }

    public static class MentoringDeclineResult {
        private final MentoringOfferData updatedOffer;
        private final RepaymentTransaction updatedTransaction;

        public MentoringDeclineResult(MentoringOfferData updatedOffer, RepaymentTransaction updatedTransaction) {
            this.updatedOffer = updatedOffer;
            this.updatedTransaction = updatedTransaction;
        }

        public MentoringOfferData getUpdatedOffer() { return updatedOffer; }
        public RepaymentTransaction getUpdatedTransaction() { return updatedTransaction; }
    }

    // Data classes for mentoring system

    public static class MentoringOfferData {
        private String offerId;
        private String studentId;
        private String loanId;
        private String transactionId;
        private BigDecimal offerAmount;
        private MentoringOfferStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private LocalDateTime respondedAt;
        private String offerType;
        private String description;
        private String mentorId;
        private String declineReason;

        // Default constructor
        public MentoringOfferData() {}

        // Getters and setters
        public String getOfferId() { return offerId; }
        public void setOfferId(String offerId) { this.offerId = offerId; }
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getLoanId() { return loanId; }
        public void setLoanId(String loanId) { this.loanId = loanId; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public BigDecimal getOfferAmount() { return offerAmount; }
        public void setOfferAmount(BigDecimal offerAmount) { this.offerAmount = offerAmount; }
        public MentoringOfferStatus getStatus() { return status; }
        public void setStatus(MentoringOfferStatus status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
        public LocalDateTime getRespondedAt() { return respondedAt; }
        public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
        public String getOfferType() { return offerType; }
        public void setOfferType(String offerType) { this.offerType = offerType; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getMentorId() { return mentorId; }
        public void setMentorId(String mentorId) { this.mentorId = mentorId; }
        public String getDeclineReason() { return declineReason; }
        public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }
    }
}