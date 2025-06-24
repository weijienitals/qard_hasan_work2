// ========== DISBURSEMENT CONTROLLER ==========
// File: src/main/java/com/example/qard_hasan_for_education/controller/DisbursementController.java
package com.example.qard_hasan_for_education.controller;

import com.example.qard_hasan_for_education.model.*;
import com.example.qard_hasan_for_education.service.DisbursementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disbursements")
@CrossOrigin(origins = "*")
public class DisbursementController {

    private static final Logger logger = LoggerFactory.getLogger(DisbursementController.class);

    @Autowired
    private DisbursementService disbursementService;

    /**
     * DEBUG: Test endpoint to verify controller is working
     */
    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        logger.info("=== DISBURSEMENT TEST ENDPOINT CALLED ===");
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "DisbursementController is working!");
        response.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * Initiate fund disbursement for an approved loan - POST with request body (as requested by boss)
     */
    @PostMapping("/initiate")
    public ResponseEntity<?> initiateDisbursement(@RequestBody InitiateDisbursementRequest request) {
        logger.info("=== INITIATE DISBURSEMENT CALLED === Loan: {}", request.getLoanId());

        try {
            // Validate request
            if (request.getLoanId() == null || request.getLoanId().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Loan ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getDisbursementMethod() == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Disbursement method is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getBankAccount() == null || request.getBankAccount().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Bank account is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getBankName() == null || request.getBankName().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Bank name is required");
                return ResponseEntity.badRequest().body(response);
            }

            DisbursementTransaction disbursement = disbursementService.initiateDisbursement(
                    request.getLoanId(),
                    request.getDisbursementMethod(),
                    request.getBankAccount(),
                    request.getBankName()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement initiated successfully");
            response.put("disbursement", disbursement);
            response.put("estimatedDelivery", disbursement.getEstimatedDelivery());
            response.put("fees", disbursement.getFees());
            response.put("netAmount", disbursement.getNetAmount());

            logger.info("Disbursement initiated: {} for loan: {}",
                    disbursement.getDisbursementId(), request.getLoanId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error initiating disbursement for loan: {}", request.getLoanId(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Process a pending disbursement (admin function) - POST with request body
     */
    @PostMapping("/process")
    public ResponseEntity<?> processDisbursement(@RequestBody ProcessDisbursementRequest request) {
        logger.info("=== PROCESS DISBURSEMENT CALLED === ID: {}", request.getDisbursementId());

        try {
            if (request.getDisbursementId() == null || request.getDisbursementId().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Disbursement ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            DisbursementTransaction disbursement = disbursementService.processDisbursement(
                    request.getDisbursementId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement processing initiated");
            response.put("disbursement", disbursement);
            response.put("trackingNumber", disbursement.getTrackingNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing disbursement: {}", request.getDisbursementId(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Complete a disbursement (mark as successfully sent) - POST with request body
     */
    @PostMapping("/complete")
    public ResponseEntity<?> completeDisbursement(@RequestBody CompleteDisbursementRequest request) {
        logger.info("=== COMPLETE DISBURSEMENT CALLED === ID: {}", request.getDisbursementId());

        try {
            if (request.getDisbursementId() == null || request.getDisbursementId().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Disbursement ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            DisbursementTransaction disbursement = disbursementService.completeDisbursement(
                    request.getDisbursementId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement completed successfully");
            response.put("disbursement", disbursement);
            response.put("completedAt", disbursement.getCompletedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error completing disbursement: {}", request.getDisbursementId(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Mark disbursement as failed - POST with request body
     */
    @PostMapping("/fail")
    public ResponseEntity<?> failDisbursement(@RequestBody FailDisbursementRequest request) {
        logger.info("=== FAIL DISBURSEMENT CALLED === ID: {} - Reason: {}",
                request.getDisbursementId(), request.getReason());

        try {
            if (request.getDisbursementId() == null || request.getDisbursementId().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Disbursement ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failure reason is required");
                return ResponseEntity.badRequest().body(response);
            }

            DisbursementTransaction disbursement = disbursementService.failDisbursement(
                    request.getDisbursementId(),
                    request.getReason()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement marked as failed");
            response.put("disbursement", disbursement);
            response.put("failureReason", disbursement.getFailureReason());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error failing disbursement: {}", request.getDisbursementId(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get disbursement details by ID - POST with request body (as requested by boss)
     */
    @PostMapping("/get")
    public ResponseEntity<?> getDisbursement(@RequestBody GetDisbursementRequest request) {
        logger.info("=== GET DISBURSEMENT CALLED === ID: {}", request.getDisbursementId());

        try {
            if (request.getDisbursementId() == null || request.getDisbursementId().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Disbursement ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            DisbursementTransaction disbursement = disbursementService.getDisbursement(
                    request.getDisbursementId()
            );

            Map<String, Object> response = new HashMap<>();
            if (disbursement == null) {
                response.put("success", false);
                response.put("message", "Disbursement not found: " + request.getDisbursementId());
                return ResponseEntity.status(404).body(response);
            }

            response.put("success", true);
            response.put("message", "Disbursement retrieved successfully");
            response.put("disbursement", disbursement);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving disbursement: {}", request.getDisbursementId(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving disbursement: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get disbursement by loan ID - POST with request body (as requested by boss)
     */
    @PostMapping("/by-loan")
    public ResponseEntity<?> getDisbursementByLoan(@RequestBody GetDisbursementByLoanRequest request) {
        logger.info("=== GET DISBURSEMENT BY LOAN CALLED === LoanID: {}", request.getLoanId());

        try {
            if (request.getLoanId() == null || request.getLoanId().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Loan ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            DisbursementTransaction disbursement = disbursementService.getDisbursementByLoanId(
                    request.getLoanId()
            );

            Map<String, Object> response = new HashMap<>();
            if (disbursement == null) {
                response.put("success", false);
                response.put("message", "No disbursement found for loan: " + request.getLoanId());
                return ResponseEntity.status(404).body(response);
            }

            response.put("success", true);
            response.put("message", "Disbursement retrieved successfully");
            response.put("disbursement", disbursement);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving disbursement by loan: {}", request.getLoanId(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving disbursement: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get all disbursements for a student - POST with request body (as requested by boss)
     */
    @PostMapping("/student")
    public ResponseEntity<?> getStudentDisbursements(@RequestBody GetStudentDisbursementsRequest request) {
        logger.info("=== GET STUDENT DISBURSEMENTS CALLED === StudentID: {}", request.getStudentId());

        try {
            if (request.getStudentId() == null || request.getStudentId().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Student ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            List<DisbursementTransaction> disbursements = disbursementService.getStudentDisbursements(
                    request.getStudentId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Student disbursements retrieved successfully");
            response.put("disbursements", disbursements);
            response.put("totalDisbursements", disbursements.size());

            // Calculate summary statistics
            BigDecimal totalAmount = disbursements.stream()
                    .map(DisbursementTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long completedCount = disbursements.stream()
                    .filter(DisbursementTransaction::isCompleted)
                    .count();

            response.put("totalAmount", totalAmount);
            response.put("completedDisbursements", completedCount);
            response.put("pendingDisbursements", disbursements.size() - completedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving student disbursements: {}", request.getStudentId(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving disbursements: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get disbursement statistics (admin function)
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getDisbursementStatistics() {
        logger.info("=== GET DISBURSEMENT STATISTICS CALLED ===");

        try {
            Map<String, Object> statistics = disbursementService.getDisbursementStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement statistics retrieved successfully");
            response.put("statistics", statistics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving disbursement statistics", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving statistics: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get available disbursement methods
     */
    @GetMapping("/methods")
    public ResponseEntity<?> getDisbursementMethods() {
        logger.info("=== GET DISBURSEMENT METHODS CALLED ===");

        try {
            Map<String, Object> methods = new HashMap<>();

            for (DisbursementMethod method : DisbursementMethod.values()) {
                Map<String, Object> methodInfo = new HashMap<>();
                methodInfo.put("description", method.getDescription());

                // Add estimated fees and delivery times
                switch (method) {
                    case BANK_TRANSFER:
                        methodInfo.put("feeRate", "1%");
                        methodInfo.put("estimatedDelivery", "1 business day");
                        break;
                    case WIRE_TRANSFER:
                        methodInfo.put("feeRate", "$25 fixed");
                        methodInfo.put("estimatedDelivery", "3 business days");
                        break;
                    case DIGITAL_WALLET:
                        methodInfo.put("feeRate", "0.5%");
                        methodInfo.put("estimatedDelivery", "1 hour");
                        break;
                    case CHECK:
                        methodInfo.put("feeRate", "$10 fixed");
                        methodInfo.put("estimatedDelivery", "7 business days");
                        break;
                }

                methods.put(method.name(), methodInfo);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Available disbursement methods");
            response.put("methods", methods);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving disbursement methods", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving methods: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Health check for disbursement service
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        logger.info("=== DISBURSEMENT HEALTH CHECK CALLED ===");

        Map<String, Object> health = new HashMap<>();
        health.put("service", "DisbursementService");
        health.put("status", "UP");
        health.put("timestamp", java.time.LocalDateTime.now());
        health.put("version", "1.0.0");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Disbursement API is running!");
        response.put("health", health);

        return ResponseEntity.ok(response);
    }

    // Request DTOs (following your pattern)
    public static class InitiateDisbursementRequest {
        private String loanId;
        private DisbursementMethod disbursementMethod;
        private String bankAccount;
        private String bankName;

        // Default constructor
        public InitiateDisbursementRequest() {}

        // Getters and setters
        public String getLoanId() { return loanId; }
        public void setLoanId(String loanId) { this.loanId = loanId; }

        public DisbursementMethod getDisbursementMethod() { return disbursementMethod; }
        public void setDisbursementMethod(DisbursementMethod disbursementMethod) { this.disbursementMethod = disbursementMethod; }

        public String getBankAccount() { return bankAccount; }
        public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

        public String getBankName() { return bankName; }
        public void setBankName(String bankName) { this.bankName = bankName; }
    }

    public static class ProcessDisbursementRequest {
        private String disbursementId;

        public ProcessDisbursementRequest() {}

        public String getDisbursementId() { return disbursementId; }
        public void setDisbursementId(String disbursementId) { this.disbursementId = disbursementId; }
    }

    public static class CompleteDisbursementRequest {
        private String disbursementId;

        public CompleteDisbursementRequest() {}

        public String getDisbursementId() { return disbursementId; }
        public void setDisbursementId(String disbursementId) { this.disbursementId = disbursementId; }
    }

    public static class FailDisbursementRequest {
        private String disbursementId;
        private String reason;

        public FailDisbursementRequest() {}

        public String getDisbursementId() { return disbursementId; }
        public void setDisbursementId(String disbursementId) { this.disbursementId = disbursementId; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class GetDisbursementRequest {
        private String disbursementId;

        public GetDisbursementRequest() {}

        public String getDisbursementId() { return disbursementId; }
        public void setDisbursementId(String disbursementId) { this.disbursementId = disbursementId; }
    }

    public static class GetDisbursementByLoanRequest {
        private String loanId;

        public GetDisbursementByLoanRequest() {}

        public String getLoanId() { return loanId; }
        public void setLoanId(String loanId) { this.loanId = loanId; }
    }

    public static class GetStudentDisbursementsRequest {
        private String studentId;

        public GetStudentDisbursementsRequest() {}

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
    }
}