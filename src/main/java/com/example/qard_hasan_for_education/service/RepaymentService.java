package com.example.qard_hasan_for_education.service;

import com.example.qard_hasan_for_education.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RepaymentService {

    private static final Logger logger = LoggerFactory.getLogger(RepaymentService.class);

    @Autowired
    private VolunteeringService volunteeringService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Create a new loan account from approved application - STATELESS
     */
    public LoanAccount createLoanAccount(StudentApplicationData application, BigDecimal loanAmount, Integer termMonths) {
        logger.info("Creating loan account for application: {}", application.getApplicationId());

        LoanAccount loanAccount = new LoanAccount(
                application.getStudentId(),
                application.getApplicationId(),
                loanAmount,
                termMonths,
                application.getPassportInfo().getFullName(),
                application.getUniversityAcceptance().getUniversityName(),
                application.getUniversityAcceptance().getProgram(),
                determineUniversityCountry(application.getUniversityAcceptance().getUniversityName()),
                application.getPassportInfo().getNationality()
        );

        logger.info("Loan account created: {} for student: {}, amount: {}, term: {} months",
                loanAccount.getLoanId(), loanAccount.getStudentId(), loanAmount, termMonths);

        return loanAccount; // ✅ Just return, don't store
    }

    /**
     * Calculate repayment result - STATELESS (no actual processing/storage)
     */
    public PaymentResult calculateRepayment(String loanId, BigDecimal amount, String paymentMethod,
                                            LoanAccount loanAccount) throws Exception {
        logger.info("Calculating repayment for loan: {}, amount: {}", loanId, amount);

        if (loanAccount == null) {
            throw new Exception("Loan account not found: " + loanId);
        }

        if (loanAccount.getLoanStatus() != LoanStatus.ACTIVE) {
            throw new Exception("Loan is not active. Current status: " + loanAccount.getLoanStatus());
        }

        // Validate payment amount
        if (amount.compareTo(loanAccount.getMonthlyInstallment()) < 0) {
            throw new Exception("Payment amount is less than required installment: " + loanAccount.getMonthlyInstallment());
        }

        // Create transaction record (but don't store it)
        RepaymentTransaction transaction = new RepaymentTransaction(
                loanId,
                loanAccount.getStudentId(),
                amount,
                loanAccount.getCompletedInstallments() + 1,
                paymentMethod
        );

        // Simulate payment success
        boolean paymentSuccessful = simulatePaymentGateway();

        if (paymentSuccessful) {
            transaction.setStatus(PaymentStatus.COMPLETED);

            // Calculate updated loan state (but don't persist it)
            LoanAccount updatedLoanAccount = calculateUpdatedLoanAccount(loanAccount, amount);

            // Calculate mentoring eligibility
            boolean mentoringEligible = updatedLoanAccount.isEligibleForMentoring();
            String offerId = null;

            if (mentoringEligible) {
                offerId = generateMentoringOfferId(updatedLoanAccount, transaction);
                transaction.setMentoringOfferSent(true);
            }

            logger.info("Repayment calculation successful for loan: {}, transaction: {}", loanId, transaction.getTransactionId());

            return new PaymentResult(transaction, updatedLoanAccount, mentoringEligible, offerId);
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            throw new Exception("Payment processing failed");
        }
    }

    /**
     * Calculate upcoming payment information - STATELESS
     */
    public Map<String, Object> calculateUpcomingPayment(LoanAccount loanAccount) {
        if (loanAccount == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> paymentInfo = new HashMap<>();
        paymentInfo.put("dueDate", loanAccount.getNextPaymentDate());
        paymentInfo.put("amount", loanAccount.getMonthlyInstallment());
        paymentInfo.put("remainingBalance", loanAccount.getRemainingBalance());
        paymentInfo.put("installmentNumber", loanAccount.getCompletedInstallments() + 1);
        paymentInfo.put("totalInstallments", loanAccount.getTotalInstallments());

        // Calculate days until due
        long daysUntilDue = Duration.between(LocalDate.now().atStartOfDay(),
                loanAccount.getNextPaymentDate().atStartOfDay()).toDays();
        paymentInfo.put("daysUntilDue", daysUntilDue);

        return paymentInfo;
    }

    /**
     * Calculate loan dashboard data - STATELESS
     */
    public Map<String, Object> calculateLoanDashboard(List<LoanAccount> loans) {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalLoans", loans.size());

        // Calculate totals
        BigDecimal totalPrincipal = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;
        int totalCompletedPayments = 0;
        int totalUpcomingPayments = 0;

        for (LoanAccount loan : loans) {
            totalPrincipal = totalPrincipal.add(loan.getPrincipalAmount());
            totalRemaining = totalRemaining.add(loan.getRemainingBalance());
            totalCompletedPayments += loan.getCompletedInstallments();
            totalUpcomingPayments += (loan.getTotalInstallments() - loan.getCompletedInstallments());
        }

        dashboard.put("totalPrincipal", totalPrincipal);
        dashboard.put("totalRemaining", totalRemaining);
        dashboard.put("totalPaid", totalPrincipal.subtract(totalRemaining));
        dashboard.put("completedPayments", totalCompletedPayments);
        dashboard.put("upcomingPayments", totalUpcomingPayments);
        dashboard.put("loans", loans);

        // Calculate repayment progress
        if (totalPrincipal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal progressPercentage = totalPrincipal.subtract(totalRemaining)
                    .divide(totalPrincipal, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            dashboard.put("repaymentProgress", progressPercentage);
        } else {
            dashboard.put("repaymentProgress", BigDecimal.ZERO);
        }

        return dashboard;
    }

    /**
     * Validate loan data - STATELESS
     */
    public Map<String, Object> validateLoan(LoanAccount loanAccount) {
        Map<String, Object> validation = new HashMap<>();

        if (loanAccount == null) {
            validation.put("valid", false);
            validation.put("message", "Loan account is required");
            return validation;
        }

        // Validate loan status
        boolean isActive = loanAccount.getLoanStatus() == LoanStatus.ACTIVE;
        boolean hasRemainingBalance = loanAccount.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0;
        boolean hasRemainingInstallments = loanAccount.getCompletedInstallments() < loanAccount.getTotalInstallments();

        validation.put("valid", isActive && hasRemainingBalance && hasRemainingInstallments);
        validation.put("isActive", isActive);
        validation.put("hasRemainingBalance", hasRemainingBalance);
        validation.put("hasRemainingInstallments", hasRemainingInstallments);
        validation.put("loanStatus", loanAccount.getLoanStatus());
        validation.put("remainingBalance", loanAccount.getRemainingBalance());
        validation.put("installmentProgress", loanAccount.getCompletedInstallments() + "/" + loanAccount.getTotalInstallments());

        return validation;
    }

    /**
     * Calculate payment schedule - STATELESS
     */
    public List<Map<String, Object>> calculatePaymentSchedule(LoanAccount loanAccount) {
        List<Map<String, Object>> schedule = new ArrayList<>();

        if (loanAccount == null) {
            return schedule;
        }

        LocalDate currentDate = loanAccount.getNextPaymentDate();
        BigDecimal remainingBalance = loanAccount.getRemainingBalance();

        for (int i = loanAccount.getCompletedInstallments() + 1; i <= loanAccount.getTotalInstallments(); i++) {
            Map<String, Object> installment = new HashMap<>();
            installment.put("installmentNumber", i);
            installment.put("dueDate", currentDate);
            installment.put("amount", loanAccount.getMonthlyInstallment());
            installment.put("remainingBalanceAfter", remainingBalance.subtract(loanAccount.getMonthlyInstallment()));
            installment.put("status", "PENDING");

            schedule.add(installment);

            currentDate = currentDate.plusMonths(1);
            remainingBalance = remainingBalance.subtract(loanAccount.getMonthlyInstallment());

            if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }

        return schedule;
    }

    /**
     * Calculate payment history summary - STATELESS
     */
    public Map<String, Object> calculatePaymentSummary(List<RepaymentTransaction> transactions, LoanAccount loanAccount) {
        Map<String, Object> summary = new HashMap<>();

        if (transactions == null) {
            transactions = Collections.emptyList();
        }

        // Calculate totals
        BigDecimal totalPaid = transactions.stream()
                .filter(t -> t.getStatus() == PaymentStatus.COMPLETED)
                .map(RepaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completedPayments = transactions.stream()
                .filter(t -> t.getStatus() == PaymentStatus.COMPLETED)
                .count();

        long failedPayments = transactions.stream()
                .filter(t -> t.getStatus() == PaymentStatus.FAILED)
                .count();

        summary.put("totalTransactions", transactions.size());
        summary.put("completedPayments", completedPayments);
        summary.put("failedPayments", failedPayments);
        summary.put("totalAmountPaid", totalPaid);
        summary.put("transactions", transactions);

        if (loanAccount != null) {
            summary.put("remainingBalance", loanAccount.getRemainingBalance());
            summary.put("nextPaymentAmount", loanAccount.getMonthlyInstallment());
            summary.put("nextPaymentDate", loanAccount.getNextPaymentDate());
        }

        return summary;
    }

    /**
     * Calculate updated loan account state after payment - PURE FUNCTION
     */
    private LoanAccount calculateUpdatedLoanAccount(LoanAccount originalLoan, BigDecimal paymentAmount) {
        // Create a copy to avoid mutating the original
        LoanAccount updatedLoan = new LoanAccount();

        // Copy all fields
        updatedLoan.setLoanId(originalLoan.getLoanId());
        updatedLoan.setStudentId(originalLoan.getStudentId());
        updatedLoan.setApplicationId(originalLoan.getApplicationId());
        updatedLoan.setStudentName(originalLoan.getStudentName());
        updatedLoan.setUniversityName(originalLoan.getUniversityName());
        updatedLoan.setProgram(originalLoan.getProgram());
        updatedLoan.setUniversityCountry(originalLoan.getUniversityCountry());
        updatedLoan.setNationality(originalLoan.getNationality());
        updatedLoan.setPrincipalAmount(originalLoan.getPrincipalAmount());
        updatedLoan.setTotalInstallments(originalLoan.getTotalInstallments());
        updatedLoan.setMonthlyInstallment(originalLoan.getMonthlyInstallment());
        updatedLoan.setLoanStartDate(originalLoan.getLoanStartDate());
        updatedLoan.setCreatedAt(originalLoan.getCreatedAt());

        // Calculate updated values
        updatedLoan.setRemainingBalance(originalLoan.getRemainingBalance().subtract(paymentAmount));
        updatedLoan.setCompletedInstallments(originalLoan.getCompletedInstallments() + 1);
        updatedLoan.setNextPaymentDate(originalLoan.getNextPaymentDate().plusMonths(1));
        updatedLoan.setUpdatedAt(LocalDateTime.now());

        // Check if loan is completed
        if (updatedLoan.getCompletedInstallments() >= updatedLoan.getTotalInstallments() ||
                updatedLoan.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            updatedLoan.setLoanStatus(LoanStatus.COMPLETED);
            updatedLoan.setRemainingBalance(BigDecimal.ZERO);
        } else {
            updatedLoan.setLoanStatus(LoanStatus.ACTIVE);
        }

        logger.info("Calculated loan update: {} - Remaining balance: {}, Completed installments: {}/{}",
                updatedLoan.getLoanId(), updatedLoan.getRemainingBalance(),
                updatedLoan.getCompletedInstallments(), updatedLoan.getTotalInstallments());

        return updatedLoan;
    }

    /**
     * Generate mentoring offer ID - PURE FUNCTION
     */
    private String generateMentoringOfferId(LoanAccount loanAccount, RepaymentTransaction transaction) {
        try {
            if (loanAccount.isEligibleForMentoring()) {
                logger.info("Generating mentoring offer for student: {} after payment: {}",
                        loanAccount.getStudentId(), transaction.getTransactionId());

                // Generate unique offer ID
                String offerId = "MENTOR_" + loanAccount.getStudentId() + "_" + System.currentTimeMillis();

                logger.info("Generated mentoring offer ID: {}", offerId);
                return offerId;
            }
        } catch (Exception e) {
            logger.error("Error generating mentoring offer for loan: {}", loanAccount.getLoanId(), e);
        }
        return null;
    }

    private boolean simulatePaymentGateway() {
        // Simulate payment gateway processing
        try {
            Thread.sleep(100); // Reduce simulation time
            return Math.random() < 0.95; // 95% success rate
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String determineUniversityCountry(String universityName) {
        // Simple mapping - in real implementation, use a comprehensive database
        Map<String, String> universityCountryMap = Map.of(
                "Stanford University", "United States",
                "MIT", "United States",
                "Harvard University", "United States",
                "University of Oxford", "United Kingdom",
                "University of Cambridge", "United Kingdom",
                "ETH Zurich", "Switzerland",
                "University of Tokyo", "Japan",
                "National University of Singapore", "Singapore"
        );

        return universityCountryMap.getOrDefault(universityName, "Unknown");
    }

    /**
     * PaymentResult class - Enhanced for stateless approach
     */
    public static class PaymentResult {
        private final RepaymentTransaction transaction;
        private final LoanAccount updatedLoanAccount;
        private final boolean mentoringOfferSent;
        private final String offerId;

        public PaymentResult(RepaymentTransaction transaction, LoanAccount updatedLoanAccount,
                             boolean mentoringOfferSent, String offerId) {
            this.transaction = transaction;
            this.updatedLoanAccount = updatedLoanAccount;
            this.mentoringOfferSent = mentoringOfferSent;
            this.offerId = offerId;
        }

        public RepaymentTransaction getTransaction() { return transaction; }
        public LoanAccount getUpdatedLoanAccount() { return updatedLoanAccount; }
        public boolean isMentoringOfferSent() { return mentoringOfferSent; }
        public String getOfferId() { return offerId; }
    }
}