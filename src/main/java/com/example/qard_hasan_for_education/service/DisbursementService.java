// ========== DISBURSEMENT SERVICE ==========
// File: src/main/java/com/example/qard_hasan_for_education/service/DisbursementService.java
package com.example.qard_hasan_for_education.service;

import com.example.qard_hasan_for_education.model.*;
import com.example.qard_hasan_for_education.model.individual.SimpleBankInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DisbursementService {

    private static final Logger logger = LoggerFactory.getLogger(DisbursementService.class);

    // Local cache for Redis deserialization workaround (same approach as your other services)
    private final Map<String, DisbursementTransaction> disbursementCache = new ConcurrentHashMap<>();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RepaymentService repaymentService;

    /**
     * Initiate fund disbursement for an approved loan
     */
    public DisbursementTransaction initiateDisbursement(String loanId,
                                                        DisbursementMethod method,
                                                        String bankAccount,
                                                        String bankName) throws Exception {
        logger.info("=== INITIATING DISBURSEMENT === Loan: {}", loanId);

        // Validate loan account
        LoanAccount loanAccount = repaymentService.getLoanAccount(loanId);
        if (loanAccount == null) {
            throw new Exception("Loan account not found: " + loanId);
        }

        if (loanAccount.getLoanStatus() != LoanStatus.ACTIVE) {
            throw new Exception("Loan is not active. Status: " + loanAccount.getLoanStatus());
        }

        // Check if disbursement already exists
        if (disbursementExistsForLoan(loanId)) {
            throw new Exception("Disbursement already initiated for loan: " + loanId);
        }

        // Create disbursement transaction
        DisbursementTransaction disbursement = new DisbursementTransaction(
                loanId,
                loanAccount.getStudentId(),
                loanAccount.getApplicationId(),
                loanAccount.getPrincipalAmount(),
                method,
                bankAccount,
                bankName
        );

        // Store disbursement
        storeDisbursement(disbursement);

        // Send notification
        notificationService.sendDisbursementInitiatedNotification(
                loanAccount.getStudentId(),
                disbursement
        );

        logger.info("Disbursement initiated: {} for loan: {}, amount: {}",
                disbursement.getDisbursementId(), loanId, disbursement.getAmount());

        return disbursement;
    }

    /**
     * Process disbursement (simulate bank processing)
     */
    public DisbursementTransaction processDisbursement(String disbursementId) throws Exception {
        logger.info("=== PROCESSING DISBURSEMENT === ID: {}", disbursementId);

        DisbursementTransaction disbursement = getDisbursement(disbursementId);
        if (disbursement == null) {
            throw new Exception("Disbursement not found: " + disbursementId);
        }

        if (disbursement.getStatus() != DisbursementStatus.PENDING) {
            throw new Exception("Disbursement not in pending status: " + disbursement.getStatus());
        }

        // Update status to processing
        disbursement.setStatus(DisbursementStatus.PROCESSING);
        disbursement.setProcessedAt(LocalDateTime.now());

        // Generate tracking number
        disbursement.setTrackingNumber("TRK" + System.currentTimeMillis());

        storeDisbursement(disbursement);

        // Send notification
        notificationService.sendDisbursementProcessingNotification(
                disbursement.getStudentId(),
                disbursement
        );

        logger.info("Disbursement processing: {}", disbursementId);

        // Simulate async bank processing
        simulateBankProcessing(disbursement);

        return disbursement;
    }

    /**
     * Complete disbursement (funds sent successfully)
     */
    public DisbursementTransaction completeDisbursement(String disbursementId) throws Exception {
        logger.info("=== COMPLETING DISBURSEMENT === ID: {}", disbursementId);

        DisbursementTransaction disbursement = getDisbursement(disbursementId);
        if (disbursement == null) {
            throw new Exception("Disbursement not found: " + disbursementId);
        }

        disbursement.setStatus(DisbursementStatus.COMPLETED);
        disbursement.setCompletedAt(LocalDateTime.now());

        storeDisbursement(disbursement);

        // Send completion notification
        notificationService.sendDisbursementCompletedNotification(
                disbursement.getStudentId(),
                disbursement
        );

        logger.info("Disbursement completed: {}, amount: {} sent to student",
                disbursementId, disbursement.getNetAmount());

        return disbursement;
    }

    /**
     * Mark disbursement as failed
     */
    public DisbursementTransaction failDisbursement(String disbursementId, String reason) throws Exception {
        logger.error("=== FAILING DISBURSEMENT === ID: {} - Reason: {}", disbursementId, reason);

        DisbursementTransaction disbursement = getDisbursement(disbursementId);
        if (disbursement == null) {
            throw new Exception("Disbursement not found: " + disbursementId);
        }

        disbursement.setStatus(DisbursementStatus.FAILED);
        disbursement.setFailureReason(reason);

        storeDisbursement(disbursement);

        // Send failure notification
        notificationService.sendDisbursementFailedNotification(
                disbursement.getStudentId(),
                disbursement,
                reason
        );

        return disbursement;
    }

    /**
     * Get disbursement by ID with LinkedHashMap conversion support (same as your pattern)
     */
    public DisbursementTransaction getDisbursement(String disbursementId) {
        logger.info("=== GETTING DISBURSEMENT === ID: {}", disbursementId);

        try {
            // Check cache first
            DisbursementTransaction cached = disbursementCache.get(disbursementId);
            if (cached != null) {
                logger.debug("Retrieved disbursement from cache: {}", disbursementId);
                return cached;
            }

            // Get from Redis
            String key = "disbursement:" + disbursementId;
            Object result = redisTemplate.opsForValue().get(key);

            if (result == null) {
                logger.warn("Disbursement not found: {}", disbursementId);
                return null;
            }

            // Handle LinkedHashMap conversion (same pattern as your ApplicationController)
            if (result instanceof java.util.LinkedHashMap) {
                logger.debug("Converting LinkedHashMap to DisbursementTransaction: {}", disbursementId);
                DisbursementTransaction disbursement = convertMapToDisbursement((java.util.LinkedHashMap<String, Object>) result);
                if (disbursement != null) {
                    disbursementCache.put(disbursementId, disbursement);
                }
                return disbursement;
            } else if (result instanceof DisbursementTransaction) {
                DisbursementTransaction disbursement = (DisbursementTransaction) result;
                disbursementCache.put(disbursementId, disbursement);
                return disbursement;
            }

            logger.error("Unexpected object type for disbursement: {}", result.getClass().getName());
            return null;

        } catch (Exception e) {
            logger.error("Error retrieving disbursement: {}", disbursementId, e);
            return null;
        }
    }

    /**
     * Get disbursements for a student
     */
    public List<DisbursementTransaction> getStudentDisbursements(String studentId) {
        logger.info("=== GETTING STUDENT DISBURSEMENTS === StudentID: {}", studentId);

        try {
            Set<String> keys = redisTemplate.keys("disbursement:*");
            if (keys == null) return Collections.emptyList();

            return keys.stream()
                    .filter(Objects::nonNull)
                    .map(key -> {
                        String disbursementId = key.replace("disbursement:", "");
                        return getDisbursement(disbursementId);
                    })
                    .filter(Objects::nonNull)
                    .filter(disbursement -> studentId.equals(disbursement.getStudentId()))
                    .sorted((d1, d2) -> d2.getInitiatedAt().compareTo(d1.getInitiatedAt()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving student disbursements: {}", studentId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get disbursement by loan ID
     */
    public DisbursementTransaction getDisbursementByLoanId(String loanId) {
        logger.info("=== GETTING DISBURSEMENT BY LOAN === LoanID: {}", loanId);

        try {
            Set<String> keys = redisTemplate.keys("disbursement:*");
            if (keys == null) return null;

            return keys.stream()
                    .filter(Objects::nonNull)
                    .map(key -> {
                        String disbursementId = key.replace("disbursement:", "");
                        return getDisbursement(disbursementId);
                    })
                    .filter(Objects::nonNull)
                    .filter(disbursement -> loanId.equals(disbursement.getLoanId()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            logger.error("Error retrieving disbursement by loan ID: {}", loanId, e);
            return null;
        }
    }

    /**
     * Auto-initiate disbursement for approved loan with student's bank info
     */
    public DisbursementTransaction autoInitiateDisbursement(LoanAccount loanAccount,
                                                            SimpleBankInfo bankInfo) throws Exception {
        logger.info("=== AUTO-INITIATING DISBURSEMENT === Loan: {}", loanAccount.getLoanId());

        // Determine best disbursement method based on location and bank
        DisbursementMethod method = determineBestDisbursementMethod(bankInfo.getBankName());

        return initiateDisbursement(
                loanAccount.getLoanId(),
                method,
                bankInfo.getAccountNumber(),
                bankInfo.getBankName()
        );
    }

    /**
     * Get disbursement statistics
     */
    public Map<String, Object> getDisbursementStatistics() {
        logger.info("=== GETTING DISBURSEMENT STATISTICS ===");

        try {
            Set<String> keys = redisTemplate.keys("disbursement:*");
            if (keys == null) return Collections.emptyMap();

            List<DisbursementTransaction> allDisbursements = keys.stream()
                    .filter(Objects::nonNull)
                    .map(key -> {
                        String disbursementId = key.replace("disbursement:", "");
                        return getDisbursement(disbursementId);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

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
        } catch (Exception e) {
            logger.error("Error calculating disbursement statistics", e);
            return Collections.emptyMap();
        }
    }

    // Private helper methods

    private void storeDisbursement(DisbursementTransaction disbursement) {
        try {
            String key = "disbursement:" + disbursement.getDisbursementId();
            logger.debug("Storing disbursement: {}", disbursement.getDisbursementId());

            // Store in Redis
            redisTemplate.opsForValue().set(key, disbursement, Duration.ofDays(365));

            // Store in cache
            disbursementCache.put(disbursement.getDisbursementId(), disbursement);

            // Index by loan ID
            redisTemplate.opsForValue().set(
                    "disbursement_by_loan:" + disbursement.getLoanId(),
                    disbursement.getDisbursementId(),
                    Duration.ofDays(365)
            );

            // Index by student ID
            redisTemplate.opsForSet().add(
                    "student_disbursements:" + disbursement.getStudentId(),
                    disbursement.getDisbursementId()
            );

        } catch (Exception e) {
            logger.error("Error storing disbursement: {}", disbursement.getDisbursementId(), e);
        }
    }

    private boolean disbursementExistsForLoan(String loanId) {
        try {
            String indexKey = "disbursement_by_loan:" + loanId;
            return redisTemplate.hasKey(indexKey);
        } catch (Exception e) {
            logger.error("Error checking disbursement existence for loan: {}", loanId, e);
            return false;
        }
    }

    private DisbursementMethod determineBestDisbursementMethod(String bankName) {
        // Simple logic - in real implementation, use comprehensive bank/country mapping
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

    private void simulateBankProcessing(DisbursementTransaction disbursement) {
        // Simulate async bank processing
        new Thread(() -> {
            try {
                // Simulate processing time
                Thread.sleep(10000); // 10 seconds for demo

                // 95% success rate simulation
                if (Math.random() < 0.95) {
                    disbursement.setStatus(DisbursementStatus.SENT);
                    storeDisbursement(disbursement);

                    // Simulate delivery time
                    Thread.sleep(5000); // 5 more seconds

                    completeDisbursement(disbursement.getDisbursementId());
                } else {
                    failDisbursement(disbursement.getDisbursementId(), "Bank processing failed");
                }
            } catch (Exception e) {
                logger.error("Error in simulated bank processing", e);
                try {
                    failDisbursement(disbursement.getDisbursementId(), "Processing error: " + e.getMessage());
                } catch (Exception ex) {
                    logger.error("Error failing disbursement", ex);
                }
            }
        }).start();
    }

    /**
     * Convert LinkedHashMap to DisbursementTransaction (same pattern as your ApplicationController)
     */
    @SuppressWarnings("unchecked")
    private DisbursementTransaction convertMapToDisbursement(Map<String, Object> map) {
        try {
            DisbursementTransaction disbursement = new DisbursementTransaction();

            // Set basic fields
            disbursement.setDisbursementId((String) map.get("disbursementId"));
            disbursement.setLoanId((String) map.get("loanId"));
            disbursement.setStudentId((String) map.get("studentId"));
            disbursement.setApplicationId((String) map.get("applicationId"));
            disbursement.setStudentBankAccount((String) map.get("studentBankAccount"));
            disbursement.setStudentBankName((String) map.get("studentBankName"));
            disbursement.setReferenceNumber((String) map.get("referenceNumber"));
            disbursement.setTrackingNumber((String) map.get("trackingNumber"));
            disbursement.setFailureReason((String) map.get("failureReason"));
            disbursement.setNotes((String) map.get("notes"));

            // Handle BigDecimal fields
            if (map.get("amount") != null) {
                disbursement.setAmount(new BigDecimal(map.get("amount").toString()));
            }
            if (map.get("exchangeRate") != null) {
                disbursement.setExchangeRate(new BigDecimal(map.get("exchangeRate").toString()));
            }
            if (map.get("fees") != null) {
                disbursement.setFees(new BigDecimal(map.get("fees").toString()));
            }
            if (map.get("netAmount") != null) {
                disbursement.setNetAmount(new BigDecimal(map.get("netAmount").toString()));
            }

            // Handle Enum fields
            if (map.get("disbursementMethod") != null) {
                disbursement.setDisbursementMethod(DisbursementMethod.valueOf(map.get("disbursementMethod").toString()));
            }
            if (map.get("status") != null) {
                disbursement.setStatus(DisbursementStatus.valueOf(map.get("status").toString()));
            }

            // Handle LocalDateTime fields (same pattern as your ApplicationController)
            if (map.get("initiatedAt") instanceof List) {
                List<Integer> dateTimeList = (List<Integer>) map.get("initiatedAt");
                disbursement.setInitiatedAt(LocalDateTime.of(
                        dateTimeList.get(0), dateTimeList.get(1), dateTimeList.get(2),
                        dateTimeList.get(3), dateTimeList.get(4), dateTimeList.get(5),
                        dateTimeList.size() > 6 ? dateTimeList.get(6) : 0
                ));
            }

            if (map.get("processedAt") instanceof List) {
                List<Integer> dateTimeList = (List<Integer>) map.get("processedAt");
                disbursement.setProcessedAt(LocalDateTime.of(
                        dateTimeList.get(0), dateTimeList.get(1), dateTimeList.get(2),
                        dateTimeList.get(3), dateTimeList.get(4), dateTimeList.get(5),
                        dateTimeList.size() > 6 ? dateTimeList.get(6) : 0
                ));
            }

            if (map.get("completedAt") instanceof List) {
                List<Integer> dateTimeList = (List<Integer>) map.get("completedAt");
                disbursement.setCompletedAt(LocalDateTime.of(
                        dateTimeList.get(0), dateTimeList.get(1), dateTimeList.get(2),
                        dateTimeList.get(3), dateTimeList.get(4), dateTimeList.get(5),
                        dateTimeList.size() > 6 ? dateTimeList.get(6) : 0
                ));
            }

            if (map.get("estimatedDelivery") instanceof List) {
                List<Integer> dateTimeList = (List<Integer>) map.get("estimatedDelivery");
                disbursement.setEstimatedDelivery(LocalDateTime.of(
                        dateTimeList.get(0), dateTimeList.get(1), dateTimeList.get(2),
                        dateTimeList.get(3), dateTimeList.get(4), dateTimeList.get(5),
                        dateTimeList.size() > 6 ? dateTimeList.get(6) : 0
                ));
            }

            return disbursement;

        } catch (Exception e) {
            logger.error("Error converting map to DisbursementTransaction", e);
            return null;
        }
    }
}