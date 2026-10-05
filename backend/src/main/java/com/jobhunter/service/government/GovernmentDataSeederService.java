package com.jobhunter.service.government;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.entity.User;
import com.jobhunter.model.entity.government.*;
import com.jobhunter.repository.UserRepository;
import com.jobhunter.repository.government.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Order(20)
public class GovernmentDataSeederService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GovernmentDataSeederService.class);

    private final GovernmentSourceRepository sourceRepository;
    private final GovernmentJobRepository jobRepository;
    private final GovernmentJobEligibilityRepository eligibilityRepository;
    private final GovernmentJobEvidenceRepository evidenceRepository;
    private final GovernmentJobCorrigendumRepository corrigendumRepository;
    private final CandidateGovernmentProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public GovernmentDataSeederService(
            GovernmentSourceRepository sourceRepository,
            GovernmentJobRepository jobRepository,
            GovernmentJobEligibilityRepository eligibilityRepository,
            GovernmentJobEvidenceRepository evidenceRepository,
            GovernmentJobCorrigendumRepository corrigendumRepository,
            CandidateGovernmentProfileRepository profileRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.sourceRepository = sourceRepository;
        this.jobRepository = jobRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.evidenceRepository = evidenceRepository;
        this.corrigendumRepository = corrigendumRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedGovernmentSources();
        seedGovernmentJobs();
        seedCandidateGovernmentProfile();
    }

    private void seedGovernmentSources() {
        if (sourceRepository.count() > 0) {
            log.info("Government sources already populated ({} sources found).", sourceRepository.count());
            return;
        }

        log.info("Populating India-wide Government Source Registry across Central, State, District, Municipal & Panchayat tiers...");

        List<GovernmentSource> sources = List.of(
                // 1. Central Government & Commissions
                new GovernmentSource("Union Public Service Commission (UPSC)", "https://upsc.gov.in", "upsc.gov.in", "Central", "All",
                        GovernmentSourceType.PSC, "Department of Personnel and Training", "UPSC", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Staff Selection Commission (SSC)", "https://ssc.gov.in", "ssc.gov.in", "Central", "All",
                        GovernmentSourceType.STAFF_SELECTION, "DoPT", "SSC", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Ministry of Railways - Railway Recruitment Boards (RRB)", "https://indianrailways.gov.in", "indianrailways.gov.in", "Central", "All",
                        GovernmentSourceType.RAILWAY, "Ministry of Railways", "Railway Recruitment Board", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Institute of Banking Personnel Selection (IBPS)", "https://ibps.in", "ibps.in", "Central", "All",
                        GovernmentSourceType.BANKING, "Ministry of Finance", "IBPS", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Defence Research and Development Organisation (DRDO)", "https://drdo.gov.in", "drdo.gov.in", "Central", "All",
                        GovernmentSourceType.MINISTRY, "Ministry of Defence", "RAC DRDO", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("All India Institute of Medical Sciences (AIIMS)", "https://aiimsexams.ac.in", "aiimsexams.ac.in", "Central", "All",
                        GovernmentSourceType.HEALTH, "Ministry of Health & Family Welfare", "AIIMS Examination Section", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Bharat Heavy Electricals Limited (BHEL)", "https://careers.bhel.in", "bhel.in", "Central", "All",
                        GovernmentSourceType.PSU, "Ministry of Heavy Industries", "BHEL Recruitment Portal", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.WEEKLY),
                new GovernmentSource("Oil and Natural Gas Corporation (ONGC)", "https://ongcindia.com", "ongcindia.com", "Central", "All",
                        GovernmentSourceType.PSU, "Ministry of Petroleum and Natural Gas", "ONGC HR", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.WEEKLY),
                new GovernmentSource("University of Delhi", "https://du.ac.in", "du.ac.in", "Delhi", "New Delhi",
                        GovernmentSourceType.UNIVERSITY, "Ministry of Education", "Delhi University Academic Council", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.WEEKLY),

                // 2. Official Aggregators (Tier 2)
                new GovernmentSource("National Career Service (NCS)", "https://ncs.gov.in", "ncs.gov.in", "Central", "All",
                        GovernmentSourceType.MISSION, "Ministry of Labour & Employment", "NCS Portal", GovernmentSourcePriority.TIER_2_OFFICIAL_AGGREGATOR, CrawlFrequency.DAILY),
                new GovernmentSource("Employment News / Rozgar Samachar", "https://employmentnews.gov.in", "employmentnews.gov.in", "Central", "All",
                        GovernmentSourceType.DEPARTMENT, "Publications Division, Ministry of I&B", "Employment News", GovernmentSourcePriority.TIER_2_OFFICIAL_AGGREGATOR, CrawlFrequency.WEEKLY),

                // 3. State Public Service Commissions & Boards
                new GovernmentSource("Uttar Pradesh Public Service Commission (UPPSC)", "https://uppsc.up.nic.in", "uppsc.up.nic.in", "Uttar Pradesh", "All",
                        GovernmentSourceType.PSC, "Government of Uttar Pradesh", "UPPSC Prayagraj", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Maharashtra Public Service Commission (MPSC)", "https://mpsc.gov.in", "mpsc.gov.in", "Maharashtra", "All",
                        GovernmentSourceType.PSC, "Government of Maharashtra", "MPSC Mumbai", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Tamil Nadu Public Service Commission (TNPSC)", "https://tnpsc.gov.in", "tnpsc.gov.in", "Tamil Nadu", "All",
                        GovernmentSourceType.PSC, "Government of Tamil Nadu", "TNPSC Chennai", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Bihar Public Service Commission (BPSC)", "https://bpsc.bih.nic.in", "bpsc.bih.nic.in", "Bihar", "All",
                        GovernmentSourceType.PSC, "Government of Bihar", "BPSC Patna", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Karnataka Public Service Commission (KPSC)", "https://kpsc.kar.nic.in", "kpsc.kar.nic.in", "Karnataka", "All",
                        GovernmentSourceType.PSC, "Government of Karnataka", "KPSC Bengaluru", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Rajasthan Public Service Commission (RPSC)", "https://rpsc.rajasthan.gov.in", "rpsc.rajasthan.gov.in", "Rajasthan", "All",
                        GovernmentSourceType.PSC, "Government of Rajasthan", "RPSC Ajmer", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),

                // 4. District Administrations (NIC District Portals)
                new GovernmentSource("District Administration Kannauj", "https://kannauj.nic.in", "kannauj.nic.in", "Uttar Pradesh", "Kannauj",
                        GovernmentSourceType.DISTRICT_ADMINISTRATION, "District Administration", "District Magistrate Kannauj", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("District Administration Pune", "https://pune.nic.in", "pune.nic.in", "Maharashtra", "Pune",
                        GovernmentSourceType.DISTRICT_ADMINISTRATION, "District Administration", "District Collector Pune", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("District Administration Varanasi", "https://varanasi.nic.in", "varanasi.nic.in", "Uttar Pradesh", "Varanasi",
                        GovernmentSourceType.DISTRICT_ADMINISTRATION, "District Administration", "District Magistrate Varanasi", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("District Administration Patna", "https://patna.nic.in", "patna.nic.in", "Bihar", "Patna",
                        GovernmentSourceType.DISTRICT_ADMINISTRATION, "District Administration", "District Magistrate Patna", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),

                // 5. Municipal Corporations
                new GovernmentSource("Municipal Corporation of Delhi (MCD)", "https://mcdonline.nic.in", "mcdonline.nic.in", "Delhi", "New Delhi",
                        GovernmentSourceType.MUNICIPALITY, "Urban Development", "Municipal Corporation of Delhi", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Brihanmumbai Municipal Corporation (BMC)", "https://portal.mcgm.gov.in", "portal.mcgm.gov.in", "Maharashtra", "Mumbai",
                        GovernmentSourceType.MUNICIPALITY, "Urban Local Body", "BMC Mumbai", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Bruhat Bengaluru Mahanagara Palike (BBMP)", "https://bbmp.gov.in", "bbmp.gov.in", "Karnataka", "Bengaluru",
                        GovernmentSourceType.MUNICIPALITY, "Urban Development", "BBMP Commissioner", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),

                // 6. Panchayati Raj & Rural Development
                new GovernmentSource("Panchayati Raj Department Uttar Pradesh", "https://panchayatiraj.up.nic.in", "panchayatiraj.up.nic.in", "Uttar Pradesh", "All",
                        GovernmentSourceType.PANCHAYAT, "Panchayati Raj", "Directorate of Panchayati Raj UP", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Rural Development & Panchayat Raj Karnataka", "https://rdpr.karnataka.gov.in", "rdpr.karnataka.gov.in", "Karnataka", "All",
                        GovernmentSourceType.PANCHAYAT, "Rural Development", "RDPR Secretariat", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),

                // 7. Health Missions (NHM)
                new GovernmentSource("National Health Mission (NHM UP)", "https://upnrhm.gov.in", "upnrhm.gov.in", "Uttar Pradesh", "All",
                        GovernmentSourceType.HEALTH, "Medical Health and Family Welfare", "Mission Director NHM UP", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Public Health Department Maharashtra", "https://arogya.maharashtra.gov.in", "arogya.maharashtra.gov.in", "Maharashtra", "All",
                        GovernmentSourceType.HEALTH, "Health Services", "Director of Health Services Maharashtra", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),

                // 8. Women & Child Development (Anganwadi / ICDS)
                new GovernmentSource("Women & Child Development (Bal Vikas UP)", "https://balvikasup.gov.in", "balvikasup.gov.in", "Uttar Pradesh", "All",
                        GovernmentSourceType.WOMEN_CHILD_DEVELOPMENT, "Bal Vikas Seva Evam Pushtahar Vibhag", "Directorate of ICDS UP", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY),
                new GovernmentSource("Women and Child Development Department Maharashtra", "https://wcd.maharashtra.gov.in", "wcd.maharashtra.gov.in", "Maharashtra", "All",
                        GovernmentSourceType.WOMEN_CHILD_DEVELOPMENT, "Women & Child Development", "WCD Commissioner Maharashtra", GovernmentSourcePriority.TIER_1_OFFICIAL, CrawlFrequency.DAILY)
        );

        Instant now = Instant.now();
        for (GovernmentSource s : sources) {
            s.setLastChecked(now.minus(2, ChronoUnit.HOURS));
            s.setLastSuccessfulCrawl(now.minus(2, ChronoUnit.HOURS));
        }

        sourceRepository.saveAll(sources);
        log.info("Successfully seeded {} official government sources across Central, State, District, Municipal, and Local tiers.", sources.size());
    }

    private void seedGovernmentJobs() {
        if (jobRepository.count() > 0) {
            log.info("Government jobs already populated ({} jobs found).", jobRepository.count());
            return;
        }

        log.info("Seeding verified Government Recruitment opportunities including small local, anganwadi, samvida, and regular positions...");
        Instant now = Instant.now();

        // 1. Anganwadi Worker - Kannauj, UP (Section 3 & 26 Example)
        GovernmentJob job1 = new GovernmentJob();
        job1.setCanonicalId("gov-up-kannauj-anganwadi-worker-2026");
        job1.setTitle("Anganwadi Worker (आंगनवाड़ी कार्यकत्री)");
        job1.setOrganization("Women & Child Development Department");
        job1.setDepartment("Bal Vikas Seva Evam Pushtahar Vibhag");
        job1.setState("Uttar Pradesh");
        job1.setDistrict("Kannauj");
        job1.setBlock("Jalalabad & Kannauj Rural");
        job1.setEmploymentType(GovernmentEmploymentType.HONORARIUM);
        job1.setHonorarium(true);
        job1.setVacanciesCount(148);
        job1.setSalary("₹12,500 / month Honorarium");
        job1.setApplicationMode("ONLINE");
        job1.setApplicationFee("Nil (Exempted for all categories)");
        job1.setApplicationStartDate(now.minus(10, ChronoUnit.DAYS));
        job1.setApplicationLastDate(now.plus(25, ChronoUnit.DAYS));
        job1.setSourceUrl("https://kannauj.nic.in/notice_category/recruitment/");
        job1.setNotificationUrl("https://kannauj.nic.in/notices/wcd_anganwadi_advt_04_2026.pdf");
        job1.setApplicationUrl("https://balvikasup.gov.in");
        job1.setAuthority("District Administration, Kannauj");
        job1.setSourceDomain("kannauj.nic.in");
        job1.setNotificationNumber("Advt. No. 04/2026/WCD-K");
        job1.setVerificationStatus(GovernmentVerificationStatus.VERIFIED_OFFICIAL);
        job1.setAuthenticityScore(BigDecimal.valueOf(98.0));
        job1.setAuthenticityLevel(AuthenticityLevel.VERIFIED);
        job1.setStatus(GovernmentJobStatus.OPEN);
        job1.setRawContent("Official Advertisement for Anganwadi Worker Recruitment in Kannauj District under ICDS scheme...");

        GovernmentJobEligibility elig1 = new GovernmentJobEligibility();
        elig1.setJob(job1);
        elig1.setGender(GenderEligibility.FEMALE_ONLY); // Extracted strictly from notification text - Section 17
        elig1.setMinimumAge(18);
        elig1.setMaximumAge(35);
        elig1.setDomicile("Resident of Kannauj District, Uttar Pradesh");
        elig1.setExperienceYearsMin(BigDecimal.ZERO);
        elig1.setEducationJson(toJson(List.of("12th Pass", "Intermediate")));
        elig1.setExperienceJson(toJson(List.of()));
        elig1.setCategoryReservationsJson(toJson(List.of("UR", "OBC", "SC", "ST", "EWS")));
        job1.setEligibility(elig1);

        job1.getEvidenceList().add(new GovernmentJobEvidence(job1, "gender", "FEMALE_ONLY", "wcd_anganwadi_advt_04_2026.pdf", "Page 2, Section 3", "Only female candidates residing in the respective Gram Sabha are eligible to apply."));
        job1.getEvidenceList().add(new GovernmentJobEvidence(job1, "maximumAge", "35", "wcd_anganwadi_advt_04_2026.pdf", "Page 2, Section 4", "Candidate must have attained 18 years and not exceeded 35 years as on 01-07-2026."));
        job1.getEvidenceList().add(new GovernmentJobEvidence(job1, "education", "12th Pass", "wcd_anganwadi_advt_04_2026.pdf", "Page 3, Section 5", "Minimum educational qualification is Intermediate (12th Pass) or equivalent recognized board examination."));

        jobRepository.save(job1);

        // 2. Anganwadi Helper - Kannauj, UP
        GovernmentJob job2 = new GovernmentJob();
        job2.setCanonicalId("gov-up-kannauj-anganwadi-helper-2026");
        job2.setTitle("Anganwadi Helper (आंगनवाड़ी सहायिका)");
        job2.setOrganization("Women & Child Development Department");
        job2.setDepartment("Bal Vikas Seva Evam Pushtahar Vibhag");
        job2.setState("Uttar Pradesh");
        job2.setDistrict("Kannauj");
        job2.setBlock("Chhibramau & Talgram");
        job2.setEmploymentType(GovernmentEmploymentType.HONORARIUM);
        job2.setHonorarium(true);
        job2.setVacanciesCount(86);
        job2.setSalary("₹6,750 / month Honorarium");
        job2.setApplicationMode("ONLINE");
        job2.setApplicationFee("Nil");
        job2.setApplicationStartDate(now.minus(10, ChronoUnit.DAYS));
        job2.setApplicationLastDate(now.plus(25, ChronoUnit.DAYS));
        job2.setSourceUrl("https://kannauj.nic.in/notice_category/recruitment/");
        job2.setNotificationUrl("https://kannauj.nic.in/notices/wcd_helper_advt_05_2026.pdf");
        job2.setApplicationUrl("https://balvikasup.gov.in");
        job2.setAuthority("District Administration, Kannauj");
        job2.setSourceDomain("kannauj.nic.in");
        job2.setNotificationNumber("Advt. No. 05/2026/WCD-K");
        job2.setVerificationStatus(GovernmentVerificationStatus.VERIFIED_OFFICIAL);
        job2.setAuthenticityScore(BigDecimal.valueOf(98.0));
        job2.setAuthenticityLevel(AuthenticityLevel.VERIFIED);
        job2.setStatus(GovernmentJobStatus.OPEN);

        GovernmentJobEligibility elig2 = new GovernmentJobEligibility();
        elig2.setJob(job2);
        elig2.setGender(GenderEligibility.FEMALE_ONLY);
        elig2.setMinimumAge(18);
        elig2.setMaximumAge(35);
        elig2.setDomicile("Resident of Kannauj District, Uttar Pradesh");
        elig2.setEducationJson(toJson(List.of("10th Pass", "Matriculation")));
        elig2.setExperienceJson(toJson(List.of()));
        elig2.setCategoryReservationsJson(toJson(List.of("UR", "OBC", "SC", "ST")));
        job2.setEligibility(elig2);

        job2.getEvidenceList().add(new GovernmentJobEvidence(job2, "gender", "FEMALE_ONLY", "wcd_helper_advt_05_2026.pdf", "Page 1, Para 2", "Applications are invited from female local residents for the post of Anganwadi Sahayika."));
        jobRepository.save(job2);

        // 3. Panchayat Assistant cum Data Entry Operator (DEO) - Samvida
        GovernmentJob job3 = new GovernmentJob();
        job3.setCanonicalId("gov-up-panchayat-assistant-deo-2026");
        job3.setTitle("Panchayat Assistant cum DEO (पंचायत सहायक / डाटा एंट्री ऑपरेटर)");
        job3.setOrganization("Panchayati Raj Department");
        job3.setDepartment("Directorate of Panchayati Raj UP");
        job3.setState("Uttar Pradesh");
        job3.setDistrict("Kannauj");
        job3.setBlock("All Gram Panchayats");
        job3.setEmploymentType(GovernmentEmploymentType.SAMVIDA);
        job3.setHonorarium(true);
        job3.setVacanciesCount(312);
        job3.setSalary("₹6,000 / month Honorarium");
        job3.setApplicationMode("OFFLINE");
        job3.setApplicationFee("Nil");
        job3.setApplicationStartDate(now.minus(5, ChronoUnit.DAYS));
        job3.setApplicationLastDate(now.plus(18, ChronoUnit.DAYS));
        job3.setSourceUrl("https://panchayatiraj.up.nic.in");
        job3.setNotificationUrl("https://panchayatiraj.up.nic.in/notices/panchayat_sahayak_2026.pdf");
        job3.setApplicationUrl("https://panchayatiraj.up.nic.in/downloads/application_form_deo.pdf");
        job3.setAuthority("Panchayati Raj Department, Government of UP");
        job3.setSourceDomain("panchayatiraj.up.nic.in");
        job3.setNotificationNumber("PRD/Samvida/2026/891");
        job3.setVerificationStatus(GovernmentVerificationStatus.VERIFIED_OFFICIAL);
        job3.setAuthenticityScore(BigDecimal.valueOf(96.0));
        job3.setAuthenticityLevel(AuthenticityLevel.VERIFIED);
        job3.setStatus(GovernmentJobStatus.OPEN);

        GovernmentJobEligibility elig3 = new GovernmentJobEligibility();
        elig3.setJob(job3);
        elig3.setGender(GenderEligibility.ALL);
        elig3.setMinimumAge(18);
        elig3.setMaximumAge(40);
        elig3.setDomicile("Resident of Gram Panchayat, Kannauj");
        elig3.setEducationJson(toJson(List.of("12th Pass", "Intermediate", "CCC Computer Certificate")));
        elig3.setExperienceJson(toJson(List.of()));
        elig3.setCategoryReservationsJson(toJson(List.of("UR", "OBC", "SC", "ST", "EWS")));
        job3.setEligibility(elig3);

        job3.getEvidenceList().add(new GovernmentJobEvidence(job3, "education", "12th Pass + CCC", "panchayat_sahayak_2026.pdf", "Page 2", "Candidate must be 12th passed and possess basic computer typing knowledge."));
        jobRepository.save(job3);

        // 4. Community Health Officer (CHO) - Contractual / NHM Maharashtra
        GovernmentJob job4 = new GovernmentJob();
        job4.setCanonicalId("gov-mh-nhm-community-health-officer-2026");
        job4.setTitle("Community Health Officer (CHO) - Contractual");
        job4.setOrganization("National Health Mission (NHM)");
        job4.setDepartment("Public Health Department Maharashtra");
        job4.setState("Maharashtra");
        job4.setDistrict("Pune");
        job4.setBlock("District Health Sub-Centers");
        job4.setEmploymentType(GovernmentEmploymentType.CONTRACTUAL);
        job4.setVacanciesCount(520);
        job4.setSalary("₹25,000 + ₹15,000 Performance Incentive / month");
        job4.setApplicationMode("ONLINE");
        job4.setApplicationFee("₹500 (Reserved: ₹350)");
        job4.setApplicationStartDate(now.minus(12, ChronoUnit.DAYS));
        job4.setApplicationLastDate(now.plus(15, ChronoUnit.DAYS));
        job4.setSourceUrl("https://arogya.maharashtra.gov.in");
        job4.setNotificationUrl("https://arogya.maharashtra.gov.in/notices/nhm_cho_pune_2026.pdf");
        job4.setApplicationUrl("https://arogya.maharashtra.gov.in/recruitment");
        job4.setAuthority("Mission Director, NHM Maharashtra");
        job4.setSourceDomain("arogya.maharashtra.gov.in");
        job4.setNotificationNumber("NHM/MH/CHO/2026/03");
        job4.setVerificationStatus(GovernmentVerificationStatus.VERIFIED_OFFICIAL);
        job4.setAuthenticityScore(BigDecimal.valueOf(95.0));
        job4.setAuthenticityLevel(AuthenticityLevel.VERIFIED);
        job4.setStatus(GovernmentJobStatus.OPEN);

        GovernmentJobEligibility elig4 = new GovernmentJobEligibility();
        elig4.setJob(job4);
        elig4.setGender(GenderEligibility.ALL);
        elig4.setMinimumAge(21);
        elig4.setMaximumAge(38);
        elig4.setDomicile("Maharashtra");
        elig4.setEducationJson(toJson(List.of("B.Sc Nursing", "GNM", "BAMS")));
        elig4.setExperienceJson(toJson(List.of()));
        elig4.setCategoryReservationsJson(toJson(List.of("UR", "OBC", "SC", "ST", "SEBC")));
        job4.setEligibility(elig4);
        jobRepository.save(job4);

        // 5. Junior Engineer (Civil) - Contractual with Corrigendum (MCD)
        GovernmentJob job5 = new GovernmentJob();
        job5.setCanonicalId("gov-delhi-mcd-je-civil-contractual-2026");
        job5.setTitle("Junior Engineer (Civil) - Contractual");
        job5.setOrganization("Municipal Corporation of Delhi (MCD)");
        job5.setDepartment("Engineering Department (Civil)");
        job5.setState("Delhi");
        job5.setDistrict("New Delhi");
        job5.setEmploymentType(GovernmentEmploymentType.MUNICIPAL);
        job5.setVacanciesCount(115);
        job5.setSalary("₹44,900 / month consolidated");
        job5.setApplicationMode("ONLINE");
        job5.setApplicationFee("₹200");
        job5.setApplicationStartDate(now.minus(20, ChronoUnit.DAYS));
        job5.setApplicationLastDate(now.plus(12, ChronoUnit.DAYS)); // revised by corrigendum
        job5.setSourceUrl("https://mcdonline.nic.in");
        job5.setNotificationUrl("https://mcdonline.nic.in/notices/je_civil_contract_2026.pdf");
        job5.setApplicationUrl("https://mcdonline.nic.in/jobs");
        job5.setAuthority("Commissioner, Municipal Corporation of Delhi");
        job5.setSourceDomain("mcdonline.nic.in");
        job5.setNotificationNumber("MCD/ENGG/2026/102");
        job5.setVerificationStatus(GovernmentVerificationStatus.VERIFIED_OFFICIAL);
        job5.setAuthenticityScore(BigDecimal.valueOf(95.0));
        job5.setAuthenticityLevel(AuthenticityLevel.VERIFIED);
        job5.setStatus(GovernmentJobStatus.OPEN);

        GovernmentJobEligibility elig5 = new GovernmentJobEligibility();
        elig5.setJob(job5);
        elig5.setGender(GenderEligibility.ALL);
        elig5.setMinimumAge(18);
        elig5.setMaximumAge(30);
        elig5.setDomicile("NOT_SPECIFIED");
        elig5.setEducationJson(toJson(List.of("Diploma in Civil Engineering", "B.Tech Civil Engineering", "B.E. Civil")));
        elig5.setExperienceYearsMin(BigDecimal.ONE);
        elig5.setExperienceJson(toJson(List.of("1 year field construction experience")));
        job5.setEligibility(elig5);

        // Corrigendum attached - Section 22
        GovernmentJobCorrigendum corr = new GovernmentJobCorrigendum(
                job5,
                CorrigendumType.EXTENSION,
                "Corrigendum No. 01/2026: Extension of Online Application Deadline",
                "https://mcdonline.nic.in/notices/corrigendum_01_je_civil_2026.pdf",
                now.minus(2, ChronoUnit.DAYS),
                "Due to server maintenance on the NIC portal, the last date for submission of online applications has been extended by 14 days.",
                now.plus(12, ChronoUnit.DAYS),
                115
        );
        job5.getCorrigenda().add(corr);

        jobRepository.save(job5);

        // 6. Assistant Section Officer (ASO) - SSC CGL Central Regular
        GovernmentJob job6 = new GovernmentJob();
        job6.setCanonicalId("gov-central-ssc-cgl-aso-2026");
        job6.setTitle("Assistant Section Officer (ASO) - Central Secretariat Service");
        job6.setOrganization("Staff Selection Commission (SSC)");
        job6.setDepartment("Department of Personnel and Training (DoPT)");
        job6.setState("Central");
        job6.setDistrict("All");
        job6.setEmploymentType(GovernmentEmploymentType.REGULAR);
        job6.setVacanciesCount(2400);
        job6.setSalary("Pay Level 7 (₹44,900 - ₹1,42,400)");
        job6.setPayLevel("Level 7 (7th CPC)");
        job6.setApplicationMode("ONLINE");
        job6.setApplicationFee("₹100 (Exempted for Women/SC/ST/PWD)");
        job6.setApplicationStartDate(now.minus(15, ChronoUnit.DAYS));
        job6.setApplicationLastDate(now.plus(20, ChronoUnit.DAYS));
        job6.setExamDate(now.plus(60, ChronoUnit.DAYS));
        job6.setSourceUrl("https://ssc.gov.in");
        job6.setNotificationUrl("https://ssc.gov.in/notices/cgl_2026_notice.pdf");
        job6.setApplicationUrl("https://ssc.gov.in/portal/apply");
        job6.setAuthority("Staff Selection Commission, Government of India");
        job6.setSourceDomain("ssc.gov.in");
        job6.setNotificationNumber("HQ-PPI03/12/2026-PP_1");
        job6.setVerificationStatus(GovernmentVerificationStatus.VERIFIED_OFFICIAL);
        job6.setAuthenticityScore(BigDecimal.valueOf(100.0));
        job6.setAuthenticityLevel(AuthenticityLevel.VERIFIED);
        job6.setStatus(GovernmentJobStatus.OPEN);

        GovernmentJobEligibility elig6 = new GovernmentJobEligibility();
        elig6.setJob(job6);
        elig6.setGender(GenderEligibility.ALL);
        elig6.setMinimumAge(20);
        elig6.setMaximumAge(30);
        elig6.setDomicile("Citizen of India");
        elig6.setEducationJson(toJson(List.of("Graduate", "B.A", "B.Sc", "B.Com", "B.Tech", "Degree")));
        elig6.setExperienceYearsMin(BigDecimal.ZERO);
        elig6.setExperienceJson(toJson(List.of()));
        elig6.setCategoryReservationsJson(toJson(List.of("UR", "OBC", "SC", "ST", "EWS", "ESM")));
        job6.setEligibility(elig6);

        job6.getEvidenceList().add(new GovernmentJobEvidence(job6, "education", "Bachelor's Degree", "cgl_2026_notice.pdf", "Page 8, Para 7.1", "Essential Educational Qualification: Bachelor's Degree from a recognized University or equivalent."));
        job6.getEvidenceList().add(new GovernmentJobEvidence(job6, "maximumAge", "30", "cgl_2026_notice.pdf", "Page 6, Para 5.2", "For ASO CSS: 20 to 30 years as of closing date with category relaxations."));
        jobRepository.save(job6);

        // 7. Graduate Engineering Apprentice - BHEL Central PSU
        GovernmentJob job7 = new GovernmentJob();
        job7.setCanonicalId("gov-central-psu-bhel-graduate-apprentice-2026");
        job7.setTitle("Graduate Apprentice (Engineering & Technology)");
        job7.setOrganization("Bharat Heavy Electricals Limited (BHEL)");
        job7.setDepartment("Human Resource Development Division");
        job7.setState("Central");
        job7.setDistrict("All");
        job7.setEmploymentType(GovernmentEmploymentType.APPRENTICESHIP);
        job7.setVacanciesCount(150);
        job7.setSalary("₹9,000 / month Stipend (Apprenticeship Act)");
        job7.setApplicationMode("ONLINE");
        job7.setApplicationFee("Nil");
        job7.setApplicationStartDate(now.minus(8, ChronoUnit.DAYS));
        job7.setApplicationLastDate(now.plus(18, ChronoUnit.DAYS));
        job7.setSourceUrl("https://careers.bhel.in");
        job7.setNotificationUrl("https://careers.bhel.in/notices/advt_apprentice_2026.pdf");
        job7.setApplicationUrl("https://careers.bhel.in/apprentice/apply");
        job7.setAuthority("BHEL Corporate Office");
        job7.setSourceDomain("bhel.in");
        job7.setNotificationNumber("BHEL/APP/2026/02");
        job7.setVerificationStatus(GovernmentVerificationStatus.VERIFIED_OFFICIAL);
        job7.setAuthenticityScore(BigDecimal.valueOf(95.0));
        job7.setAuthenticityLevel(AuthenticityLevel.VERIFIED);
        job7.setStatus(GovernmentJobStatus.OPEN);

        GovernmentJobEligibility elig7 = new GovernmentJobEligibility();
        elig7.setJob(job7);
        elig7.setGender(GenderEligibility.ALL);
        elig7.setMinimumAge(18);
        elig7.setMaximumAge(27);
        elig7.setDomicile("Citizen of India");
        elig7.setEducationJson(toJson(List.of("B.Tech", "B.E", "Graduation in Engineering")));
        elig7.setExperienceYearsMin(BigDecimal.ZERO);
        elig7.setExperienceJson(toJson(List.of()));
        job7.setEligibility(elig7);
        jobRepository.save(job7);

        log.info("Successfully seeded verified Government jobs across Central, State, Municipal, Samvida, and Anganwadi categories.");
    }

    private void seedCandidateGovernmentProfile() {
        String defaultEmail = "candidate@jobhunter.ai";
        User user = userRepository.findByEmail(defaultEmail).orElse(null);
        if (user == null) return;

        if (profileRepository.findByUserId(user.getId()).isPresent()) {
            log.info("Candidate government profile already exists for {}", defaultEmail);
            return;
        }

        log.info("Seeding initial Candidate Government Profile for {}", defaultEmail);
        CandidateGovernmentProfile p = new CandidateGovernmentProfile(user);
        p.setAge(24);
        p.setDob(LocalDate.of(2002, 5, 14));
        p.setGender("FEMALE");
        p.setState("Uttar Pradesh");
        p.setDistrict("Kannauj");
        p.setDomicileState("Uttar Pradesh");
        p.setDomicileDistrict("Kannauj");
        p.setHighestEducation("Graduate");
        p.setDegreesJson(toJson(List.of("B.Tech Computer Science", "12th Pass", "Intermediate", "10th Pass")));
        p.setPassingYear(2024);
        p.setYearsOfExperience(new BigDecimal("2.5"));
        p.setCategory("UR/GEN");
        p.setPwd(false);
        p.setExServiceman(false);
        p.setPreferredStatesJson(toJson(List.of("Uttar Pradesh", "Delhi", "Maharashtra", "Central")));
        p.setPreferredDistrictsJson(toJson(List.of("Kannauj", "Lucknow", "Noida", "All")));
        p.setPreferredEmploymentTypesJson(toJson(List.of("PERMANENT", "REGULAR", "CONTRACTUAL", "SAMVIDA", "SCHEME_BASED", "HONORARIUM")));
        p.setSkillsJson(toJson(List.of("Computer Typing", "Data Entry", "Office Administration", "Software Engineering")));

        profileRepository.save(p);
        log.info("Candidate Government Profile seeded successfully.");
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
