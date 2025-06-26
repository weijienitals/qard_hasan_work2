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
     * Calculate disbursement initiation - STATELESS
     */
    @PostMapping("/calculate-initiation")
    public ResponseEntity<?> calculateDisbursementInitiation(@RequestBody CalculateInitiationRequest request) {
        logger.info("=== CALCULATE DISBURSEMENT INITIATION CALLED === Loan: {}", request.getLoanId());

        try {
            // Validate request
            if (request.getLoanId() == null || request.getLoanId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Loan ID is required"));
            }
            if (request.getLoanAccount() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Loan account data is required"));
            }
            if (request.getDisbursementMethod() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Disbursement method is required"));
            }
            if (request.getBankAccount() == null || request.getBankAccount().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Bank account is required"));
            }
            if (request.getBankName() == null || request.getBankName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Bank name is required"));
            }

            DisbursementService.DisbursementCalculationResult result = disbursementService.calculateDisbursementInitiation(
                    request.getLoanId(),
                    request.getLoanAccount(),
                    request.getDisbursementMethod(),
                    request.getBankAccount(),
                    request.getBankName()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement calculation completed successfully");
            response.put("disbursement", result.getDisbursement());
            response.put("notification", result.getNotification());
            response.put("estimatedDelivery", result.getDisbursement().getEstimatedDelivery());
            response.put("fees", result.getDisbursement().getFees());
            response.put("netAmount", result.getDisbursement().getNetAmount());

            logger.info("Disbursement calculation completed: {} for loan: {}",
                    result.getDisbursement().getDisbursementId(), request.getLoanId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating disbursement initiation for loan: {}", request.getLoanId(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Calculate disbursement processing - STATELESS
     */
    @PostMapping("/calculate-processing")
    public ResponseEntity<?> calculateDisbursementProcessing(@RequestBody CalculateProcessingRequest request) {
        logger.info("=== CALCULATE DISBURSEMENT PROCESSING CALLED === ID: {}", request.getDisbursementId());

        try {
            if (request.getDisbursementId() == null || request.getDisbursementId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Disbursement ID is required"));
            }
            if (request.getCurrentDisbursement() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Current disbursement data is required"));
            }

            DisbursementService.DisbursementProcessingResult result = disbursementService.calculateDisbursementProcessing(
                    request.getDisbursementId(),
                    request.getCurrentDisbursement()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement processing calculation completed");
            response.put("updatedDisbursement", result.getUpdatedDisbursement());
            response.put("notification", result.getNotification());
            response.put("trackingNumber", result.getUpdatedDisbursement().getTrackingNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating disbursement processing: {}", request.getDisbursementId(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Calculate disbursement completion - STATELESS
     */
    @PostMapping("/calculate-completion")
    public ResponseEntity<?> calculateDisbursementCompletion(@RequestBody CalculateCompletionRequest request) {
        logger.info("=== CALCULATE DISBURSEMENT COMPLETION CALLED === ID: {}", request.getDisbursementId());

        try {
            if (request.getDisbursementId() == null || request.getDisbursementId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Disbursement ID is required"));
            }
            if (request.getCurrentDisbursement() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Current disbursement data is required"));
            }

            DisbursementService.DisbursementCompletionResult result = disbursementService.calculateDisbursementCompletion(
                    request.getDisbursementId(),
                    request.getCurrentDisbursement()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement completion calculation completed");
            response.put("completedDisbursement", result.getCompletedDisbursement());
            response.put("notification", result.getNotification());
            response.put("completedAt", result.getCompletedDisbursement().getCompletedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating disbursement completion: {}", request.getDisbursementId(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Calculate disbursement failure - STATELESS
     */
    @PostMapping("/calculate-failure")
    public ResponseEntity<?> calculateDisbursementFailure(@RequestBody CalculateFailureRequest request) {
        logger.info("=== CALCULATE DISBURSEMENT FAILURE CALLED === ID: {} - Reason: {}",
                request.getDisbursementId(), request.getReason());

        try {
            if (request.getDisbursementId() == null || request.getDisbursementId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Disbursement ID is required"));
            }
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failure reason is required"));
            }
            if (request.getCurrentDisbursement() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Current disbursement data is required"));
            }

            DisbursementService.DisbursementFailureResult result = disbursementService.calculateDisbursementFailure(
                    request.getDisbursementId(),
                    request.getReason(),
                    request.getCurrentDisbursement()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement failure calculation completed");
            response.put("failedDisbursement", result.getFailedDisbursement());
            response.put("notification", result.getNotification());
            response.put("failureReason", result.getFailedDisbursement().getFailureReason());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating disbursement failure: {}", request.getDisbursementId(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Calculate auto-disbursement initiation - STATELESS
     */
    @PostMapping("/calculate-auto-initiation")
    public ResponseEntity<?> calculateAutoDisbursementInitiation(@RequestBody CalculateAutoInitiationRequest request) {
        logger.info("=== CALCULATE AUTO-DISBURSEMENT INITIATION CALLED === Loan: {}",
                request.getLoanAccount() != null ? request.getLoanAccount().getLoanId() : "null");

        try {
            if (request.getLoanAccount() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Loan account data is required"));
            }
            if (request.getBankInfo() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Bank information is required"));
            }

            DisbursementService.DisbursementCalculationResult result = disbursementService.calculateAutoDisbursementInitiation(
                    request.getLoanAccount(),
                    request.getBankInfo()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Auto-disbursement calculation completed successfully");
            response.put("disbursement", result.getDisbursement());
            response.put("notification", result.getNotification());
            response.put("selectedMethod", result.getDisbursement().getDisbursementMethod());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating auto-disbursement initiation", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Calculate disbursement statistics - STATELESS
     */
    @PostMapping("/calculate-statistics")
    public ResponseEntity<?> calculateDisbursementStatistics(@RequestBody CalculateStatisticsRequest request) {
        logger.info("=== CALCULATE DISBURSEMENT STATISTICS CALLED ===");

        try {
            Map<String, Object> statistics = disbursementService.calculateDisbursementStatistics(request.getAllDisbursements());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disbursement statistics calculated successfully");
            response.put("statistics", statistics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating disbursement statistics", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error calculating statistics: " + e.getMessage()));
        }
    }

    /**
     * Filter student disbursements - STATELESS
     */
    @PostMapping("/filter-student-disbursements")
    public ResponseEntity<?> filterStudentDisbursements(@RequestBody FilterStudentDisbursementsRequest request) {
        logger.info("=== FILTER STUDENT DISBURSEMENTS CALLED === StudentID: {}", request.getStudentId());

        try {
            if (request.getStudentId() == null || request.getStudentId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Student ID is required"));
            }

            List<DisbursementTransaction> filteredDisbursements = disbursementService.filterStudentDisbursements(
                    request.getAllDisbursements(),
                    request.getStudentId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Student disbursements filtered successfully");
            response.put("disbursements", filteredDisbursements);
            response.put("totalDisbursements", filteredDisbursements.size());

            // Calculate summary statistics
            BigDecimal totalAmount = filteredDisbursements.stream()
                    .map(DisbursementTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long completedCount = filteredDisbursements.stream()
                    .filter(DisbursementTransaction::isCompleted)
                    .count();

            response.put("totalAmount", totalAmount);
            response.put("completedDisbursements", completedCount);
            response.put("pendingDisbursements", filteredDisbursements.size() - completedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error filtering student disbursements: {}", request.getStudentId(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error filtering disbursements: " + e.getMessage()));
        }
    }

    /**
     * Find disbursement by loan ID - STATELESS
     */
    @PostMapping("/find-by-loan")
    public ResponseEntity<?> findDisbursementByLoan(@RequestBody FindByLoanRequest request) {
        logger.info("=== FIND DISBURSEMENT BY LOAN CALLED === LoanID: {}", request.getLoanId());

        try {
            if (request.getLoanId() == null || request.getLoanId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Loan ID is required"));
            }

            DisbursementTransaction disbursement = disbursementService.findDisbursementByLoanId(
                    request.getAllDisbursements(),
                    request.getLoanId()
            );

            Map<String, Object> response = new HashMap<>();
            if (disbursement == null) {
                response.put("success", false);
                response.put("message", "No disbursement found for loan: " + request.getLoanId());
                return ResponseEntity.status(404).body(response);
            }

            response.put("success", true);
            response.put("message", "Disbursement found successfully");
            response.put("disbursement", disbursement);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error finding disbursement by loan: {}", request.getLoanId(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error finding disbursement: " + e.getMessage()));
        }
    }

    /**
     * Validate disbursement method - STATELESS
     */
    @PostMapping("/validate-method")
    public ResponseEntity<?> validateDisbursementMethod(@RequestBody ValidateMethodRequest request) {
        logger.info("=== VALIDATE DISBURSEMENT METHOD CALLED === Method: {}", request.getMethod());

        try {
            if (request.getMethod() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Disbursement method is required"));
            }

            Map<String, Object> validation = disbursementService.validateDisbursementMethod(
                    request.getMethod(),
                    request.getBankName(),
                    request.getCountry()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Method validation completed");
            response.put("validation", validation);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating disbursement method", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error validating method: " + e.getMessage()));
        }
    }

    /**
     * Get available disbursement methods - STATELESS
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
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error retrieving methods: " + e.getMessage()));
        }
    }

    /**
     * Health check for disbursement service - STATELESS
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        logger.info("=== DISBURSEMENT HEALTH CHECK CALLED ===");

        Map<String, Object> health = new HashMap<>();
        health.put("service", "DisbursementService");
        health.put("status", "UP");
        health.put("timestamp", java.time.LocalDateTime.now());
        health.put("version", "2.0.0-STATELESS");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Disbursement API is running in stateless mode!");
        response.put("health", health);

        return ResponseEntity.ok(response);
    }

    // Request DTOs for stateless operations
    public static class CalculateInitiationRequest {
        private String loanId;
        private LoanAccount loanAccount;
        private DisbursementMethod disbursementMethod;
        private String bankAccount;
        private String bankName;

        // Getters and setters
        public String getLoanId() { return loanId; }
        public void setLoanId(String loanId) { this.loanId = loanId; }
        public LoanAccount getLoanAccount() { return loanAccount; }
        public void setLoanAccount(LoanAccount loanAccount) { this.loanAccount = loanAccount; }
        public DisbursementMethod getDisbursementMethod() { return disbursementMethod; }
        public void setDisbursementMethod(DisbursementMethod disbursementMethod) { this.disbursementMethod = disbursementMethod; }
        public String getBankAccount() { return bankAccount; }
        public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
        public String getBankName() { return bankName; }
        public void setBankName(String bankName) { this.bankName = bankName; }
    }

    public static class CalculateProcessingRequest {
        private String disbursementId;
        private DisbursementTransaction currentDisbursement;

        public String getDisbursementId() { return disbursementId; }
        public void setDisbursementId(String disbursementId) { this.disbursementId = disbursementId; }
        public DisbursementTransaction getCurrentDisbursement() { return currentDisbursement; }
        public void setCurrentDisbursement(DisbursementTransaction currentDisbursement) { this.currentDisbursement = currentDisbursement; }
    }

    public static class CalculateCompletionRequest {
        private String disbursementId;
        private DisbursementTransaction currentDisbursement;

        public String getDisbursementId() { return disbursementId; }
        public void setDisbursementId(String disbursementId) { this.disbursementId = disbursementId; }
        public DisbursementTransaction getCurrentDisbursement() { return currentDisbursement; }
        public void setCurrentDisbursement(DisbursementTransaction currentDisbursement) { this.currentDisbursement = currentDisbursement; }
    }

    public static class CalculateFailureRequest {
        private String disbursementId;
        private String reason;
        private DisbursementTransaction currentDisbursement;

        public String getDisbursementId() { return disbursementId; }
        public void setDisbursementId(String disbursementId) { this.disbursementId = disbursementId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public DisbursementTransaction getCurrentDisbursement() { return currentDisbursement; }
        public void setCurrentDisbursement(DisbursementTransaction currentDisbursement) { this.currentDisbursement = currentDisbursement; }
    }

    public static class CalculateAutoInitiationRequest {
        private LoanAccount loanAccount;
        private com.example.qard_hasan_for_education.model.individual.SimpleBankInfo bankInfo;

        public LoanAccount getLoanAccount() { return loanAccount; }
        public void setLoanAccount(LoanAccount loanAccount) { this.loanAccount = loanAccount; }
        public com.example.qard_hasan_for_education.model.individual.SimpleBankInfo getBankInfo() { return bankInfo; }
        public void setBankInfo(com.example.qard_hasan_for_education.model.individual.SimpleBankInfo bankInfo) { this.bankInfo = bankInfo; }
    }

    public static class CalculateStatisticsRequest {
        private List<DisbursementTransaction> allDisbursements;

        public List<DisbursementTransaction> getAllDisbursements() { return allDisbursements; }
        public void setAllDisbursements(List<DisbursementTransaction> allDisbursements) { this.allDisbursements = allDisbursements; }
    }

    public static class FilterStudentDisbursementsRequest {
        private String studentId;
        private List<DisbursementTransaction> allDisbursements;

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public List<DisbursementTransaction> getAllDisbursements() { return allDisbursements; }
        public void setAllDisbursements(List<DisbursementTransaction> allDisbursements) { this.allDisbursements = allDisbursements; }
    }

    public static class FindByLoanRequest {
        private String loanId;
        private List<DisbursementTransaction> allDisbursements;

        public String getLoanId() { return loanId; }
        public void setLoanId(String loanId) { this.loanId = loanId; }
        public List<DisbursementTransaction> getAllDisbursements() { return allDisbursements; }
        public void setAllDisbursements(List<DisbursementTransaction> allDisbursements) { this.allDisbursements = allDisbursements; }
    }

    public static class ValidateMethodRequest {
        private DisbursementMethod method;
        private String bankName;
        private String country;

        public DisbursementMethod getMethod() { return method; }
        public void setMethod(DisbursementMethod method) { this.method = method; }
        public String getBankName() { return bankName; }
        public void setBankName(String bankName) { this.bankName = bankName; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }
}