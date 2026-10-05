import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/client';
import { Job } from '../types';
import { CheckCircle2, Building2, MapPin, Calendar, ExternalLink, Loader2, Briefcase } from 'lucide-react';

interface ApplicationsTrackerProps {
  onNavigateToJobs?: () => void;
  onNavigateToGovJobs?: () => void;
  onOpenJobDetail?: (job: Job) => void;
}

export const ApplicationsTracker: React.FC<ApplicationsTrackerProps> = ({
  onNavigateToJobs,
  onNavigateToGovJobs,
  onOpenJobDetail,
}) => {
  const [appliedJobs, setAppliedJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const fetchAppliedJobs = async () => {
    setLoading(true);
    try {
      const data = await apiClient.jobs.getApplied();
      setAppliedJobs(data);
    } catch (err) {
      console.error('Failed to fetch applied jobs', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAppliedJobs();
  }, []);

  const handleToggleApplied = async (job: Job) => {
    setUpdatingId(job.id);
    try {
      await apiClient.jobs.setApplied(job.id, false);
      setAppliedJobs((prev) => prev.filter((j) => j.id !== job.id));
    } catch (err) {
      console.error('Failed to remove applied status', err);
    } finally {
      setUpdatingId(null);
    }
  };

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-20 flex flex-col items-center justify-center text-ink-muted font-mono gap-3">
        <Loader2 className="w-6 h-6 animate-spin text-signal-emerald" />
        <span className="text-xs uppercase tracking-wider">Loading applied opportunities...</span>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-white/[0.08]">
        <div>
          <div className="inline-flex items-center gap-2 px-2.5 py-0.5 rounded-full bg-signal-emerald/10 border border-signal-emerald/20 text-signal-emerald font-mono text-[11px] mb-2">
            <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald" />
            <span>APPLICATION TRACKER</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-light text-ink-primary">Your Applications</h1>
          <p className="text-xs sm:text-sm text-ink-secondary mt-1 font-light">
            Keep track of opportunities you've applied to across private ATS portals and government recruitment boards.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span className="px-3 py-1.5 rounded-xl bg-dark-900 border border-white/10 font-mono text-xs text-ink-secondary">
            Total Applied: <span className="text-signal-emerald font-bold">{appliedJobs.length}</span>
          </span>
        </div>
      </div>

      {/* Applied List or Empty State */}
      {appliedJobs.length === 0 ? (
        <div className="p-8 sm:p-12 rounded-3xl bg-dark-900/40 border border-white/[0.08] text-center max-w-xl mx-auto my-12">
          <div className="w-12 h-12 rounded-2xl bg-white/[0.04] border border-white/10 mx-auto flex items-center justify-center text-ink-muted mb-4">
            <Briefcase className="w-6 h-6" />
          </div>
          <h3 className="text-lg font-light text-ink-primary">No applications recorded yet</h3>
          <p className="text-xs sm:text-sm text-ink-secondary mt-2 font-light leading-relaxed">
            When you apply to a private job or official government posting, mark it as applied to keep track of your history here.
          </p>
          <div className="flex flex-wrap items-center justify-center gap-3 mt-6">
            <button
              onClick={onNavigateToJobs}
              className="px-4 py-2.5 rounded-xl bg-signal-emerald hover:bg-emerald-400 text-dark-950 font-mono text-xs font-bold transition shadow-sm"
            >
              FIND PRIVATE JOBS
            </button>
            <button
              onClick={onNavigateToGovJobs}
              className="px-4 py-2.5 rounded-xl bg-dark-900 hover:bg-white/[0.08] text-ink-secondary hover:text-ink-primary border border-white/10 font-mono text-xs transition"
            >
              EXPLORE GOV JOBS
            </button>
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          {appliedJobs.map((job) => (
            <div
              key={job.id}
              className="p-5 rounded-2xl bg-dark-900/60 border border-white/[0.08] hover:border-white/15 transition flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div className="space-y-1.5 flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-mono font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                    <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                    APPLIED
                  </span>
                  {job.appliedAt && (
                    <span className="text-[11px] font-mono text-ink-muted flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      {new Date(job.appliedAt).toLocaleDateString(undefined, {
                        month: 'short',
                        day: 'numeric',
                        year: 'numeric',
                      })}
                    </span>
                  )}
                </div>

                <h3
                  onClick={() => onOpenJobDetail?.(job)}
                  className="text-base sm:text-lg font-medium text-ink-primary hover:text-signal-emerald cursor-pointer transition truncate"
                >
                  {job.title}
                </h3>

                <div className="flex flex-wrap items-center gap-3 text-xs text-ink-secondary font-light">
                  <div className="flex items-center gap-1">
                    <Building2 className="w-3.5 h-3.5 text-signal-emerald" />
                    <span className="font-medium text-ink-primary">{job.companyName}</span>
                  </div>
                  <div className="flex items-center gap-1 text-ink-muted">
                    <MapPin className="w-3.5 h-3.5" />
                    <span>{job.location}</span>
                  </div>
                  {job.workMode && (
                    <span className="px-2 py-0.5 rounded bg-dark-950 border border-white/[0.06] font-mono text-[10px] text-ink-muted">
                      {job.workMode}
                    </span>
                  )}
                </div>
              </div>

              {/* Actions */}
              <div className="flex items-center gap-2.5 sm:self-center">
                <a
                  href={job.jobUrl || job.canonicalUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-3.5 py-2 rounded-xl bg-white/[0.06] hover:bg-white/[0.1] text-ink-primary border border-white/10 font-mono text-xs font-medium transition flex items-center gap-1.5 shadow-sm"
                  title="Open application career portal"
                >
                  <span>Portal</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>

                <button
                  onClick={() => handleToggleApplied(job)}
                  disabled={updatingId === job.id}
                  className="px-3 py-2 rounded-xl text-xs font-mono text-ink-muted hover:text-rose-400 hover:bg-rose-500/10 border border-transparent hover:border-rose-500/20 transition disabled:opacity-50"
                  title="Remove from applied list"
                >
                  {updatingId === job.id ? 'Updating...' : 'Undo'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
