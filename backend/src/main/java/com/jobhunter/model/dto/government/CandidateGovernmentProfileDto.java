package com.jobhunter.model.dto.government;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CandidateGovernmentProfileDto {
    private Integer age;
    private LocalDate dob;
    private String gender; // MALE, FEMALE, TRANSGENDER, OTHER
    private String state;
    private String district;
    private String domicileState;
    private String domicileDistrict;
    private String highestEducation; // 8th, 10th, 12th, ITI, Diploma, Graduate, Post Graduate, B.Tech, etc.
    private List<String> degrees = new ArrayList<>();
    private Integer passingYear;
    private BigDecimal yearsOfExperience = BigDecimal.ZERO;
    private String category = "UR/GEN"; // UR/GEN, OBC_NCL, OBC_CL, SC, ST, EWS
    private boolean pwd = false;
    private boolean exServiceman = false;
    private List<String> preferredStates = new ArrayList<>();
    private List<String> preferredDistricts = new ArrayList<>();
    private List<String> preferredEmploymentTypes = new ArrayList<>();
    private List<String> skills = new ArrayList<>();

    public CandidateGovernmentProfileDto() {}

    // Getters and Setters
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getDomicileState() { return domicileState; }
    public void setDomicileState(String domicileState) { this.domicileState = domicileState; }

    public String getDomicileDistrict() { return domicileDistrict; }
    public void setDomicileDistrict(String domicileDistrict) { this.domicileDistrict = domicileDistrict; }

    public String getHighestEducation() { return highestEducation; }
    public void setHighestEducation(String highestEducation) { this.highestEducation = highestEducation; }

    public List<String> getDegrees() { return degrees; }
    public void setDegrees(List<String> degrees) { this.degrees = degrees; }

    public Integer getPassingYear() { return passingYear; }
    public void setPassingYear(Integer passingYear) { this.passingYear = passingYear; }

    public BigDecimal getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(BigDecimal yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isPwd() { return pwd; }
    public void setPwd(boolean pwd) { this.pwd = pwd; }

    public boolean isExServiceman() { return exServiceman; }
    public void setExServiceman(boolean exServiceman) { this.exServiceman = exServiceman; }

    public List<String> getPreferredStates() { return preferredStates; }
    public void setPreferredStates(List<String> preferredStates) { this.preferredStates = preferredStates; }

    public List<String> getPreferredDistricts() { return preferredDistricts; }
    public void setPreferredDistricts(List<String> preferredDistricts) { this.preferredDistricts = preferredDistricts; }

    public List<String> getPreferredEmploymentTypes() { return preferredEmploymentTypes; }
    public void setPreferredEmploymentTypes(List<String> preferredEmploymentTypes) { this.preferredEmploymentTypes = preferredEmploymentTypes; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
}
