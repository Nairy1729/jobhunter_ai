import React from 'react';
import { Job, MatchAnalysisResponse } from '../types';
import {
  Building2,
  MapPin,
  DollarSign,
  Calendar,
  ExternalLink,
  Briefcase,
  Sparkles,
  CheckCircle2,
  AlertTriangle,
  AlertOctagon,
  Check,
  Undo2,
} from 'lucide-react';

interface JobCardProps {
  job: Job;
  match?: MatchAnalysisResponse | null;
  onSelect: (job: Job) => void;
  onToggleApplied?: (job: Job, applied: boolean) => void;
}

export const JobCard: React.FC<JobCardProps> = ({ job, match, onSelect, onToggleApplied }) => {
  const getWorkModeBadge = (mode: string) => {
    switch (mode) {
      case 'REMOTE':
        return <span className="px-2 py-0.5 rounded-full text-[11px] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">Remote</span>;
      case 'HYBRID':
        return <span className="px-2 py-0.5 rounded-full text-[11px] font-medium bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">Hybrid</span>;
      case 'ON_SITE':
        return <span className="px-2 py-0.5 rounded-full text-[11px] font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20">On-Site</span>;
      default:
        return null;
    }
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
      return job.salaryCurrency === 'INR' ? `₹${(job.minSalary / 100000).toFixed(1)} LPA+` : `$${(job.minSalary / 1000).toFixed(0)}k+`;
    }
    return null;
  };

  const getSourceBadge = (source?: string) => {
    if (!source) return null;
    const clean = source.replace('FIRECRAWL_', '');
    return (
      <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
        {clean}
      </span>
    );
  };

  const getPriorityBadge = () => {
    const category = job.priorityCategory;
    const score = job.priorityScore ?? match?.priorityScore;

    if (!category && !match) return null;

    if (category === 'HIGH_PRIORITY' || match?.recommendation === 'APPLY') {
      return (
        <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1 shadow-sm shadow-emerald-500/10">
          <CheckCircle2 className="w-3 h-3 text-emerald-400" />
          {score ? `${score}% High Priority` : 'High Priority'}
        </span>
      );
    }
    if (category === 'MEDIUM_PRIORITY' || match?.recommendation === 'APPLY_AFTER_TAILORING') {
      return (
        <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-cyan-500/20 text-cyan-300 border border-cyan-500/30 flex items-center gap-1 shadow-sm shadow-cyan-500/10">
          <Sparkles className="w-3 h-3 text-cyan-400" />
          {score ? `${score}% Medium Priority` : 'Medium Priority'}
        </span>
      );
    }
    if (category === 'LOW_PRIORITY' || match?.recommendation === 'LOW_PRIORITY') {
      return (
        <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1 shadow-sm shadow-amber-500/10">
          <AlertTriangle className="w-3 h-3 text-amber-400" />
          {score ? `${score}% Low Priority` : 'Low Priority'}
        </span>
      );
    }
    if (category === 'NOT_RECOMMENDED' || match?.recommendation === 'DO_NOT_APPLY') {
      return (
        <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/20 text-rose-300 border border-rose-500/30 flex items-center gap-1 shadow-sm shadow-rose-500/10">
          <AlertOctagon className="w-3 h-3 text-rose-400" />
          {score ? `${score}% Not Rec` : 'Not Recommended'}
        </span>
      );
    }

    return null;
  };

  const getFreshnessBadge = () => {
    const freshness = job.freshness;
    const days = job.daysSincePosted;

    switch (freshness) {
      case 'NEW':
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/15 text-emerald-300 border border-emerald-500/25 flex items-center gap-1">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            New {days != null ? `(${days}d)` : ''}
          </span>
        );
      case 'RECENT':
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-cyan-500/15 text-cyan-300 border border-cyan-500/25">
            Recent {days != null ? `(${days}d)` : ''}
          </span>
        );
      case 'OLDER':
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-medium bg-amber-500/15 text-amber-300 border border-amber-500/25">
            Older {days != null ? `(${days}d)` : ''}
          </span>
        );
      case 'STALE':
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-medium bg-slate-800 text-slate-400 border border-slate-700">
            Stale {days != null ? `(>${days}d)` : '(>60d)'}
          </span>
        );
      case 'UNKNOWN':
      default:
        return null;
    }
  };

  return (
    <div
      onClick={() => onSelect(job)}
      className={`p-5 rounded-2xl bg-slate-900/90 border transition-all duration-200 cursor-pointer flex flex-col justify-between group shadow-lg hover:shadow-emerald-500/5 relative overflow-hidden ${
        job.applied
          ? 'border-emerald-500/40 bg-slate-900/95 ring-1 ring-emerald-500/20'
          : job.priorityCategory === 'HIGH_PRIORITY'
          ? 'border-emerald-500/40 hover:border-emerald-400 hover:bg-slate-900/95'
          : job.priorityCategory === 'NOT_RECOMMENDED'
          ? 'border-rose-900/40 opacity-75 hover:opacity-100 hover:border-rose-800'
          : 'border-slate-800 hover:border-emerald-500/50 hover:bg-slate-900'
      }`}
    >
      {/* Top score progress line if priority score available */}
      {(job.priorityScore != null || match?.priorityScore != null) && (
        <div
          className={`absolute top-0 left-0 right-0 h-1 ${
            job.priorityCategory === 'HIGH_PRIORITY' || match?.recommendation === 'APPLY'
              ? 'bg-emerald-500'
              : job.priorityCategory === 'MEDIUM_PRIORITY' || match?.recommendation === 'APPLY_AFTER_TAILORING'
              ? 'bg-cyan-500'
              : job.priorityCategory === 'LOW_PRIORITY' || match?.recommendation === 'LOW_PRIORITY'
              ? 'bg-amber-500'
              : 'bg-rose-500'
          }`}
          style={{ width: `${Math.max(5, job.priorityScore ?? match?.priorityScore ?? 10)}%` }}
        />
      )}

      <div>
        <div className="flex items-start justify-between gap-3 mb-2">
          <div className="flex-1 min-w-0">
            <h3 className="text-base font-semibold text-white group-hover:text-emerald-300 transition truncate">
              {job.title}
            </h3>
            <div className="flex items-center gap-1.5 text-xs text-slate-400 mt-0.5">
              <Building2 className="w-3.5 h-3.5 flex-shrink-0 text-slate-500" />
              <span className="font-medium text-slate-300 truncate">{job.companyName}</span>
            </div>
          </div>
          <div className="flex flex-col items-end gap-1 flex-shrink-0">
            {getPriorityBadge() || (
              <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center gap-1">
                <Sparkles className="w-2.5 h-2.5" /> Discovered
              </span>
            )}
            <div className="flex items-center gap-1">
              {getFreshnessBadge()}
              {getSourceBadge(job.sourceName)}
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2 mt-3 text-xs text-slate-400">
          <div className="flex items-center gap-1">
            <MapPin className="w-3.5 h-3.5 text-slate-500" />
            <span className="truncate max-w-[160px]">{job.location}</span>
          </div>
          {getWorkModeBadge(job.workMode)}

          {formatSalary() && (
            <div className="flex items-center gap-1 text-emerald-400 font-medium">
              <DollarSign className="w-3.5 h-3.5" />
              <span>{formatSalary()}</span>
            </div>
          )}

          {job.minExperienceYears && (
            <div className="flex items-center gap-1 text-slate-400">
              <Briefcase className="w-3.5 h-3.5 text-slate-500" />
              <span>{job.minExperienceYears}{job.maxExperienceYears ? ` - ${job.maxExperienceYears}` : '+'} yrs</span>
            </div>
          )}
        </div>

        {/* Key Technologies with Coverage Icons */}
        {job.keyTechnologies && job.keyTechnologies.length > 0 ? (
          <div className="flex flex-wrap gap-1.5 mt-3 pt-2.5 border-t border-slate-800/80">
            {job.keyTechnologies.slice(0, 5).map((kt, i) => (
              <span
                key={i}
                className={`text-[10px] px-2 py-0.5 rounded-md font-mono flex items-center gap-1 border ${
                  kt.coverage === 'STRONG'
                    ? 'bg-emerald-950/30 text-emerald-300 border-emerald-500/30'
                    : kt.coverage === 'PARTIAL'
                    ? 'bg-amber-950/30 text-amber-300 border-amber-500/30'
                    : 'bg-rose-950/30 text-rose-300 border-rose-500/30'
                }`}
                title={`${kt.technology} (${kt.coverage}): ${kt.candidateEvidence}`}
              >
                <span>{kt.coverage === 'STRONG' ? '✓' : kt.coverage === 'PARTIAL' ? '△' : '✕'}</span>
                <span>{kt.technology}</span>
              </span>
            ))}
            {job.keyTechnologies.length > 5 && (
              <span className="text-[10px] px-1 py-0.5 text-slate-500">
                +{job.keyTechnologies.length - 5}
              </span>
            )}
          </div>
        ) : job.detectedTechnologies && job.detectedTechnologies.length > 0 ? (
          <div className="flex flex-wrap gap-1.5 mt-3 pt-2.5 border-t border-slate-800/80">
            {job.detectedTechnologies.slice(0, 5).map((tech) => (
              <span
                key={tech}
                className="text-[11px] px-2 py-0.5 rounded-lg bg-slate-800 text-slate-300 font-medium border border-slate-700/60"
              >
                {tech}
              </span>
            ))}
            {job.detectedTechnologies.length > 5 && (
              <span className="text-[11px] px-1.5 py-0.5 text-slate-500">
                +{job.detectedTechnologies.length - 5}
              </span>
            )}
          </div>
        ) : null}

        {/* Why This Job Highlight */}
        {job.whyThisJob && job.whyThisJob.length > 0 && (
          <div className="mt-2.5 p-2 rounded-xl bg-slate-950/70 border border-emerald-500/15 text-[11px] text-emerald-200/90 flex items-start gap-1.5">
            <Sparkles className="w-3.5 h-3.5 text-emerald-400 flex-shrink-0 mt-0.5" />
            <span className="line-clamp-2 leading-relaxed">{job.whyThisJob[0]}</span>
          </div>
        )}

        {/* Potential Concerns Callout */}
        {job.potentialConcerns && job.potentialConcerns.length > 0 && (job.priorityCategory === 'NOT_RECOMMENDED' || job.priorityCategory === 'LOW_PRIORITY') && (
          <div className="mt-1.5 p-1.5 rounded-lg bg-rose-500/10 border border-rose-500/20 text-[10px] text-rose-300 flex items-start gap-1">
            <AlertOctagon className="w-3 h-3 text-rose-400 flex-shrink-0 mt-0.5" />
            <span className="line-clamp-1">{job.potentialConcerns[0]}</span>
          </div>
        )}
      </div>

      <div className="mt-4 pt-3 border-t border-slate-800/60 flex items-center justify-between text-xs gap-2">
        <div className="text-slate-500 flex items-center gap-1 flex-shrink-0">
          {job.postingDate ? (
            <>
              <Calendar className="w-3.5 h-3.5" />
              <span>Posted {job.postingDate}</span>
            </>
          ) : (
            <span>Unknown Posting Date</span>
          )}
        </div>

        {/* Application State Action Buttons */}
        <div className="flex items-center gap-2">
          {job.applied ? (
            <div className="flex items-center gap-1.5">
              <span className="px-2.5 py-1 rounded-lg text-[11px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1">
                <Check className="w-3.5 h-3.5 text-emerald-400" /> Applied
              </span>
              {onToggleApplied && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onToggleApplied(job, false);
                  }}
                  className="p-1 rounded text-slate-500 hover:text-rose-400 hover:bg-slate-800 transition"
                  title="Mark as not applied"
                >
                  <Undo2 className="w-3.5 h-3.5" />
                </button>
              )}
            </div>
          ) : (
            onToggleApplied && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onToggleApplied(job, true);
                }}
                className="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-emerald-600/25 hover:border-emerald-500/40 text-slate-300 hover:text-emerald-300 text-[11px] font-medium border border-slate-700/80 transition flex items-center gap-1"
                title="Mark this job as applied"
              >
                <Check className="w-3 h-3 text-slate-400" /> Mark as Applied
              </button>
            )
          )}

          <button
            onClick={() => onSelect(job)}
            className="px-2.5 py-1 rounded-lg bg-emerald-600/15 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/30 text-[11px] font-semibold transition"
          >
            Open
          </button>

          <a
            href={job.jobUrl || job.canonicalUrl}
            target="_blank"
            rel="noopener noreferrer"
            onClick={(e) => e.stopPropagation()}
            className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-emerald-400 transition"
            title="Open application URL directly in new tab"
          >
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>
      </div>
    </div>
  );
};
