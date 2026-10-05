import React, { useState } from 'react';
import { CandidateProfile } from '../types';
import { CandidateProfileEditor } from './CandidateProfileEditor';
import { GovernmentProfileEditor } from './government/GovernmentProfileEditor';
import { UserCheck, ShieldCheck, FileText } from 'lucide-react';

interface CandidateProfileHubProps {
  profile: CandidateProfile;
  onRefresh: () => void;
  onOpenUpload: () => void;
  initialSubTab?: 'private' | 'government';
}

export const CandidateProfileHub: React.FC<CandidateProfileHubProps> = ({
  profile,
  onRefresh,
  onOpenUpload,
  initialSubTab = 'private',
}) => {
  const [activeSubTab, setActiveSubTab] = useState<'private' | 'government'>(initialSubTab);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/[0.08] pb-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/[0.04] border border-white/[0.08] text-signal-emerald font-mono text-[10px] tracking-widest uppercase mb-2">
            <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald" />
            <span>PROFILE & ELIGIBILITY SYSTEM</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-light tracking-tight text-ink-primary">
            Candidate Profile Hub
          </h1>
          <p className="text-xs sm:text-sm text-ink-secondary mt-1 font-light">
            Manage your verified master resume facts for private ATS matching and official eligibility attributes for government recruitment.
          </p>
        </div>

        {/* Master Resume Quick CTA if on private tab */}
        {activeSubTab === 'private' && (
          <button
            onClick={onOpenUpload}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-signal-emerald/10 hover:bg-signal-emerald/20 text-signal-emerald border border-signal-emerald/30 font-mono text-xs font-semibold tracking-wider transition self-start sm:self-auto shadow-sm"
          >
            <FileText className="w-3.5 h-3.5" />
            <span>UPDATE MASTER RESUME (PDF)</span>
          </button>
        )}
      </div>

      {/* Profile Switcher Tabs */}
      <div className="flex items-center gap-2 p-1 bg-dark-900/60 border border-white/[0.06] rounded-xl max-w-md">
        <button
          onClick={() => setActiveSubTab('private')}
          className={`flex-1 py-2 px-3 rounded-lg text-xs font-mono tracking-wider flex items-center justify-center gap-2 transition ${
            activeSubTab === 'private'
              ? 'bg-white/[0.08] text-ink-primary border border-white/10 shadow-sm font-semibold'
              : 'text-ink-secondary hover:text-ink-primary hover:bg-white/[0.02]'
          }`}
        >
          <UserCheck className={`w-3.5 h-3.5 ${activeSubTab === 'private' ? 'text-signal-emerald' : ''}`} />
          <span>CAREER & RESUME</span>
        </button>

        <button
          onClick={() => setActiveSubTab('government')}
          className={`flex-1 py-2 px-3 rounded-lg text-xs font-mono tracking-wider flex items-center justify-center gap-2 transition ${
            activeSubTab === 'government'
              ? 'bg-signal-emerald/15 text-signal-emerald border border-signal-emerald/30 shadow-sm font-semibold'
              : 'text-ink-secondary hover:text-ink-primary hover:bg-white/[0.02]'
          }`}
        >
          <ShieldCheck className="w-3.5 h-3.5" />
          <span>GOV ELIGIBILITY</span>
        </button>
      </div>

      {/* Content View */}
      {activeSubTab === 'private' ? (
        <div className="animate-in fade-in duration-200">
          <CandidateProfileEditor
            profile={profile}
            onRefresh={onRefresh}
            onOpenUpload={onOpenUpload}
          />
        </div>
      ) : (
        <div className="animate-in fade-in duration-200">
          <GovernmentProfileEditor />
        </div>
      )}
    </div>
  );
};
