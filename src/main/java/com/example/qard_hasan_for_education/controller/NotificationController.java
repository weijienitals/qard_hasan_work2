package com.example.qard_hasan_for_education.controller;

import com.example.qard_hasan_for_education.service.NotificationService;
import com.example.qard_hasan_for_education.model.DisbursementTransaction;
import com.example.qard_hasan_for_education.model.RepaymentTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    /**
     * Generate mentoring offer notification - STATELESS
     */
    @PostMapping("/generate-mentoring-offer")
    public ResponseEntity<?> generateMentoringOfferNotification(@RequestBody GenerateMentoringOfferNotificationRequest request) {
        try {
            if (request.getStudentId() == null || request.getStudentId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Student ID is required"));
            }
            if (request.getTransaction() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Transaction data is required"));
            }

            NotificationService.NotificationData notification = notificationService.generateMentoringOfferNotification(
                    request.getStudentId(),
                    request.getTransaction()
            );

            if (notification == null) {
                return ResponseEntity.status(500)
                        .body(Map.of("success", false, "message", "Failed to generate notification"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mentoring offer notification generated");
            response.put("notification", notification);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating mentoring offer notification", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error generating notification"));
        }
    }

    /**
     * Generate mentorship match notifications - STATELESS
     */
    @PostMapping("/generate-mentorship-match")
    public ResponseEntity<?> generateMentorshipMatchNotifications(@RequestBody GenerateMentorshipMatchRequest request) {
        try {
            if (request.getMentorId() == null || request.getMentorId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mentor ID is required"));
            }
            if (request.getMenteeId() == null || request.getMenteeId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mentee ID is required"));
            }
            if (request.getMatchId() == null || request.getMatchId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Match ID is required"));
            }

            List<NotificationService.NotificationData> notifications = notificationService.generateMentorshipMatchNotifications(
                    request.getMentorId(),
                    request.getMenteeId(),
                    request.getMatchId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mentorship match notifications generated");
            response.put("notifications", notifications);
            response.put("count", notifications.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating mentorship match notifications", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error generating notifications"));
        }
    }

    /**
     * Generate mentorship completion notifications - STATELESS
     */
    @PostMapping("/generate-mentorship-completion")
    public ResponseEntity<?> generateMentorshipCompletionNotifications(@RequestBody GenerateMentorshipCompletionRequest request) {
        try {
            if (request.getMentorId() == null || request.getMentorId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mentor ID is required"));
            }
            if (request.getMenteeId() == null || request.getMenteeId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mentee ID is required"));
            }
            if (request.getMatchId() == null || request.getMatchId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Match ID is required"));
            }

            List<NotificationService.NotificationData> notifications = notificationService.generateMentorshipCompletionNotifications(
                    request.getMentorId(),
                    request.getMenteeId(),
                    request.getMatchId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mentorship completion notifications generated");
            response.put("notifications", notifications);
            response.put("count", notifications.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating mentorship completion notifications", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error generating notifications"));
        }
    }

    /**
     * Generate payment reminder notification - STATELESS
     */
    @PostMapping("/generate-payment-reminder")
    public ResponseEntity<?> generatePaymentReminderNotification(@RequestBody GeneratePaymentReminderRequest request) {
        try {
            if (request.getStudentId() == null || request.getStudentId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Student ID is required"));
            }
            if (request.getLoanId() == null || request.getLoanId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Loan ID is required"));
            }
            if (request.getAmount() == null || request.getAmount().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Amount is required"));
            }
            if (request.getDueDate() == null || request.getDueDate().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Due date is required"));
            }

            NotificationService.NotificationData notification = notificationService.generatePaymentReminderNotification(
                    request.getStudentId(),
                    request.getLoanId(),
                    request.getAmount(),
                    request.getDueDate()
            );

            if (notification == null) {
                return ResponseEntity.status(500)
                        .body(Map.of("success", false, "message", "Failed to generate notification"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment reminder notification generated");
            response.put("notification", notification);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating payment reminder notification", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error generating notification"));
        }
    }

    /**
     * Generate mentor welcome notification - STATELESS
     */
    @PostMapping("/generate-mentor-welcome")
    public ResponseEntity<?> generateMentorWelcomeNotification(@RequestBody GenerateMentorWelcomeRequest request) {
        try {
            if (request.getMentorId() == null || request.getMentorId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mentor ID is required"));
            }
            if (request.getMentorName() == null || request.getMentorName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mentor name is required"));
            }

            NotificationService.NotificationData notification = notificationService.generateMentorWelcomeNotification(
                    request.getMentorId(),
                    request.getMentorName()
            );

            if (notification == null) {
                return ResponseEntity.status(500)
                        .body(Map.of("success", false, "message", "Failed to generate notification"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mentor welcome notification generated");
            response.put("notification", notification);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating mentor welcome notification", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error generating notification"));
        }
    }

    /**
     * Generate disbursement initiated notification - STATELESS
     */
    @PostMapping("/generate-disbursement-initiated")
    public ResponseEntity<?> generateDisbursementInitiatedNotification(@RequestBody GenerateDisbursementNotificationRequest request) {
        try {
            if (request.getStudentId() == null || request.getStudentId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Student ID is required"));
            }
            if (request.getDisbursement() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Disbursement data is required"));
            }

            NotificationService.NotificationData notification = notificationService.generateDisbursementInitiatedNotification(
                    request.getStudentId(),
                    request.getDisbursement()
            );

            if (notification == null) {
                return ResponseEntity.status(500)
                        .body(Map.of("success", false, "message", "Failed to generate notification"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement initiated notification generated");
            response.put("notification", notification);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating disbursement initiated notification", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error generating notification"));
        }
    }

    /**
     * Generate disbursement processing notification - STATELESS
     */
    @PostMapping("/generate-disbursement-processing")
    public ResponseEntity<?> generateDisbursementProcessingNotification(@RequestBody GenerateDisbursementNotificationRequest request) {
        try {
            if (request.getStudentId() == null || request.getStudentId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Student ID is required"));
            }
            if (request.getDisbursement() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Disbursement data is required"));
            }

            NotificationService.NotificationData notification = notificationService.generateDisbursementProcessingNotification(
                    request.getStudentId(),
                    request.getDisbursement()
            );

            if (notification == null) {
                return ResponseEntity.status(500)
                        .body(Map.of("success", false, "message", "Failed to generate notification"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement processing notification generated");
            response.put("notification", notification);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating disbursement processing notification", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error generating notification"));
        }
    }

    /**
     * Generate disbursement completed notification - STATELESS
     */
    @PostMapping("/generate-disbursement-completed")
    public ResponseEntity<?> generateDisbursementCompletedNotification(@RequestBody GenerateDisbursementNotificationRequest request) {
        try {
            if (request.getStudentId() == null || request.getStudentId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Student ID is required"));
            }
            if (request.getDisbursement() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Disbursement data is required"));
            }

            NotificationService.NotificationData notification = notificationService.generateDisbursementCompletedNotification(
                    request.getStudentId(),
                    request.getDisbursement()
            );

            if (notification == null) {
                return ResponseEntity.status(500)
                        .body(Map.of("success", false, "message", "Failed to generate notification"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement completed notification generated");
            response.put("notification", notification);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating disbursement completed notification", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error generating notification"));
        }
    }

    /**
     * Generate disbursement failed notification - STATELESS
     */
    @PostMapping("/generate-disbursement-failed")
    public ResponseEntity<?> generateDisbursementFailedNotification(@RequestBody GenerateDisbursementFailedNotificationRequest request) {
        try {
            if (request.getStudentId() == null || request.getStudentId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Student ID is required"));
            }
            if (request.getDisbursement() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Disbursement data is required"));
            }
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failure reason is required"));
            }

            NotificationService.NotificationData notification = notificationService.generateDisbursementFailedNotification(
                    request.getStudentId(),
                    request.getDisbursement(),
                    request.getReason()
            );

            if (notification == null) {
                return ResponseEntity.status(500)
                        .body(Map.of("success", false, "message", "Failed to generate notification"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement failed notification generated");
            response.put("notification", notification);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating disbursement failed notification", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error generating notification"));
        }
    }

    /**
     * Calculate notification statistics - STATELESS
     */
    @PostMapping("/calculate-stats")
    public ResponseEntity<?> calculateNotificationStats(@RequestBody CalculateNotificationStatsRequest request) {
        try {
            if (request.getNotifications() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Notifications data is required"));
            }

            Map<String, Object> stats = notificationService.calculateNotificationStats(request.getNotifications());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating notification statistics", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error calculating notification statistics"));
        }
    }

    /**
     * Mark notification as read - STATELESS
     */
    @PostMapping("/calculate-mark-read")
    public ResponseEntity<?> calculateMarkNotificationAsRead(@RequestBody CalculateMarkReadRequest request) {
        try {
            if (request.getNotification() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Notification data is required"));
            }

            NotificationService.NotificationData updatedNotification = notificationService.markNotificationAsRead(request.getNotification());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification marked as read");
            response.put("updatedNotification", updatedNotification);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error marking notification as read", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error updating notification"));
        }
    }

    /**
     * Mark all notifications as read - STATELESS
     */
    @PostMapping("/calculate-mark-all-read")
    public ResponseEntity<?> calculateMarkAllNotificationsAsRead(@RequestBody CalculateMarkAllReadRequest request) {
        try {
            if (request.getNotifications() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Notifications data is required"));
            }

            List<NotificationService.NotificationData> updatedNotifications = notificationService.markAllNotificationsAsRead(request.getNotifications());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All notifications marked as read");
            response.put("updatedNotifications", updatedNotifications);
            response.put("count", updatedNotifications.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error marking all notifications as read", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error updating notifications"));
        }
    }

    /**
     * Get notification types - STATELESS
     */
    @GetMapping("/types")
    public ResponseEntity<?> getNotificationTypes() {
        try {
            Map<String, Object> types = new HashMap<>();
            for (NotificationService.NotificationType type : NotificationService.NotificationType.values()) {
                types.put(type.name(), type.name().toLowerCase().replace("_", " "));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "types", types,
                    "message", "Available notification types"
            ));

        } catch (Exception e) {
            logger.error("Error retrieving notification types", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error retrieving notification types"));
        }
    }

    /**
     * Health check for notification service - STATELESS
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        logger.info("=== NOTIFICATION HEALTH CHECK CALLED ===");

        Map<String, Object> health = new HashMap<>();
        health.put("service", "NotificationService");
        health.put("status", "UP");
        health.put("timestamp", java.time.LocalDateTime.now());
        health.put("version", "2.0.0-STATELESS");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notification API is running in stateless mode!");
        response.put("health", health);

        return ResponseEntity.ok(response);
    }

    // Request DTOs for stateless operations
    public static class GenerateMentoringOfferNotificationRequest {
        private String studentId;
        private RepaymentTransaction transaction;

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public RepaymentTransaction getTransaction() { return transaction; }
        public void setTransaction(RepaymentTransaction transaction) { this.transaction = transaction; }
    }

    public static class GenerateMentorshipMatchRequest {
        private String mentorId;
        private String menteeId;
        private String matchId;

        public String getMentorId() { return mentorId; }
        public void setMentorId(String mentorId) { this.mentorId = mentorId; }
        public String getMenteeId() { return menteeId; }
        public void setMenteeId(String menteeId) { this.menteeId = menteeId; }
        public String getMatchId() { return matchId; }
        public void setMatchId(String matchId) { this.matchId = matchId; }
    }

    public static class GenerateMentorshipCompletionRequest {
        private String mentorId;
        private String menteeId;
        private String matchId;

        public String getMentorId() { return mentorId; }
        public void setMentorId(String mentorId) { this.mentorId = mentorId; }
        public String getMenteeId() { return menteeId; }
        public void setMenteeId(String menteeId) { this.menteeId = menteeId; }
        public String getMatchId() { return matchId; }
        public void setMatchId(String matchId) { this.matchId = matchId; }
    }

    public static class GeneratePaymentReminderRequest {
        private String studentId;
        private String loanId;
        private String amount;
        private String dueDate;

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getLoanId() { return loanId; }
        public void setLoanId(String loanId) { this.loanId = loanId; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    }

    public static class GenerateMentorWelcomeRequest {
        private String mentorId;
        private String mentorName;

        public String getMentorId() { return mentorId; }
        public void setMentorId(String mentorId) { this.mentorId = mentorId; }
        public String getMentorName() { return mentorName; }
        public void setMentorName(String mentorName) { this.mentorName = mentorName; }
    }

    public static class GenerateDisbursementNotificationRequest {
        private String studentId;
        private DisbursementTransaction disbursement;

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public DisbursementTransaction getDisbursement() { return disbursement; }
        public void setDisbursement(DisbursementTransaction disbursement) { this.disbursement = disbursement; }
    }

    public static class GenerateDisbursementFailedNotificationRequest {
        private String studentId;
        private DisbursementTransaction disbursement;
        private String reason;

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public DisbursementTransaction getDisbursement() { return disbursement; }
        public void setDisbursement(DisbursementTransaction disbursement) { this.disbursement = disbursement; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class CalculateNotificationStatsRequest {
        private List<NotificationService.NotificationData> notifications;

        public List<NotificationService.NotificationData> getNotifications() { return notifications; }
        public void setNotifications(List<NotificationService.NotificationData> notifications) { this.notifications = notifications; }
    }

    public static class CalculateMarkReadRequest {
        private NotificationService.NotificationData notification;

        public NotificationService.NotificationData getNotification() { return notification; }
        public void setNotification(NotificationService.NotificationData notification) { this.notification = notification; }
    }

    public static class CalculateMarkAllReadRequest {
        private List<NotificationService.NotificationData> notifications;

        public List<NotificationService.NotificationData> getNotifications() { return notifications; }
        public void setNotifications(List<NotificationService.NotificationData> notifications) { this.notifications = notifications; }
    }
}