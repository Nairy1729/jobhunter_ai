import React, { useState, useEffect } from 'react';
import {
  Job,
  MatchAnalysisResponse,
  TailoredResume,
  TailoringDiff,
} from '../types';
import { apiClient } from '../api/client';
import {
  X,
  ExternalLink,
  Check,
  Sparkles,
  Download,
  CheckCircle2,
  Loader2,
  Cpu,
  AlertCircle,
  Eye,
  FileText,
} from 'lucide-react';

interface JobIntelligenceDetailProps {
  job: Job | null;
  onClose: () => void;
  initialMatch?: MatchAnalysisResponse | null;
  onMatchUpdated?: (jobId: string, match: MatchAnalysisResponse) => void;
  onJobUpdated?: (updatedJob: Job) => void;
}

type SectionKey = 'why' | 'match' | 'edge' | 'constraints' | 'tailor';

export const JobIntelligenceDetail: React.FC<JobIntelligenceDetailProps> = ({
  job,
  onClose,
  initialMatch,
  onMatchUpdated,
  onJobUpdated,
}) => {
  if (!job) return null;

  const [currentJob, setCurrentJob] = useState<Job>(job);
  const [activeSection, setActiveSection] = useState<SectionKey>('why');
  const [matchData, setMatchData] = useState<MatchAnalysisResponse | null>(initialMatch || null);
  const [loadingMatch, setLoadingMatch] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isUpdatingApplied, setIsUpdatingApplied] = useState(false);

  // Resume Tailoring State
  const [tailoredResume, setTailoredResume] = useState<TailoredResume | null>(null);
  const [tailoringDiff, setTailoringDiff] = useState<TailoringDiff | null>(null);
  const [isTailoring, setIsTailoring] = useState(false);
  const [activeStepIndex, setActiveStepIndex] = useState<number>(-1);
  const [viewMode, setViewMode] = useState<'diff' | 'preview'>('diff');
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);
  const [isDownloadingLatex, setIsDownloadingLatex] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setCurrentJob(job);
    if (!matchData && !initialMatch) {
      loadMatchData();
    }
    loadTailoringData();
  }, [job.id]);

  const loadMatchData = async () => {
    setLoadingMatch(true);
    setError(null);
    try {
      const match = await apiClient.matching.getMatch(job.id);
      setMatchData(match);
      if (onMatchUpdated) onMatchUpdated(job.id, match);
    } catch (err: any) {
      console.warn('Match not yet computed for job', err);
    } finally {
      setLoadingMatch(false);
    }
  };

  const handleRunAnalysis = async () => {
    setIsAnalyzing(true);
    setError(null);
    try {
      const match = await apiClient.matching.analyze(job.id);
      setMatchData(match);
      if (onMatchUpdated) onMatchUpdated(job.id, match);
      const updated = await apiClient.jobs.getById(job.id);
      setCurrentJob(updated);
      if (onJobUpdated) onJobUpdated(updated);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Analysis failed to execute');
    } finally {
      setIsAnalyzing(false);
    }
  };

  const PROGRESSION_STAGES = [
    'ANALYZING JOB',
    'UNDERSTANDING YOUR PROFILE',
    'TAILORING YOUR MASTER RESUME',
    'VERIFYING CONTENT',
    'GENERATING RESUME',
    'READY',
  ] as const;

  const loadTailoringData = async () => {
    try {
      const list = await apiClient.tailoring.getTailoredResumesForJob(job.id);
      if (list && list.length > 0) {
        const latest = list[0];
        setTailoredResume(latest);
        setActiveStepIndex(5);
        try {
          const diff = await apiClient.tailoring.getDiff(latest.id);
          setTailoringDiff(diff);
        } catch (dErr) {
          console.warn('Could not load diff', dErr);
        }
      } else {
        setTailoredResume(null);
        setTailoringDiff(null);
        setActiveStepIndex(-1);
      }
    } catch (err) {
      console.warn('No tailored resumes for job', err);
    }
  };

  const handleGenerateTailoredResume = async () => {
    setIsTailoring(true);
    setActiveStepIndex(0);
    setError(null);

    const t1 = setTimeout(() => {
      setActiveStepIndex(1);
    }, 1200);

    const t2 = setTimeout(() => {
      setActiveStepIndex(2);
    }, 2800);

    const t3 = setTimeout(() => {
      setActiveStepIndex(3);
    }, 4500);

    const t4 = setTimeout(() => {
      setActiveStepIndex(4);
    }, 6200);

    try {
      const tailored = await apiClient.tailoring.tailorResume(job.id);
      clearTimeout(t1);
      clearTimeout(t2);
      clearTimeout(t3);
      clearTimeout(t4);
      setActiveStepIndex(5);
      setTailoredResume(tailored);
      const diff = await apiClient.tailoring.getDiff(tailored.id);
      setTailoringDiff(diff);
    } catch (err: any) {
      clearTimeout(t1);
      clearTimeout(t2);
      clearTimeout(t3);
      clearTimeout(t4);
      setActiveStepIndex(-1);
      setError(err?.response?.data?.message || 'Resume tailoring generation failed');
    } finally {
      setIsTailoring(false);
    }
  };

  const handleDownloadPdf = async () => {
    if (!tailoredResume) return;
    setIsDownloadingPdf(true);
    setError(null);
    try {
      await apiClient.tailoring.downloadPdf(tailoredResume.id);
    } catch (err: any) {
      console.error('Failed to download PDF', err);
      setError('Failed to download tailored PDF. Please verify backend connection.');
    } finally {
      setIsDownloadingPdf(false);
    }
  };

  const handleDownloadLatex = async () => {
    if (!tailoredResume) return;
    setIsDownloadingLatex(true);
    setError(null);
    try {
      await apiClient.tailoring.downloadLatex(tailoredResume.id);
    } catch (err: any) {
      console.error('Failed to download LaTeX', err);
      setError('Failed to download tailored LaTeX (.tex) file. Please verify backend connection.');
    } finally {
      setIsDownloadingLatex(false);
    }
  };

  const handleToggleApplied = async (newAppliedState: boolean) => {
    setIsUpdatingApplied(true);
    try {
      const res = await apiClient.jobs.setApplied(job.id, newAppliedState);
      const updated = { ...currentJob, applied: res.applied, appliedAt: res.appliedAt };
      setCurrentJob(updated);
      if (onJobUpdated) onJobUpdated(updated);
    } catch (err: any) {
      console.error('Failed to toggle applied state', err);
    } finally {
      setIsUpdatingApplied(false);
    }
  };

  const getMatchScorePill = () => {
    const score = currentJob.priorityScore ?? matchData?.priorityScore;
    const cat = currentJob.priorityCategory;

    if (cat === 'HIGH_PRIORITY' || matchData?.recommendation === 'APPLY') {
      return (
        <span className="font-mono text-xs text-signal-emerald border border-signal-emerald/30 bg-signal-emerald/10 px-3 py-1 rounded-md">
          {score ? `${score}% · HIGH PRIORITY` : 'HIGH PRIORITY FIT'}
        </span>
      );
    }
    if (cat === 'MEDIUM_PRIORITY' || matchData?.recommendation === 'APPLY_AFTER_TAILORING') {
      return (
        <span className="font-mono text-xs text-signal-cyan border border-signal-cyan/30 bg-signal-cyan/10 px-3 py-1 rounded-md">
          {score ? `${score}% · MEDIUM PRIORITY` : 'MEDIUM PRIORITY STRETCH'}
        </span>
      );
    }
    if (cat === 'LOW_PRIORITY' || matchData?.recommendation === 'LOW_PRIORITY') {
      return (
        <span className="font-mono text-xs text-signal-amber border border-signal-amber/30 bg-signal-amber/10 px-3 py-1 rounded-md">
          {score ? `${score}% · LOW PRIORITY` : 'LOW PRIORITY'}
        </span>
      );
    }
    return (
      <span className="font-mono text-xs text-ink-muted border border-white/10 bg-white/[0.03] px-3 py-1 rounded-md">
        {score ? `${score}%` : 'CONSTRAINT GAP'} · NOT RECOMMENDED
      </span>
    );
  };

  // Sections navigation tabs
  const sections: { key: SectionKey; label: string; count?: number }[] = [
    { key: 'why', label: 'WHY THIS JOB' },
    { key: 'match', label: 'MATCH READOUT', count: matchData?.requirementAnalysis?.length },
    { key: 'edge', label: 'THE EDGE' },
    { key: 'constraints', label: 'CONSTRAINTS', count: currentJob.potentialConcerns?.length },
    { key: 'tailor', label: 'RESUME TAILORING' },
  ];

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-dark-950/85 backdrop-blur-2xl flex justify-center p-2 sm:p-6 lg:p-10 animate-in fade-in duration-200">
      <div className="relative w-full max-w-5xl bg-dark-900 border border-white/10 rounded-2xl shadow-2xl flex flex-col my-auto overflow-hidden">
        {/* Top Header Bar */}
        <div className="p-6 sm:p-8 border-b border-white/[0.06] bg-dark-950/50 flex flex-col md:flex-row md:items-start justify-between gap-6">
          <div className="flex-1">
            <div className="flex items-center gap-3 mb-2">
              <span className="font-mono text-[11px] text-ink-muted uppercase tracking-widest">
                SPECIMEN // {currentJob.companyName}
              </span>
              <span className="text-white/20">/</span>
              <span className="font-mono text-[10px] text-ink-muted uppercase">
                {currentJob.workMode} · {currentJob.location}
              </span>
            </div>

            <h1 className="text-2xl sm:text-4xl font-light tracking-tight text-ink-primary mb-3">
              {currentJob.title}
            </h1>

            <div className="flex flex-wrap items-center gap-3">
              {getMatchScorePill()}
              {currentJob.sourceName && (
                <span className="font-mono text-[10px] text-ink-muted border border-white/[0.06] px-2 py-0.5 rounded">
                  SOURCE: {currentJob.sourceName.replace('FIRECRAWL_', '')}
                </span>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2 self-start">
            <button
              onClick={handleRunAnalysis}
              disabled={isAnalyzing}
              className="px-3.5 py-2 rounded-lg bg-white/[0.04] hover:bg-white/[0.08] border border-white/10 font-mono text-xs text-ink-secondary hover:text-ink-primary transition flex items-center gap-2"
              title="Re-run Grounding & Semantic Engine"
            >
              {isAnalyzing ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin text-signal-emerald" />
              ) : (
                <Cpu className="w-3.5 h-3.5 text-signal-emerald" />
              )}
              <span>RE-ANALYZE</span>
            </button>

            <button
              onClick={onClose}
              className="p-2 text-ink-muted hover:text-ink-primary hover:bg-white/5 rounded-lg transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Section Navigation Hairline Tabs */}
        <div className="px-6 sm:px-8 border-b border-white/[0.06] bg-dark-950/30 flex items-center overflow-x-auto space-x-6">
          {sections.map((s) => (
            <button
              key={s.key}
              onClick={() => setActiveSection(s.key)}
              className={`py-3.5 font-mono text-xs tracking-wider uppercase border-b-2 transition flex items-center gap-2 whitespace-nowrap ${
                activeSection === s.key
                  ? 'border-signal-emerald text-ink-primary font-semibold'
                  : 'border-transparent text-ink-muted hover:text-ink-secondary'
              }`}
            >
              <span>{s.label}</span>
              {s.count !== undefined && s.count > 0 && (
                <span className="text-[10px] text-ink-muted font-mono">[{s.count}]</span>
              )}
            </button>
          ))}
        </div>
        {error && (
          <div className="mx-6 sm:mx-8 mt-4 p-3 rounded-xl bg-signal-rose/10 border border-signal-rose/20 text-signal-rose text-xs font-mono flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Content Body Area */}
        <div className="p-6 sm:p-8 space-y-8 flex-1 overflow-y-auto max-h-[65vh]">
          {/* SECTION 1: WHY THIS JOB */}
          {activeSection === 'why' && (
            <div className="space-y-6">
              <div>
                <div className="font-mono text-[10px] text-signal-emerald uppercase tracking-widest mb-1">
                  EVALUATION STATEMENT
                </div>
                <h2 className="text-xl sm:text-2xl font-light text-ink-primary tracking-tight">
                  Grounding & Relevancy Alignment
                </h2>
              </div>

              {/* Verified Evidence Points */}
              <div className="space-y-3">
                {currentJob.whyThisJob && currentJob.whyThisJob.length > 0 ? (
                  currentJob.whyThisJob.map((statement, idx) => (
                    <div
                      key={idx}
                      className="p-4 rounded-xl bg-dark-950/60 border border-white/[0.06] flex items-start gap-3.5"
                    >
                      <CheckCircle2 className="w-4 h-4 text-signal-emerald flex-shrink-0 mt-0.5" />
                      <p className="text-sm text-ink-secondary font-light leading-relaxed">
                        {statement}
                      </p>
                    </div>
                  ))
                ) : (
                  <p className="text-sm text-ink-muted font-light">
                    No explicit verified statements synthesized. Click "Re-Analyze" to evaluate.
                  </p>
                )}
              </div>

              {/* Technologies Detected */}
              {currentJob.detectedTechnologies && currentJob.detectedTechnologies.length > 0 && (
                <div className="pt-4 border-t border-white/[0.06]">
                  <span className="font-mono text-[10px] text-ink-muted uppercase tracking-widest block mb-2">
                    RECOGNIZED JOB SIGNALS
                  </span>
                  <div className="flex flex-wrap gap-2">
                    {currentJob.detectedTechnologies.map((t) => (
                      <span
                        key={t}
                        className="font-mono text-xs px-2.5 py-1 rounded bg-dark-950 border border-white/[0.08] text-ink-primary"
                      >
                        {t}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* SECTION 2: MATCH VISUALIZATION READOUT */}
          {activeSection === 'match' && (
            <div className="space-y-6">
              <div>
                <div className="font-mono text-[10px] text-signal-emerald uppercase tracking-widest mb-1">
                  SYSTEM READOUT
                </div>
                <h2 className="text-xl sm:text-2xl font-light text-ink-primary tracking-tight">
                  Requirement Coverage Gauge
                </h2>
              </div>

              {loadingMatch ? (
                <div className="py-12 text-center text-ink-muted font-mono text-xs flex items-center justify-center gap-2">
                  <Loader2 className="w-4 h-4 animate-spin text-signal-emerald" />
                  <span>Loading requirement analysis...</span>
                </div>
              ) : matchData?.requirementAnalysis && matchData.requirementAnalysis.length > 0 ? (
                <div className="space-y-4">
                  {matchData.requirementAnalysis.map((item, idx) => {
                    const isStrong = item.matchType === 'STRONG';
                    const isPartial = item.matchType === 'PARTIAL' || item.matchType === 'TRANSFERABLE';

                    return (
                      <div
                        key={idx}
                        className="p-4 rounded-xl bg-dark-950/60 border border-white/[0.06] space-y-2"
                      >
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                          <span className="font-mono text-xs text-ink-primary uppercase tracking-wider font-semibold">
                            {item.requirement}
                          </span>
                          <span
                            className={`font-mono text-[10px] tracking-widest px-2 py-0.5 rounded border uppercase w-fit ${
                              isStrong
                                ? 'text-signal-emerald border-signal-emerald/30 bg-signal-emerald/10'
                                : isPartial
                                ? 'text-signal-cyan border-signal-cyan/30 bg-signal-cyan/10'
                                : 'text-signal-rose border-signal-rose/30 bg-signal-rose/10'
                            }`}
                          >
                            {item.matchType}
                          </span>
                        </div>

                        {/* ASCII / Gauge Bar */}
                        <div className="w-full bg-dark-900 h-1.5 rounded-full overflow-hidden border border-white/[0.04]">
                          <div
                            className={`h-full rounded-full transition-all duration-500 ${
                              isStrong ? 'w-full bg-signal-emerald' : isPartial ? 'w-1/2 bg-signal-cyan' : 'w-1/12 bg-signal-rose'
                            }`}
                          />
                        </div>

                        <p className="text-xs text-ink-secondary font-light leading-relaxed">
                          {item.explanation || item.candidateEvidence}
                        </p>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="p-8 rounded-xl bg-dark-950/60 border border-white/[0.06] text-center">
                  <p className="text-xs font-mono text-ink-muted mb-4">
                    Requirements have not been structured yet.
                  </p>
                  <button
                    onClick={handleRunAnalysis}
                    disabled={isAnalyzing}
                    className="px-4 py-2 rounded-lg bg-signal-emerald text-dark-950 font-mono text-xs font-bold"
                  >
                    RUN GROUNDING ANALYSIS
                  </button>
                </div>
              )}
            </div>
          )}

          {/* SECTION 3: THE EDGE */}
          {activeSection === 'edge' && (
            <div className="space-y-6">
              <div>
                <div className="font-mono text-[10px] text-signal-emerald uppercase tracking-widest mb-1">
                  STRATEGIC ADVANTAGE
                </div>
                <h2 className="text-xl sm:text-2xl font-light text-ink-primary tracking-tight">
                  The Edge
                </h2>
              </div>

              {matchData?.advantageReport ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* What You Already Have */}
                  <div className="p-5 rounded-xl bg-dark-950/60 border border-white/[0.06] space-y-3">
                    <span className="font-mono text-[10px] text-signal-emerald uppercase tracking-widest block">
                      WHAT YOU ALREADY HAVE
                    </span>
                    <ul className="space-y-2 text-xs text-ink-secondary font-light">
                      {(matchData.advantageReport.strongestEvidence || []).map((item, i) => (
                        <li key={i} className="flex items-start gap-2">
                          <span className="text-signal-emerald font-mono">0{i + 1}.</span>
                          <span>{item}</span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  {/* What Matters Most */}
                  <div className="p-5 rounded-xl bg-dark-950/60 border border-white/[0.06] space-y-3">
                    <span className="font-mono text-[10px] text-signal-cyan uppercase tracking-widest block">
                      WHAT MATTERS MOST
                    </span>
                    <ul className="space-y-2 text-xs text-ink-secondary font-light">
                      {(matchData.advantageReport.employerPriorities || []).map((item, i) => (
                        <li key={i} className="flex items-start gap-2">
                          <span className="text-signal-cyan font-mono">0{i + 1}.</span>
                          <span>{item}</span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  {/* What to Emphasize */}
                  <div className="p-5 rounded-xl bg-dark-950/60 border border-white/[0.06] space-y-3">
                    <span className="font-mono text-[10px] text-signal-emerald uppercase tracking-widest block">
                      WHAT TO EMPHASIZE
                    </span>
                    <ul className="space-y-2 text-xs text-ink-secondary font-light">
                      {(matchData.advantageReport.whatToEmphasize || []).map((item, i) => (
                        <li key={i} className="flex items-start gap-2">
                          <span className="text-signal-emerald font-mono">✓</span>
                          <span>{item}</span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  {/* What Not to Claim (Grounding Invariant) */}
                  <div className="p-5 rounded-xl bg-dark-950/60 border border-white/[0.06] space-y-3">
                    <span className="font-mono text-[10px] text-signal-amber uppercase tracking-widest block">
                      WHAT NOT TO CLAIM
                    </span>
                    <ul className="space-y-2 text-xs text-ink-secondary font-light">
                      {(matchData.advantageReport.honestGaps || []).map((item, i) => (
                        <li key={i} className="flex items-start gap-2">
                          <span className="text-signal-amber font-mono">✗</span>
                          <span>{item}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>
              ) : (
                <div className="p-8 rounded-xl bg-dark-950/60 border border-white/[0.06] text-center">
                  <p className="text-xs font-mono text-ink-muted mb-4">
                    The Edge has not yet been derived for this job.
                  </p>
                  <button
                    onClick={handleRunAnalysis}
                    disabled={isAnalyzing}
                    className="px-4 py-2 rounded-lg bg-signal-emerald text-dark-950 font-mono text-xs font-bold"
                  >
                    SYNTHESIZE THE EDGE
                  </button>
                </div>
              )}
            </div>
          )}

          {/* SECTION 4: DIAGNOSTIC CONSTRAINTS */}
          {activeSection === 'constraints' && (
            <div className="space-y-6">
              <div>
                <div className="font-mono text-[10px] text-signal-rose uppercase tracking-widest mb-1">
                  DIAGNOSTIC REPORT
                </div>
                <h2 className="text-xl sm:text-2xl font-light text-ink-primary tracking-tight">
                  Constraints & Potential Risks
                </h2>
              </div>

              <div className="space-y-3">
                {currentJob.potentialConcerns && currentJob.potentialConcerns.length > 0 ? (
                  currentJob.potentialConcerns.map((concern, idx) => (
                    <div
                      key={idx}
                      className="p-4 rounded-xl bg-dark-950/60 border border-white/[0.06] flex items-start gap-3.5"
                    >
                      <span className="font-mono text-xs text-signal-rose">
                        {String(idx + 1).padStart(2, '0')}
                      </span>
                      <p className="text-xs sm:text-sm text-ink-secondary font-light leading-relaxed">
                        {concern}
                      </p>
                    </div>
                  ))
                ) : (
                  <p className="text-xs text-ink-muted font-mono">
                    Zero hard constraint violations or fatal gaps identified for this role.
                  </p>
                )}
              </div>
            </div>
          )}

          {/* SECTION 5: RESUME TAILORING WORKSTATION */}
          {activeSection === 'tailor' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <div className="font-mono text-[10px] text-signal-emerald uppercase tracking-widest mb-1">
                    GROUNDED TAILORING WORKSTATION
                  </div>
                  <h2 className="text-xl sm:text-2xl font-light text-ink-primary tracking-tight">
                    Targeted Application Package
                  </h2>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  {tailoredResume && (
                    <button
                      onClick={() => setViewMode(viewMode === 'diff' ? 'preview' : 'diff')}
                      title="Toggle between Diff View and Tailored Resume Preview"
                      className="px-3.5 py-2 rounded-lg border border-white/10 bg-white/[0.04] hover:bg-white/[0.08] text-ink-primary font-mono text-xs flex items-center gap-1.5 transition"
                    >
                      {viewMode === 'diff' ? (
                        <>
                          <Eye className="w-3.5 h-3.5 text-signal-cyan" />
                          <span>VIEW RESUME</span>
                        </>
                      ) : (
                        <>
                          <Cpu className="w-3.5 h-3.5 text-signal-cyan" />
                          <span>VIEW DIFF</span>
                        </>
                      )}
                    </button>
                  )}

                  {tailoredResume && tailoredResume.pdfAvailable && (
                    <button
                      onClick={handleDownloadPdf}
                      disabled={isDownloadingPdf}
                      title="Download ATS-compliant tailored PDF resume"
                      className="px-3.5 py-2 rounded-lg border border-signal-emerald/40 bg-signal-emerald/15 hover:bg-signal-emerald/25 text-signal-emerald font-mono text-xs font-semibold flex items-center gap-1.5 transition disabled:opacity-50"
                    >
                      {isDownloadingPdf ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin text-signal-emerald" />
                      ) : (
                        <Download className="w-3.5 h-3.5 text-signal-emerald" />
                      )}
                      <span>{isDownloadingPdf ? 'DOWNLOADING...' : 'DOWNLOAD PDF'}</span>
                    </button>
                  )}

                  {tailoredResume && (
                    <button
                      onClick={handleDownloadLatex}
                      disabled={isDownloadingLatex}
                      title="Download clean ATS-optimized LaTeX source file (.tex)"
                      className="px-3.5 py-2 rounded-lg border border-signal-cyan/30 bg-signal-cyan/10 hover:bg-signal-cyan/20 text-signal-cyan font-mono text-xs flex items-center gap-1.5 transition disabled:opacity-50"
                    >
                      {isDownloadingLatex ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin text-signal-cyan" />
                      ) : (
                        <Download className="w-3.5 h-3.5 text-signal-cyan" />
                      )}
                      <span>{isDownloadingLatex ? 'DOWNLOADING...' : 'DOWNLOAD LATEX (.TEX)'}</span>
                    </button>
                  )}

                  <button
                    onClick={handleGenerateTailoredResume}
                    disabled={isTailoring}
                    className="px-4 py-2 rounded-lg bg-signal-emerald text-dark-950 font-mono text-xs font-bold tracking-wide transition flex items-center gap-2 hover:bg-signal-emerald/90 disabled:opacity-50"
                  >
                    {isTailoring ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : (
                      <Sparkles className="w-3.5 h-3.5" />
                    )}
                    <span>{tailoredResume ? 'RE-GENERATE' : 'GENERATE TAILORED RESUME'}</span>
                  </button>
                </div>
              </div>

              {/* 6-Stage Pipeline Progression Stepper */}
              {(isTailoring || activeStepIndex >= 0 || tailoredResume) && (
                <div className="p-4 rounded-xl bg-dark-950/70 border border-white/[0.08] space-y-3">
                  <div className="flex items-center justify-between font-mono text-[10px] uppercase tracking-wider">
                    <span className="text-ink-secondary">AI TAILORING PIPELINE</span>
                    <span className={activeStepIndex === 5 ? 'text-signal-emerald font-bold' : 'text-signal-cyan animate-pulse'}>
                      {activeStepIndex >= 0 ? PROGRESSION_STAGES[activeStepIndex] : 'INITIALIZING'}
                    </span>
                  </div>
                  <div className="grid grid-cols-2 sm:grid-cols-6 gap-2">
                    {PROGRESSION_STAGES.map((step, idx) => {
                      const isPast = idx < activeStepIndex;
                      const isCurrent = idx === activeStepIndex;
                      return (
                        <div
                          key={step}
                          className={`p-2 rounded-lg border font-mono text-[9px] flex items-center gap-1.5 transition-all ${
                            isCurrent
                              ? 'bg-signal-cyan/15 border-signal-cyan text-signal-cyan font-bold shadow-sm shadow-signal-cyan/20'
                              : isPast
                              ? 'bg-signal-emerald/10 border-signal-emerald/30 text-signal-emerald'
                              : 'bg-dark-900/40 border-white/[0.04] text-ink-muted opacity-50'
                          }`}
                        >
                          {isPast ? (
                            <CheckCircle2 className="w-3 h-3 text-signal-emerald flex-shrink-0" />
                          ) : isCurrent ? (
                            <Loader2 className="w-3 h-3 animate-spin text-signal-cyan flex-shrink-0" />
                          ) : (
                            <span className="w-2.5 h-2.5 rounded-full border border-white/20 flex-shrink-0" />
                          )}
                          <span className="truncate">{step}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {tailoredResume && tailoringDiff ? (
                <div className="space-y-6">
                  {/* Summary Bar */}
                  <div className="p-4 rounded-xl bg-dark-950/60 border border-white/[0.06] flex flex-wrap items-center justify-between gap-3 font-mono text-xs">
                    <span className="text-ink-secondary">
                      STATUS: <span className={tailoredResume.status === 'READY_FOR_DOWNLOAD' ? 'text-signal-emerald font-bold' : tailoredResume.status === 'VALIDATION_FAILED' ? 'text-signal-rose font-bold' : 'text-signal-cyan'}>{tailoredResume.status}</span>
                    </span>
                    <span className="text-ink-secondary">
                      ATS SCORE ESTIMATE: <span className="text-signal-emerald font-semibold">{tailoredResume.atsScoreEstimate}%</span>
                    </span>
                    <div className="flex items-center gap-3">
                      <span className="text-ink-muted">VERSION 0{tailoredResume.versionNumber}</span>
                      <div className="flex items-center gap-1 bg-dark-900/80 p-0.5 rounded-lg border border-white/10">
                        <button
                          onClick={() => setViewMode('diff')}
                          className={`px-2.5 py-1 rounded text-[10px] font-mono transition ${
                            viewMode === 'diff' ? 'bg-signal-cyan/20 text-signal-cyan font-bold' : 'text-ink-muted hover:text-ink-primary'
                          }`}
                        >
                          DIFF VIEW
                        </button>
                        <button
                          onClick={() => setViewMode('preview')}
                          className={`px-2.5 py-1 rounded text-[10px] font-mono transition ${
                            viewMode === 'preview' ? 'bg-signal-emerald/20 text-signal-emerald font-bold' : 'text-ink-muted hover:text-ink-primary'
                          }`}
                        >
                          RESUME PREVIEW
                        </button>
                      </div>
                    </div>
                  </div>

                  {/* Validation Failure Warning Banner */}
                  {tailoredResume.status === 'VALIDATION_FAILED' && (
                    <div className="p-5 rounded-xl bg-signal-rose/10 border border-signal-rose/30 space-y-3 animate-in fade-in duration-300">
                      <div className="flex items-center gap-2 text-signal-rose">
                        <AlertCircle className="w-5 h-5 flex-shrink-0" />
                        <h4 className="font-mono text-xs uppercase tracking-wider font-bold">
                          Resume generation was blocked because unsupported candidate information was detected.
                        </h4>
                      </div>
                      <p className="text-xs text-ink-secondary font-light">
                        JobHunter AI strictly enforces the Grounding Verification Gate. Resumes containing claims without verified evidence in your profile cannot be exported.
                      </p>
                      {tailoredResume.validationReport?.failedChecks && tailoredResume.validationReport.failedChecks.length > 0 && (
                        <div className="space-y-2 pt-2 border-t border-signal-rose/20">
                          <span className="font-mono text-[10px] text-signal-rose uppercase tracking-widest block">
                            FAILED GROUNDING CLAIMS:
                          </span>
                          <ul className="space-y-1.5">
                            {tailoredResume.validationReport.failedChecks.map((fail, fIdx) => (
                              <li key={fIdx} className="font-mono text-xs text-signal-rose flex items-start gap-2 bg-dark-950/60 p-2.5 rounded-lg border border-signal-rose/20">
                                <span>✗</span>
                                <span>{fail}</span>
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  )}

                  {/* VIEW MODE 1: RESUME PREVIEW */}
                  {viewMode === 'preview' ? (
                    <div className="p-6 sm:p-8 rounded-2xl bg-dark-950/90 border border-white/10 space-y-6 shadow-2xl">
                      <div className="flex items-center justify-between border-b border-white/[0.08] pb-4">
                        <div className="flex items-center gap-2">
                          <FileText className="w-4 h-4 text-signal-emerald" />
                          <span className="font-mono text-xs text-signal-emerald uppercase tracking-wider font-bold">
                            ATS-COMPLIANT TAILORED RESUME DOCUMENT
                          </span>
                        </div>
                        <span className="font-mono text-[10px] text-ink-muted">
                          A4 · TIMES NEW ROMAN · 0.5IN MARGINS
                        </span>
                      </div>

                      <div className="prose prose-invert max-w-none text-ink-primary font-mono text-xs whitespace-pre-wrap leading-relaxed bg-dark-900/60 p-6 rounded-xl border border-white/[0.04]">
                        {tailoredResume.tailoredMarkdown || tailoringDiff.tailoredResumeContent}
                      </div>
                    </div>
                  ) : (
                    /* VIEW MODE 2: DIFF VIEW */
                    <div className="space-y-6">
                      {/* Skills to Emphasize */}
                      {tailoringDiff.addedEmphasis && tailoringDiff.addedEmphasis.length > 0 && (
                        <div className="p-4 rounded-xl bg-dark-950/70 border border-white/[0.06] space-y-2">
                          <span className="font-mono text-[10px] text-signal-emerald uppercase tracking-widest block">
                            PROMOTED COMPETENCIES & KEY STACK MATCHES
                          </span>
                          <div className="flex flex-wrap gap-2">
                            {tailoringDiff.addedEmphasis.map((em, eIdx) => (
                              <span
                                key={eIdx}
                                className="font-mono text-xs px-2.5 py-1 rounded bg-signal-emerald/10 border border-signal-emerald/30 text-signal-emerald"
                              >
                                {em}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* Bullet Modifications: Master vs Tailored */}
                      <div className="space-y-4">
                        <span className="font-mono text-[10px] text-ink-muted uppercase tracking-widest block">
                          GROUNDED BULLET SHARPENING (MASTER EVIDENCE ➔ TAILORED REPHRASING)
                        </span>

                        {tailoringDiff.modifiedBullets && tailoringDiff.modifiedBullets.length > 0 ? (
                          tailoringDiff.modifiedBullets.map((mod, idx) => (
                            <div
                              key={idx}
                              className="p-5 rounded-xl bg-dark-950/70 border border-white/[0.06] space-y-3"
                            >
                              <div className="space-y-1">
                                <span className="font-mono text-[9px] text-ink-muted uppercase tracking-widest block">
                                  MASTER PROFILE BULLET:
                                </span>
                                <p className="text-xs text-ink-muted font-mono line-through opacity-80">
                                  {mod.originalBullet}
                                </p>
                              </div>

                              <div className="space-y-1">
                                <span className="font-mono text-[9px] text-signal-emerald uppercase tracking-widest block">
                                  TAILORED RE-PHRASING:
                                </span>
                                <p className="text-xs text-ink-primary font-mono font-medium">
                                  {mod.proposedBullet}
                                </p>
                              </div>

                              <div className="pt-2 border-t border-white/[0.04] text-[11px] font-sans text-ink-secondary font-light">
                                <span className="font-mono text-[9px] text-ink-muted uppercase mr-1">RATIONALE:</span>
                                {mod.reason}
                              </div>
                            </div>
                          ))
                        ) : (
                          <p className="text-xs font-mono text-ink-muted">
                            No specific bullet modifications required; master resume fits target role specs.
                          </p>
                        )}
                      </div>

                      {/* Rejected Keywords Safeguard */}
                      {tailoringDiff.rejectedKeywords && tailoringDiff.rejectedKeywords.length > 0 && (
                        <div className="p-4 rounded-xl bg-dark-950/60 border border-white/[0.06] space-y-2">
                          <span className="font-mono text-[10px] text-signal-amber uppercase tracking-widest block">
                            EXCLUDED KEYWORDS (ANTI-HALLUCINATION GUARDRAIL)
                          </span>
                          <p className="text-xs text-ink-secondary font-light">
                            These job requirements were identified in the JD but excluded from resume claims because they are not present in verified candidate profile evidence:
                          </p>
                          <div className="flex flex-wrap gap-2 pt-1">
                            {tailoringDiff.rejectedKeywords.map((rej, rIdx) => (
                              <span
                                key={rIdx}
                                title={rej.reason}
                                className="font-mono text-xs px-2.5 py-1 rounded bg-signal-amber/10 border border-signal-amber/30 text-signal-amber"
                              >
                                {rej.keyword}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ) : (
                <div className="p-8 rounded-xl bg-dark-950/60 border border-white/[0.06] text-center">
                  <p className="text-xs font-mono text-ink-muted mb-4">
                    Click generate to assemble an ATS-compliant tailored resume grounded strictly in your verified profile.
                  </p>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Bottom Application Action Footprint */}
        <div className="p-6 sm:p-8 border-t border-white/[0.06] bg-dark-950/80 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="font-mono text-xs text-ink-muted">
            READY TO APPLY? ORIGINAL CAREER PORTAL PRESERVED.
          </div>

          <div className="flex items-center gap-3 w-full sm:w-auto">
            {/* Direct Open Job Application */}
            <a
              href={currentJob.jobUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex-1 sm:flex-none px-5 py-2.5 rounded-xl bg-white/[0.06] hover:bg-white/[0.1] border border-white/10 font-mono text-xs text-ink-primary tracking-wider transition flex items-center justify-center gap-2"
            >
              <span>OPEN APPLICATION</span>
              <ExternalLink className="w-3.5 h-3.5 text-signal-emerald" />
            </a>

            {/* Invariant Applied State Toggle */}
            {currentJob.applied ? (
              <button
                onClick={() => handleToggleApplied(false)}
                disabled={isUpdatingApplied}
                className="flex-1 sm:flex-none px-5 py-2.5 rounded-xl border border-signal-emerald/40 bg-signal-emerald/15 font-mono text-xs text-signal-emerald font-semibold tracking-wider transition flex items-center justify-center gap-2 hover:bg-signal-emerald/25"
                title="Click to revert to unapplied"
              >
                <Check className="w-4 h-4" />
                <span>APPLIED</span>
              </button>
            ) : (
              <button
                onClick={() => handleToggleApplied(true)}
                disabled={isUpdatingApplied}
                className="flex-1 sm:flex-none px-5 py-2.5 rounded-xl bg-signal-emerald hover:bg-signal-emerald/90 text-dark-950 font-mono text-xs font-bold tracking-wider transition flex items-center justify-center gap-2 shadow-lg shadow-signal-emerald/20"
              >
                {isUpdatingApplied ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                ) : (
                  <Check className="w-3.5 h-3.5" />
                )}
                <span>MARK AS APPLIED</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
