import React from 'react';
import { GovernmentJob } from '../../types';
import {
  FileText,
  ExternalLink,
  Calendar,
  Users,
  MapPin,
  Building2,
  Clock,
  CheckCircle2,
  XCircle,
  HelpCircle,
  Sparkles,
} from 'lucide-react';

interface GovernmentJobCardProps {
  job: GovernmentJob;
  onViewDetails: (job: GovernmentJob) => void;
}

export const GovernmentJobCard: React.FC<GovernmentJobCardProps> = ({ job, onViewDetails }) => {
  // Verification status UI mapping (Section 10 & 25)
  const renderVerificationBadge = () => {
    switch (job.verificationStatus) {
      case 'VERIFIED_OFFICIAL':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-mono font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            🟢 Officially Verified
          </span>
        );
      case 'VERIFIED_OFFICIAL_NOTIFICATION':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-mono font-medium bg-amber-500/10 text-amber-300 border border-amber-500/30">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-400" />
            🟡 Official Notification Verified
          </span>
        );
      case 'EXPIRED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-mono font-medium bg-gray-500/10 text-gray-400 border border-gray-500/20">
            <Clock className="w-3 h-3" />
            Expired
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-mono font-medium bg-rose-500/10 text-rose-400 border border-rose-500/20">
            🔴 Unverified
          </span>
        );
    }
  };

  // Deterministic candidate eligibility badge (Section 18 & 19)
  const renderEligibilityBadge = () => {
    if (!job.candidateEligibility) return null;

    const { status, summaryMessage } = job.candidateEligibility;
    switch (status) {
      case 'ELIGIBLE':
        return (
          <div
            title={summaryMessage}
            className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md text-[11px] font-mono font-semibold bg-emerald-500/15 text-emerald-300 border border-emerald-500/40 shadow-sm"
          >
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
            <span>ELIGIBLE</span>
          </div>
        );
      case 'LIKELY_ELIGIBLE':
        return (
          <div
            title={summaryMessage}
            className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md text-[11px] font-mono font-medium bg-amber-500/15 text-amber-300 border border-amber-500/40"
          >
            <Sparkles className="w-3.5 h-3.5 text-amber-400" />
            <span>LIKELY ELIGIBLE</span>
          </div>
        );
      case 'NOT_ELIGIBLE':
        return (
          <div
            title={summaryMessage}
            className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md text-[11px] font-mono font-medium bg-rose-500/15 text-rose-300 border border-rose-500/40"
          >
            <XCircle className="w-3.5 h-3.5 text-rose-400" />
            <span>NOT ELIGIBLE</span>
          </div>
        );
      case 'UNKNOWN':
      default:
        return (
          <div
            title={summaryMessage}
            className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md text-[11px] font-mono font-medium bg-blue-500/10 text-blue-300 border border-blue-500/30"
          >
            <HelpCircle className="w-3.5 h-3.5 text-blue-400" />
            <span>CHECK ELIGIBILITY</span>
          </div>
        );
    }
  };

  const formatLastDate = (dateStr?: string) => {
    if (!dateStr) return 'Not Specified';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="bg-dark-900/80 border border-white/[0.08] hover:border-signal-emerald/40 transition-all duration-200 rounded-xl p-5 shadow-lg flex flex-col justify-between group relative overflow-hidden">
      {/* Decorative gradient highlight */}
      <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-emerald-500/30 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />

      <div>
        {/* Top Header: Organization & Verification Badges */}
        <div className="flex flex-wrap items-center justify-between gap-2 mb-2.5">
          <div className="flex items-center gap-2">
            {renderVerificationBadge()}
            {job.corrigendaCount > 0 && (
              <span className="px-2 py-0.5 rounded-full text-[10px] font-mono bg-purple-500/10 text-purple-300 border border-purple-500/30">
                Corrigendum Active ({job.corrigendaCount})
              </span>
            )}
          </div>
          {renderEligibilityBadge()}
        </div>

        {/* Title */}
        <h3
          onClick={() => onViewDetails(job)}
          className="text-base sm:text-lg font-semibold text-ink-primary hover:text-signal-emerald cursor-pointer transition-colors leading-snug mb-1"
        >
          {job.title}
        </h3>

        {/* Organization & Department */}
        <div className="text-xs text-ink-secondary flex items-center gap-1.5 mb-3 font-mono">
          <Building2 className="w-3.5 h-3.5 text-ink-muted flex-shrink-0" />
          <span className="truncate">{job.organization}</span>
          {job.department && (
            <>
              <span className="text-white/20">•</span>
              <span className="truncate text-ink-muted">{job.department}</span>
            </>
          )}
        </div>

        {/* Location & Employment Type Metadata Pill Strip */}
        <div className="flex flex-wrap items-center gap-2 mb-4 text-xs">
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-white/[0.04] border border-white/[0.06] text-ink-secondary">
            <MapPin className="w-3 h-3 text-signal-emerald" />
            <span>
              {job.district ? `${job.district}, ` : ''}
              {job.state}
            </span>
          </span>

          <span className="px-2.5 py-1 rounded-md bg-white/[0.04] border border-white/[0.06] text-ink-secondary font-mono text-[11px]">
            {job.employmentType.replace('_', ' ')}
            {job.honorarium && ' • Honorarium'}
          </span>

          {job.vacanciesCount && (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-white/[0.04] border border-white/[0.06] text-ink-secondary font-mono text-[11px]">
              <Users className="w-3 h-3 text-ink-muted" />
              <span>{job.vacanciesCount} Vacancies</span>
            </span>
          )}
        </div>

        {/* Eligibility Highlights Grid */}
        <div className="bg-dark-950/60 border border-white/[0.05] rounded-lg p-3 mb-4 space-y-1.5 text-xs font-mono">
          <div className="flex items-center justify-between text-ink-secondary">
            <span className="text-ink-muted">Education:</span>
            <span className="text-ink-primary font-medium truncate max-w-[200px]">
              {job.educationList && job.educationList.length > 0
                ? job.educationList.join(', ')
                : 'See Notification'}
            </span>
          </div>

          <div className="flex items-center justify-between text-ink-secondary">
            <span className="text-ink-muted">Age Limit:</span>
            <span className="text-ink-primary font-medium">
              {job.minimumAge || job.maximumAge
                ? `${job.minimumAge || 18}–${job.maximumAge || 'Max'} yrs`
                : 'Not Specified'}
            </span>
          </div>

          {job.genderEligibility && job.genderEligibility !== 'NOT_SPECIFIED' && (
            <div className="flex items-center justify-between text-ink-secondary">
              <span className="text-ink-muted">Gender:</span>
              <span className={`font-semibold ${job.genderEligibility === 'FEMALE_ONLY' ? 'text-pink-400' : 'text-blue-400'}`}>
                {job.genderEligibility === 'FEMALE_ONLY' ? 'Women Only' : job.genderEligibility.replace('_', ' ')}
              </span>
            </div>
          )}

          {job.salary && (
            <div className="flex items-center justify-between text-ink-secondary pt-1 border-t border-white/[0.04]">
              <span className="text-ink-muted">Compensation:</span>
              <span className="text-emerald-400 font-medium truncate max-w-[220px]">
                {job.salary}
              </span>
            </div>
          )}
        </div>

        {/* Source Display (Section 27) */}
        <div className="text-[11px] text-ink-muted mb-4 font-mono">
          <div className="flex items-center gap-1.5">
            <span className="text-white/40">Authority:</span>
            <span className="text-ink-secondary truncate">{job.authority || job.sourceDomain || 'Official Government Portal'}</span>
          </div>
          {job.notificationNumber && (
            <div className="flex items-center gap-1.5 text-[10px]">
              <span className="text-white/40">Advt No:</span>
              <span className="text-ink-muted truncate">{job.notificationNumber}</span>
            </div>
          )}
        </div>
      </div>

      {/* Footer Dates & Action Buttons (Strictly NO resume tailoring buttons - Section 26 & 39) */}
      <div className="pt-3 border-t border-white/[0.06] flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <div className="flex items-center gap-1.5 text-xs font-mono text-ink-muted">
          <Calendar className="w-3.5 h-3.5 text-amber-400" />
          <span>Last Date:</span>
          <span className="text-ink-primary font-medium">
            {formatLastDate(job.applicationLastDate)}
          </span>
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          {job.notificationUrl && (
            <a
              href={job.notificationUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-mono bg-white/[0.04] hover:bg-white/[0.08] text-ink-secondary hover:text-ink-primary border border-white/10 transition"
              title="Open original official notification document"
            >
              <FileText className="w-3.5 h-3.5 text-blue-400" />
              <span>Notification</span>
            </a>
          )}

          {job.applicationUrl ? (
            <a
              href={job.applicationUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-mono bg-signal-emerald/15 hover:bg-signal-emerald/25 text-signal-emerald border border-signal-emerald/30 font-medium transition shadow-sm"
              title="Apply directly on the official government recruitment portal"
            >
              <span>Apply Officially</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </a>
          ) : (
            <button
              onClick={() => onViewDetails(job)}
              className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-mono bg-white/[0.06] hover:bg-white/[0.1] text-ink-primary border border-white/10 font-medium transition"
            >
              <span>View Details</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
