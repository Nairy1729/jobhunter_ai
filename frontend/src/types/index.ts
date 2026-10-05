export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface CandidateSkill {
  id?: string;
  skillId?: string;
  skillName: string;
  category: string;
  proficiencyLevel: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';
  yearsExperience: number;
  primary: boolean;
  evidenceText?: string;
}

export interface CandidateProfile {
  id: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  headline: string;
  summary: string;
  yearsOfExperience: number;
  currentLocation: string;
  preferredLocations: string[];
  workModes: string[];
  minSalaryInr: number;
  currency: string;
  targetRoles: string[];
  githubUrl?: string;
  linkedinUrl?: string;
  portfolioUrl?: string;
  phoneNumber?: string;
  skills: CandidateSkill[];
  masterResumeTitle?: string;
  masterResumeId?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface ResumeUploadResponse {
  resumeId: string;
  title: string;
  fileType: string;
  fileSizeBytes: number;
  characterCount: number;
  detectedSkills: string[];
  message: string;
  headline?: string;
  yearsOfExperience?: number;
  experiencesCount?: number;
  projectsCount?: number;
  extractionSource?: string;
}

export interface Job {
  id: string;
  companyName: string;
  companyDomain?: string;
  title: string;
  normalizedTitle: string;
  department?: string;
  location: string;
  workMode: 'REMOTE' | 'HYBRID' | 'ON_SITE' | 'UNKNOWN';
  employmentType?: string;
  minExperienceYears?: number;
  maxExperienceYears?: number;
  minSalary?: number;
  maxSalary?: number;
  salaryCurrency?: string;
  jobUrl: string;
  canonicalUrl: string;
  rawDescriptionMarkdown: string;
  structuredJobSpec?: string;
  detectedTechnologies: string[];
  postingDate?: string;
  deadlineDate?: string;
  active: boolean;
  pipelineStatus: string;
  sourceName?: string;
  createdAt: string;
  applied?: boolean;
  appliedAt?: string | null;
  priorityCategory?: PriorityCategory;
  priorityScore?: number;
  freshness?: JobFreshness;
  daysSincePosted?: number | null;
  whyThisJob?: string[];
  potentialConcerns?: string[];
  keyTechnologies?: KeyTechCoverageDto[];
  usefulnessStatus?: 'USEFUL' | 'NOT_USEFUL';
  matchCategory?: 'HIGH_RELEVANCE' | 'GOOD_MATCH' | 'POSSIBLE_MATCH' | 'NOT_USEFUL';
  matchedRequirements?: string[];
  missingRequirements?: string[];
  candidateEvidence?: string[];
  hardEligibilityPassed?: boolean;
}

export type ProfileReadinessState = 'PROFILE_READY' | 'PROFILE_NOT_READY';

export interface ReadinessItem {
  key: string;
  label: string;
  satisfied: boolean;
  mandatory: boolean;
  details: string;
}

export interface ProfileReadinessReport {
  state: ProfileReadinessState;
  score: number;
  canDiscover: boolean;
  headline: string;
  message: string;
  items: ReadinessItem[];
  missingItems: string[];
}

export type PriorityCategory = 'HIGH_PRIORITY' | 'MEDIUM_PRIORITY' | 'LOW_PRIORITY' | 'NOT_RECOMMENDED';
export type JobFreshness = 'NEW' | 'RECENT' | 'OLDER' | 'STALE' | 'UNKNOWN';

export interface RequirementCoverageItem {
  requirement: string;
  importance: string; // MUST_HAVE | PREFERRED
  candidateEvidenceType: string; // COMMERCIAL | PROJECT | LEARNING | NONE
  coverage: string; // STRONG | PARTIAL | TRANSFERABLE | GAP
  explanation: string;
}

export interface KeyTechCoverageDto {
  technology: string;
  coverage: string; // STRONG | PARTIAL | GAP
  candidateEvidence: string;
}

export interface DiscoverySummary {
  queriesExecuted: number;
  searchResultsFound: number;
  pagesScraped: number;
  jobsCreated: number;
  duplicatesSkipped: number;
  invalidPagesSkipped: number;
  errors: string[];
  jobs: Job[];
  executionTimeMs: number;
}

export interface SearchRun {
  id: string;
  jobSourceName?: string;
  queryString: string;
  status: string;
  jobsDiscoveredCount: number;
  jobsIngestedCount: number;
  errorMessage?: string;
  startedAt: string;
  completedAt?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export type MatchRecommendation = 'APPLY' | 'APPLY_AFTER_TAILORING' | 'LOW_PRIORITY' | 'DO_NOT_APPLY';
export type QueueTier = 'HIGH_PRIORITY' | 'REVIEW' | 'LOW_PRIORITY';
export type RequirementMatchType = 'STRONG' | 'PARTIAL' | 'TRANSFERABLE' | 'GAP';
export type RequirementConfidence = 'HIGH' | 'MEDIUM' | 'LOW';

export interface RequirementMatchItem {
  requirement: string;
  category: string;
  candidateEvidence: string;
  matchType: RequirementMatchType;
  confidence: RequirementConfidence;
  explanation: string;
  isImplied: boolean;
}

export interface ApplicationAdvantageReport {
  employerPriorities: string[];
  candidateRelevance: string;
  strongestEvidence: string[];
  whatToEmphasize: string[];
  whatToDeemphasize: string[];
  honestGaps: string[];
  transferableSkills: string[];
  resumePositioning: string[];
  applicationFitAndPositioning: string[];
  applicationStrategy: string;
}

export interface BulletSharpeningProposal {
  originalBullet: string;
  proposedBullet: string;
  groundedEvidenceReference: string;
  atsKeywordRationale: string;
}

export interface TailoringRecommendation {
  jobId: string;
  jobTitle: string;
  companyName: string;
  sectionsToReorder: string[];
  skillsToFeature: string[];
  skillsToDeemphasize: string[];
  bulletSharpeningProposals: BulletSharpeningProposal[];
  rationale: string;
}

export interface JobRequirementResponse {
  id: string;
  requirementType: string;
  category: string;
  description: string;
  skillName?: string;
  inferredImportance?: number;
  isImplied: boolean;
  rawTextSnippet?: string;
}

export interface MatchAnalysisResponse {
  jobId: string;
  jobTitle: string;
  companyName: string;
  candidateProfileId: string;
  recommendation: MatchRecommendation;
  priorityScore: number;
  queueTier: QueueTier;
  semanticSimilarityScore: number;
  overallAssessment: string;
  strongMatches: string[];
  partialMatches: string[];
  gaps: string[];
  transferableExperience: string[];
  riskFactors: string[];
  supportingEvidence: string[];
  requirementAnalysis: RequirementMatchItem[];
  advantageReport?: ApplicationAdvantageReport;
  priorityCategory?: PriorityCategory;
  freshness?: JobFreshness;
  daysSincePosted?: number | null;
  whyThisJob?: string[];
  potentialConcerns?: string[];
  hardConstraintViolations?: string[];
  requirementCoverage?: RequirementCoverageItem[];
  keyTechnologies?: KeyTechCoverageDto[];
  evaluatedAt: string;
}

// --- MILESTONE 4: RESUME TAILORING & APPLICATION OPTIMIZATION ---

export interface BulletTailoringItem {
  originalBullet: string;
  proposedBullet: string;
  reason: string;
  jobRequirement: string;
  evidenceReferences: string[];
  groundingStatus: 'VERIFIED_GROUNDED' | 'ADJUSTED' | 'REJECTED';
}

export interface AtsKeywordItem {
  keyword: string;
  status: 'SUPPORTED' | 'PARTIALLY_SUPPORTED' | 'UNSUPPORTED';
  context: string;
  evidenceReference: string;
}

export interface RejectedKeywordItem {
  keyword: string;
  reason: string;
}

export interface ResumeValidationReport {
  passed: boolean;
  passedChecks: string[];
  warnings: string[];
  failedChecks: string[];
  qualityScore: number;
}

export interface TailoringPlan {
  targetJobId: string;
  targetJobTitle: string;
  targetCompany: string;
  targetRole: string;
  resumeSectionsAffected: string[];
  skillsToEmphasize: string[];
  skillsToDeemphasize: string[];
  projectsToEmphasize: string[];
  projectsToDeemphasize: string[];
  bulletSharpeningProposals: BulletTailoringItem[];
  recommendedSectionOrder: string[];
  sectionOrderRationale: string;
  atsTerminology: AtsKeywordItem[];
  rejectedKeywords: RejectedKeywordItem[];
  evidenceReferences: string[];
  overallStrategy: string;
}

export interface TailoringDiff {
  masterResumeContent: string;
  tailoredResumeContent: string;
  addedEmphasis: string[];
  removedOrDeemphasized: string[];
  reorderedSections: string[];
  modifiedBullets: BulletTailoringItem[];
  atsTerminologyChanges: AtsKeywordItem[];
  rejectedKeywords: RejectedKeywordItem[];
}

export interface TailoredResume {
  id: string;
  candidateProfileId: string;
  masterResumeId?: string;
  jobId: string;
  versionNumber: number;
  status: 'DRAFT' | 'GENERATED' | 'VALIDATING' | 'VALIDATED' | 'PDF_GENERATED' | 'READY_FOR_DOWNLOAD' | 'VALIDATION_FAILED' | 'PDF_GENERATION_FAILED' | 'USER_REVIEW' | 'APPROVED' | 'EXPORTED';
  targetRole: string;
  targetCompany: string;
  tailoringPlan: TailoringPlan;
  tailoredMarkdown: string;
  latexSource?: string;
  pdfAvailable: boolean;
  pdfDownloadUrl?: string;
  pdfFileSizeBytes?: number;
  atsScoreEstimate: number;
  validationReport: ResumeValidationReport;
  createdAt: string;
  updatedAt: string;
}

// --- GOVERNMENT JOB DISCOVERY ENGINE TYPES ---
export type GovernmentEmploymentType =
  | 'PERMANENT'
  | 'REGULAR'
  | 'CONTRACTUAL'
  | 'SAMVIDA'
  | 'TEMPORARY'
  | 'OUTSOURCED'
  | 'SCHEME_BASED'
  | 'MISSION_BASED'
  | 'HONORARIUM'
  | 'APPRENTICESHIP'
  | 'PART_TIME'
  | 'DISTRICT_LEVEL'
  | 'BLOCK_LEVEL'
  | 'PANCHAYAT_LEVEL'
  | 'MUNICIPAL';

export type GovernmentVerificationStatus =
  | 'VERIFIED_OFFICIAL'
  | 'VERIFIED_OFFICIAL_NOTIFICATION'
  | 'OFFICIAL_SOURCE'
  | 'SECONDARY_SOURCE_VERIFIED'
  | 'UNVERIFIED'
  | 'EXPIRED';

export type AuthenticityLevel = 'VERIFIED' | 'PARTIALLY_VERIFIED' | 'UNVERIFIED';

export type GovernmentJobStatus = 'OPEN' | 'CLOSING_SOON' | 'CLOSED' | 'CANCELLED' | 'POSTPONED';

export type EligibilityStatus = 'ELIGIBLE' | 'LIKELY_ELIGIBLE' | 'NOT_ELIGIBLE' | 'UNKNOWN';

export interface EligibilityReason {
  criterion: string;
  status: 'PASS' | 'FAIL' | 'UNKNOWN';
  explanation: string;
}

export interface EligibilityEvaluationResult {
  status: EligibilityStatus;
  summaryMessage: string;
  reasons: EligibilityReason[];
  missingProfileFields: string[];
}

export interface GovernmentJob {
  id: string;
  title: string;
  organization: string;
  department?: string;
  state: string;
  district?: string;
  block?: string;
  employmentType: GovernmentEmploymentType;
  vacanciesCount?: number;
  salary?: string;
  honorarium: boolean;
  applicationMode: string;
  applicationStartDate?: string;
  applicationLastDate?: string;
  sourceUrl?: string;
  notificationUrl?: string;
  applicationUrl?: string;
  authority?: string;
  sourceDomain?: string;
  notificationNumber?: string;
  verificationStatus: GovernmentVerificationStatus;
  authenticityScore: number;
  authenticityLevel: AuthenticityLevel;
  status: GovernmentJobStatus;
  genderEligibility?: string;
  minimumAge?: number;
  maximumAge?: number;
  educationList?: string[];
  domicile?: string;
  candidateEligibility?: EligibilityEvaluationResult;
  corrigendaCount: number;
  createdAt: string;
}

export interface EvidenceItem {
  fieldName: string;
  fieldValue: string;
  sourceDocument: string;
  pageOrSection: string;
  excerpt: string;
}

export interface CorrigendumItem {
  noticeType: string;
  title: string;
  documentUrl?: string;
  issueDate?: string;
  description: string;
  revisedLastDate?: string;
  revisedVacancies?: number;
}

export interface GovernmentJobDetail extends GovernmentJob {
  canonicalId: string;
  vacanciesBreakdown?: string[];
  salaryMin?: number;
  salaryMax?: number;
  payLevel?: string;
  applicationFee?: string;
  examDate?: string;
  interviewDate?: string;
  rawContent?: string;
  gender?: string;
  education?: string[];
  experience?: string[];
  experienceYearsMin?: number;
  categoryReservations?: string[];
  pwdEligible?: boolean;
  exServicemanEligible?: boolean;
  otherConditions?: string;
  evidenceList: EvidenceItem[];
  corrigenda: CorrigendumItem[];
  updatedAt: string;
}

export interface CandidateGovernmentProfile {
  age?: number;
  dob?: string;
  gender?: string;
  state?: string;
  district?: string;
  domicileState?: string;
  domicileDistrict?: string;
  highestEducation?: string;
  degrees: string[];
  passingYear?: number;
  yearsOfExperience: number;
  category: string;
  pwd: boolean;
  exServiceman: boolean;
  preferredStates: string[];
  preferredDistricts: string[];
  preferredEmploymentTypes: string[];
  skills: string[];
}

export interface GovernmentCoverageMetrics {
  totalSources: number;
  activeSources: number;
  failedSources: number;
  lastSuccessfulCrawl?: string;
  centralGovernment: number;
  stateGovernment: number;
  districtAdministration: number;
  municipal: number;
  panchayat: number;
  universities: number;
  psus: number;
  departments: number;
  health: number;
  education: number;
  womenChildDevelopment: number;
  other: number;
  totalJobs: number;
  verifiedOfficialJobs: number;
  contractualSamvidaJobs: number;
  smallLocalJobs: number;
  openJobs: number;
  employmentTypeBreakdown: Record<string, number>;
  verificationStatusBreakdown: Record<string, number>;
}

