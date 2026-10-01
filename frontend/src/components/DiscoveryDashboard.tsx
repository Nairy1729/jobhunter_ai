import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/client';
import { Job, DiscoverySummary, MatchAnalysisResponse, ProfileReadinessReport } from '../types';
import { IntelligenceFieldScene } from './IntelligenceFieldScene';
import { AgentJourneyPhases } from './AgentJourneyPhases';
import { MinimalSearchInterface } from './MinimalSearchInterface';
import { EditorialJobSpecimen } from './EditorialJobSpecimen';
import { JobIntelligenceDetail } from './JobIntelligenceDetail';
import { CheckCircle2, Loader2, AlertCircle, ArrowRight } from 'lucide-react';

interface DiscoveryDashboardProps {
  onNavigateToProfile?: () => void;
}

export const DiscoveryDashboard: React.FC<DiscoveryDashboardProps> = ({ onNavigateToProfile }) => {
  const [readiness, setReadiness] = useState<ProfileReadinessReport | null>(null);
  const [checkingReadiness, setCheckingReadiness] = useState(true);

  const [jobs, setJobs] = useState<Job[]>([]);
  const [matchMap, setMatchMap] = useState<Record<string, MatchAnalysisResponse>>({});
  const [loading, setLoading] = useState(false);
  const [isDiscovering, setIsDiscovering] = useState(false);
  const [discoveryStep, setDiscoveryStep] = useState(0);
  const [discoverySummary, setDiscoverySummary] = useState<DiscoverySummary | null>(null);
  const [selectedJob, setSelectedJob] = useState<Job | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Milestone 5: Priority, Freshness & Sorting states
  const [priorityFilter, setPriorityFilter] = useState<string>('ALL');
  const [freshnessFilter, setFreshnessFilter] = useState<string>('ALL');
  const [sortBy, setSortBy] = useState<string>('RECOMMENDED');

  // Filters state
  const [role, setRole] = useState('');
  const [location, setLocation] = useState('');
  const [workMode, setWorkMode] = useState('');
  const [technology, setTechnology] = useState('');
  const [source, setSource] = useState('');
  const [page, setPage] = useState(0);
  const [totalJobs, setTotalJobs] = useState(0);

  const fetchReadiness = async () => {
    setCheckingReadiness(true);
    try {
      const rep = await apiClient.profile.getReadiness();
      setReadiness(rep);
      if (rep.canDiscover) {
        fetchJobs();
      }
    } catch (e: any) {
      console.error('Failed to check profile readiness', e);
    } finally {
      setCheckingReadiness(false);
    }
  };

  useEffect(() => {
    fetchReadiness();
  }, []);

  const fetchMatchesForJobs = async (jobsList: Job[]) => {
    const updates: Record<string, MatchAnalysisResponse> = {};
    await Promise.all(
      jobsList.map(async (j) => {
        try {
          const match = await apiClient.matching.getMatch(j.id);
          updates[j.id] = match;
        } catch (e) {
          // ignore unanalyzed
        }
      })
    );
    setMatchMap((prev) => ({ ...prev, ...updates }));
  };

  const fetchJobs = async () => {
    setLoading(true);
    try {
      const res = await apiClient.jobs.list({
        role: role || undefined,
        location: location || undefined,
        workMode: workMode || undefined,
        technology: technology || undefined,
        source: source || undefined,
        priority: priorityFilter !== 'ALL' && priorityFilter !== 'APPLIED' ? priorityFilter : undefined,
        freshness: freshnessFilter !== 'ALL' ? freshnessFilter : undefined,
        sortBy,
        page,
        size: 15,
      });
      setJobs(res.content);
      setTotalJobs(res.totalElements);
      fetchMatchesForJobs(res.content);
    } catch (err: any) {
      console.error('Failed to load jobs', err);
      if (err?.response?.status === 428 || err?.message?.includes('PROFILE_REQUIRED')) {
        fetchReadiness();
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (readiness?.canDiscover) {
      fetchJobs();
    }
  }, [role, location, workMode, technology, source, priorityFilter, freshnessFilter, sortBy, page]);

  const handleDiscover = async () => {
    setIsDiscovering(true);
    setError(null);
    setDiscoverySummary(null);
    setDiscoveryStep(1);

    const stepInterval = setInterval(() => {
      setDiscoveryStep((prev) => (prev < 5 ? prev + 1 : prev));
    }, 1200);

    try {
      const summary = await apiClient.jobs.discover({
        maxQueries: 4,
        searchLimitPerQuery: 10,
      });
      clearInterval(stepInterval);
      setDiscoveryStep(6);
      setDiscoverySummary(summary);
      await fetchJobs();
    } catch (err: any) {
      clearInterval(stepInterval);
      const msg = err?.response?.data?.message || err?.message || 'Discovery run failed';
      if (err?.response?.status === 428 || msg.includes('PROFILE_REQUIRED')) {
        fetchReadiness();
      }
      setError(msg);
    } finally {
      setIsDiscovering(false);
    }
  };

  const handleResetFilters = () => {
    setRole('');
    setLocation('');
    setWorkMode('');
    setTechnology('');
    setSource('');
    setPriorityFilter('ALL');
    setFreshnessFilter('ALL');
    setSortBy('RECOMMENDED');
    setPage(0);
  };

  const handleToggleApplied = async (job: Job, applied: boolean) => {
    try {
      const res = await apiClient.jobs.setApplied(job.id, applied);
      setJobs((prev) =>
        prev.map((j) => (j.id === job.id ? { ...j, applied: res.applied, appliedAt: res.appliedAt } : j))
      );
      if (selectedJob?.id === job.id) {
        setSelectedJob((prev) => (prev ? { ...prev, applied: res.applied, appliedAt: res.appliedAt } : null));
      }
    } catch (err: any) {
      console.error('Failed to update applied status', err);
    }
  };

  const handleJobUpdated = (updatedJob: Job) => {
    setJobs((prev) => prev.map((j) => (j.id === updatedJob.id ? updatedJob : j)));
    setSelectedJob(updatedJob);
  };

  if (checkingReadiness && !readiness) {
    return (
      <div className="py-32 flex flex-col items-center justify-center text-center text-ink-muted font-mono gap-3">
        <Loader2 className="w-7 h-7 animate-spin text-signal-emerald" />
        <span className="text-xs uppercase tracking-wider">Evaluating candidate profile readiness...</span>
      </div>
    );
  }

  // 1. PROFILE-FIRST ENFORCEMENT: DISCOVERY LOCKED WHEN PROFILE IS NOT READY
  if (readiness && !readiness.canDiscover) {
    const filledBlocks = Math.round((readiness.score / 100) * 20);
    const emptyBlocks = 20 - filledBlocks;
    const progressVisual = '█'.repeat(filledBlocks) + '░'.repeat(emptyBlocks);

    return (
      <div className="max-w-4xl mx-auto px-4 py-16 font-sans">
        <div className="p-8 sm:p-12 rounded-3xl bg-dark-900 border border-signal-amber/30 shadow-2xl relative overflow-hidden">
          <div className="absolute top-0 right-0 w-96 h-96 bg-signal-amber/5 rounded-full blur-3xl pointer-events-none" />

          {/* Technical Pill */}
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-signal-amber/10 border border-signal-amber/30 text-signal-amber font-mono text-[10px] tracking-widest uppercase mb-6">
            <span className="w-1.5 h-1.5 rounded-full bg-signal-amber animate-pulse" />
            <span>DISCOVERY LOCKED // PROFILE INCOMPLETE</span>
          </div>

          <h1 className="text-3xl sm:text-5xl font-light tracking-tight text-ink-primary leading-tight">
            YOUR PROFILE ISN'T READY
          </h1>

          <p className="text-sm sm:text-base text-ink-secondary mt-3 max-w-xl font-light leading-relaxed">
            JobHunter needs to understand you before it can find jobs worth your time. Discovery is locked until your candidate profile meets the minimum required truth threshold.
          </p>

          {/* Visual Progress Bar */}
          <div className="my-8 p-5 rounded-2xl bg-dark-950 border border-white/[0.08] font-mono">
            <div className="flex items-center justify-between text-xs mb-2">
              <span className="text-ink-muted uppercase tracking-wider">PROFILE READINESS</span>
              <span className="text-signal-amber font-bold">{readiness.score}%</span>
            </div>
            <div className="text-base sm:text-lg text-signal-amber tracking-tight font-mono select-none overflow-x-auto whitespace-nowrap">
              {progressVisual} <span className="text-sm text-ink-muted ml-2">{readiness.score}%</span>
            </div>
          </div>

          {/* Checklist */}
          <div className="space-y-6">
            <div>
              <h3 className="font-mono text-xs text-ink-muted uppercase tracking-wider mb-3">
                REQUIRED CANDIDATE EVIDENCE:
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {readiness.items.map((item) => (
                  <div
                    key={item.key}
                    className={`p-3.5 rounded-xl border flex items-start gap-3 transition-colors ${
                      item.satisfied
                        ? 'bg-signal-emerald/[0.03] border-signal-emerald/20 text-ink-primary'
                        : 'bg-white/[0.01] border-white/[0.06] text-ink-muted'
                    }`}
                  >
                    <div className="mt-0.5">
                      {item.satisfied ? (
                        <CheckCircle2 className="w-4 h-4 text-signal-emerald flex-shrink-0" />
                      ) : (
                        <span className="w-4 h-4 rounded-full border border-signal-amber/60 text-signal-amber text-[10px] font-mono flex items-center justify-center flex-shrink-0">
                          ○
                        </span>
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <span className={`text-xs font-medium font-sans ${item.satisfied ? 'text-ink-primary' : 'text-signal-amber'}`}>
                          {item.label}
                        </span>
                        {item.mandatory && !item.satisfied && (
                          <span className="text-[9px] font-mono text-signal-rose uppercase tracking-wider">Required</span>
                        )}
                      </div>
                      <p className="text-[11px] text-ink-muted mt-0.5 font-light truncate">{item.details}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {readiness.missingItems.length > 0 && (
              <div className="p-4 rounded-xl bg-signal-amber/[0.05] border border-signal-amber/20 font-mono text-xs text-signal-amber space-y-1">
                <span className="font-bold tracking-wider uppercase block">Missing Information:</span>
                <div className="flex flex-wrap gap-2 pt-1">
                  {readiness.missingItems.map((m) => (
                    <span key={m} className="px-2.5 py-1 rounded bg-signal-amber/10 border border-signal-amber/30 text-[11px]">
                      ○ {m}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Action Button */}
          <div className="mt-8 pt-6 border-t border-white/[0.08] flex items-center justify-between">
            <p className="text-xs text-ink-muted font-light hidden sm:block">
              Upload your master resume or enter your details manually.
            </p>
            <button
              onClick={onNavigateToProfile}
              className="px-6 py-3 rounded-xl bg-signal-emerald hover:bg-signal-emerald/90 text-dark-950 font-mono font-bold text-xs tracking-wider uppercase transition shadow-lg shadow-signal-emerald/20 flex items-center gap-2"
            >
              <span>COMPLETE PROFILE</span>
              <ArrowRight className="w-4 h-4 text-dark-950" />
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full flex flex-col">
      {/* Profile Ready Technical Status Pill */}
      {readiness && readiness.canDiscover && (
        <div className="max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 pt-4">
          <div className="p-3.5 rounded-2xl bg-signal-emerald/[0.04] border border-signal-emerald/20 flex flex-col sm:flex-row sm:items-center justify-between gap-3 font-mono">
            <div className="flex items-center gap-2.5">
              <span className="w-2 h-2 rounded-full bg-signal-emerald" />
              <span className="text-xs text-signal-emerald font-bold tracking-wide uppercase">
                PROFILE READY // DISCOVERY UNLOCKED
              </span>
              <span className="text-xs text-ink-muted hidden md:inline">
                — JobHunter understands enough about you to start finding relevant opportunities.
              </span>
            </div>
            <span className="text-[10px] text-ink-muted uppercase tracking-widest">
              READINESS: {readiness.score}%
            </span>
          </div>
        </div>
      )}

      {/* 1. CINEMATIC HERO OPENING & THREE.JS INTELLIGENCE FIELD */}
      <section className="relative w-full pt-10 pb-16 overflow-hidden flex flex-col items-center text-center">
        <div className="max-w-4xl mx-auto px-4 z-10 space-y-6">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/[0.04] border border-white/[0.08] text-signal-emerald font-mono text-[11px] tracking-widest uppercase">
            <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald animate-pulse" />
            <span>INTELLIGENT JOB SEARCH + APPLICATION OPTIMIZATION</span>
          </div>

          <h1 className="text-4xl sm:text-6xl lg:text-7xl font-light tracking-tight text-ink-primary leading-[1.08]">
            Find the jobs <br />
            <span className="font-normal italic text-signal-emerald">worth your time.</span>
          </h1>

          <p className="max-w-2xl mx-auto text-sm sm:text-base text-ink-secondary font-light leading-relaxed">
            JobHunter continuously searches, understands and prioritizes engineering opportunities based strictly on your verified commercial experience.
          </p>
        </div>

        {/* Central Three.js Visual Representation */}
        <div className="w-full max-w-5xl mx-auto my-4">
          <IntelligenceFieldScene isSearching={isDiscovering} activePhase={discoveryStep} />
        </div>
      </section>

      {/* 2. THE FIVE-PHASE AGENT JOURNEY STORYTELLING */}
      <AgentJourneyPhases currentStep={discoveryStep} isDiscovering={isDiscovering} />

      {/* 3. MINIMAL TECHNICAL SEARCH & CONSTRAINTS INTERFACE */}
      <section className="max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8">
        <MinimalSearchInterface
          role={role}
          onRoleChange={setRole}
          location={location}
          onLocationChange={setLocation}
          workMode={workMode}
          onWorkModeChange={setWorkMode}
          priorityFilter={priorityFilter}
          onPriorityFilterChange={setPriorityFilter}
          freshnessFilter={freshnessFilter}
          onFreshnessFilterChange={setFreshnessFilter}
          sortBy={sortBy}
          onSortByChange={setSortBy}
          onDiscover={handleDiscover}
          isDiscovering={isDiscovering}
          onReset={handleResetFilters}
          totalJobs={totalJobs}
        />

        {/* Error Alert Banner */}
        {error && (
          <div className="max-w-4xl mx-auto mb-6 p-4 rounded-xl bg-signal-rose/10 border border-signal-rose/20 text-signal-rose text-xs font-mono flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Live Search Discovery Notification Banner */}
        {discoverySummary && !isDiscovering && (
          <div className="max-w-4xl mx-auto mb-8 p-5 rounded-2xl bg-dark-900 border border-signal-emerald/30 shadow-2xl animate-in fade-in duration-300">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-signal-emerald" />
                <h3 className="font-mono text-xs font-bold text-ink-primary tracking-wide">
                  FIRECRAWL DISCOVERY COMPLETED
                </h3>
              </div>
              <span className="font-mono text-[10px] text-ink-muted">
                EXECUTION: {(discoverySummary.executionTimeMs / 1000).toFixed(2)}S
              </span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-center font-mono">
              <div className="p-2.5 rounded-lg bg-dark-950 border border-white/[0.04]">
                <span className="text-[10px] text-ink-muted block">FOUND</span>
                <span className="text-sm font-bold text-ink-primary">{discoverySummary.searchResultsFound}</span>
              </div>
              <div className="p-2.5 rounded-lg bg-dark-950 border border-white/[0.04]">
                <span className="text-[10px] text-ink-muted block">SCRAPED</span>
                <span className="text-sm font-bold text-signal-cyan">{discoverySummary.pagesScraped}</span>
              </div>
              <div className="p-2.5 rounded-lg bg-dark-950 border border-signal-emerald/30 bg-signal-emerald/[0.04]">
                <span className="text-[10px] text-signal-emerald block font-semibold">INGESTED</span>
                <span className="text-sm font-bold text-signal-emerald">{discoverySummary.jobsCreated}</span>
              </div>
              <div className="p-2.5 rounded-lg bg-dark-950 border border-white/[0.04]">
                <span className="text-[10px] text-ink-muted block">DEDUPED</span>
                <span className="text-sm font-bold text-ink-muted">{discoverySummary.duplicatesSkipped}</span>
              </div>
            </div>
          </div>
        )}

        {/* 4. EDITORIAL JOB SPECIMEN LIST */}
        <div className="mt-8 mb-20 max-w-5xl mx-auto">
          {/* Section Header */}
          <div className="flex items-center justify-between py-4 border-b border-white/[0.08] mb-2 font-mono text-xs text-ink-muted uppercase tracking-widest">
            <div className="flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald" />
              <span>OPPORTUNITY SPECIMENS // VERIFIED RELEVANCE</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-signal-emerald font-bold">{totalJobs} ROLES MATCH YOUR PROFILE</span>
              <span className="text-[10px] text-ink-muted hidden sm:inline">(AGGRESSIVELY FILTERED)</span>
            </div>
          </div>

          {/* Job Feed */}
          {loading ? (
            <div className="py-24 text-center text-ink-muted font-mono text-xs flex flex-col items-center justify-center gap-3">
              <Loader2 className="w-6 h-6 animate-spin text-signal-emerald" />
              <span>Querying verified opportunity queue...</span>
            </div>
          ) : jobs.length > 0 ? (
            <div className="divide-y divide-transparent">
              {jobs.map((job, idx) => (
                <EditorialJobSpecimen
                  key={job.id}
                  job={job}
                  index={page * 15 + idx}
                  match={matchMap[job.id]}
                  onSelect={setSelectedJob}
                  onToggleApplied={handleToggleApplied}
                />
              ))}
            </div>
          ) : (
            <div className="py-24 text-center text-ink-muted font-mono text-xs border-y border-white/[0.04]">
              No active job specimens match current constraints.
            </div>
          )}

          {/* Minimal Pagination */}
          {totalJobs > 15 && (
            <div className="mt-12 flex items-center justify-between font-mono text-xs text-ink-muted pt-6 border-t border-white/[0.06]">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-4 py-2 rounded-lg border border-white/[0.08] hover:border-white/20 disabled:opacity-30 disabled:pointer-events-none transition"
              >
                ← PREVIOUS
              </button>

              <span>
                PAGE {page + 1} OF {Math.ceil(totalJobs / 15)}
              </span>

              <button
                disabled={(page + 1) * 15 >= totalJobs}
                onClick={() => setPage((p) => p + 1)}
                className="px-4 py-2 rounded-lg border border-white/[0.08] hover:border-white/20 disabled:opacity-30 disabled:pointer-events-none transition"
              >
                NEXT →
              </button>
            </div>
          )}
        </div>
      </section>

      {/* 5. DEDICATED IMMERSIVE JOB DETAIL INSPECTION DRAWER */}
      {selectedJob && (
        <JobIntelligenceDetail
          job={selectedJob}
          onClose={() => setSelectedJob(null)}
          initialMatch={matchMap[selectedJob.id]}
          onMatchUpdated={(jobId, match) => {
            setMatchMap((prev) => ({ ...prev, [jobId]: match }));
          }}
          onJobUpdated={handleJobUpdated}
        />
      )}
    </div>
  );
};
