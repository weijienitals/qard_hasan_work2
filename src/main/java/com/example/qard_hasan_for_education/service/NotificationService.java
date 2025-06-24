package com.example.qard_hasan_for_education.service;

import com.example.qard_hasan_for_education.model.RepaymentTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Send mentoring offer notification after successful payment
     */
    public void sendMentoringOfferNotification(String studentId, RepaymentTransaction transaction) {
        try {
            Notification notification = new Notification(
                    studentId,
                    "Mentoring Opportunity Available",
                    String.format("Congratulations on your payment! You can now help other Indonesian students by becoming a mentor. " +
                            "Your experience studying abroad can make a real difference. Would you like to volunteer as a mentor?"),
                    NotificationType.MENTORING_OFFER,
                    createMentoringOfferActions(transaction.getTransactionId())
            );

            storeNotification(notification);

            // In a real implementation, you would also:
            // - Send email notification
            // - Send push notification via mobile app
            // - Send SMS if configured

            logger.info("Mentoring offer notification sent to student: {}", studentId);

        } catch (Exception e) {
            logger.error("Error sending mentoring offer notification to student: {}", studentId, e);
        }
    }

    /**
     * Send notification when mentor and mentee are matched
     */
    public void sendMentorshipMatchNotification(String mentorId, String menteeId, String matchId) {
        try {
            // Notification to mentor
            Notification mentorNotification = new Notification(
                    mentorId,
                    "New Mentee Matched",
                    "You've been matched with a new mentee who needs your guidance. Check your mentorship dashboard to connect with them.",
                    NotificationType.MENTORSHIP_MATCH,
                    createMentorshipActions(matchId, true)
            );
            storeNotification(mentorNotification);

            // Notification to mentee
            Notification menteeNotification = new Notification(
                    menteeId,
                    "Mentor Found",
                    "Great news! We've found a mentor who can help you. They're ready to share their experience and support your journey.",
                    NotificationType.MENTORSHIP_MATCH,
                    createMentorshipActions(matchId, false)
            );
            storeNotification(menteeNotification);

            logger.info("Mentorship match notifications sent for match: {}", matchId);

        } catch (Exception e) {
            logger.error("Error sending mentorship match notifications for match: {}", matchId, e);
        }
    }

    /**
     * Send notification when mentorship is completed
     */
    public void sendMentorshipCompletionNotification(String mentorId, String menteeId, String matchId) {
        try {
            // Notification to mentor
            Notification mentorNotification = new Notification(
                    mentorId,
                    "Mentorship Completed",
                    "Your mentorship has been completed. Thank you for making a difference in someone's educational journey!",
                    NotificationType.MENTORSHIP_COMPLETION,
                    Collections.singletonList(new NotificationAction("VIEW_STATS", "View Your Impact", "/mentor/stats"))
            );
            storeNotification(mentorNotification);

            // Notification to mentee
            Notification menteeNotification = new Notification(
                    menteeId,
                    "Mentorship Completed",
                    "Your mentorship has been completed. We hope it was helpful! Please consider rating your mentor.",
                    NotificationType.MENTORSHIP_COMPLETION,
                    Arrays.asList(
                            new NotificationAction("RATE_MENTOR", "Rate Mentor", "/mentorship/rate/" + matchId),
                            new NotificationAction("FIND_NEW_MENTOR", "Find Another Mentor", "/mentee/search")
                    )
            );
            storeNotification(menteeNotification);

            logger.info("Mentorship completion notifications sent for match: {}", matchId);

        } catch (Exception e) {
            logger.error("Error sending mentorship completion notifications for match: {}", matchId, e);
        }
    }

    /**
     * Send payment reminder notification
     */
    public void sendPaymentReminderNotification(String studentId, String loanId, String amount, String dueDate) {
        try {
            Notification notification = new Notification(
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

            storeNotification(notification);
            logger.info("Payment reminder notification sent to student: {}", studentId);

        } catch (Exception e) {
            logger.error("Error sending payment reminder notification to student: {}", studentId, e);
        }
    }

    /**
     * Send welcome notification for new mentors
     */
    public void sendMentorWelcomeNotification(String mentorId, String mentorName) {
        try {
            Notification notification = new Notification(
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

            storeNotification(notification);
            logger.info("Mentor welcome notification sent to: {}", mentorId);

        } catch (Exception e) {
            logger.error("Error sending mentor welcome notification to: {}", mentorId, e);
        }
    }

    /**
     * Get notifications for a user
     */
    public List<Notification> getUserNotifications(String userId, boolean unreadOnly) {
        try {
            Set<String> keys = redisTemplate.keys("notification:" + userId + ":*");
            if (keys == null) return Collections.emptyList();

            return keys.stream()
                    .filter(Objects::nonNull)
                    .map(key -> (Notification) redisTemplate.opsForValue().get(key))
                    .filter(Objects::nonNull)
                    .filter(notification -> !unreadOnly || !notification.isRead())
                    .sorted((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()))
                    .limit(50) // Return max 50 notifications
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving notifications for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Mark notification as read
     */
    public void markNotificationAsRead(String notificationId, String userId) {
        try {
            Notification notification = (Notification) redisTemplate.opsForValue()
                    .get("notification:" + userId + ":" + notificationId);

            if (notification != null && !notification.isRead()) {
                notification.setRead(true);
                notification.setReadAt(LocalDateTime.now());
                storeNotification(notification);
                logger.debug("Notification marked as read: {}", notificationId);
            }
        } catch (Exception e) {
            logger.error("Error marking notification as read: {}", notificationId, e);
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllNotificationsAsRead(String userId) {
        try {
            List<Notification> unreadNotifications = getUserNotifications(userId, true);

            for (Notification notification : unreadNotifications) {
                notification.setRead(true);
                notification.setReadAt(LocalDateTime.now());
                storeNotification(notification);
            }

            logger.info("Marked {} notifications as read for user: {}", unreadNotifications.size(), userId);
        } catch (Exception e) {
            logger.error("Error marking all notifications as read for user: {}", userId, e);
        }
    }

    /**
     * Get notification statistics for a user
     */
    public Map<String, Object> getNotificationStats(String userId) {
        try {
            List<Notification> allNotifications = getUserNotifications(userId, false);

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", allNotifications.size());
            stats.put("unread", allNotifications.stream().filter(n -> !n.isRead()).count());
            stats.put("byType", allNotifications.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            n -> n.getType().toString(),
                            java.util.stream.Collectors.counting())));

            return stats;
        } catch (Exception e) {
            logger.error("Error calculating notification stats for user: {}", userId, e);
            return Collections.emptyMap();
        }
    }

    // Private helper methods

    private void storeNotification(Notification notification) {
        try {
            String key = "notification:" + notification.getUserId() + ":" + notification.getNotificationId();
            redisTemplate.opsForValue().set(key, notification, Duration.ofDays(30));
        } catch (Exception e) {
            logger.error("Error storing notification: {}", notification.getNotificationId(), e);
        }
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

    // Inner classes for notification system

    public static class Notification {
        private String notificationId;
        private String userId;
        private String title;
        private String message;
        private NotificationType type;
        private List<NotificationAction> actions;
        private boolean read;
        private LocalDateTime createdAt;
        private LocalDateTime readAt;

        public Notification() {}

        public Notification(String userId, String title, String message, NotificationType type, List<NotificationAction> actions) {
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


        /**
         * Send notification when disbursement is initiated
         */
        public void sendDisbursementInitiatedNotification(String studentId, DisbursementTransaction disbursement) {
            try {
                logger.info("=== SENDING DISBURSEMENT INITIATED NOTIFICATION === StudentID: {}", studentId);

                Notification notification = new Notification(
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

                storeNotification(notification);
                logger.info("Disbursement initiated notification sent to student: {}", studentId);

            } catch (Exception e) {
                logger.error("Error sending disbursement initiated notification to student: {}", studentId, e);
            }
        }

        /**
         * Send notification when disbursement is being processed
         */
        public void sendDisbursementProcessingNotification(String studentId, DisbursementTransaction disbursement) {
            try {
                logger.info("=== SENDING DISBURSEMENT PROCESSING NOTIFICATION === StudentID: {}", studentId);

                Notification notification = new Notification(
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

                storeNotification(notification);
                logger.info("Disbursement processing notification sent to student: {}", studentId);

            } catch (Exception e) {
                logger.error("Error sending disbursement processing notification to student: {}", studentId, e);
            }
        }

        /**
         * Send notification when disbursement is completed
         */
        public void sendDisbursementCompletedNotification(String studentId, DisbursementTransaction disbursement) {
            try {
                logger.info("=== SENDING DISBURSEMENT COMPLETED NOTIFICATION === StudentID: {}", studentId);

                Notification notification = new Notification(
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

                storeNotification(notification);
                logger.info("Disbursement completed notification sent to student: {}", studentId);

            } catch (Exception e) {
                logger.error("Error sending disbursement completed notification to student: {}", studentId, e);
            }
        }

        /**
         * Send notification when disbursement fails
         */
        public void sendDisbursementFailedNotification(String studentId, DisbursementTransaction disbursement, String reason) {
            try {
                logger.info("=== SENDING DISBURSEMENT FAILED NOTIFICATION === StudentID: {}", studentId);

                Notification notification = new Notification(
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

                storeNotification(notification);
                logger.info("Disbursement failed notification sent to student: {}", studentId);

            } catch (Exception e) {
                logger.error("Error sending disbursement failed notification to student: {}", studentId, e);
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


