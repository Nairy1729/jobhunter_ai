import React from 'react';
import { Job, MatchAnalysisResponse } from '../types';
import { ArrowRight, Check } from 'lucide-react';

interface EditorialJobSpecimenProps {
  job: Job;
  index: number;
  match?: MatchAnalysisResponse | null;
  onSelect: (job: Job) => void;
  onToggleApplied?: (job: Job, applied: boolean) => void;
}

export const EditorialJobSpecimen: React.FC<EditorialJobSpecimenProps> = ({
  job,
  index,
  match,
  onSelect,
  onToggleApplied,
}) => {
  const formattedIndex = String(index + 1).padStart(2, '0');
  const category = job.priorityCategory;
  const score = job.priorityScore ?? match?.priorityScore;

  // Restrained priority & match category styling
  const getPrioritySpec = () => {
    if (job.matchCategory === 'HIGH_RELEVANCE' || category === 'HIGH_PRIORITY' || match?.recommendation === 'APPLY') {
      return {
        label: job.matchCategory ? job.matchCategory.replace('_', ' ') : 'HIGH RELEVANCE',
        scoreLabel: score ? `${score}% FIT` : 'STRONG FIT',
        pillClass: 'text-signal-emerald border-signal-emerald/30 bg-signal-emerald/[0.06]',
        dotClass: 'bg-signal-emerald',
      };
    }
    if (job.matchCategory === 'GOOD_MATCH' || category === 'MEDIUM_PRIORITY' || match?.recommendation === 'APPLY_AFTER_TAILORING') {
      return {
        label: job.matchCategory ? job.matchCategory.replace('_', ' ') : 'GOOD MATCH',
        scoreLabel: score ? `${score}% FIT` : 'SOLID FIT',
        pillClass: 'text-signal-cyan border-signal-cyan/30 bg-signal-cyan/[0.06]',
        dotClass: 'bg-signal-cyan',
      };
    }
    if (job.matchCategory === 'POSSIBLE_MATCH' || category === 'LOW_PRIORITY' || match?.recommendation === 'LOW_PRIORITY') {
      return {
        label: job.matchCategory ? job.matchCategory.replace('_', ' ') : 'POSSIBLE MATCH',
        scoreLabel: score ? `${score}% FIT` : 'PARTIAL FIT',
        pillClass: 'text-signal-amber border-signal-amber/30 bg-signal-amber/[0.06]',
        dotClass: 'bg-signal-amber',
      };
    }
    return {
      label: 'NOT RECOMMENDED',
      scoreLabel: score ? `${score}%` : 'CONSTRAINT GAP',
      pillClass: 'text-ink-muted border-white/[0.08] bg-white/[0.02]',
      dotClass: 'bg-ink-muted',
    };
  };

  const priority = getPrioritySpec();

  // Why rationale: extract the primary verified experience statement or default
  const getWhyRationale = () => {
    if (job.whyThisJob && job.whyThisJob.length > 0) {
      return job.whyThisJob[0].replace(/^[A-Za-z\s]+:\s*/, '');
    }
    if (match?.overallAssessment) {
      return match.overallAssessment;
    }
    return `Verified backend match across core qualifications with ${job.workMode.toLowerCase()} deployment.`;
  };

  return (
    <article className="group relative py-7 sm:py-8 border-b border-white/[0.06] hover:bg-white/[0.015] transition-all duration-300">
      <div className="flex flex-col md:flex-row md:items-start justify-between gap-6">
        {/* Left Column: Index & Core Identity */}
        <div className="flex items-start gap-5 sm:gap-8 flex-1 min-w-0">
          {/* Monospace Index Specimen */}
          <div className="font-mono text-xs sm:text-sm font-semibold text-ink-muted/80 tracking-widest pt-1 select-none">
            {formattedIndex}
          </div>

          <div className="flex-1 min-w-0">
            {/* Metadata Bar: Priority + Source */}
            <div className="flex flex-wrap items-center gap-2.5 mb-2.5">
              <span
                className={`font-mono text-[10px] tracking-widest px-2 py-0.5 rounded border flex items-center gap-1.5 ${priority.pillClass}`}
              >
                <span className={`w-1 h-1 rounded-full ${priority.dotClass}`} />
                {priority.label} · {priority.scoreLabel}
              </span>

              {job.usefulnessStatus === 'USEFUL' && (
                <span className="font-mono text-[9px] tracking-widest text-signal-emerald uppercase px-1.5 py-0.5 rounded border border-signal-emerald/20 bg-signal-emerald/[0.04]">
                  USEFUL
                </span>
              )}

              {job.sourceName && (
                <span className="font-mono text-[9px] tracking-widest text-ink-muted uppercase">
                  [{job.sourceName.replace('FIRECRAWL_', '')}]
                </span>
              )}

              {job.freshness === 'NEW' && (
                <span className="font-mono text-[9px] tracking-widest text-signal-emerald uppercase flex items-center gap-1">
                  <span className="w-1 h-1 rounded-full bg-signal-emerald animate-pulse" />
                  NEW {job.daysSincePosted != null ? `(${job.daysSincePosted}D)` : ''}
                </span>
              )}
            </div>

            {/* Role Title */}
            <h3
              onClick={() => onSelect(job)}
              className="text-xl sm:text-2xl font-light tracking-tight text-ink-primary group-hover:text-signal-emerald transition cursor-pointer mb-1.5"
            >
              {job.title}
            </h3>

            {/* Company & Location Metas */}
            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 font-mono text-xs text-ink-secondary mb-3.5">
              <span className="text-ink-primary font-medium">{job.companyName}</span>
              <span className="text-white/20">/</span>
              <span>{job.location || 'Location Not Specified'}</span>
              <span className="text-white/20">/</span>
              <span className="text-ink-muted">{job.workMode}</span>
              {job.minSalary ? (
                <>
                  <span className="text-white/20">/</span>
                  <span className="text-signal-emerald">
                    {job.salaryCurrency === 'INR'
                      ? `₹${(job.minSalary / 100000).toFixed(1)} LPA+`
                      : `$${(job.minSalary / 1000).toFixed(0)}k+`}
                  </span>
                </>
              ) : null}
            </div>

            {/* Technology Signals */}
            {job.detectedTechnologies && job.detectedTechnologies.length > 0 && (
              <div className="flex flex-wrap items-center gap-1.5 mb-3.5">
                {job.detectedTechnologies.slice(0, 6).map((tech) => (
                  <span
                    key={tech}
                    className="font-mono text-[10px] tracking-wider px-2 py-0.5 rounded bg-dark-950/70 border border-white/[0.06] text-ink-secondary"
                  >
                    {tech}
                  </span>
                ))}
              </div>
            )}

            {/* Grounding Why Statement */}
            <p className="text-xs sm:text-sm text-ink-secondary/90 font-light leading-relaxed max-w-3xl line-clamp-2">
              <span className="font-mono text-[10px] text-ink-muted uppercase tracking-widest mr-2">
                SIGNAL:
              </span>
              "{getWhyRationale()}"
            </p>
          </div>
        </div>

        {/* Right Column: Actions (Applied State + Inspect) */}
        <div className="flex items-center md:flex-col md:items-end justify-between md:justify-center gap-3 pt-2 md:pt-1 flex-shrink-0">
          {/* Applied Status Control */}
          {job.applied ? (
            <button
              onClick={() => onToggleApplied && onToggleApplied(job, false)}
              className="px-3 py-1.5 rounded-lg border border-signal-emerald/30 bg-signal-emerald/10 font-mono text-[11px] text-signal-emerald tracking-wide flex items-center gap-1.5 hover:bg-signal-emerald/20 transition"
              title="Click to revert to unapplied"
            >
              <Check className="w-3.5 h-3.5" />
              <span>APPLIED</span>
            </button>
          ) : (
            <button
              onClick={() => onToggleApplied && onToggleApplied(job, true)}
              className="px-3 py-1.5 rounded-lg border border-white/[0.08] hover:border-white/20 bg-dark-950 font-mono text-[11px] text-ink-muted hover:text-ink-primary tracking-wide transition"
            >
              MARK APPLIED
            </button>
          )}

          {/* Inspect Action */}
          <button
            onClick={() => onSelect(job)}
            className="px-4 py-2 rounded-lg bg-white/[0.04] hover:bg-white/[0.08] border border-white/[0.08] hover:border-white/20 text-ink-primary font-mono text-xs tracking-wider transition flex items-center gap-2 group-hover:border-signal-emerald/40"
          >
            <span>INSPECT</span>
            <ArrowRight className="w-3.5 h-3.5 text-signal-emerald group-hover:translate-x-1 transition-transform" />
          </button>
        </div>
      </div>
    </article>
  );
};
