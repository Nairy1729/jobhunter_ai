import React, { useEffect, useState } from 'react';
import { GovernmentJob, GovernmentJobDetail } from '../../types';
import { apiClient } from '../../api/client';
import {
  X,
  Building2,
  MapPin,
  Calendar,
  ShieldCheck,
  FileText,
  ExternalLink,
  CheckCircle2,
  XCircle,
  HelpCircle,
  AlertCircle,
  Sparkles,
  BookOpen,
  DollarSign,
  FileCheck2,
  RefreshCw,
  Loader2,
} from 'lucide-react';

interface GovernmentJobDetailModalProps {
  job: GovernmentJob | null;
  isOpen: boolean;
  onClose: () => void;
}

export const GovernmentJobDetailModal: React.FC<GovernmentJobDetailModalProps> = ({
  job,
  isOpen,
  onClose,
}) => {
  const [detail, setDetail] = useState<GovernmentJobDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (job && isOpen) {
      setLoading(true);
      setError(null);
      apiClient.government
        .getById(job.id)
        .then((res) => {
          setDetail(res);
        })
        .catch((err) => {
          console.error('Failed to fetch government job detail', err);
          setError('Unable to load full job details. Please try again.');
        })
        .finally(() => {
          setLoading(false);
        });
    } else {
      setDetail(null);
    }
  }, [job, isOpen]);

  if (!isOpen || !job) return null;

  const currentJob = detail || (job as unknown as GovernmentJobDetail);

  const formatDateTime = (dateStr?: string) => {
    if (!dateStr) return 'Not Specified';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-dark-950/80 backdrop-blur-md flex items-center justify-center p-3 sm:p-6 animate-fadeIn">
      <div className="bg-dark-900 border border-white/10 rounded-2xl w-full max-w-4xl max-h-[92vh] flex flex-col shadow-2xl relative overflow-hidden">
        {/* Modal Top Header */}
        <div className="px-6 py-4 border-b border-white/[0.08] flex items-start justify-between bg-dark-950/50 sticky top-0 z-10">
          <div>
            <div className="flex flex-wrap items-center gap-2 mb-1.5">
              {/* Verification badge */}
              {currentJob.verificationStatus === 'VERIFIED_OFFICIAL' ? (
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-mono font-medium bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
                  <ShieldCheck className="w-3.5 h-3.5" />
                  🟢 Officially Verified Source
                </span>
              ) : currentJob.verificationStatus === 'VERIFIED_OFFICIAL_NOTIFICATION' ? (
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-mono font-medium bg-amber-500/15 text-amber-300 border border-amber-500/30">
                  <ShieldCheck className="w-3.5 h-3.5" />
                  🟡 Verified From Official Notification
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-mono font-medium bg-rose-500/15 text-rose-300 border border-rose-500/30">
                  🔴 Unverified Notice
                </span>
              )}

              <span className="px-2.5 py-0.5 rounded-full text-[11px] font-mono bg-white/[0.05] border border-white/10 text-ink-secondary">
                {currentJob.employmentType?.replace('_', ' ')}
              </span>

              {currentJob.status && currentJob.status !== 'OPEN' && (
                <span className="px-2.5 py-0.5 rounded-full text-[11px] font-mono bg-purple-500/15 text-purple-300 border border-purple-500/30">
                  {currentJob.status}
                </span>
              )}
            </div>

            <h2 className="text-xl sm:text-2xl font-semibold text-ink-primary leading-snug">
              {currentJob.title}
            </h2>
            <div className="flex flex-wrap items-center gap-2 text-xs font-mono text-ink-secondary mt-1">
              <span className="flex items-center gap-1">
                <Building2 className="w-3 h-3 text-ink-muted" />
                {currentJob.organization}
              </span>
              {currentJob.department && (
                <>
                  <span className="text-white/20">•</span>
                  <span>{currentJob.department}</span>
                </>
              )}
              <span className="text-white/20">•</span>
              <span className="flex items-center gap-1 text-signal-emerald">
                <MapPin className="w-3 h-3" />
                {currentJob.district ? `${currentJob.district}, ` : ''}
                {currentJob.state}
              </span>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 text-ink-muted hover:text-ink-primary hover:bg-white/5 rounded-xl transition ml-2"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Scrollable Body */}
        <div className="flex-1 overflow-y-auto px-6 py-6 space-y-6">
          {loading && (
            <div className="flex items-center justify-center py-10 gap-2 text-ink-secondary font-mono text-xs">
              <Loader2 className="w-5 h-5 text-signal-emerald animate-spin" />
              <span>Fetching full official notification intelligence...</span>
            </div>
          )}

          {error && (
            <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs font-mono flex items-center gap-2">
              <AlertCircle className="w-4 h-4" />
              <span>{error}</span>
            </div>
          )}

          {/* 1. Candidate Eligibility Analysis (Section 18 & 19) */}
          {currentJob.candidateEligibility && (
            <div className="rounded-xl border border-white/10 bg-dark-950/60 p-5">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-signal-emerald" />
                  <h4 className="text-sm font-semibold tracking-wide uppercase font-mono text-ink-primary">
                    Candidate Eligibility Status
                  </h4>
                </div>
                <div className="font-mono text-xs font-bold">
                  {currentJob.candidateEligibility.status === 'ELIGIBLE' && (
                    <span className="px-2.5 py-1 rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">
                      🟢 FULLY ELIGIBLE
                    </span>
                  )}
                  {currentJob.candidateEligibility.status === 'LIKELY_ELIGIBLE' && (
                    <span className="px-2.5 py-1 rounded bg-amber-500/20 text-amber-300 border border-amber-500/40">
                      🟡 LIKELY ELIGIBLE
                    </span>
                  )}
                  {currentJob.candidateEligibility.status === 'NOT_ELIGIBLE' && (
                    <span className="px-2.5 py-1 rounded bg-rose-500/20 text-rose-300 border border-rose-500/40">
                      🔴 NOT ELIGIBLE
                    </span>
                  )}
                  {currentJob.candidateEligibility.status === 'UNKNOWN' && (
                    <span className="px-2.5 py-1 rounded bg-blue-500/20 text-blue-300 border border-blue-500/40">
                      ⚪ PROFILE INCOMPLETE
                    </span>
                  )}
                </div>
              </div>

              <p className="text-xs text-ink-secondary mb-3">
                {currentJob.candidateEligibility.summaryMessage}
              </p>

              {/* Checklist */}
              <div className="space-y-2 border-t border-white/[0.06] pt-3">
                {currentJob.candidateEligibility.reasons.map((r, idx) => (
                  <div key={idx} className="flex items-start gap-2.5 text-xs font-mono">
                    {r.status === 'PASS' && (
                      <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
                    )}
                    {r.status === 'FAIL' && (
                      <XCircle className="w-4 h-4 text-rose-400 flex-shrink-0 mt-0.5" />
                    )}
                    {r.status === 'UNKNOWN' && (
                      <HelpCircle className="w-4 h-4 text-blue-400 flex-shrink-0 mt-0.5" />
                    )}
                    <div className="flex-1">
                      <span className="text-white/40 mr-1.5">[{r.criterion}]:</span>
                      <span className={r.status === 'FAIL' ? 'text-rose-300' : 'text-ink-secondary'}>
                        {r.explanation}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 2. Key Recruitment Overview Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="bg-dark-950/40 border border-white/[0.06] rounded-xl p-3">
              <span className="text-[10px] font-mono text-ink-muted uppercase tracking-wider block mb-1">
                Total Vacancies
              </span>
              <span className="text-base font-semibold text-ink-primary font-mono">
                {currentJob.vacanciesCount ? `${currentJob.vacanciesCount} Posts` : 'See Notice'}
              </span>
            </div>

            <div className="bg-dark-950/40 border border-white/[0.06] rounded-xl p-3">
              <span className="text-[10px] font-mono text-ink-muted uppercase tracking-wider block mb-1">
                Employment Type
              </span>
              <span className="text-base font-semibold text-ink-primary font-mono">
                {currentJob.employmentType?.replace('_', ' ')}
              </span>
            </div>

            <div className="bg-dark-950/40 border border-white/[0.06] rounded-xl p-3">
              <span className="text-[10px] font-mono text-ink-muted uppercase tracking-wider block mb-1">
                Application Mode
              </span>
              <span className="text-base font-semibold text-ink-primary font-mono">
                {currentJob.applicationMode || 'Online'}
              </span>
            </div>

            <div className="bg-dark-950/40 border border-white/[0.06] rounded-xl p-3">
              <span className="text-[10px] font-mono text-ink-muted uppercase tracking-wider block mb-1">
                Application Fee
              </span>
              <span className="text-base font-semibold text-ink-primary font-mono truncate block">
                {currentJob.applicationFee || 'Nil'}
              </span>
            </div>
          </div>

          {/* 3. Eligibility Criteria Breakdown */}
          <div className="space-y-3">
            <h4 className="text-xs font-mono uppercase tracking-wider text-ink-muted font-semibold flex items-center gap-1.5">
              <BookOpen className="w-3.5 h-3.5 text-signal-emerald" />
              Eligibility Requirements
            </h4>
            <div className="bg-dark-950/40 border border-white/[0.06] rounded-xl p-4 divide-y divide-white/[0.05] text-xs font-mono">
              <div className="flex items-center justify-between py-2">
                <span className="text-ink-muted">Educational Qualification:</span>
                <span className="text-ink-primary font-medium text-right max-w-[60%]">
                  {currentJob.education && currentJob.education.length > 0
                    ? currentJob.education.join(' / ')
                    : (currentJob.educationList && currentJob.educationList.join(' / ')) || 'Not Specified'}
                </span>
              </div>

              <div className="flex items-center justify-between py-2">
                <span className="text-ink-muted">Age Limits:</span>
                <span className="text-ink-primary font-medium">
                  {currentJob.minimumAge || currentJob.maximumAge
                    ? `${currentJob.minimumAge || 18} to ${currentJob.maximumAge || 'Max'} years (As on notification cutoff date)`
                    : 'Not Specified'}
                </span>
              </div>

              <div className="flex items-center justify-between py-2">
                <span className="text-ink-muted">Gender Eligibility:</span>
                <span className={`font-semibold ${currentJob.gender === 'FEMALE_ONLY' ? 'text-pink-400' : 'text-ink-primary'}`}>
                  {currentJob.gender === 'FEMALE_ONLY'
                    ? 'Women Only (Strictly specified in notice)'
                    : currentJob.gender === 'MALE_ONLY'
                    ? 'Men Only'
                    : currentJob.gender || currentJob.genderEligibility || 'Open to All'}
                </span>
              </div>

              <div className="flex items-center justify-between py-2">
                <span className="text-ink-muted">Domicile / Residence Requirement:</span>
                <span className="text-ink-primary font-medium text-right max-w-[60%]">
                  {currentJob.domicile || 'All-India / Not Specified'}
                </span>
              </div>

              {currentJob.experienceYearsMin && currentJob.experienceYearsMin > 0 && (
                <div className="flex items-center justify-between py-2">
                  <span className="text-ink-muted">Experience Required:</span>
                  <span className="text-ink-primary font-medium">
                    {currentJob.experienceYearsMin} Years
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* 4. Compensation & Pay Scale */}
          {currentJob.salary && (
            <div className="space-y-3">
              <h4 className="text-xs font-mono uppercase tracking-wider text-ink-muted font-semibold flex items-center gap-1.5">
                <DollarSign className="w-3.5 h-3.5 text-signal-emerald" />
                Compensation & Benefits
              </h4>
              <div className="bg-dark-950/40 border border-white/[0.06] rounded-xl p-4 text-xs font-mono flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
                <div>
                  <span className="text-ink-muted block text-[11px]">Salary / Honorarium:</span>
                  <span className="text-emerald-400 font-semibold text-sm">
                    {currentJob.salary}
                  </span>
                </div>
                {currentJob.payLevel && (
                  <div className="px-3 py-1 rounded bg-white/[0.04] border border-white/10 text-ink-secondary">
                    Pay Level: {currentJob.payLevel}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* 5. Important Dates */}
          <div className="space-y-3">
            <h4 className="text-xs font-mono uppercase tracking-wider text-ink-muted font-semibold flex items-center gap-1.5">
              <Calendar className="w-3.5 h-3.5 text-signal-emerald" />
              Recruitment Timeline
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="bg-dark-950/40 border border-white/[0.06] rounded-xl p-3">
                <span className="text-[10px] font-mono text-ink-muted uppercase block mb-1">
                  Application Start Date
                </span>
                <span className="text-xs font-mono text-ink-primary font-medium">
                  {formatDateTime(currentJob.applicationStartDate)}
                </span>
              </div>
              <div className="bg-dark-950/40 border border-amber-500/20 rounded-xl p-3">
                <span className="text-[10px] font-mono text-amber-400 uppercase block mb-1">
                  Application Last Date
                </span>
                <span className="text-xs font-mono text-amber-300 font-bold">
                  {formatDateTime(currentJob.applicationLastDate)}
                </span>
              </div>
              <div className="bg-dark-950/40 border border-white/[0.06] rounded-xl p-3">
                <span className="text-[10px] font-mono text-ink-muted uppercase block mb-1">
                  Exam / Interview Date
                </span>
                <span className="text-xs font-mono text-ink-primary font-medium">
                  {currentJob.examDate
                    ? formatDateTime(currentJob.examDate)
                    : currentJob.interviewDate
                    ? formatDateTime(currentJob.interviewDate)
                    : 'To be notified'}
                </span>
              </div>
            </div>
          </div>

          {/* 6. Corrigenda & Amendments History (Section 22) */}
          {currentJob.corrigenda && currentJob.corrigenda.length > 0 && (
            <div className="space-y-3">
              <h4 className="text-xs font-mono uppercase tracking-wider text-purple-400 font-semibold flex items-center gap-1.5">
                <RefreshCw className="w-3.5 h-3.5" />
                Corrigenda & Official Amendments ({currentJob.corrigenda.length})
              </h4>
              <div className="space-y-2">
                {currentJob.corrigenda.map((c, idx) => (
                  <div
                    key={idx}
                    className="p-3.5 rounded-xl bg-purple-500/10 border border-purple-500/25 text-xs font-mono space-y-1"
                  >
                    <div className="flex items-center justify-between text-purple-300 font-semibold">
                      <span>[{c.noticeType}] {c.title}</span>
                      <span className="text-[10px] text-purple-400/80">{formatDateTime(c.issueDate)}</span>
                    </div>
                    {c.description && (
                      <p className="text-ink-secondary text-[11px] leading-relaxed">
                        {c.description}
                      </p>
                    )}
                    {c.revisedLastDate && (
                      <div className="text-emerald-400 text-[11px]">
                        Revised Application Deadline: {formatDateTime(c.revisedLastDate)}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 7. Zero-Fabrication Field Evidence Citations (Section 29 & 30) */}
          {currentJob.evidenceList && currentJob.evidenceList.length > 0 && (
            <div className="space-y-3">
              <h4 className="text-xs font-mono uppercase tracking-wider text-ink-muted font-semibold flex items-center gap-1.5">
                <FileCheck2 className="w-3.5 h-3.5 text-signal-emerald" />
                Notification Grounding & Field Evidence (Zero Fabrication Guarantee)
              </h4>
              <div className="bg-dark-950/60 border border-white/[0.06] rounded-xl p-3 space-y-2 text-xs font-mono">
                {currentJob.evidenceList.map((ev, idx) => (
                  <div
                    key={idx}
                    className="p-2.5 rounded bg-white/[0.02] border border-white/[0.04] space-y-1"
                  >
                    <div className="flex items-center justify-between text-[11px]">
                      <span className="text-signal-emerald font-semibold uppercase">
                        {ev.fieldName}: {ev.fieldValue}
                      </span>
                      <span className="text-ink-muted text-[10px]">
                        {ev.sourceDocument} • {ev.pageOrSection}
                      </span>
                    </div>
                    {ev.excerpt && (
                      <p className="text-ink-muted italic text-[11px] bg-dark-900/60 p-1.5 rounded border border-white/[0.02]">
                        "{ev.excerpt}"
                      </p>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Modal Bottom Actions Bar */}
        <div className="px-6 py-4 border-t border-white/[0.08] bg-dark-950/80 flex flex-col sm:flex-row items-center justify-between gap-3 sticky bottom-0 z-10">
          <div className="text-xs font-mono text-ink-muted flex items-center gap-2">
            <span>Authority:</span>
            <span className="text-ink-primary font-medium">{currentJob.authority || currentJob.organization}</span>
          </div>

          <div className="flex items-center gap-2.5 w-full sm:w-auto">
            {currentJob.notificationUrl && (
              <a
                href={currentJob.notificationUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl text-xs font-mono bg-white/[0.06] hover:bg-white/[0.1] text-ink-primary border border-white/10 transition"
              >
                <FileText className="w-4 h-4 text-blue-400" />
                <span>View Official Notification PDF</span>
              </a>
            )}

            {currentJob.applicationUrl && (
              <a
                href={currentJob.applicationUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl text-xs font-mono bg-signal-emerald hover:bg-emerald-400 text-dark-950 font-bold transition shadow-lg shadow-signal-emerald/20"
              >
                <span>APPLY ON OFFICIAL WEBSITE</span>
                <ExternalLink className="w-4 h-4" />
              </a>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
