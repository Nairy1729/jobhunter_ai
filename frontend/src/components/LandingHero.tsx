import React from 'react';
import { Compass, Building2, CheckCircle2, ArrowRight, ShieldCheck } from 'lucide-react';

interface LandingHeroProps {
  onFindJobs: () => void;
  onExploreGovJobs: () => void;
  isAuthenticated?: boolean;
}

export const LandingHero: React.FC<LandingHeroProps> = ({
  onFindJobs,
  onExploreGovJobs,
  isAuthenticated = false,
}) => {
  return (
    <div className="w-full flex flex-col items-center">
      {/* 1. Primary Editorial Hero Section */}
      <section className="relative w-full pt-16 sm:pt-24 pb-16 overflow-hidden flex flex-col items-center text-center">
        {/* Subtle Ambient Glow */}
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-[600px] h-[300px] bg-signal-emerald/5 rounded-full blur-3xl pointer-events-none" />

        <div className="max-w-4xl mx-auto px-4 z-10 space-y-6">
          {/* System Badge */}
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/[0.04] border border-white/[0.08] text-signal-emerald font-mono text-[11px] tracking-widest uppercase">
            <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald animate-pulse" />
            <span>JOB DISCOVERY & INTELLIGENCE ENGINE</span>
          </div>

          {/* Primary Headline (Section 5) */}
          <h1 className="text-4xl sm:text-6xl lg:text-7xl font-light tracking-tight text-ink-primary leading-[1.08]">
            Find jobs worth applying to <br />
            <span className="font-normal italic text-signal-emerald">— based on your profile.</span>
          </h1>

          {/* Secondary Message (Section 5) */}
          <p className="max-w-2xl mx-auto text-sm sm:text-base text-ink-secondary font-light leading-relaxed">
            Search private and government opportunities, understand your fit, and apply with confidence.
          </p>

          {/* Primary Action Buttons (Section 5) */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
            <button
              onClick={onFindJobs}
              className="w-full sm:w-auto px-6 py-3.5 rounded-xl bg-signal-emerald hover:bg-emerald-400 text-dark-950 font-mono text-xs font-bold tracking-wider uppercase transition shadow-lg shadow-signal-emerald/20 flex items-center justify-center gap-2"
            >
              <Compass className="w-4 h-4 text-dark-950" />
              <span>FIND JOBS</span>
            </button>

            <button
              onClick={onExploreGovJobs}
              className="w-full sm:w-auto px-6 py-3.5 rounded-xl bg-white/[0.06] hover:bg-white/[0.1] text-ink-primary border border-white/10 font-mono text-xs font-semibold tracking-wider uppercase transition flex items-center justify-center gap-2"
            >
              <Building2 className="w-4 h-4 text-signal-emerald" />
              <span>EXPLORE GOVERNMENT JOBS</span>
            </button>
          </div>

          {!isAuthenticated && (
            <p className="text-xs text-ink-muted font-light pt-2">
              Create your profile once. JobHunter handles matching, eligibility, and tailoring.
            </p>
          )}
        </div>
      </section>

      {/* 2. Two Distinct Systems: Private vs Government (Section 1) */}
      <section className="max-w-6xl w-full mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Pillar 1: Private Jobs Engine */}
          <div className="p-6 sm:p-8 rounded-2xl bg-dark-900/60 border border-white/[0.08] relative overflow-hidden flex flex-col justify-between hover:border-white/20 transition-all group">
            <div className="space-y-4">
              <div className="w-10 h-10 rounded-xl bg-signal-emerald/10 border border-signal-emerald/20 flex items-center justify-center text-signal-emerald">
                <Compass className="w-5 h-5" />
              </div>

              <div>
                <span className="font-mono text-[10px] text-signal-emerald tracking-widest uppercase">
                  PRIVATE SECTOR ENGINE
                </span>
                <h3 className="text-xl sm:text-2xl font-light text-ink-primary mt-1">
                  Relevance-First Job Matching
                </h3>
              </div>

              <p className="text-xs sm:text-sm text-ink-secondary font-light leading-relaxed">
                Prioritizes engineering roles matching your verified skills and experience. Never applies with fabricated claims or generic resumes.
              </p>

              <div className="space-y-2 pt-2 text-xs font-mono text-ink-muted">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-signal-emerald" />
                  <span>Profile-first discovery gate</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-signal-emerald" />
                  <span>Truthful ATS resume tailoring</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-signal-emerald" />
                  <span>Verified commercial experience validation</span>
                </div>
              </div>
            </div>

            <div className="mt-8 pt-4 border-t border-white/[0.06]">
              <button
                onClick={onFindJobs}
                className="text-xs font-mono text-signal-emerald group-hover:text-emerald-300 flex items-center gap-1.5 transition"
              >
                <span>Explore Private Jobs</span>
                <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
              </button>
            </div>
          </div>

          {/* Pillar 2: Government Discovery Engine */}
          <div className="p-6 sm:p-8 rounded-2xl bg-dark-900/60 border border-white/[0.08] relative overflow-hidden flex flex-col justify-between hover:border-white/20 transition-all group">
            <div className="space-y-4">
              <div className="w-10 h-10 rounded-xl bg-signal-emerald/10 border border-signal-emerald/20 flex items-center justify-center text-signal-emerald">
                <ShieldCheck className="w-5 h-5" />
              </div>

              <div>
                <span className="font-mono text-[10px] text-signal-emerald tracking-widest uppercase">
                  PUBLIC SECTOR ENGINE
                </span>
                <h3 className="text-xl sm:text-2xl font-light text-ink-primary mt-1">
                  Officially Verified Public Recruitment
                </h3>
              </div>

              <p className="text-xs sm:text-sm text-ink-secondary font-light leading-relaxed">
                Direct discovery across Central, State, NIC District Portals, Municipal, Panchayat, NHM, and Samvida opportunities with zero resume tailoring.
              </p>

              <div className="space-y-2 pt-2 text-xs font-mono text-ink-muted">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-signal-emerald" />
                  <span>Direct official .gov.in and .nic.in portals</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-signal-emerald" />
                  <span>Original recruitment PDF notices</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-signal-emerald" />
                  <span>Deterministic age, education & domicile checks</span>
                </div>
              </div>
            </div>

            <div className="mt-8 pt-4 border-t border-white/[0.06]">
              <button
                onClick={onExploreGovJobs}
                className="text-xs font-mono text-signal-emerald group-hover:text-emerald-300 flex items-center gap-1.5 transition"
              >
                <span>Explore Government Jobs</span>
                <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};
