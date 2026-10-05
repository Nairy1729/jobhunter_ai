import React, { useState, useEffect } from 'react';
import { GovernmentJob } from '../../types';
import { apiClient } from '../../api/client';
import { GovernmentJobCard } from './GovernmentJobCard';
import { GovernmentJobDetailModal } from './GovernmentJobDetailModal';
import { GovernmentCoverageDashboard } from './GovernmentCoverageDashboard';
import {
  Compass,
  Search,
  Building2,
  RefreshCw,
  Loader2,
  X,
  Play,
  Server,
} from 'lucide-react';

const INDIAN_STATES = [
  'ALL',
  'Central',
  'Andhra Pradesh',
  'Arunachal Pradesh',
  'Assam',
  'Bihar',
  'Chhattisgarh',
  'Delhi',
  'Goa',
  'Gujarat',
  'Haryana',
  'Himachal Pradesh',
  'Jharkhand',
  'Karnataka',
  'Kerala',
  'Madhya Pradesh',
  'Maharashtra',
  'Manipur',
  'Meghalaya',
  'Mizoram',
  'Nagaland',
  'Odisha',
  'Punjab',
  'Rajasthan',
  'Sikkim',
  'Tamil Nadu',
  'Telangana',
  'Tripura',
  'Uttar Pradesh',
  'Uttarakhand',
  'West Bengal',
];

const EDUCATION_OPTIONS = [
  { value: '', label: 'All Qualifications' },
  { value: '8th', label: '8th Pass' },
  { value: '10th', label: '10th Pass / Matric' },
  { value: '12th', label: '12th Pass / Intermediate' },
  { value: 'ITI', label: 'ITI' },
  { value: 'Diploma', label: 'Diploma / Polytechnic' },
  { value: 'Graduate', label: 'Graduate' },
  { value: 'B.Tech', label: 'B.Tech / B.E.' },
  { value: 'Post Graduate', label: 'Post Graduate' },
];

const EMPLOYMENT_TYPES = [
  { value: '', label: 'All Engagement Types' },
  { value: 'REGULAR', label: 'Permanent / Regular' },
  { value: 'CONTRACTUAL', label: 'Contractual' },
  { value: 'SAMVIDA', label: 'Samvida (संविदा)' },
  { value: 'HONORARIUM', label: 'Honorarium (Anganwadi, etc.)' },
  { value: 'SCHEME_BASED', label: 'Scheme / Mission-based' },
  { value: 'APPRENTICESHIP', label: 'Apprenticeship' },
  { value: 'MUNICIPAL', label: 'Municipal Corporation' },
  { value: 'TEMPORARY', label: 'Temporary / Ad-hoc' },
];

export const GovernmentJobsFeed: React.FC = () => {
  const [jobs, setJobs] = useState<GovernmentJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedJob, setSelectedJob] = useState<GovernmentJob | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);

  // Filters
  const [state, setState] = useState('ALL');
  const [district, setDistrict] = useState('');
  const [education, setEducation] = useState('');
  const [employmentType, setEmploymentType] = useState('');
  const [gender, setGender] = useState('');
  const [eligibilityFilter, setEligibilityFilter] = useState('ALL'); // ALL, ELIGIBLE_ONLY, LIKELY_ELIGIBLE
  const [query, setQuery] = useState('');
  const [includeUnverified, setIncludeUnverified] = useState(false);

  // Quick preset filter
  const [activePreset, setActivePreset] = useState<'ALL' | 'ANGANWADI' | 'SAMVIDA' | 'REGULAR' | 'ELIGIBLE'>('ALL');

  // Discovery Trigger Modal
  const [isDiscoverModalOpen, setIsDiscoverModalOpen] = useState(false);
  const [showCoverageModal, setShowCoverageModal] = useState(false);
  const [discoverState, setDiscoverState] = useState('Uttar Pradesh');
  const [discoverKeyword, setDiscoverKeyword] = useState('');
  const [discovering, setDiscovering] = useState(false);
  const [discoverResult, setDiscoverResult] = useState<string | null>(null);

  const fetchJobs = async () => {
    setLoading(true);
    try {
      const data = await apiClient.government.list({
        state: state !== 'ALL' ? state : undefined,
        district: district.trim() ? district.trim() : undefined,
        employmentType: employmentType || undefined,
        education: education || undefined,
        gender: gender || undefined,
        query: query.trim() ? query.trim() : undefined,
        eligibilityFilter,
        includeUnverified,
      });
      setJobs(data.content || []);
    } catch (err) {
      console.error('Failed to fetch government jobs', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJobs();
  }, [state, district, education, employmentType, gender, eligibilityFilter, includeUnverified]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchJobs();
  };

  const handlePresetSelect = (preset: 'ALL' | 'ANGANWADI' | 'SAMVIDA' | 'REGULAR' | 'ELIGIBLE') => {
    setActivePreset(preset);
    if (preset === 'ALL') {
      setEmploymentType('');
      setEligibilityFilter('ALL');
      setQuery('');
    } else if (preset === 'ANGANWADI') {
      setEmploymentType('HONORARIUM');
      setEligibilityFilter('ALL');
      setQuery('Anganwadi');
    } else if (preset === 'SAMVIDA') {
      setEmploymentType('SAMVIDA');
      setEligibilityFilter('ALL');
      setQuery('');
    } else if (preset === 'REGULAR') {
      setEmploymentType('REGULAR');
      setEligibilityFilter('ALL');
      setQuery('');
    } else if (preset === 'ELIGIBLE') {
      setEmploymentType('');
      setEligibilityFilter('ELIGIBLE_ONLY');
      setQuery('');
    }
  };

  const handleRunDiscovery = async () => {
    setDiscovering(true);
    setDiscoverResult(null);
    try {
      const res = await apiClient.government.discover({
        state: discoverState !== 'ALL' ? discoverState : undefined,
        searchKeyword: discoverKeyword.trim() ? discoverKeyword.trim() : undefined,
        maxQueries: 4,
      });
      setDiscoverResult(
        `Discovery completed. ${res.jobsDiscovered || 0} new jobs added, ${res.jobsUpdated || 0} updated across ${res.sourcesChecked || 0} official sources.`
      );
      fetchJobs();
    } catch (err) {
      console.error('Failed to trigger discovery', err);
      setDiscoverResult('Discovery run failed. Please check network connection.');
    } finally {
      setDiscovering(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
      {/* Product Banner (Section 35 - Accurate Language) */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-dark-900 via-dark-900/90 to-dark-950 border border-white/[0.08] p-6 sm:p-8 shadow-xl">
        <div className="relative z-10 max-w-3xl">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-signal-emerald/10 border border-signal-emerald/20 text-signal-emerald font-mono text-xs mb-3">
            <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald animate-pulse" />
            <span>INDIA-WIDE VERIFIED GOVERNMENT ENGINE</span>
          </div>

          <h1 className="text-2xl sm:text-3xl lg:text-4xl font-light tracking-tight text-ink-primary">
            Search government recruitment opportunities from verified sources across India.
          </h1>

          <p className="text-xs sm:text-sm text-ink-secondary mt-2.5 leading-relaxed font-light">
            Extensive discovery covering Central Ministries, State PSCs, District Administrations, Municipal Corporations,
            Panchayats, Health Missions (NHM), Women & Child Development (Anganwadi), and Samvida appointments.
            Verified official notifications with deterministic eligibility matching.
          </p>

          <div className="flex flex-wrap items-center gap-3 mt-5">
            <button
              onClick={() => setIsDiscoverModalOpen(true)}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-signal-emerald hover:bg-emerald-400 text-dark-950 font-mono text-xs font-bold transition shadow-lg shadow-signal-emerald/15"
            >
              <Compass className="w-3.5 h-3.5" />
              <span>TRIGGER SOURCE DISCOVERY</span>
            </button>

            <button
              onClick={() => setShowCoverageModal(true)}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-ink-primary border border-white/[0.1] font-mono text-xs transition shadow-sm"
            >
              <Server className="w-3.5 h-3.5 text-signal-emerald" />
              <span>REGISTRY TELEMETRY</span>
            </button>

            <span className="text-[11px] font-mono text-ink-muted">
              Official Domains Only (.gov.in / .nic.in / Official Portals)
            </span>
          </div>
        </div>
      </div>

      {/* Preset Filter Pills */}
      <div className="flex flex-wrap items-center gap-2">
        <button
          onClick={() => handlePresetSelect('ALL')}
          className={`px-3.5 py-1.5 rounded-lg text-xs font-mono transition ${
            activePreset === 'ALL'
              ? 'bg-signal-emerald text-dark-950 font-bold'
              : 'bg-dark-900/80 border border-white/[0.08] text-ink-secondary hover:text-ink-primary'
          }`}
        >
          All Opportunities
        </button>

        <button
          onClick={() => handlePresetSelect('ANGANWADI')}
          className={`px-3.5 py-1.5 rounded-lg text-xs font-mono transition ${
            activePreset === 'ANGANWADI'
              ? 'bg-signal-emerald text-dark-950 font-bold'
              : 'bg-dark-900/80 border border-white/[0.08] text-ink-secondary hover:text-ink-primary'
          }`}
        >
          Anganwadi & Local (WCD)
        </button>

        <button
          onClick={() => handlePresetSelect('SAMVIDA')}
          className={`px-3.5 py-1.5 rounded-lg text-xs font-mono transition ${
            activePreset === 'SAMVIDA'
              ? 'bg-signal-emerald text-dark-950 font-bold'
              : 'bg-dark-900/80 border border-white/[0.08] text-ink-secondary hover:text-ink-primary'
          }`}
        >
          Samvida & Contractual (संविदा)
        </button>

        <button
          onClick={() => handlePresetSelect('REGULAR')}
          className={`px-3.5 py-1.5 rounded-lg text-xs font-mono transition ${
            activePreset === 'REGULAR'
              ? 'bg-signal-emerald text-dark-950 font-bold'
              : 'bg-dark-900/80 border border-white/[0.08] text-ink-secondary hover:text-ink-primary'
          }`}
        >
          Central & State Regular
        </button>

        <button
          onClick={() => handlePresetSelect('ELIGIBLE')}
          className={`px-3.5 py-1.5 rounded-lg text-xs font-mono transition ${
            activePreset === 'ELIGIBLE'
              ? 'bg-signal-emerald text-dark-950 font-bold'
              : 'bg-dark-900/80 border border-white/[0.08] text-ink-secondary hover:text-ink-primary'
          }`}
        >
          🟢 Fully Eligible Only
        </button>
      </div>

      {/* Main Filter & Search Control Bar */}
      <div className="bg-dark-900/80 border border-white/[0.08] rounded-xl p-4 shadow-sm space-y-3">
        {/* Keyword Search Row */}
        <form onSubmit={handleSearchSubmit} className="flex gap-2">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-ink-muted absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search by department, post title, advertisement number, samvida, honorarium, or location..."
              className="w-full pl-10 pr-4 py-2 bg-dark-950 border border-white/10 rounded-lg text-xs sm:text-sm text-ink-primary focus:outline-none focus:border-signal-emerald font-mono placeholder:text-ink-muted"
            />
          </div>
          <button
            type="submit"
            className="px-4 py-2 bg-white/[0.08] hover:bg-white/[0.12] text-xs font-mono text-ink-primary rounded-lg transition"
          >
            Search
          </button>
        </form>

        {/* Multi-tier Dropdown Filters */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2 text-xs font-mono">
          {/* State */}
          <select
            value={state}
            onChange={(e) => setState(e.target.value)}
            className="px-2.5 py-2 bg-dark-950 border border-white/10 rounded-lg text-ink-primary focus:outline-none focus:border-signal-emerald"
          >
            {INDIAN_STATES.map((st) => (
              <option key={st} value={st}>
                {st === 'ALL' ? 'All States & UTs' : st}
              </option>
            ))}
          </select>

          {/* District Input */}
          <input
            type="text"
            value={district}
            onChange={(e) => setDistrict(e.target.value)}
            placeholder="District (e.g. Kannauj)"
            className="px-2.5 py-2 bg-dark-950 border border-white/10 rounded-lg text-ink-primary focus:outline-none focus:border-signal-emerald"
          />

          {/* Education */}
          <select
            value={education}
            onChange={(e) => setEducation(e.target.value)}
            className="px-2.5 py-2 bg-dark-950 border border-white/10 rounded-lg text-ink-primary focus:outline-none focus:border-signal-emerald"
          >
            {EDUCATION_OPTIONS.map((ed) => (
              <option key={ed.value} value={ed.value}>
                {ed.label}
              </option>
            ))}
          </select>

          {/* Employment Type */}
          <select
            value={employmentType}
            onChange={(e) => setEmploymentType(e.target.value)}
            className="px-2.5 py-2 bg-dark-950 border border-white/10 rounded-lg text-ink-primary focus:outline-none focus:border-signal-emerald"
          >
            {EMPLOYMENT_TYPES.map((et) => (
              <option key={et.value} value={et.value}>
                {et.label}
              </option>
            ))}
          </select>

          {/* Eligibility Filter */}
          <select
            value={eligibilityFilter}
            onChange={(e) => setEligibilityFilter(e.target.value)}
            className="px-2.5 py-2 bg-dark-950 border border-white/10 rounded-lg text-ink-primary focus:outline-none focus:border-signal-emerald"
          >
            <option value="ALL">All Candidates</option>
            <option value="ELIGIBLE_ONLY">Eligible Only</option>
            <option value="LIKELY_ELIGIBLE">Likely Eligible</option>
          </select>

          {/* Gender */}
          <select
            value={gender}
            onChange={(e) => setGender(e.target.value)}
            className="px-2.5 py-2 bg-dark-950 border border-white/10 rounded-lg text-ink-primary focus:outline-none focus:border-signal-emerald"
          >
            <option value="">Any Gender</option>
            <option value="FEMALE_ONLY">Women Only</option>
            <option value="MALE_ONLY">Men Only</option>
          </select>
        </div>

        {/* Include unverified toggle */}
        <div className="flex items-center justify-between pt-2 border-t border-white/[0.04] text-[11px] font-mono text-ink-muted">
          <span>Displaying verified canonical recruitment notices across official domains.</span>
          <label className="flex items-center gap-2 cursor-pointer hover:text-ink-secondary">
            <input
              type="checkbox"
              checked={includeUnverified}
              onChange={(e) => setIncludeUnverified(e.target.checked)}
              className="rounded border-white/20 bg-dark-950 text-signal-emerald focus:ring-0"
            />
            <span>Show Unverified Sources (Disabled by default)</span>
          </label>
        </div>
      </div>

      {/* Results Header */}
      <div className="flex items-center justify-between text-xs font-mono text-ink-secondary px-1">
        <span>
          Found <strong className="text-signal-emerald">{jobs.length}</strong> verified opportunities
        </span>
        <button
          onClick={fetchJobs}
          className="flex items-center gap-1.5 hover:text-ink-primary transition"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Jobs Grid */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-24 gap-3 font-mono text-xs text-ink-muted">
          <Loader2 className="w-7 h-7 text-signal-emerald animate-spin" />
          <span>MATCHING VERIFIED GOVERNMENT RECRUITMENT OPPORTUNITIES...</span>
        </div>
      ) : jobs.length === 0 ? (
        <div className="bg-dark-900/40 border border-white/[0.06] rounded-2xl p-12 text-center max-w-xl mx-auto space-y-3">
          <Building2 className="w-10 h-10 text-ink-muted mx-auto stroke-1" />
          <h3 className="text-base font-semibold text-ink-primary font-mono">
            No recruitment notices match current criteria
          </h3>
          <p className="text-xs text-ink-secondary leading-relaxed">
            Try broadening your state, district, or qualification filters, or trigger source discovery
            to search recent official notifications across district administrations.
          </p>
          <button
            onClick={() => {
              setState('ALL');
              setDistrict('');
              setEducation('');
              setEmploymentType('');
              setQuery('');
              setEligibilityFilter('ALL');
            }}
            className="px-4 py-2 rounded-lg bg-white/[0.08] hover:bg-white/[0.12] text-xs font-mono text-ink-primary transition"
          >
            Clear All Filters
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {jobs.map((job) => (
            <GovernmentJobCard
              key={job.id}
              job={job}
              onViewDetails={(j) => {
                setSelectedJob(j);
                setIsDetailOpen(true);
              }}
            />
          ))}
        </div>
      )}

      {/* Job Details Modal */}
      <GovernmentJobDetailModal
        job={selectedJob}
        isOpen={isDetailOpen}
        onClose={() => {
          setIsDetailOpen(false);
          setSelectedJob(null);
        }}
      />

      {/* Discovery Trigger Modal */}
      {isDiscoverModalOpen && (
        <div className="fixed inset-0 z-50 bg-dark-950/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-dark-900 border border-white/10 rounded-2xl w-full max-w-md p-6 space-y-4 shadow-2xl relative">
            <div className="flex items-center justify-between pb-3 border-b border-white/[0.08]">
              <div className="flex items-center gap-2">
                <Compass className="w-4 h-4 text-signal-emerald" />
                <h3 className="text-sm font-semibold font-mono uppercase text-ink-primary">
                  Trigger Government Source Discovery
                </h3>
              </div>
              <button
                onClick={() => setIsDiscoverModalOpen(false)}
                className="text-ink-muted hover:text-ink-primary"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-ink-secondary leading-relaxed">
              Crawl official government portals (.gov.in / .nic.in) and district vacancy boards using Firecrawl & Gemini intelligence extraction.
            </p>

            <div className="space-y-3 text-xs font-mono">
              <div>
                <label className="block text-ink-muted mb-1">Target State:</label>
                <select
                  value={discoverState}
                  onChange={(e) => setDiscoverState(e.target.value)}
                  className="w-full px-3 py-2 bg-dark-950 border border-white/10 rounded-lg text-ink-primary focus:outline-none focus:border-signal-emerald"
                >
                  {INDIAN_STATES.map((st) => (
                    <option key={st} value={st}>
                      {st}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-ink-muted mb-1">Specialized Search Term (Optional):</label>
                <input
                  type="text"
                  value={discoverKeyword}
                  onChange={(e) => setDiscoverKeyword(e.target.value)}
                  placeholder="e.g. Anganwadi, Samvida, NHM CHO, DEO"
                  className="w-full px-3 py-2 bg-dark-950 border border-white/10 rounded-lg text-ink-primary focus:outline-none focus:border-signal-emerald"
                />
              </div>
            </div>

            {discoverResult && (
              <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs font-mono">
                {discoverResult}
              </div>
            )}

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                onClick={() => setIsDiscoverModalOpen(false)}
                className="px-4 py-2 rounded-lg text-xs font-mono text-ink-secondary hover:text-ink-primary"
              >
                Close
              </button>
              <button
                onClick={handleRunDiscovery}
                disabled={discovering}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-signal-emerald hover:bg-emerald-400 text-dark-950 text-xs font-mono font-bold transition disabled:opacity-50"
              >
                {discovering ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>CRAWLING SOURCES...</span>
                  </>
                ) : (
                  <>
                    <Play className="w-3.5 h-3.5" />
                    <span>START DISCOVERY</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Coverage Telemetry Modal */}
      {showCoverageModal && (
        <div className="fixed inset-0 z-50 bg-dark-950/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-dark-900 border border-white/10 rounded-2xl w-full max-w-5xl max-h-[90vh] overflow-y-auto p-6 shadow-2xl relative">
            <button
              onClick={() => setShowCoverageModal(false)}
              className="absolute top-4 right-4 p-2 text-ink-muted hover:text-ink-primary rounded-lg transition"
              aria-label="Close telemetry modal"
            >
              <X className="w-5 h-5" />
            </button>
            <GovernmentCoverageDashboard />
          </div>
        </div>
      )}
    </div>
  );
};
