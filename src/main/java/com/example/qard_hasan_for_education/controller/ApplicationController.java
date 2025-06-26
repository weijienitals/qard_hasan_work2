// ApplicationController.java - DEBUG VERSION
package com.example.qard_hasan_for_education.controller;
import com.example.qard_hasan_for_education.model.StudentApplicationData;
import com.example.qard_hasan_for_education.service.DocumentOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "*")
public class ApplicationController {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationController.class);

    @Autowired
    private DocumentOrchestrationService orchestrationService;



    /**
     * Submit complete application
     */
    @PostMapping("/submit-complete")
    public ResponseEntity<?> submitCompleteApplication(
            @RequestParam("studentId") String studentId,
            @RequestParam("bankStatement") MultipartFile bankStatement,
            @RequestParam("universityLetter") MultipartFile universityLetter,
            @RequestParam("scholarshipLetter") MultipartFile scholarshipLetter,
            @RequestParam("passportImage") MultipartFile passportImage) {

        logger.info("=== SUBMIT COMPLETE APPLICATION CALLED === Student: {}", studentId);

        try {
            // Validate all files first
            orchestrationService.validateFiles(bankStatement, universityLetter, scholarshipLetter, passportImage);

            // Process the complete application with risk assessment
            StudentApplicationData result = orchestrationService.processCompleteApplication(
                    studentId, bankStatement, universityLetter, scholarshipLetter, passportImage);

            logger.info("Complete application processed successfully for student: {}, applicationId: {}, riskLevel: {}",
                    studentId, result.getApplicationId(),
                    result.getRiskProfile() != null ? result.getRiskProfile().getOverallRisk() : "N/A");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application processed successfully");
            response.put("application", result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing complete application for student: {}", studentId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error processing complete application: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    }
