package com.jobhunter.dto.tailoring.gemini;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured Job Description Intelligence.
 * Stage 2 of the Gemini AI Resume Tailoring Pipeline.
 */
public class JobDescriptionModel {

    public String role;
    public String seniority;
    public List<String> mandatory = new ArrayList<>();
    public List<String> preferred = new ArrayList<>();
    public List<String> requiredTechnologies = new ArrayList<>();
    public List<String> preferredTechnologies = new ArrayList<>();
    public List<String> responsibilities = new ArrayList<>();
    public String domainRequirements;
    public String educationRequirements;
    public String location;
    public String workMode;
    public String experienceRequirements;
    public List<String> keywords = new ArrayList<>();
}
