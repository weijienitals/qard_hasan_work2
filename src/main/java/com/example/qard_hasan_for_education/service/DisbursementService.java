package com.example.qard_hasan_for_education.service;

import com.example.qard_hasan_for_education.model.*;
import com.example.qard_hasan_for_education.model.individual.SimpleBankInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DisbursementService {

    private static final Logger logger = LoggerFactory.getLogger(DisbursementService.class);

    @Autowired
    private NotificationService notificationService;

    /**
     * Calculate disbursement transaction data - STATELESS
     */
    public DisbursementCalculationResult calculateDisbursementInitiation(String loanId,
                                                                         LoanAccount loanAccount,
                                                                         DisbursementMethod method,
                                                                         String bankAccount,
                                                                         String bankName) throws Exception {
        logger.info("=== CALCULATING DISBURSEMENT INITIATION === Loan: {}", loanId);

        // Validate loan account
        if (loanAccount == null) {
            throw new Exception("Loan account not found: " + loanId);
        }

        if (loanAccount.getLoanStatus() != LoanStatus.ACTIVE) {
            throw new Exception("Loan is not active. Status: " + loanAccount.getLoanStatus());
        }

        // Create disbursement transaction data
        DisbursementTransaction disbursement = new DisbursementTransaction(
                loanId,
                loanAccount.getStudentId(),
                loanAccount.getApplicationId(),
                loanAccount.getPrincipalAmount(),
                method,
                bankAccount,
                bankName
        );

        // Generate notification data
        NotificationService.NotificationData notification = notificationService.generateDisbursementInitiatedNotification(
                loanAccount.getStudentId(),
                disbursement
        );

        logger.info("Disbursement calculation completed: {} for loan: {}, amount: {}",
                disbursement.getDisbursementId(), loanId, disbursement.getAmount());

        return new DisbursementCalculationResult(disbursement, notification);
    }

    /**
     * Calculate disbursement processing data - STATELESS
     */
    public DisbursementProcessingResult calculateDisbursementProcessing(String disbursementId,
                                                                        DisbursementTransaction currentDisbursement) throws Exception {
        logger.info("=== CALCULATING DISBURSEMENT PROCESSING === ID: {}", disbursementId);

        if (currentDisbursement == null) {
            throw new Exception("Disbursement not found: " + disbursementId);
        }

        if (currentDisbursement.getStatus() != DisbursementStatus.PENDING) {
            throw new Exception("Disbursement not in pending status: " + currentDisbursement.getStatus());
        }

        // Create updated disbursement data
        DisbursementTransaction updatedDisbursement = copyDisbursement(currentDisbursement);
        updatedDisbursement.setStatus(DisbursementStatus.PROCESSING);
        updatedDisbursement.setProcessedAt(LocalDateTime.now());
        updatedDisbursement.setTrackingNumber("TRK" + System.currentTimeMillis());

        // Generate notification data
        NotificationService.NotificationData notification = notificationService.generateDisbursementProcessingNotification(
                updatedDisbursement.getStudentId(),
                updatedDisbursement
        );

        logger.info("Disbursement processing calculation completed: {}", disbursementId);

        return new DisbursementProcessingResult(updatedDisbursement, notification);
    }

    /**
     * Calculate disbursement completion data - STATELESS
     */
    public DisbursementCompletionResult calculateDisbursementCompletion(String disbursementId,
                                                                        DisbursementTransaction currentDisbursement) throws Exception {
        logger.info("=== CALCULATING DISBURSEMENT COMPLETION === ID: {}", disbursementId);

        if (currentDisbursement == null) {
            throw new Exception("Disbursement not found: " + disbursementId);
        }

        // Create completed disbursement data
        DisbursementTransaction completedDisbursement = copyDisbursement(currentDisbursement);
        completedDisbursement.setStatus(DisbursementStatus.COMPLETED);
        completedDisbursement.setCompletedAt(LocalDateTime.now());

        // Generate notification data
        NotificationService.NotificationData notification = notificationService.generateDisbursementCompletedNotification(
                completedDisbursement.getStudentId(),
                completedDisbursement
        );

        logger.info("Disbursement completion calculation: {}, amount: {} calculated",
                disbursementId, completedDisbursement.getNetAmount());

        return new DisbursementCompletionResult(completedDisbursement, notification);
    }

    /**
     * Calculate disbursement failure data - STATELESS
     */
    public DisbursementFailureResult calculateDisbursementFailure(String disbursementId,
                                                                  String reason,
                                                                  DisbursementTransaction currentDisbursement) throws Exception {
        logger.error("=== CALCULATING DISBURSEMENT FAILURE === ID: {} - Reason: {}", disbursementId, reason);

        if (currentDisbursement == null) {
            throw new Exception("Disbursement not found: " + disbursementId);
        }

        // Create failed disbursement data
        DisbursementTransaction failedDisbursement = copyDisbursement(currentDisbursement);
        failedDisbursement.setStatus(DisbursementStatus.FAILED);
        failedDisbursement.setFailureReason(reason);

        // Generate notification data
        NotificationService.NotificationData notification = notificationService.generateDisbursementFailedNotification(
                failedDisbursement.getStudentId(),
                failedDisbursement,
                reason
        );

        return new DisbursementFailureResult(failedDisbursement, notification);
    }

    /**
     * Calculate auto-disbursement initiation - STATELESS
     */
    public DisbursementCalculationResult calculateAutoDisbursementInitiation(LoanAccount loanAccount,
                                                                             SimpleBankInfo bankInfo) throws Exception {
        logger.info("=== CALCULATING AUTO-DISBURSEMENT INITIATION === Loan: {}", loanAccount.getLoanId());

        // Determine best disbursement method based on location and bank
        DisbursementMethod method = determineBestDisbursementMethod(bankInfo.getBankName());

        return calculateDisbursementInitiation(
                loanAccount.getLoanId(),
                loanAccount,
                method,
                bankInfo.getAccountNumber(),
                bankInfo.getBankName()
        );
    }

    /**
     * Calculate disbursement statistics - STATELESS
     */
    public Map<String, Object> calculateDisbursementStatistics(List<DisbursementTransaction> allDisbursements) {
        logger.info("=== CALCULATING DISBURSEMENT STATISTICS ===");

        if (allDisbursements == null) {
            allDisbursements = Collections.emptyList();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDisbursements", allDisbursements.size());

        long completed = allDisbursements.stream().filter(DisbursementTransaction::isCompleted).count();
        long failed = allDisbursements.stream().filter(DisbursementTransaction::hasFailed).count();
        long pending = allDisbursements.size() - completed - failed;

        stats.put("completedDisbursements", completed);
        stats.put("failedDisbursements", failed);
        stats.put("pendingDisbursements", pending);

        BigDecimal totalAmount = allDisbursements.stream()
                .map(DisbursementTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalDisbursedAmount", totalAmount);

        BigDecimal completedAmount = allDisbursements.stream()
                .filter(DisbursementTransaction::isCompleted)
                .map(DisbursementTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("completedDisbursedAmount", completedAmount);

        return stats;
    }

    /**
     * Filter student disbursements - STATELESS
     */
    public List<DisbursementTransaction> filterStudentDisbursements(List<DisbursementTransaction> allDisbursements, String studentId) {
        logger.info("=== FILTERING STUDENT DISBURSEMENTS === StudentID: {}", studentId);

        if (allDisbursements == null) {
            return Collections.emptyList();
        }

        return allDisbursements.stream()
                .filter(Objects::nonNull)
                .filter(disbursement -> studentId.equals(disbursement.getStudentId()))
                .sorted((d1, d2) -> d2.getInitiatedAt().compareTo(d1.getInitiatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Find disbursement by loan ID - STATELESS
     */
    public DisbursementTransaction findDisbursementByLoanId(List<DisbursementTransaction> allDisbursements, String loanId) {
        logger.info("=== FINDING DISBURSEMENT BY LOAN === LoanID: {}", loanId);

        if (allDisbursements == null) {
            return null;
        }

        return allDisbursements.stream()
                .filter(Objects::nonNull)
                .filter(disbursement -> loanId.equals(disbursement.getLoanId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if disbursement exists for loan - STATELESS
     */
    public boolean checkDisbursementExistsForLoan(List<DisbursementTransaction> allDisbursements, String loanId) {
        return findDisbursementByLoanId(allDisbursements, loanId) != null;
    }

    /**
     * Validate disbursement method compatibility - STATELESS
     */
    public Map<String, Object> validateDisbursementMethod(DisbursementMethod method, String bankName, String country) {
        Map<String, Object> validation = new HashMap<>();

        boolean isCompatible = true;
        List<String> warnings = new ArrayList<>();
        List<String> requirements = new ArrayList<>();

        switch (method) {
            case WIRE_TRANSFER:
                if (bankName != null && !bankName.toLowerCase().contains("international")) {
                    warnings.add("Wire transfers work best with international banks");
                }
                requirements.add("SWIFT code required");
                requirements.add("Full bank address required");
                break;

            case DIGITAL_WALLET:
                if (bankName != null && !bankName.toLowerCase().contains("digital")) {
                    warnings.add("Digital wallet transfers require compatible bank");
                }
                requirements.add("Mobile number required");
                break;

            case BANK_TRANSFER:
                requirements.add("Valid account number required");
                requirements.add("Bank routing number required");
                break;

            case CHECK:
                requirements.add("Physical mailing address required");
                warnings.add("Checks have longer delivery times");
                break;
        }

        validation.put("compatible", isCompatible);
        validation.put("warnings", warnings);
        validation.put("requirements", requirements);
        validation.put("estimatedFees", calculateEstimatedFees(method, BigDecimal.valueOf(50000)));
        validation.put("estimatedDeliveryDays", getEstimatedDeliveryDays(method));

        return validation;
    }

    // Private helper methods

    private DisbursementTransaction copyDisbursement(DisbursementTransaction original) {
        DisbursementTransaction copy = new DisbursementTransaction();
        copy.setDisbursementId(original.getDisbursementId());
        copy.setLoanId(original.getLoanId());
        copy.setStudentId(original.getStudentId());
        copy.setApplicationId(original.getApplicationId());
        copy.setAmount(original.getAmount());
        copy.setDisbursementMethod(original.getDisbursementMethod());
        copy.setStudentBankAccount(original.getStudentBankAccount());
        copy.setStudentBankName(original.getStudentBankName());
        copy.setReferenceNumber(original.getReferenceNumber());
        copy.setTrackingNumber(original.getTrackingNumber());
        copy.setStatus(original.getStatus());
        copy.setExchangeRate(original.getExchangeRate());
        copy.setFees(original.getFees());
        copy.setNetAmount(original.getNetAmount());
        copy.setInitiatedAt(original.getInitiatedAt());
        copy.setProcessedAt(original.getProcessedAt());
        copy.setCompletedAt(original.getCompletedAt());
        copy.setEstimatedDelivery(original.getEstimatedDelivery());
        copy.setFailureReason(original.getFailureReason());
        copy.setNotes(original.getNotes());
        return copy;
    }

    private DisbursementMethod determineBestDisbursementMethod(String bankName) {
        if (bankName != null && bankName.toLowerCase().contains("digital")) {
            return DisbursementMethod.DIGITAL_WALLET;
        } else if (bankName != null && (bankName.toLowerCase().contains("international") ||
                bankName.toLowerCase().contains("chase") ||
                bankName.toLowerCase().contains("wells"))) {
            return DisbursementMethod.WIRE_TRANSFER;
        } else {
            return DisbursementMethod.BANK_TRANSFER;
        }
    }

    private BigDecimal calculateEstimatedFees(DisbursementMethod method, BigDecimal amount) {
        switch (method) {
            case BANK_TRANSFER:
                return amount.multiply(new BigDecimal("0.01")); // 1%
            case WIRE_TRANSFER:
                return new BigDecimal("25.00"); // $25 fixed
            case DIGITAL_WALLET:
                return amount.multiply(new BigDecimal("0.005")); // 0.5%
            case CHECK:
                return new BigDecimal("10.00"); // $10 fixed
            default:
                return BigDecimal.ZERO;
        }
    }

    private int getEstimatedDeliveryDays(DisbursementMethod method) {
        switch (method) {
            case BANK_TRANSFER:
                return 1;
            case WIRE_TRANSFER:
                return 3;
            case DIGITAL_WALLET:
                return 0; // Same day
            case CHECK:
                return 7;
            default:
                return 1;
        }
    }

    // Result classes for stateless operations

    public static class DisbursementCalculationResult {
        private final DisbursementTransaction disbursement;
        private final NotificationService.NotificationData notification;

        public DisbursementCalculationResult(DisbursementTransaction disbursement, NotificationService.NotificationData notification) {
            this.disbursement = disbursement;
            this.notification = notification;
        }

        public DisbursementTransaction getDisbursement() { return disbursement; }
        public NotificationService.NotificationData getNotification() { return notification; }
    }

    public static class DisbursementProcessingResult {
        private final DisbursementTransaction updatedDisbursement;
        private final NotificationService.NotificationData notification;

        public DisbursementProcessingResult(DisbursementTransaction updatedDisbursement, NotificationService.NotificationData notification) {
            this.updatedDisbursement = updatedDisbursement;
            this.notification = notification;
        }

        public DisbursementTransaction getUpdatedDisbursement() { return updatedDisbursement; }
        public NotificationService.NotificationData getNotification() { return notification; }
    }

    public static class DisbursementCompletionResult {
        private final DisbursementTransaction completedDisbursement;
        private final NotificationService.NotificationData notification;

        public DisbursementCompletionResult(DisbursementTransaction completedDisbursement, NotificationService.NotificationData notification) {
            this.completedDisbursement = completedDisbursement;
            this.notification = notification;
        }

        public DisbursementTransaction getCompletedDisbursement() { return completedDisbursement; }
        public NotificationService.NotificationData getNotification() { return notification; }
    }

    public static class DisbursementFailureResult {
        private final DisbursementTransaction failedDisbursement;
        private final NotificationService.NotificationData notification;

        public DisbursementFailureResult(DisbursementTransaction failedDisbursement, NotificationService.NotificationData notification) {
            this.failedDisbursement = failedDisbursement;
            this.notification = notification;
        }

        public DisbursementTransaction getFailedDisbursement() { return failedDisbursement; }
        public NotificationService.NotificationData getNotification() { return notification; }
    }
}