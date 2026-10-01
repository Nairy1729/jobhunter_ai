package com.jobhunter.service.tailoring.offerpilot;

/**
 * Text Extractor & Sanitizer for Master Resumes.
 * Enforces OfferPilot sanitization rules and minimum text length thresholds.
 */
public class ResumeTextSanitizer {

    public static final int MIN_READABLE_TEXT_LENGTH = 100;

    /**
     * Sanitizes raw extracted resume text according to OfferPilot specification:
     * 1. Strip null bytes: replace("\u0000", "")
     * 2. Replace horizontal/vertical tabs and form feeds with a single space: replaceAll("[\\t\\x0B\\f\\r]+", " ")
     * 3. Collapse consecutive spaces into a single space: replaceAll(" +", " ")
     * 4. Collapse excessive newlines (3 or more) into double newlines: replaceAll("\\n{3,}", "\n\n")
     * 5. Validation: Ensure extracted text length is at least 100 characters.
     */
    public static String sanitize(String rawText) {
        if (rawText == null) {
            throw new IllegalArgumentException("Could not extract readable text from resume (resume text is null)");
        }

        // 1. Strip null bytes
        String text = rawText.replace("\u0000", "");

        // 2. Replace horizontal/vertical tabs and form feeds with a single space
        text = text.replaceAll("[\\t\\x0B\\f\\r]+", " ");

        // 3. Collapse consecutive spaces into a single space
        text = text.replaceAll(" +", " ");

        // 4. Collapse excessive newlines (3 or more) into double newlines
        text = text.replaceAll("\\n{3,}", "\n\n");

        text = text.trim();

        // 5. Validation: Minimum 100 characters
        if (text.length() < MIN_READABLE_TEXT_LENGTH) {
            throw new IllegalArgumentException("Could not extract readable text from resume (extracted text length: " 
                    + text.length() + ", minimum required: " + MIN_READABLE_TEXT_LENGTH + ")");
        }

        return text;
    }
}
