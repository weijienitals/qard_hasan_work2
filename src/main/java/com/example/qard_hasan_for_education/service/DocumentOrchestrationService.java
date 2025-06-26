package com.example.qard_hasan_for_education.service;

import com.example.qard_hasan_for_education.model.*;
import com.example.qard_hasan_for_education.model.individual.PassportInfo;
import com.example.qard_hasan_for_education.model.individual.ScholarshipAcceptance;
import com.example.qard_hasan_for_education.model.individual.SimpleBankInfo;
import com.example.qard_hasan_for_education.model.individual.UniversityAcceptance;
import com.example.qard_hasan_for_education.model.riskAnalysis.ApplicationRiskProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class DocumentOrchestrationService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentOrchestrationService.class);

    @Autowired
    private DocumentProcessor documentProcessor;

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    public StudentApplicationData processCompleteApplication(
            String studentId,
            MultipartFile bankStatement,
            MultipartFile universityLetter,
            MultipartFile scholarshipLetter,
            MultipartFile passportImage) throws Exception {

        String applicationId = generateApplicationId();
        logger.info("Starting document aggregation for student: {}, applicationId: {}",
                studentId, applicationId);

        StudentApplicationData application = new StudentApplicationData(applicationId, studentId);
        application.setProcessingStartTime(LocalDateTime.now());
        application.setStatus(ApplicationStatus.PROCESSING_DOCUMENTS);

        try {
            // Process all documents concurrently
            logger.info("Processing documents concurrently for application: {}", applicationId);

            long startTime = System.currentTimeMillis();

            CompletableFuture<SimpleBankInfo> bankFuture =
                    CompletableFuture.supplyAsync(() -> processDocumentSafely(() ->
                            documentProcessor.processBankDocument(bankStatement), "Bank Statement"));

            CompletableFuture<UniversityAcceptance> universityFuture =
                    CompletableFuture.supplyAsync(() -> processDocumentSafely(() ->
                            documentProcessor.processUniversityLetter(universityLetter), "University Letter"));

            CompletableFuture<ScholarshipAcceptance> scholarshipFuture =
                    CompletableFuture.supplyAsync(() -> processDocumentSafely(() ->
                            documentProcessor.processScholarshipLetter(scholarshipLetter), "Scholarship Letter"));

            CompletableFuture<PassportInfo> passportFuture =
                    CompletableFuture.supplyAsync(() -> processDocumentSafely(() ->
                            documentProcessor.processPassportImage(passportImage), "Passport"));

            // Wait for all documents to be processed
            CompletableFuture.allOf(bankFuture, universityFuture, scholarshipFuture, passportFuture)
                    .join();

            // Aggregate results
            application.setBankInfo(bankFuture.get());
            application.setUniversityAcceptance(universityFuture.get());
            application.setScholarshipAcceptance(scholarshipFuture.get());
            application.setPassportInfo(passportFuture.get());
            application.setStatus(ApplicationStatus.DOCUMENTS_PROCESSED);

            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("Documents processed and aggregated successfully for application: {} in {}ms",
                    applicationId, processingTime);

            // Perform risk assessment
            logger.info("Starting risk assessment for application: {}", applicationId);
            long riskAssessmentStart = System.currentTimeMillis();

            ApplicationRiskProfile riskProfile = riskAssessmentService.assessRisk(application);
            application.setRiskProfile(riskProfile);

            long riskAssessmentTime = System.currentTimeMillis() - riskAssessmentStart;
            logger.info("Risk assessment completed for application: {} in {}ms - Overall Risk: {}, Score: {}, Recommendation: {}",
                    applicationId, riskAssessmentTime, riskProfile.getOverallRisk(),
                    riskProfile.getRiskScore(), riskProfile.getApprovalRecommendation());

            // Set completion status and time
            application.setStatus(ApplicationStatus.COMPLETED);
            application.setProcessingEndTime(LocalDateTime.now());
            application.calculateProcessingTime();

            logger.info("Application aggregation complete: {}, status: {}, processing time: {}ms, risk level: {}",
                    applicationId, application.getStatus(), application.getProcessingTimeMs(),
                    application.getRiskProfile().getOverallRisk());

            return application;

        } catch (Exception e) {
            logger.error("Error processing application: {}", applicationId, e);
            application.setStatus(ApplicationStatus.FAILED);
            application.setProcessingEndTime(LocalDateTime.now());
            application.calculateProcessingTime();
            throw e;
        }
    }

    // Safe document processing for CompletableFuture
    private <T> T processDocumentSafely(CheckedSupplier<T> processor, String documentType) {
        try {
            logger.debug("Processing {} document", documentType);
            T result = processor.get();
            logger.debug("Completed processing {} document", documentType);
            return result;
        } catch (Exception e) {
            logger.error("Error processing {} document", documentType, e);
            throw new RuntimeException("Failed to process " + documentType + ": " + e.getMessage(), e);
        }
    }

    private String generateApplicationId() {
        return "APP_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Utility method to validate files before processing
    public void validateFiles(MultipartFile bankStatement, MultipartFile universityLetter,
                              MultipartFile scholarshipLetter, MultipartFile passportImage) throws Exception {

        validatePdfFile(bankStatement, "Bank Statement");
        validatePdfFile(universityLetter, "University Letter");
        validatePdfFile(scholarshipLetter, "Scholarship Letter");
        validateImageFile(passportImage, "Passport");
    }

    private void validatePdfFile(MultipartFile file, String documentType) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception(documentType + " is required");
        }

        if (!"application/pdf".equals(file.getContentType())) {
            throw new Exception(documentType + " must be a PDF file");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
            throw new Exception(documentType + " file size exceeds 10MB limit");
        }
    }

    private void validateImageFile(MultipartFile file, String documentType) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception(documentType + " is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") &&
                !contentType.equals("image/jpg") &&
                !contentType.equals("image/png") &&
                !contentType.equals("image/webp"))) {
            throw new Exception(documentType + " must be an image file (JPEG, PNG, or WEBP)");
        }

        if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit for images
            throw new Exception(documentType + " file size exceeds 5MB limit");
        }
    }

    // Functional interface to handle checked exceptions
    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}