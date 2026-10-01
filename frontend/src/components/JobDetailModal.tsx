import React, { useState, useEffect } from 'react';
import {
  Job,
  MatchAnalysisResponse,
  MatchRecommendation,
  RequirementMatchType,
  TailoredResume,
  TailoringDiff,
} from '../types';
import { apiClient } from '../api/client';
import {
  X,
  Building2,
  MapPin,
  DollarSign,
  Calendar,
  ExternalLink,
  Briefcase,
  Sparkles,
  ShieldAlert,
  Cpu,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  TrendingUp,
  Award,
  FileText,
  Loader2,
  RefreshCw,
  Target,
  Compass,
  AlertOctagon,
  FileCheck,
  Download,
  Eye,
  Check,
  RotateCcw,
  ShieldCheck,
} from 'lucide-react';

interface JobDetailModalProps {
  job: Job | null;
  onClose: () => void;
  initialMatch?: MatchAnalysisResponse | null;
  onMatchUpdated?: (jobId: string, match: MatchAnalysisResponse) => void;
  onJobUpdated?: (updatedJob: Job) => void;
}

type TabType = 'overview' | 'match' | 'advantage' | 'tailoring';

export const JobDetailModal: React.FC<JobDetailModalProps> = ({
  job,
  onClose,
  initialMatch,
  onMatchUpdated,
  onJobUpdated,
}) => {
  if (!job) return null;

  const [currentJob, setCurrentJob] = useState<Job>(job);
  const [isUpdatingApplied, setIsUpdatingApplied] = useState(false);
  const [activeTab, setActiveTab] = useState<TabType>(initialMatch ? 'match' : 'overview');
  const [matchData, setMatchData] = useState<MatchAnalysisResponse | null>(initialMatch || null);
  const [loadingMatch, setLoadingMatch] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reqFilter, setReqFilter] = useState<'ALL' | 'STRONG' | 'PARTIAL' | 'TRANSFERABLE' | 'GAP'>('ALL');

  useEffect(() => {
    if (job) {
      setCurrentJob(job);
    }
  }, [job]);

  // Milestone 4: Resume Tailoring State
  const [tailoredResume, setTailoredResume] = useState<TailoredResume | null>(null);
  const [tailoringDiff, setTailoringDiff] = useState<TailoringDiff | null>(null);
  const [loadingTailoring, setLoadingTailoring] = useState(false);
  const [isTailoring, setIsTailoring] = useState(false);
  const [isValidating, setIsValidating] = useState(false);
  const [tailoringSubTab, setTailoringSubTab] = useState<'diff' | 'preview' | 'validation'>('diff');

  useEffect(() => {
    if (job) {
      if (!matchData && !initialMatch) {
        loadMatchData();
      }
      loadTailoringData();
    }
  }, [job?.id]);

  const loadMatchData = async () => {
    if (!job) return;
    setLoadingMatch(true);
    setError(null);
    try {
      const match = await apiClient.matching.getMatch(job.id);
      setMatchData(match);
      if (onMatchUpdated) onMatchUpdated(job.id, match);
    } catch (err: any) {
      console.warn('No existing match found or failed to fetch', err);
    } finally {
      setLoadingMatch(false);
    }
  };

  const loadTailoringData = async () => {
    if (!job) return;
    setLoadingTailoring(true);
    try {
      const list = await apiClient.tailoring.getTailoredResumesForJob(job.id);
      if (list && list.length > 0) {
        const latest = list[0];
        setTailoredResume(latest);
        try {
          const diff = await apiClient.tailoring.getDiff(latest.id);
          setTailoringDiff(diff);
        } catch (dErr) {
          console.warn('Could not load diff', dErr);
        }
      } else {
        setTailoredResume(null);
        setTailoringDiff(null);
      }
    } catch (err) {
      console.warn('No tailored resumes found for job', err);
    } finally {
      setLoadingTailoring(false);
    }
  };

  const handleGenerateTailoredResume = async () => {
    if (!job) return;
    setIsTailoring(true);
    setError(null);
    try {
      const tailored = await apiClient.tailoring.tailorResume(job.id);
      setTailoredResume(tailored);
      const diff = await apiClient.tailoring.getDiff(tailored.id);
      setTailoringDiff(diff);
      setActiveTab('tailoring');
    } catch (err: any) {
      console.error('Tailoring failed', err);
      setError(err?.response?.data?.message || 'Failed to tailor resume');
    } finally {
      setIsTailoring(false);
    }
  };

  const handleValidateTailoredResume = async () => {
    if (!tailoredResume) return;
    setIsValidating(true);
    try {
      const report = await apiClient.tailoring.validate(tailoredResume.id);
      setTailoredResume({ ...tailoredResume, validationReport: report });
    } catch (err: any) {
      console.error('Validation failed', err);
    } finally {
      setIsValidating(false);
    }
  };

  const handleRunAnalysis = async () => {
    if (!job) return;
    setIsAnalyzing(true);
    setError(null);
    try {
      const freshMatch = await apiClient.matching.analyze(job.id);
      setMatchData(freshMatch);
      if (onMatchUpdated) onMatchUpdated(job.id, freshMatch);
      setActiveTab('match');
    } catch (err: any) {
      console.error('Match analysis failed', err);
      setError(err?.response?.data?.message || 'Failed to analyze match');
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleToggleApplied = async (newAppliedStatus: boolean) => {
    if (!currentJob) return;
    setIsUpdatingApplied(true);
    try {
      const res = await apiClient.jobs.setApplied(currentJob.id, newAppliedStatus);
      const updated: Job = {
        ...currentJob,
        applied: res.applied,
        appliedAt: res.appliedAt,
      };
      setCurrentJob(updated);
      if (onJobUpdated) {
        onJobUpdated(updated);
      }
    } catch (err: any) {
      console.error('Failed to update application status', err);
    } finally {
      setIsUpdatingApplied(false);
    }
  };

  const handleTabChange = (tab: TabType) => {
    setActiveTab(tab);
  };

  const formatSalary = () => {
    if (job.minSalary && job.maxSalary) {
      if (job.salaryCurrency === 'INR') {
        const minLpa = (job.minSalary / 100000).toFixed(1);
        const maxLpa = (job.maxSalary / 100000).toFixed(1);
        return `₹${minLpa} - ₹${maxLpa} LPA`;
      }
      return `$${(job.minSalary / 1000).toFixed(0)}k - $${(job.maxSalary / 1000).toFixed(0)}k`;
    } else if (job.minSalary) {
      return job.salaryCurrency === 'INR'
        ? `₹${(job.minSalary / 100000).toFixed(1)} LPA+`
        : `$${(job.minSalary / 1000).toFixed(0)}k+`;
    }
    return 'Not Disclosed';
  };

  const getRecommendationBadge = (rec?: MatchRecommendation) => {
    if (!rec) return null;
    switch (rec) {
      case 'APPLY':
        return (
          <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 flex items-center gap-1.5 shadow-sm shadow-emerald-500/10">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> RECOMMENDED: APPLY NOW
          </span>
        );
      case 'APPLY_AFTER_TAILORING':
        return (
          <span className="px-3 py-1 rounded-full text-xs font-bold bg-cyan-500/15 text-cyan-400 border border-cyan-500/30 flex items-center gap-1.5 shadow-sm shadow-cyan-500/10">
            <Sparkles className="w-3.5 h-3.5 text-cyan-400" /> APPLY AFTER TAILORING
          </span>
        );
      case 'LOW_PRIORITY':
        return (
          <span className="px-3 py-1 rounded-full text-xs font-bold bg-amber-500/15 text-amber-400 border border-amber-500/30 flex items-center gap-1.5 shadow-sm shadow-amber-500/10">
            <AlertTriangle className="w-3.5 h-3.5 text-amber-400" /> LOW PRIORITY
          </span>
        );
      case 'DO_NOT_APPLY':
        return (
          <span className="px-3 py-1 rounded-full text-xs font-bold bg-rose-500/15 text-rose-400 border border-rose-500/30 flex items-center gap-1.5 shadow-sm shadow-rose-500/10">
            <AlertOctagon className="w-3.5 h-3.5 text-rose-400" /> DO NOT APPLY (GAP TOO HIGH)
          </span>
        );
      default:
        return null;
    }
  };

  const getMatchTypeBadge = (matchType: RequirementMatchType) => {
    switch (matchType) {
      case 'STRONG':
        return (
          <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-500/15 text-emerald-400 border border-emerald-500/25 flex items-center gap-1">
            <CheckCircle2 className="w-3 h-3 text-emerald-400" /> Strong
          </span>
        );
      case 'PARTIAL':
        return (
          <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-500/15 text-amber-400 border border-amber-500/25 flex items-center gap-1">
            <TrendingUp className="w-3 h-3 text-amber-400" /> Partial
          </span>
        );
      case 'TRANSFERABLE':
        return (
          <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-cyan-500/15 text-cyan-400 border border-cyan-500/25 flex items-center gap-1">
            <ArrowRight className="w-3 h-3 text-cyan-400" /> Transferable
          </span>
        );
      case 'GAP':
        return (
          <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-rose-500/15 text-rose-400 border border-rose-500/25 flex items-center gap-1">
            <AlertTriangle className="w-3 h-3 text-rose-400" /> Gap
          </span>
        );
    }
  };

  const filteredRequirements = matchData?.requirementAnalysis.filter((item) => {
    if (reqFilter === 'ALL') return true;
    return item.matchType === reqFilter;
  }) || [];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-slate-950/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-5xl max-h-[92vh] bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl flex flex-col overflow-hidden">
        {/* Header */}
        <div className="p-6 border-b border-slate-800 bg-slate-900/90 flex items-start justify-between gap-4">
          <div className="flex-1 min-w-0">
            <div className="flex flex-wrap items-center gap-2 mb-2">
              {matchData ? (
                getRecommendationBadge(matchData.recommendation)
              ) : (
                <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-800 text-slate-400 border border-slate-700 flex items-center gap-1">
                  <Compass className="w-3 h-3 text-slate-400" /> Discovered Posting
                </span>
              )}
              {job.sourceName && (
                <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                  {job.sourceName.replace('FIRECRAWL_', '')}
                </span>
              )}
              {matchData && (
                <span className="text-xs font-mono px-2 py-0.5 rounded bg-emerald-950/40 text-emerald-300 border border-emerald-800/50">
                  Score: {matchData.priorityScore}%
                </span>
              )}
            </div>

            <h2 className="text-2xl font-bold text-white tracking-tight truncate">{job.title}</h2>

            <div className="flex flex-wrap items-center gap-4 mt-2 text-sm text-slate-400">
              <div className="flex items-center gap-1.5 text-slate-200 font-medium">
                <Building2 className="w-4 h-4 text-emerald-400" />
                <span>{job.companyName}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <MapPin className="w-4 h-4 text-slate-500" />
                <span>{job.location}</span>
              </div>
              <span className="px-2 py-0.5 rounded-md text-xs font-medium bg-slate-800 text-slate-300 border border-slate-700">
                {job.workMode}
              </span>
              <span className="text-xs text-slate-400">
                {job.minExperienceYears ? `${job.minExperienceYears}${job.maxExperienceYears ? ` - ${job.maxExperienceYears}` : '+'} yrs` : 'Any exp'}
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleRunAnalysis}
              disabled={isAnalyzing}
              className="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-emerald-400 hover:text-emerald-300 text-xs font-semibold border border-slate-700 transition flex items-center gap-1.5 disabled:opacity-50"
              title="Execute full AI semantic match evaluation against your master profile"
            >
              {isAnalyzing ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Evaluating...</span>
                </>
              ) : (
                <>
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>{matchData ? 'Re-analyze' : 'Analyze Match'}</span>
                </>
              )}
            </button>
            <button
              onClick={onClose}
              className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="px-6 border-b border-slate-800 bg-slate-900/60 flex items-center gap-2 overflow-x-auto">
          <button
            onClick={() => handleTabChange('overview')}
            className={`py-3 px-3 text-xs font-semibold border-b-2 transition flex items-center gap-1.5 whitespace-nowrap ${
              activeTab === 'overview'
                ? 'border-emerald-400 text-emerald-300'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <FileText className="w-3.5 h-3.5" /> Overview
          </button>

          <button
            onClick={() => handleTabChange('match')}
            className={`py-3 px-3 text-xs font-semibold border-b-2 transition flex items-center gap-1.5 whitespace-nowrap ${
              activeTab === 'match'
                ? 'border-emerald-400 text-emerald-300'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Target className="w-3.5 h-3.5" /> Match Analysis
            {matchData && (
              <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono ${
                matchData.priorityScore >= 68
                  ? 'bg-emerald-500/20 text-emerald-300'
                  : matchData.priorityScore >= 48
                  ? 'bg-cyan-500/20 text-cyan-300'
                  : 'bg-amber-500/20 text-amber-300'
              }`}>
                {matchData.priorityScore}%
              </span>
            )}
          </button>

          <button
            onClick={() => handleTabChange('advantage')}
            className={`py-3 px-3 text-xs font-semibold border-b-2 transition flex items-center gap-1.5 whitespace-nowrap ${
              activeTab === 'advantage'
                ? 'border-emerald-400 text-emerald-300'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Award className="w-3.5 h-3.5 text-amber-400" /> The Edge (Advantage)
          </button>

          <button
            onClick={() => handleTabChange('tailoring')}
            className={`py-3 px-3 text-xs font-semibold border-b-2 transition flex items-center gap-1.5 whitespace-nowrap ${
              activeTab === 'tailoring'
                ? 'border-emerald-400 text-emerald-300'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <FileCheck className="w-3.5 h-3.5 text-indigo-400" /> Resume Tailoring
          </button>
        </div>

        {/* Tab Content Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {error && (
            <div className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-center gap-2">
              <AlertOctagon className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* TAB 1: OVERVIEW */}
          {activeTab === 'overview' && (
            <div className="space-y-6">
              {/* Key Facts Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 p-4 rounded-2xl bg-slate-950/60 border border-slate-800">
                <div>
                  <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider block">Compensation</span>
                  <span className="text-sm font-semibold text-emerald-400 flex items-center gap-1 mt-0.5">
                    <DollarSign className="w-3.5 h-3.5 flex-shrink-0" />
                    {formatSalary()}
                  </span>
                </div>
                <div>
                  <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider block">Experience</span>
                  <span className="text-sm font-semibold text-slate-200 flex items-center gap-1 mt-0.5">
                    <Briefcase className="w-3.5 h-3.5 flex-shrink-0 text-slate-400" />
                    {job.minExperienceYears ? `${job.minExperienceYears}${job.maxExperienceYears ? ` - ${job.maxExperienceYears}` : '+'} yrs` : 'Not Specified'}
                  </span>
                </div>
                <div>
                  <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider block">Posting Date</span>
                  <span className="text-sm font-semibold text-slate-200 flex items-center gap-1 mt-0.5">
                    <Calendar className="w-3.5 h-3.5 flex-shrink-0 text-slate-400" />
                    {job.postingDate || 'Recent'}
                  </span>
                </div>
                <div>
                  <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider block">Employment</span>
                  <span className="text-sm font-semibold text-slate-200 mt-0.5 block">
                    {job.employmentType?.replace('_', ' ') || 'Full Time'}
                  </span>
                </div>
              </div>

              {/* Match CTA banner if not analyzed yet */}
              {!matchData && (
                <div className="p-5 rounded-2xl bg-gradient-to-r from-emerald-950/40 via-slate-900 to-cyan-950/30 border border-emerald-500/20 flex items-center justify-between gap-4">
                  <div>
                    <h4 className="text-sm font-bold text-white flex items-center gap-1.5">
                      <Sparkles className="w-4 h-4 text-emerald-400" /> Run Candidate Semantic Match
                    </h4>
                    <p className="text-xs text-slate-300 mt-1 max-w-xl">
                      Evaluate this job against your verified skills, commercial experience, and projects. See your grounded match score, requirement gaps, and strategic application positioning.
                    </p>
                  </div>
                  <button
                    onClick={handleRunAnalysis}
                    disabled={isAnalyzing}
                    className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold transition flex items-center gap-2 shadow-lg shadow-emerald-600/20 flex-shrink-0"
                  >
                    {isAnalyzing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Target className="w-4 h-4" />}
                    <span>Evaluate Match</span>
                  </button>
                </div>
              )}

              {/* Detected Technologies */}
              {job.detectedTechnologies && job.detectedTechnologies.length > 0 && (
                <div>
                  <div className="flex items-center gap-2 mb-2.5">
                    <Cpu className="w-4 h-4 text-emerald-400" />
                    <h3 className="text-sm font-semibold text-white uppercase tracking-wider">Detected Technologies</h3>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {job.detectedTechnologies.map((tech) => (
                      <span
                        key={tech}
                        className="px-3 py-1 rounded-xl text-xs font-semibold bg-emerald-500/10 text-emerald-300 border border-emerald-500/20"
                      >
                        {tech}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Job Description */}
              <div>
                <h3 className="text-sm font-semibold text-white uppercase tracking-wider mb-2.5">Cleaned Job Description</h3>
                <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800 text-slate-300 text-sm leading-relaxed whitespace-pre-wrap font-sans max-h-96 overflow-y-auto">
                  {job.rawDescriptionMarkdown || 'No detailed description available.'}
                </div>
              </div>

              {/* Source Integrity Notice */}
              <div className="p-4 rounded-2xl bg-slate-950/50 border border-slate-800/80 flex items-start gap-3">
                <ShieldAlert className="w-5 h-5 text-emerald-400 flex-shrink-0 mt-0.5" />
                <div className="text-xs text-slate-400">
                  <span className="font-semibold text-slate-200 block mb-0.5">Human-In-The-Loop Enforcement</span>
                  This job posting was retrieved directly from the verified ATS portal (<span className="text-slate-300 font-mono">{job.canonicalUrl}</span>).
                  JobHunter AI never submits applications automatically; you review all findings and launch the official application yourself.
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: MATCH ANALYSIS */}
          {activeTab === 'match' && (
            <div className="space-y-6">
              {loadingMatch ? (
                <div className="py-12 flex flex-col items-center justify-center text-slate-400 gap-3">
                  <Loader2 className="w-6 h-6 animate-spin text-emerald-400" />
                  <span className="text-sm">Retrieving semantic match analysis...</span>
                </div>
              ) : !matchData ? (
                <div className="py-12 text-center space-y-3">
                  <Compass className="w-10 h-10 text-slate-600 mx-auto" />
                  <h4 className="text-base font-semibold text-white">No Match Evaluation Yet</h4>
                  <p className="text-xs text-slate-400 max-w-md mx-auto">
                    Click "Analyze Match" to run multi-dimensional semantic comparison, requirement verification, and risk analysis against your profile.
                  </p>
                  <button
                    onClick={handleRunAnalysis}
                    disabled={isAnalyzing}
                    className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold transition inline-flex items-center gap-2"
                  >
                    {isAnalyzing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
                    <span>Run Analysis Now</span>
                  </button>
                </div>
              ) : (
                <>
                  {/* Score & Recommendation Banner */}
                  <div className="p-6 rounded-3xl bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 border border-slate-800 shadow-xl">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                      <div>
                        <div className="text-xs uppercase font-medium tracking-wider text-slate-400 mb-1">
                          Semantic Match &amp; Intelligent Prioritization
                        </div>
                        <div className="flex flex-wrap items-center gap-2.5">
                          {getRecommendationBadge(matchData.recommendation)}
                          {matchData.priorityCategory && (
                            <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold border ${
                              matchData.priorityCategory === 'HIGH_PRIORITY'
                                ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                                : matchData.priorityCategory === 'MEDIUM_PRIORITY'
                                ? 'bg-cyan-500/20 text-cyan-300 border-cyan-500/40'
                                : matchData.priorityCategory === 'LOW_PRIORITY'
                                ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                                : 'bg-rose-500/20 text-rose-300 border-rose-500/40'
                            }`}>
                              {matchData.priorityCategory.replace('_', ' ')}
                            </span>
                          )}
                          {matchData.freshness && (
                            <span className="px-2.5 py-0.5 rounded-full text-xs font-mono bg-slate-800 text-slate-300 border border-slate-700">
                              Freshness: {matchData.freshness} {matchData.daysSincePosted != null ? `(${matchData.daysSincePosted}d ago)` : ''}
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="flex items-center gap-6">
                        <div className="text-right">
                          <span className="text-3xl font-extrabold text-white tracking-tight">
                            {matchData.priorityScore}%
                          </span>
                          <span className="text-[11px] font-medium text-slate-400 block uppercase">
                            Priority Match Score
                          </span>
                        </div>
                        <div className="text-right pl-6 border-l border-slate-800">
                          <span className="text-xl font-bold text-cyan-400 tracking-tight">
                            {Math.round(matchData.semanticSimilarityScore * 100)}%
                          </span>
                          <span className="text-[11px] font-medium text-slate-400 block uppercase">
                            Vector Cosine Sim
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Overall Assessment Narrative */}
                    <div className="mt-5 pt-4 border-t border-slate-800/80 text-sm text-slate-300 leading-relaxed">
                      <span className="font-semibold text-emerald-400">Assessment: </span>
                      {matchData.overallAssessment}
                    </div>
                  </div>

                  {/* Prioritization Analysis: Why This Job vs Potential Concerns */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="p-5 rounded-2xl bg-emerald-950/20 border border-emerald-500/25">
                      <div className="flex items-center gap-2 text-emerald-400 text-xs font-bold uppercase tracking-wider mb-2.5">
                        <Sparkles className="w-4 h-4" /> Why This Job? (Evidence-Backed Positives)
                      </div>
                      {matchData.whyThisJob && matchData.whyThisJob.length > 0 ? (
                        <ul className="space-y-2 text-xs text-slate-200">
                          {matchData.whyThisJob.map((item, idx) => (
                            <li key={idx} className="flex items-start gap-2 p-2 rounded-xl bg-slate-900/60 border border-emerald-500/10">
                              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 flex-shrink-0 mt-0.5" />
                              <span>{item}</span>
                            </li>
                          ))}
                        </ul>
                      ) : (
                        <p className="text-xs text-slate-400 italic">No strong positives detected for this posting.</p>
                      )}
                    </div>

                    <div className="p-5 rounded-2xl bg-amber-950/20 border border-amber-500/25">
                      <div className="flex items-center gap-2 text-amber-400 text-xs font-bold uppercase tracking-wider mb-2.5">
                        <AlertTriangle className="w-4 h-4" /> Potential Concerns &amp; Friction
                      </div>
                      {matchData.potentialConcerns && matchData.potentialConcerns.length > 0 ? (
                        <ul className="space-y-2 text-xs text-slate-200">
                          {matchData.potentialConcerns.map((item, idx) => (
                            <li key={idx} className="flex items-start gap-2 p-2 rounded-xl bg-slate-900/60 border border-amber-500/10">
                              <AlertOctagon className="w-3.5 h-3.5 text-amber-400 flex-shrink-0 mt-0.5" />
                              <span>{item}</span>
                            </li>
                          ))}
                        </ul>
                      ) : (
                        <p className="text-xs text-emerald-400 italic flex items-center gap-1.5">
                          <Check className="w-3.5 h-3.5" /> Zero hard constraints or critical concerns detected.
                        </p>
                      )}
                    </div>
                  </div>

                  {/* 4 Cards: Strong / Transferable / Gaps / Risk */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
                    <div className="p-4 rounded-2xl bg-emerald-950/20 border border-emerald-500/20">
                      <div className="flex items-center gap-2 text-emerald-400 text-xs font-semibold mb-2">
                        <CheckCircle2 className="w-4 h-4" /> Strong Matches ({matchData.strongMatches.length})
                      </div>
                      <div className="space-y-1.5 text-xs text-slate-300 max-h-40 overflow-y-auto">
                        {matchData.strongMatches.length > 0 ? (
                          matchData.strongMatches.map((s, idx) => (
                            <div key={idx} className="p-1.5 rounded-lg bg-emerald-900/10 border border-emerald-500/10 text-[11px] leading-tight">
                              {s}
                            </div>
                          ))
                        ) : (
                          <span className="text-slate-500 italic">None detected</span>
                        )}
                      </div>
                    </div>

                    <div className="p-4 rounded-2xl bg-cyan-950/20 border border-cyan-500/20">
                      <div className="flex items-center gap-2 text-cyan-400 text-xs font-semibold mb-2">
                        <ArrowRight className="w-4 h-4" /> Transferable ({matchData.transferableExperience.length})
                      </div>
                      <div className="space-y-1.5 text-xs text-slate-300 max-h-40 overflow-y-auto">
                        {matchData.transferableExperience.length > 0 ? (
                          matchData.transferableExperience.map((t, idx) => (
                            <div key={idx} className="p-1.5 rounded-lg bg-cyan-900/10 border border-cyan-500/10 text-[11px] leading-tight">
                              {t}
                            </div>
                          ))
                        ) : (
                          <span className="text-slate-500 italic">None detected</span>
                        )}
                      </div>
                    </div>

                    <div className="p-4 rounded-2xl bg-rose-950/20 border border-rose-500/20">
                      <div className="flex items-center gap-2 text-rose-400 text-xs font-semibold mb-2">
                        <AlertTriangle className="w-4 h-4" /> Requirement Gaps ({matchData.gaps.length})
                      </div>
                      <div className="space-y-1.5 text-xs text-slate-300 max-h-40 overflow-y-auto">
                        {matchData.gaps.length > 0 ? (
                          matchData.gaps.map((g, idx) => (
                            <div key={idx} className="p-1.5 rounded-lg bg-rose-900/10 border border-rose-500/10 text-[11px] leading-tight">
                              {g}
                            </div>
                          ))
                        ) : (
                          <span className="text-slate-500 italic">Zero gaps found!</span>
                        )}
                      </div>
                    </div>

                    <div className="p-4 rounded-2xl bg-amber-950/20 border border-amber-500/20">
                      <div className="flex items-center gap-2 text-amber-400 text-xs font-semibold mb-2">
                        <AlertOctagon className="w-4 h-4" /> Risk & Friction ({matchData.riskFactors.length})
                      </div>
                      <div className="space-y-1.5 text-xs text-slate-300 max-h-40 overflow-y-auto">
                        {matchData.riskFactors.length > 0 ? (
                          matchData.riskFactors.map((r, idx) => (
                            <div key={idx} className="p-1.5 rounded-lg bg-amber-900/10 border border-amber-500/10 text-[11px] leading-tight">
                              {r}
                            </div>
                          ))
                        ) : (
                          <span className="text-emerald-400 italic">No friction factors</span>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Transparent Requirement-by-Requirement Table */}
                  <div>
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-3">
                      <div>
                        <h3 className="text-sm font-bold text-white uppercase tracking-wider">
                          Requirement-by-Requirement Grounded Audit
                        </h3>
                        <p className="text-xs text-slate-400">
                          Zero-hallucination verification gate: Each JD requirement checked against verified candidate profile
                        </p>
                      </div>

                      {/* Filter Pills */}
                      <div className="flex items-center gap-1.5 overflow-x-auto">
                        {(['ALL', 'STRONG', 'PARTIAL', 'TRANSFERABLE', 'GAP'] as const).map((filter) => (
                          <button
                            key={filter}
                            onClick={() => setReqFilter(filter)}
                            className={`px-2.5 py-1 rounded-lg text-xs font-medium transition whitespace-nowrap ${
                              reqFilter === filter
                                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                                : 'bg-slate-800 text-slate-400 hover:text-slate-200 border border-transparent'
                            }`}
                          >
                            {filter}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="rounded-2xl border border-slate-800 overflow-hidden bg-slate-950/70">
                      <div className="overflow-x-auto">
                        <table className="w-full text-left border-collapse text-xs">
                          <thead>
                            <tr className="border-b border-slate-800 bg-slate-900/80 text-slate-400 font-semibold">
                              <th className="p-3 w-1/3">JD Requirement</th>
                              <th className="p-3 w-28">Match Status</th>
                              <th className="p-3">Candidate Grounded Evidence</th>
                              <th className="p-3">Assessment Explanation</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800/60">
                            {filteredRequirements.map((item, idx) => (
                              <tr key={idx} className="hover:bg-slate-900/40 transition">
                                <td className="p-3 text-slate-200">
                                  <div className="font-medium">{item.requirement}</div>
                                  <div className="flex items-center gap-2 mt-1">
                                    <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-800 text-slate-400 border border-slate-700">
                                      {item.category}
                                    </span>
                                    {item.isImplied && (
                                      <span className="text-[10px] text-cyan-400 italic">Implied</span>
                                    )}
                                  </div>
                                </td>
                                <td className="p-3">
                                  {getMatchTypeBadge(item.matchType)}
                                  <span className="text-[10px] text-slate-500 mt-1 block">
                                    Conf: {item.confidence}
                                  </span>
                                </td>
                                <td className="p-3 text-slate-300">
                                  <div className="font-mono text-[11px] leading-relaxed">
                                    {item.candidateEvidence}
                                  </div>
                                </td>
                                <td className="p-3 text-slate-400 leading-relaxed text-[11px]">
                                  {item.explanation}
                                </td>
                              </tr>
                            ))}
                            {filteredRequirements.length === 0 && (
                              <tr>
                                <td colSpan={4} className="p-6 text-center text-slate-500">
                                  No requirements matching filter '{reqFilter}'
                                </td>
                              </tr>
                            )}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  </div>
                </>
              )}
            </div>
          )}

          {/* TAB 3: THE EDGE (10-DIMENSION APPLICATION ADVANTAGE REPORT) */}
          {activeTab === 'advantage' && (
            <div className="space-y-6">
              {!matchData?.advantageReport ? (
                <div className="py-12 text-center space-y-3">
                  <Award className="w-10 h-10 text-slate-600 mx-auto" />
                  <h4 className="text-base font-semibold text-white">No Advantage Report Generated</h4>
                  <p className="text-xs text-slate-400 max-w-md mx-auto">
                    Run the semantic match analysis to synthesize your tactical application positioning and candidate advantage report.
                  </p>
                  <button
                    onClick={handleRunAnalysis}
                    disabled={isAnalyzing}
                    className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold transition inline-flex items-center gap-2"
                  >
                    {isAnalyzing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
                    <span>Generate Advantage Report</span>
                  </button>
                </div>
              ) : (
                <div className="space-y-6">
                  {/* Dimension 2: Candidate Relevance Banner */}
                  <div className="p-5 rounded-2xl bg-gradient-to-r from-amber-950/30 via-slate-900 to-emerald-950/20 border border-amber-500/20">
                    <div className="flex items-center gap-2 text-amber-400 text-xs font-bold uppercase tracking-wider mb-2">
                      <Award className="w-4 h-4" /> Dimension 2: Candidate Relevance
                    </div>
                    <p className="text-sm text-slate-200 leading-relaxed">
                      {matchData.advantageReport.candidateRelevance}
                    </p>
                  </div>

                  {/* Dimension 1 & Dimension 3: Employer Priorities vs Strongest Evidence */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                      <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-3 flex items-center gap-2">
                        <Target className="w-4 h-4 text-emerald-400" /> Dimension 1: Employer Priorities
                      </h4>
                      <ul className="space-y-2 text-xs text-slate-300">
                        {matchData.advantageReport.employerPriorities?.map((item, idx) => (
                          <li key={idx} className="flex items-start gap-2 p-2 rounded-xl bg-slate-900/60 border border-slate-800">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 mt-1.5 flex-shrink-0" />
                            <span>{item}</span>
                          </li>
                        ))}
                      </ul>
                    </div>

                    <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                      <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-3 flex items-center gap-2">
                        <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Dimension 3: Strongest Evidence
                      </h4>
                      <ul className="space-y-2 text-xs text-slate-300">
                        {matchData.advantageReport.strongestEvidence?.map((item, idx) => (
                          <li key={idx} className="flex items-start gap-2 p-2 rounded-xl bg-emerald-950/20 border border-emerald-500/20">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 mt-1.5 flex-shrink-0" />
                            <span>{item}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>

                  {/* Dimension 4 & Dimension 5: What to Emphasize vs What to De-emphasize */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                      <h4 className="text-xs font-bold text-emerald-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                        <TrendingUp className="w-4 h-4" /> Dimension 4: What to Emphasize
                      </h4>
                      <ul className="space-y-2 text-xs text-slate-300">
                        {matchData.advantageReport.whatToEmphasize?.map((item, idx) => (
                          <li key={idx} className="flex items-start gap-2 p-2 rounded-xl bg-slate-900/60 border border-slate-800">
                            <span className="text-emerald-400 font-bold">✓</span>
                            <span>{item}</span>
                          </li>
                        ))}
                      </ul>
                    </div>

                    <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                      <h4 className="text-xs font-bold text-rose-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                        <AlertTriangle className="w-4 h-4" /> Dimension 5: What to De-emphasize
                      </h4>
                      <ul className="space-y-2 text-xs text-slate-300">
                        {matchData.advantageReport.whatToDeemphasize?.map((item, idx) => (
                          <li key={idx} className="flex items-start gap-2 p-2 rounded-xl bg-slate-900/60 border border-slate-800">
                            <span className="text-rose-400 font-bold">✕</span>
                            <span>{item}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>

                  {/* Dimension 6 & Dimension 7: Honest Gaps vs Transferable Skills */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                      <h4 className="text-xs font-bold text-amber-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                        <ShieldAlert className="w-4 h-4" /> Dimension 6: Honest Gaps
                      </h4>
                      <ul className="space-y-2 text-xs text-slate-300">
                        {(matchData.advantageReport.honestGaps || []).map((item, idx) => (
                          <li key={idx} className="p-2.5 rounded-xl bg-amber-950/15 border border-amber-500/20 text-xs text-slate-300">
                            {item}
                          </li>
                        ))}
                      </ul>
                    </div>

                    <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                      <h4 className="text-xs font-bold text-cyan-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                        <Cpu className="w-4 h-4" /> Dimension 7: Transferable Skills
                      </h4>
                      <ul className="space-y-2 text-xs text-slate-300">
                        {matchData.advantageReport.transferableSkills?.map((item, idx) => (
                          <li key={idx} className="flex items-start gap-2 p-2 rounded-xl bg-cyan-950/15 border border-cyan-500/20">
                            <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 mt-1.5 flex-shrink-0" />
                            <span>{item}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>

                  {/* Dimension 8 & Dimension 9: Resume Positioning vs Application Fit & Positioning */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                      <h4 className="text-xs font-bold text-indigo-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                        <FileCheck className="w-4 h-4" /> Dimension 8: Resume Positioning
                      </h4>
                      <ul className="space-y-2 text-xs text-slate-300">
                        {matchData.advantageReport.resumePositioning?.map((item, idx) => (
                          <li key={idx} className="flex items-start gap-2 p-2 rounded-xl bg-indigo-950/15 border border-indigo-500/20">
                            <span className="text-indigo-400 font-bold">→</span>
                            <span>{item}</span>
                          </li>
                        ))}
                      </ul>
                    </div>

                    <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                      <h4 className="text-xs font-bold text-emerald-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                        <Briefcase className="w-4 h-4" /> Dimension 9: Application Fit & Positioning
                      </h4>
                      <ul className="space-y-2 text-xs text-slate-300">
                        {matchData.advantageReport.applicationFitAndPositioning?.map((item, idx) => (
                          <li key={idx} className="flex items-start gap-2 p-2 rounded-xl bg-emerald-950/15 border border-emerald-500/20">
                            <span className="text-emerald-400 font-bold">✓</span>
                            <span>{item}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>

                  {/* Dimension 10: Application Strategy */}
                  <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                    <h4 className="text-xs font-bold text-indigo-400 uppercase tracking-wider mb-2 flex items-center gap-2">
                      <Compass className="w-4 h-4" /> Dimension 10: Application Strategy
                    </h4>
                    <p className="text-xs text-slate-300 leading-relaxed">
                      {matchData.advantageReport.applicationStrategy}
                    </p>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* TAB 4: RESUME TAILORING */}
          {activeTab === 'tailoring' && (
            <div className="space-y-6">
              {/* Header & Status Card */}
              <div className="p-5 rounded-2xl bg-gradient-to-r from-indigo-950/40 via-slate-900 to-cyan-950/30 border border-indigo-500/20 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2 text-indigo-400 text-xs font-bold uppercase tracking-wider mb-1.5">
                    <FileCheck className="w-4 h-4" /> Intelligent Resume Tailoring & Optimization
                  </div>
                  <p className="text-xs text-slate-300 leading-relaxed max-w-xl">
                    Generates a truthful, job-specific application package without hallucinating or inventing qualifications. All claims are strictly grounded in your verified experience.
                  </p>
                  <div className="flex flex-wrap items-center gap-2 mt-3">
                    <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-950/50 text-emerald-400 border border-emerald-500/30 text-[11px] font-medium">
                      <ShieldCheck className="w-3.5 h-3.5" /> Zero-Hallucination Grounded
                    </span>
                    {tailoredResume && (
                      <>
                        <span className="px-2.5 py-1 rounded-lg bg-indigo-950/50 text-indigo-300 border border-indigo-500/30 text-[11px] font-mono font-medium">
                          v{tailoredResume.versionNumber} ({tailoredResume.status})
                        </span>
                        <span className="px-2.5 py-1 rounded-lg bg-cyan-950/50 text-cyan-300 border border-cyan-500/30 text-[11px] font-medium">
                          ATS Score: {tailoredResume.atsScoreEstimate}%
                        </span>
                        {tailoredResume.validationReport?.passed && (
                          <span className="px-2.5 py-1 rounded-lg bg-emerald-950/50 text-emerald-300 border border-emerald-500/30 text-[11px] font-medium">
                            Quality: {tailoredResume.validationReport.qualityScore}/100
                          </span>
                        )}
                      </>
                    )}
                  </div>
                </div>

                <div className="flex flex-wrap md:flex-col gap-2 flex-shrink-0">
                  <button
                    onClick={handleGenerateTailoredResume}
                    disabled={isTailoring}
                    className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-xs font-semibold transition flex items-center justify-center gap-2 shadow-lg shadow-indigo-600/20"
                  >
                    {isTailoring ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        <span>Tailoring Resume...</span>
                      </>
                    ) : (
                      <>
                        <Sparkles className="w-4 h-4" />
                        <span>{tailoredResume ? 'Re-Tailor Resume' : 'Generate Tailored Resume'}</span>
                      </>
                    )}
                  </button>

                  {tailoredResume?.pdfAvailable && (
                    <a
                      href={apiClient.tailoring.getPdfDownloadUrl(tailoredResume.id)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold transition flex items-center justify-center gap-2 shadow-lg shadow-emerald-600/20"
                    >
                      <Download className="w-4 h-4" />
                      <span>Download PDF</span>
                    </a>
                  )}
                </div>
              </div>

              {loadingTailoring && (
                <div className="py-12 text-center text-slate-400 flex items-center justify-center gap-2 text-xs">
                  <Loader2 className="w-4 h-4 animate-spin text-indigo-400" />
                  <span>Loading tailored resume details...</span>
                </div>
              )}

              {!loadingTailoring && !tailoredResume && (
                <div className="py-12 text-center space-y-4 rounded-2xl bg-slate-950/50 border border-slate-800 p-8">
                  <FileText className="w-12 h-12 text-indigo-400/60 mx-auto" />
                  <h4 className="text-base font-semibold text-white">No Tailored Resume Generated Yet</h4>
                  <p className="text-xs text-slate-400 max-w-md mx-auto leading-relaxed">
                    Click <strong className="text-indigo-300">Generate Tailored Resume</strong> to analyze this job's requirements, optimize your verified bullet points for ATS screening, generate a single-column LaTeX PDF, and audit all technical claims against the Grounding Verification Gate.
                  </p>
                  <button
                    onClick={handleGenerateTailoredResume}
                    disabled={isTailoring}
                    className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold transition inline-flex items-center gap-2 shadow-lg shadow-indigo-600/20"
                  >
                    {isTailoring ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
                    <span>Generate Tailored Resume Now</span>
                  </button>
                </div>
              )}

              {!loadingTailoring && tailoredResume && (
                <div className="space-y-6">
                  {/* Tailoring Sub-tabs */}
                  <div className="flex border-b border-slate-800 gap-1">
                    <button
                      onClick={() => setTailoringSubTab('diff')}
                      className={`px-4 py-2.5 text-xs font-medium border-b-2 transition flex items-center gap-2 ${
                        tailoringSubTab === 'diff'
                          ? 'border-indigo-400 text-indigo-300 bg-indigo-950/10'
                          : 'border-transparent text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      <RotateCcw className="w-3.5 h-3.5" />
                      <span>Before / After Comparison</span>
                    </button>

                    <button
                      onClick={() => setTailoringSubTab('preview')}
                      className={`px-4 py-2.5 text-xs font-medium border-b-2 transition flex items-center gap-2 ${
                        tailoringSubTab === 'preview'
                          ? 'border-indigo-400 text-indigo-300 bg-indigo-950/10'
                          : 'border-transparent text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      <Eye className="w-3.5 h-3.5" />
                      <span>Resume Content Preview</span>
                    </button>

                    <button
                      onClick={() => setTailoringSubTab('validation')}
                      className={`px-4 py-2.5 text-xs font-medium border-b-2 transition flex items-center gap-2 ${
                        tailoringSubTab === 'validation'
                          ? 'border-indigo-400 text-indigo-300 bg-indigo-950/10'
                          : 'border-transparent text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      <ShieldCheck className="w-3.5 h-3.5" />
                      <span>Quality & Validation Report</span>
                    </button>
                  </div>

                  {/* SUBTAB 1: BEFORE / AFTER COMPARISON */}
                  {tailoringSubTab === 'diff' && (
                    <div className="space-y-6">
                      {/* 1. Bullet Point Sharpening Proposals */}
                      <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                        <div className="flex items-center justify-between mb-3">
                          <h4 className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
                            <TrendingUp className="w-4 h-4 text-emerald-400" />
                            <span>Grounded Bullet Point Sharpening</span>
                          </h4>
                          <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-emerald-950/50 text-emerald-300 border border-emerald-500/30">
                            Strict Zero-Hallucination Enforced
                          </span>
                        </div>
                        <p className="text-xs text-slate-400 mb-4">
                          Bullets are sharpened to highlight production metrics, latency optimization, and core competencies without fabricating unverified claims.
                        </p>

                        <div className="space-y-4">
                          {tailoredResume.tailoringPlan?.bulletSharpeningProposals?.map((item, idx) => (
                            <div key={idx} className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
                              {/* Original */}
                              <div>
                                <div className="text-[10px] font-mono text-slate-500 uppercase tracking-wider mb-1 flex items-center gap-1.5">
                                  <span className="w-2 h-2 rounded-full bg-slate-600 inline-block" />
                                  Master Resume Baseline:
                                </div>
                                <div className="text-xs text-slate-300 bg-slate-950/60 p-2.5 rounded-lg border border-slate-800/80 italic">
                                  "{item.originalBullet}"
                                </div>
                              </div>

                              {/* Proposed */}
                              <div>
                                <div className="text-[10px] font-mono text-emerald-400 uppercase tracking-wider mb-1 flex items-center justify-between">
                                  <div className="flex items-center gap-1.5">
                                    <span className="w-2 h-2 rounded-full bg-emerald-400 inline-block" />
                                    <span>ATS-Sharpened Bullet:</span>
                                  </div>
                                  <span className="px-2 py-0.2 rounded bg-emerald-950/50 text-emerald-400 border border-emerald-500/20 text-[10px]">
                                    {item.groundingStatus}
                                  </span>
                                </div>
                                <div className="text-xs font-medium text-emerald-300 bg-emerald-950/20 p-2.5 rounded-lg border border-emerald-500/20">
                                  "{item.proposedBullet}"
                                </div>
                              </div>

                              {/* Metadata & Rationale */}
                              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px] pt-1 border-t border-slate-800/60 text-slate-400">
                                <div>
                                  <span className="font-semibold text-slate-300">Target Requirement: </span>
                                  <span>{item.jobRequirement}</span>
                                </div>
                                <div>
                                  <span className="font-semibold text-slate-300">Grounding Evidence: </span>
                                  <span>{item.evidenceReferences?.join(' | ') || 'Verified Candidate Profile'}</span>
                                </div>
                              </div>
                              <div className="text-[11px] text-slate-400">
                                <span className="font-semibold text-slate-300">Sharpening Rationale: </span>
                                <span>{item.reason}</span>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>

                      {/* 2. Skills Alignment: Emphasized vs De-emphasized */}
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                          <h4 className="text-xs font-bold text-emerald-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                            <CheckCircle2 className="w-4 h-4" /> Skills Promoted to Primary Bar
                          </h4>
                          <p className="text-[11px] text-slate-400 mb-3">
                            These verified skills directly match the job description and are promoted to the first line of the technical skills bar for ATS parsing:
                          </p>
                          <div className="flex flex-wrap gap-1.5">
                            {tailoredResume.tailoringPlan?.skillsToEmphasize?.map((sk, idx) => (
                              <span
                                key={idx}
                                className="px-2.5 py-1 rounded-lg bg-emerald-950/40 text-emerald-300 border border-emerald-500/30 text-xs font-medium"
                              >
                                {sk}
                              </span>
                            ))}
                          </div>
                        </div>

                        <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                          <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                            <AlertTriangle className="w-4 h-4 text-amber-400" /> Skills De-emphasized
                          </h4>
                          <p className="text-[11px] text-slate-400 mb-3">
                            Secondary skills not requested in this job description are de-emphasized to prevent recruiter confusion:
                          </p>
                          <ul className="space-y-1.5 text-xs text-slate-300">
                            {tailoredResume.tailoringPlan?.skillsToDeemphasize?.map((sk, idx) => (
                              <li key={idx} className="flex items-center gap-2 p-1.5 rounded-lg bg-slate-900 border border-slate-800">
                                <span className="text-slate-500">•</span>
                                <span>{sk}</span>
                              </li>
                            ))}
                          </ul>
                        </div>
                      </div>

                      {/* 3. Section Ordering Recommendation */}
                      <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                        <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-2 flex items-center gap-2">
                          <Compass className="w-4 h-4 text-indigo-400" /> Recommended Resume Section Ordering
                        </h4>
                        <p className="text-xs text-slate-400 mb-3">
                          {tailoredResume.tailoringPlan?.sectionOrderRationale}
                        </p>
                        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-2">
                          {tailoredResume.tailoringPlan?.recommendedSectionOrder?.map((sec, idx) => (
                            <div key={idx} className="p-2.5 rounded-xl bg-slate-900 border border-slate-800 text-center">
                              <span className="text-[10px] font-mono text-indigo-400 block mb-1">Step {idx + 1}</span>
                              <span className="text-xs font-medium text-slate-200">{sec.split('(')[0].trim()}</span>
                            </div>
                          ))}
                        </div>
                      </div>

                      {/* 4. Zero-Hallucination Rejection Audit */}
                      <div className="p-5 rounded-2xl bg-slate-950/70 border border-rose-500/20">
                        <div className="flex items-center justify-between mb-3">
                          <h4 className="text-xs font-bold text-rose-400 uppercase tracking-wider flex items-center gap-2">
                            <ShieldAlert className="w-4 h-4" /> Zero-Hallucination Defense & Rejection Audit
                          </h4>
                          <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-rose-950/50 text-rose-300 border border-rose-500/30">
                            Grounding Verification Gate
                          </span>
                        </div>
                        <p className="text-xs text-slate-300 mb-4 leading-relaxed">
                          Job descriptions frequently ask for technologies you have not worked with. Unlike dishonest resume generators, JobHunter AI <strong>never invents qualifications</strong>. The following keywords requested by this employer were explicitly rejected:
                        </p>

                        {tailoredResume.tailoringPlan?.rejectedKeywords && tailoredResume.tailoringPlan.rejectedKeywords.length > 0 ? (
                          <div className="space-y-2.5">
                            {tailoredResume.tailoringPlan.rejectedKeywords.map((rej, idx) => (
                              <div key={idx} className="p-3 rounded-xl bg-rose-950/15 border border-rose-500/20 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs">
                                <div className="flex items-center gap-2 font-mono text-rose-300 font-semibold">
                                  <span className="px-2 py-0.5 rounded bg-rose-900/40 text-rose-300 border border-rose-700/40">
                                    REJECTED: {rej.keyword}
                                  </span>
                                </div>
                                <div className="text-slate-300 text-[11px] leading-relaxed">
                                  {rej.reason}
                                </div>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <div className="p-3 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-400">
                            All requested keywords in this job description are supported by your verified candidate experience!
                          </div>
                        )}
                      </div>

                      {/* 5. Master vs Tailored Side-by-Side Comparison */}
                      {tailoringDiff && (
                        <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                          <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-2 flex items-center gap-2">
                            <FileText className="w-4 h-4 text-indigo-400" />
                            <span>Master Resume vs Tailored Resume Document Comparison</span>
                          </h4>
                          <p className="text-xs text-slate-400 mb-4">
                            Master resume remains strictly immutable. Tailored resume reflects job-specific emphasis, bullet sharpening, and ATS optimization.
                          </p>

                          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 font-mono text-xs">
                            <div className="space-y-2">
                              <div className="text-[11px] text-slate-400 font-bold uppercase tracking-wider flex items-center gap-1.5">
                                <span className="w-2 h-2 rounded-full bg-slate-500" /> Master Baseline Resume (Immutable)
                              </div>
                              <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-400 whitespace-pre-wrap max-h-96 overflow-y-auto leading-relaxed">
                                {tailoringDiff.masterResumeContent}
                              </div>
                            </div>

                            <div className="space-y-2">
                              <div className="text-[11px] text-emerald-400 font-bold uppercase tracking-wider flex items-center gap-1.5">
                                <span className="w-2 h-2 rounded-full bg-emerald-400" /> Tailored Application Resume
                              </div>
                              <div className="p-4 rounded-xl bg-slate-900 border border-emerald-500/30 text-emerald-300/90 whitespace-pre-wrap max-h-96 overflow-y-auto leading-relaxed">
                                {tailoringDiff.tailoredResumeContent || tailoredResume.tailoredMarkdown}
                              </div>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  )}

                  {/* SUBTAB 2: RESUME CONTENT PREVIEW */}
                  {tailoringSubTab === 'preview' && (
                    <div className="space-y-4">
                      <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                        <div className="flex items-center justify-between mb-4">
                          <h4 className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
                            <FileText className="w-4 h-4 text-indigo-400" /> Tailored Resume Text Content
                          </h4>
                          {tailoredResume.pdfAvailable && (
                            <a
                              href={apiClient.tailoring.getPdfDownloadUrl(tailoredResume.id)}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-medium transition inline-flex items-center gap-1.5"
                            >
                              <Download className="w-3.5 h-3.5" /> Download PDF
                            </a>
                          )}
                        </div>

                        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 font-mono text-xs text-slate-300 whitespace-pre-wrap leading-relaxed max-h-[500px] overflow-y-auto">
                          {tailoredResume.tailoredMarkdown}
                        </div>
                      </div>
                    </div>
                  )}

                  {/* SUBTAB 3: QUALITY & VALIDATION REPORT */}
                  {tailoringSubTab === 'validation' && (
                    <div className="space-y-4">
                      <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800">
                        <div className="flex items-center justify-between mb-4">
                          <div>
                            <h4 className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
                              <ShieldCheck className="w-4 h-4 text-emerald-400" /> Resume Quality & ATS Verification Gate
                            </h4>
                            <p className="text-xs text-slate-400 mt-1">
                              Deterministic validation checks ensure the generated resume parses flawlessly through applicant tracking systems.
                            </p>
                          </div>
                          <div className="text-right">
                            <div className="text-2xl font-bold font-mono text-emerald-400">
                              {tailoredResume.validationReport?.qualityScore || 100}/100
                            </div>
                            <span className="text-[10px] text-slate-400 uppercase tracking-wider">Quality Score</span>
                          </div>
                        </div>

                        {/* Checks list */}
                        <div className="space-y-2 mt-4">
                          {tailoredResume.validationReport?.passedChecks?.map((check, idx) => (
                            <div key={idx} className="flex items-center gap-2 p-2.5 rounded-xl bg-emerald-950/20 border border-emerald-500/20 text-xs text-emerald-300">
                              <Check className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                              <span>{check}</span>
                            </div>
                          ))}

                          {tailoredResume.validationReport?.warnings?.map((warn, idx) => (
                            <div key={idx} className="flex items-center gap-2 p-2.5 rounded-xl bg-amber-950/20 border border-amber-500/20 text-xs text-amber-300">
                              <AlertTriangle className="w-4 h-4 text-amber-400 flex-shrink-0" />
                              <span>{warn}</span>
                            </div>
                          ))}

                          {tailoredResume.validationReport?.failedChecks?.map((fail, idx) => (
                            <div key={idx} className="flex items-center gap-2 p-2.5 rounded-xl bg-rose-950/20 border border-rose-500/20 text-xs text-rose-300">
                              <AlertOctagon className="w-4 h-4 text-rose-400 flex-shrink-0" />
                              <span>{fail}</span>
                            </div>
                          ))}
                        </div>

                        <div className="mt-4 pt-4 border-t border-slate-800 flex justify-end">
                          <button
                            onClick={handleValidateTailoredResume}
                            disabled={isValidating}
                            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition inline-flex items-center gap-2"
                          >
                            {isValidating ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <RefreshCw className="w-3.5 h-3.5" />}
                            <span>Re-run Quality Audit</span>
                          </button>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer Application Actions */}
        <div className="p-4 border-t border-slate-800 bg-slate-900/95 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            {currentJob.applied ? (
              <div className="flex items-center gap-2">
                <span className="px-3 py-1.5 rounded-xl text-xs font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1.5 shadow-sm shadow-emerald-500/10">
                  <Check className="w-4 h-4 text-emerald-400" /> APPLIED
                </span>
                {currentJob.appliedAt && (
                  <span className="text-[11px] text-slate-400">
                    on {new Date(currentJob.appliedAt).toLocaleDateString()}
                  </span>
                )}
                <button
                  onClick={() => handleToggleApplied(false)}
                  disabled={isUpdatingApplied}
                  className="px-2.5 py-1 rounded-lg text-xs text-slate-400 hover:text-rose-400 hover:bg-slate-800 transition"
                  title="Undo if marked by mistake"
                >
                  {isUpdatingApplied ? 'Updating...' : 'Mark as Not Applied'}
                </button>
              </div>
            ) : (
              <button
                onClick={() => handleToggleApplied(true)}
                disabled={isUpdatingApplied}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-emerald-600/30 hover:border-emerald-500/40 text-slate-200 hover:text-emerald-300 border border-slate-700 text-xs font-semibold transition flex items-center gap-2"
              >
                {isUpdatingApplied ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
                <span>Mark as Applied</span>
              </button>
            )}
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium transition"
            >
              Close
            </button>
            <a
              href={currentJob.jobUrl || currentJob.canonicalUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold transition flex items-center gap-2 shadow-lg shadow-emerald-600/20"
              title="Opens the original ATS career portal in a new tab"
            >
              <span>Open Application Portal</span>
              <ExternalLink className="w-4 h-4" />
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
