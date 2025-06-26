package com.example.qard_hasan_for_education.controller;

import com.example.qard_hasan_for_education.model.*;
import com.example.qard_hasan_for_education.model.individual.*;
import com.example.qard_hasan_for_education.service.RepaymentService;
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
@RequestMapping("/api/repayment")
@CrossOrigin(origins = "*")
public class RepaymentController {

    private static final Logger logger = LoggerFactory.getLogger(RepaymentController.class);

    @Autowired
    private RepaymentService repaymentService;

    /**
     * Create a new loan account from approved application - STATELESS
     */
    @PostMapping("/create-loan")
    public ResponseEntity<?> createLoanAccount(@RequestBody CreateLoanRequest request) {
        try {
            StudentApplicationData mockApplication = createMockApplication(request);

            LoanAccount loanAccount = repaymentService.createLoanAccount(
                    mockApplication,
                    request.getLoanAmount(),
                    request.getTermMonths()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Loan account calculated successfully");
            response.put("loanAccount", loanAccount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error creating loan account", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error creating loan account: " + e.getMessage()));
        }
    }

    /**
     * Calculate repayment result - STATELESS
     */
    @PostMapping("/calculate-payment")
    public ResponseEntity<?> calculateRepayment(@RequestBody PaymentCalculationRequest request) {
        try {
            RepaymentService.PaymentResult result = repaymentService.calculateRepayment(
                    request.getLoanId(),
                    request.getAmount(),
                    request.getPaymentMethod(),
                    request.getLoanAccount() // Framework will provide current loan state
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment calculated successfully");
            response.put("transaction", result.getTransaction());
            response.put("updatedLoanAccount", result.getUpdatedLoanAccount());
            response.put("mentoringOfferSent", result.isMentoringOfferSent());

            if (result.getOfferId() != null) {
                response.put("offerId", result.getOfferId());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating payment", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Calculate upcoming payment information - STATELESS
     */
    @PostMapping("/calculate-upcoming-payment")
    public ResponseEntity<?> calculateUpcomingPayment(@RequestBody UpcomingPaymentRequest request) {
        try {
            Map<String, Object> paymentInfo = repaymentService.calculateUpcomingPayment(request.getLoanAccount());

            if (paymentInfo.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Unable to calculate payment info"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("upcomingPayment", paymentInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating upcoming payment info", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error calculating payment information"));
        }
    }

    /**
     * Calculate loan dashboard summary - STATELESS
     */
    @PostMapping("/calculate-dashboard")
    public ResponseEntity<?> calculateLoanDashboard(@RequestBody DashboardCalculationRequest request) {
        try {
            Map<String, Object> dashboard = repaymentService.calculateLoanDashboard(request.getLoans());

            return ResponseEntity.ok(Map.of("success", true, "dashboard", dashboard));

        } catch (Exception e) {
            logger.error("Error calculating loan dashboard", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error calculating dashboard"));
        }
    }

    /**
     * Validate loan data - STATELESS
     */
    @PostMapping("/validate-loan")
    public ResponseEntity<?> validateLoan(@RequestBody LoanValidationRequest request) {
        try {
            Map<String, Object> validation = repaymentService.validateLoan(request.getLoanAccount());

            return ResponseEntity.ok(Map.of("success", true, "validation", validation));

        } catch (Exception e) {
            logger.error("Error validating loan", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error validating loan"));
        }
    }

    /**
     * Calculate payment schedule - STATELESS
     */
    @PostMapping("/calculate-payment-schedule")
    public ResponseEntity<?> calculatePaymentSchedule(@RequestBody PaymentScheduleRequest request) {
        try {
            List<Map<String, Object>> schedule = repaymentService.calculatePaymentSchedule(request.getLoanAccount());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("paymentSchedule", schedule);
            response.put("totalRemainingInstallments", schedule.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating payment schedule", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error calculating payment schedule"));
        }
    }

    /**
     * Calculate payment history summary - STATELESS
     */
    @PostMapping("/calculate-payment-summary")
    public ResponseEntity<?> calculatePaymentSummary(@RequestBody PaymentSummaryRequest request) {
        try {
            Map<String, Object> summary = repaymentService.calculatePaymentSummary(
                    request.getTransactions(),
                    request.getLoanAccount()
            );

            return ResponseEntity.ok(Map.of("success", true, "summary", summary));

        } catch (Exception e) {
            logger.error("Error calculating payment summary", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error calculating payment summary"));
        }
    }

    // Helper method to create mock application for testing
    private StudentApplicationData createMockApplication(CreateLoanRequest request) {
        StudentApplicationData application = new StudentApplicationData(
                "APP_MOCK_" + System.currentTimeMillis(),
                request.getStudentId()
        );

        // Create mock passport info
        PassportInfo passportInfo = new PassportInfo();
        passportInfo.setFullName(request.getStudentName());
        passportInfo.setPassportNumber("MOCK123456");
        passportInfo.setNationality("Indonesian");
        application.setPassportInfo(passportInfo);

        // Create mock university acceptance
        UniversityAcceptance universityAcceptance = new UniversityAcceptance();
        universityAcceptance.setUniversityName(request.getUniversityName());
        universityAcceptance.setStudentName(request.getStudentName());
        universityAcceptance.setProgram(request.getProgram());
        application.setUniversityAcceptance(universityAcceptance);

        return application;
    }

    // ===============================
    // REQUEST DTOs - UPDATED FOR STATELESS
    // ===============================

    public static class CreateLoanRequest {
        private String studentId;
        private String studentName;
        private String universityName;
        private String program;
        private BigDecimal loanAmount;
        private Integer termMonths;

        // Getters and setters
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public String getUniversityName() { return universityName; }
        public void setUniversityName(String universityName) { this.universityName = universityName; }
        public String getProgram() { return program; }
        public void setProgram(String program) { this.program = program; }
        public BigDecimal getLoanAmount() { return loanAmount; }
        public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }
        public Integer getTermMonths() { return termMonths; }
        public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }
    }

    public static class PaymentCalculationRequest {
        private String loanId;
        private BigDecimal amount;
        private String paymentMethod;
        private LoanAccount loanAccount; // Framework will provide current state

        // Getters and setters
        public String getLoanId() { return loanId; }
        public void setLoanId(String loanId) { this.loanId = loanId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public LoanAccount getLoanAccount() { return loanAccount; }
        public void setLoanAccount(LoanAccount loanAccount) { this.loanAccount = loanAccount; }
    }

    public static class UpcomingPaymentRequest {
        private LoanAccount loanAccount;

        public LoanAccount getLoanAccount() { return loanAccount; }
        public void setLoanAccount(LoanAccount loanAccount) { this.loanAccount = loanAccount; }
    }

    public static class DashboardCalculationRequest {
        private List<LoanAccount> loans;

        public List<LoanAccount> getLoans() { return loans; }
        public void setLoans(List<LoanAccount> loans) { this.loans = loans; }
    }

    public static class LoanValidationRequest {
        private LoanAccount loanAccount;

        public LoanAccount getLoanAccount() { return loanAccount; }
        public void setLoanAccount(LoanAccount loanAccount) { this.loanAccount = loanAccount; }
    }

    public static class PaymentScheduleRequest {
        private LoanAccount loanAccount;

        public LoanAccount getLoanAccount() { return loanAccount; }
        public void setLoanAccount(LoanAccount loanAccount) { this.loanAccount = loanAccount; }
    }

    public static class PaymentSummaryRequest {
        private List<RepaymentTransaction> transactions;
        private LoanAccount loanAccount;

        public List<RepaymentTransaction> getTransactions() { return transactions; }
        public void setTransactions(List<RepaymentTransaction> transactions) { this.transactions = transactions; }
        public LoanAccount getLoanAccount() { return loanAccount; }
        public void setLoanAccount(LoanAccount loanAccount) { this.loanAccount = loanAccount; }
    }
}