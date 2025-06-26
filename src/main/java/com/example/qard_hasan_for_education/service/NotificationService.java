package com.example.qard_hasan_for_education.service;

import com.example.qard_hasan_for_education.model.DisbursementTransaction;
import com.example.qard_hasan_for_education.model.RepaymentTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Generate mentoring offer notification data - STATELESS
     */
    public NotificationData generateMentoringOfferNotification(String studentId, RepaymentTransaction transaction) {
        try {
            NotificationData notification = new NotificationData(
                    studentId,
                    "Mentoring Opportunity Available",
                    String.format("Congratulations on your payment! You can now help other Indonesian students by becoming a mentor. " +
                            "Your experience studying abroad can make a real difference. Would you like to volunteer as a mentor?"),
                    NotificationType.MENTORING_OFFER,
                    createMentoringOfferActions(transaction.getTransactionId())
            );

            logger.info("Mentoring offer notification data generated for student: {}", studentId);
            return notification;

        } catch (Exception e) {
            logger.error("Error generating mentoring offer notification data for student: {}", studentId, e);
            return null;
        }
    }

    /**
     * Generate mentorship match notification data - STATELESS
     */
    public List<NotificationData> generateMentorshipMatchNotifications(String mentorId, String menteeId, String matchId) {
        try {
            List<NotificationData> notifications = new ArrayList<>();

            // Notification to mentor
            NotificationData mentorNotification = new NotificationData(
                    mentorId,
                    "New Mentee Matched",
                    "You've been matched with a new mentee who needs your guidance. Check your mentorship dashboard to connect with them.",
                    NotificationType.MENTORSHIP_MATCH,
                    createMentorshipActions(matchId, true)
            );
            notifications.add(mentorNotification);

            // Notification to mentee
            NotificationData menteeNotification = new NotificationData(
                    menteeId,
                    "Mentor Found",
                    "Great news! We've found a mentor who can help you. They're ready to share their experience and support your journey.",
                    NotificationType.MENTORSHIP_MATCH,
                    createMentorshipActions(matchId, false)
            );
            notifications.add(menteeNotification);

            logger.info("Mentorship match notification data generated for match: {}", matchId);
            return notifications;

        } catch (Exception e) {
            logger.error("Error generating mentorship match notification data for match: {}", matchId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Generate mentorship completion notification data - STATELESS
     */
    public List<NotificationData> generateMentorshipCompletionNotifications(String mentorId, String menteeId, String matchId) {
        try {
            List<NotificationData> notifications = new ArrayList<>();

            // Notification to mentor
            NotificationData mentorNotification = new NotificationData(
                    mentorId,
                    "Mentorship Completed",
                    "Your mentorship has been completed. Thank you for making a difference in someone's educational journey!",
                    NotificationType.MENTORSHIP_COMPLETION,
                    Collections.singletonList(new NotificationAction("VIEW_STATS", "View Your Impact", "/mentor/stats"))
            );
            notifications.add(mentorNotification);

            // Notification to mentee
            NotificationData menteeNotification = new NotificationData(
                    menteeId,
                    "Mentorship Completed",
                    "Your mentorship has been completed. We hope it was helpful! Please consider rating your mentor.",
                    NotificationType.MENTORSHIP_COMPLETION,
                    Arrays.asList(
                            new NotificationAction("RATE_MENTOR", "Rate Mentor", "/mentorship/rate/" + matchId),
                            new NotificationAction("FIND_NEW_MENTOR", "Find Another Mentor", "/mentee/search")
                    )
            );
            notifications.add(menteeNotification);

            logger.info("Mentorship completion notification data generated for match: {}", matchId);
            return notifications;

        } catch (Exception e) {
            logger.error("Error generating mentorship completion notification data for match: {}", matchId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Generate payment reminder notification data - STATELESS
     */
    public NotificationData generatePaymentReminderNotification(String studentId, String loanId, String amount, String dueDate) {
        try {
            NotificationData notification = new NotificationData(
                    studentId,
                    "Payment Reminder",
                    String.format("Your loan payment of %s is due on %s. Don't forget that after each payment, " +
                            "you'll have the opportunity to help fellow Indonesian students!", amount, dueDate),
                    NotificationType.PAYMENT_REMINDER,
                    Arrays.asList(
                            new NotificationAction("MAKE_PAYMENT", "Make Payment", "/loan/payment/" + loanId),
                            new NotificationAction("VIEW_SCHEDULE", "View Schedule", "/loan/schedule/" + loanId)
                    )
            );

            logger.info("Payment reminder notification data generated for student: {}", studentId);
            return notification;

        } catch (Exception e) {
            logger.error("Error generating payment reminder notification data for student: {}", studentId, e);
            return null;
        }
    }

    /**
     * Generate welcome notification data for new mentors - STATELESS
     */
    public NotificationData generateMentorWelcomeNotification(String mentorId, String mentorName) {
        try {
            NotificationData notification = new NotificationData(
                    mentorId,
                    "Welcome to the Mentorship Program",
                    String.format("Welcome %s! Thank you for joining our mentorship program. " +
                            "Your experience and guidance will help Indonesian students succeed in their studies abroad. " +
                            "We'll notify you when mentees who match your expertise need help.", mentorName),
                    NotificationType.MENTOR_WELCOME,
                    Arrays.asList(
                            new NotificationAction("VIEW_PROFILE", "View Profile", "/mentor/profile"),
                            new NotificationAction("FIND_MENTEES", "Find Mentees", "/mentor/mentees"),
                            new NotificationAction("MENTOR_GUIDE", "Mentoring Guide", "/mentor/guide")
                    )
            );

            logger.info("Mentor welcome notification data generated for: {}", mentorId);
            return notification;

        } catch (Exception e) {
            logger.error("Error generating mentor welcome notification data for: {}", mentorId, e);
            return null;
        }
    }

    /**
     * Generate disbursement initiated notification data - STATELESS
     */
    public NotificationData generateDisbursementInitiatedNotification(String studentId, DisbursementTransaction disbursement) {
        try {
            logger.info("=== GENERATING DISBURSEMENT INITIATED NOTIFICATION DATA === StudentID: {}", studentId);

            NotificationData notification = new NotificationData(
                    studentId,
                    "Fund Disbursement Initiated",
                    String.format("Great news! Your loan disbursement of %s has been initiated. " +
                                    "Funds will be sent to your account ending in %s via %s. " +
                                    "Expected delivery: %s. Reference: %s",
                            formatCurrency(disbursement.getNetAmount()),
                            maskAccountNumber(disbursement.getStudentBankAccount()),
                            disbursement.getDisbursementMethod().getDescription(),
                            formatDateTime(disbursement.getEstimatedDelivery()),
                            disbursement.getReferenceNumber()),
                    NotificationType.DISBURSEMENT_INITIATED,
                    createDisbursementActions(disbursement.getDisbursementId(), "initiated")
            );

            logger.info("Disbursement initiated notification data generated for student: {}", studentId);
            return notification;

        } catch (Exception e) {
            logger.error("Error generating disbursement initiated notification data for student: {}", studentId, e);
            return null;
        }
    }

    /**
     * Generate disbursement processing notification data - STATELESS
     */
    public NotificationData generateDisbursementProcessingNotification(String studentId, DisbursementTransaction disbursement) {
        try {
            logger.info("=== GENERATING DISBURSEMENT PROCESSING NOTIFICATION DATA === StudentID: {}", studentId);

            NotificationData notification = new NotificationData(
                    studentId,
                    "Funds Being Processed",
                    String.format("Your disbursement is now being processed by the bank. " +
                                    "Amount: %s | Tracking: %s | " +
                                    "You'll receive another update once the funds are sent to your account.",
                            formatCurrency(disbursement.getNetAmount()),
                            disbursement.getTrackingNumber()),
                    NotificationType.DISBURSEMENT_PROCESSING,
                    createDisbursementActions(disbursement.getDisbursementId(), "processing")
            );

            logger.info("Disbursement processing notification data generated for student: {}", studentId);
            return notification;

        } catch (Exception e) {
            logger.error("Error generating disbursement processing notification data for student: {}", studentId, e);
            return null;
        }
    }

    /**
     * Generate disbursement completed notification data - STATELESS
     */
    public NotificationData generateDisbursementCompletedNotification(String studentId, DisbursementTransaction disbursement) {
        try {
            logger.info("=== GENERATING DISBURSEMENT COMPLETED NOTIFICATION DATA === StudentID: {}", studentId);

            NotificationData notification = new NotificationData(
                    studentId,
                    "Funds Successfully Sent! 🎉",
                    String.format("Congratulations! Your loan funds of %s have been successfully sent to your account. " +
                                    "Transaction completed at %s. Reference: %s. " +
                                    "Please check your bank account and confirm receipt. " +
                                    "Your first repayment will be due next month.",
                            formatCurrency(disbursement.getNetAmount()),
                            formatDateTime(disbursement.getCompletedAt()),
                            disbursement.getReferenceNumber()),
                    NotificationType.DISBURSEMENT_COMPLETED,
                    createDisbursementActions(disbursement.getDisbursementId(), "completed")
            );

            logger.info("Disbursement completed notification data generated for student: {}", studentId);
            return notification;

        } catch (Exception e) {
            logger.error("Error generating disbursement completed notification data for student: {}", studentId, e);
            return null;
        }
    }

    /**
     * Generate disbursement failed notification data - STATELESS
     */
    public NotificationData generateDisbursementFailedNotification(String studentId, DisbursementTransaction disbursement, String reason) {
        try {
            logger.info("=== GENERATING DISBURSEMENT FAILED NOTIFICATION DATA === StudentID: {}", studentId);

            NotificationData notification = new NotificationData(
                    studentId,
                    "Disbursement Issue - Action Required",
                    String.format("There was an issue with your fund disbursement. " +
                                    "Amount: %s | Reason: %s | " +
                                    "Please contact our support team immediately to resolve this issue. " +
                                    "Reference: %s",
                            formatCurrency(disbursement.getAmount()),
                            reason,
                            disbursement.getReferenceNumber()),
                    NotificationType.DISBURSEMENT_FAILED,
                    Arrays.asList(
                            new NotificationAction("CONTACT_SUPPORT", "Contact Support", "/support/contact"),
                            new NotificationAction("VIEW_DISBURSEMENT", "View Details", "/disbursement/" + disbursement.getDisbursementId()),
                            new NotificationAction("RETRY_DISBURSEMENT", "Retry Disbursement", "/disbursement/retry/" + disbursement.getDisbursementId())
                    )
            );

            logger.info("Disbursement failed notification data generated for student: {}", studentId);
            return notification;

        } catch (Exception e) {
            logger.error("Error generating disbursement failed notification data for student: {}", studentId, e);
            return null;
        }
    }

    /**
     * Calculate notification statistics - STATELESS
     */
    public Map<String, Object> calculateNotificationStats(List<NotificationData> notifications) {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", notifications.size());
            stats.put("unread", notifications.stream().filter(n -> !n.isRead()).count());
            stats.put("byType", notifications.stream()
                    .collect(Collectors.groupingBy(
                            n -> n.getType().toString(),
                            Collectors.counting())));

            return stats;
        } catch (Exception e) {
            logger.error("Error calculating notification statistics", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Mark notification as read - STATELESS (returns updated notification)
     */
    public NotificationData markNotificationAsRead(NotificationData notification) {
        if (notification == null || notification.isRead()) {
            return notification;
        }

        // Create updated notification (don't mutate original)
        NotificationData updatedNotification = copyNotification(notification);
        updatedNotification.setRead(true);
        updatedNotification.setReadAt(LocalDateTime.now());

        logger.debug("Notification marked as read: {}", notification.getNotificationId());
        return updatedNotification;
    }

    /**
     * Mark all notifications as read - STATELESS (returns updated list)
     */
    public List<NotificationData> markAllNotificationsAsRead(List<NotificationData> notifications) {
        return notifications.stream()
                .map(this::markNotificationAsRead)
                .collect(Collectors.toList());
    }

    // Private helper methods

    private NotificationData copyNotification(NotificationData original) {
        NotificationData copy = new NotificationData();
        copy.setNotificationId(original.getNotificationId());
        copy.setUserId(original.getUserId());
        copy.setTitle(original.getTitle());
        copy.setMessage(original.getMessage());
        copy.setType(original.getType());
        copy.setActions(new ArrayList<>(original.getActions()));
        copy.setRead(original.isRead());
        copy.setCreatedAt(original.getCreatedAt());
        copy.setReadAt(original.getReadAt());
        return copy;
    }

    private List<NotificationAction> createMentoringOfferActions(String transactionId) {
        return Arrays.asList(
                new NotificationAction("ACCEPT_MENTORING", "Yes, I'd like to mentor", "/mentoring/accept/" + transactionId),
                new NotificationAction("DECLINE_MENTORING", "Not right now", "/mentoring/decline/" + transactionId),
                new NotificationAction("LEARN_MORE", "Learn More", "/mentoring/info")
        );
    }

    private List<NotificationAction> createMentorshipActions(String matchId, boolean isMentor) {
        if (isMentor) {
            return Arrays.asList(
                    new NotificationAction("CONTACT_MENTEE", "Contact Mentee", "/mentorship/contact/" + matchId),
                    new NotificationAction("VIEW_MATCH", "View Match Details", "/mentorship/match/" + matchId)
            );
        } else {
            return Arrays.asList(
                    new NotificationAction("CONTACT_MENTOR", "Contact Mentor", "/mentorship/contact/" + matchId),
                    new NotificationAction("VIEW_MATCH", "View Match Details", "/mentorship/match/" + matchId)
            );
        }
    }

    // Helper methods for disbursement notifications
    private List<NotificationAction> createDisbursementActions(String disbursementId, String stage) {
        switch (stage) {
            case "initiated":
                return Arrays.asList(
                        new NotificationAction("VIEW_DISBURSEMENT", "View Details", "/disbursement/" + disbursementId),
                        new NotificationAction("TRACK_DISBURSEMENT", "Track Progress", "/disbursement/track/" + disbursementId),
                        new NotificationAction("CONTACT_SUPPORT", "Contact Support", "/support/contact")
                );
            case "processing":
                return Arrays.asList(
                        new NotificationAction("TRACK_DISBURSEMENT", "Track Progress", "/disbursement/track/" + disbursementId),
                        new NotificationAction("VIEW_DETAILS", "View Details", "/disbursement/" + disbursementId)
                );
            case "completed":
                return Arrays.asList(
                        new NotificationAction("VIEW_DISBURSEMENT", "View Details", "/disbursement/" + disbursementId),
                        new NotificationAction("VIEW_LOAN", "View Loan Details", "/loan/dashboard"),
                        new NotificationAction("PAYMENT_SCHEDULE", "View Payment Schedule", "/loan/schedule")
                );
            default:
                return Arrays.asList(
                        new NotificationAction("VIEW_DISBURSEMENT", "View Details", "/disbursement/" + disbursementId)
                );
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return String.format("$%,.2f", amount);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "TBD";
        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    // Data classes for stateless notifications

    public static class NotificationData {
        private String notificationId;
        private String userId;
        private String title;
        private String message;
        private NotificationType type;
        private List<NotificationAction> actions;
        private boolean read;
        private LocalDateTime createdAt;
        private LocalDateTime readAt;

        public NotificationData() {}

        public NotificationData(String userId, String title, String message, NotificationType type, List<NotificationAction> actions) {
            this.notificationId = UUID.randomUUID().toString();
            this.userId = userId;
            this.title = title;
            this.message = message;
            this.type = type;
            this.actions = actions != null ? actions : Collections.emptyList();
            this.read = false;
            this.createdAt = LocalDateTime.now();
        }

        // Getters and setters
        public String getNotificationId() { return notificationId; }
        public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public NotificationType getType() { return type; }
        public void setType(NotificationType type) { this.type = type; }

        public List<NotificationAction> getActions() { return actions; }
        public void setActions(List<NotificationAction> actions) { this.actions = actions; }

        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getReadAt() { return readAt; }
        public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    }

    public static class NotificationAction {
        private String actionId;
        private String label;
        private String url;

        public NotificationAction() {}

        public NotificationAction(String actionId, String label, String url) {
            this.actionId = actionId;
            this.label = label;
            this.url = url;
        }

        // Getters and setters
        public String getActionId() { return actionId; }
        public void setActionId(String actionId) { this.actionId = actionId; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public enum NotificationType {
        MENTORING_OFFER,
        MENTORSHIP_MATCH,
        MENTORSHIP_COMPLETION,
        PAYMENT_REMINDER,
        MENTOR_WELCOME,
        SYSTEM_ANNOUNCEMENT,
        DISBURSEMENT_INITIATED,
        DISBURSEMENT_PROCESSING,
        DISBURSEMENT_COMPLETED,
        DISBURSEMENT_FAILED
    }
}