import React, { useState } from 'react';
import { Search, Compass, SlidersHorizontal, X, Loader2 } from 'lucide-react';

interface MinimalSearchInterfaceProps {
  role: string;
  onRoleChange: (val: string) => void;
  location: string;
  onLocationChange: (val: string) => void;
  workMode: string;
  onWorkModeChange: (val: string) => void;
  priorityFilter: string;
  onPriorityFilterChange: (val: string) => void;
  freshnessFilter: string;
  onFreshnessFilterChange: (val: string) => void;
  sortBy: string;
  onSortByChange: (val: string) => void;
  onDiscover: () => void;
  isDiscovering: boolean;
  onReset: () => void;
  totalJobs: number;
}

export const MinimalSearchInterface: React.FC<MinimalSearchInterfaceProps> = ({
  role,
  onRoleChange,
  location,
  onLocationChange,
  workMode,
  onWorkModeChange,
  priorityFilter,
  onPriorityFilterChange,
  freshnessFilter,
  onFreshnessFilterChange,
  sortBy,
  onSortByChange,
  onDiscover,
  isDiscovering,
  onReset,
  totalJobs,
}) => {
  const [showConstraints, setShowConstraints] = useState(false);

  const quickPills = [
    { label: 'Bangalore', type: 'loc', val: 'Bangalore' },
    { label: 'Remote', type: 'mode', val: 'REMOTE' },
    { label: 'Hybrid', type: 'mode', val: 'HYBRID' },
  ];

  return (
    <div className="w-full max-w-4xl mx-auto my-8">
      {/* Search Container */}
      <div className="relative rounded-2xl bg-dark-900 border border-white/10 shadow-2xl p-4 sm:p-6 backdrop-blur-xl">
        <div className="flex items-center justify-between mb-3">
          <span className="font-mono text-[10px] text-ink-muted uppercase tracking-widest flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald" />
            <span>FIND YOUR NEXT ROLE</span>
          </span>
          <span className="font-mono text-[10px] text-ink-muted">
            {totalJobs} ACTIVE SPECIMENS
          </span>
        </div>

        {/* Minimal Search Bar */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-ink-muted" />
            <input
              type="text"
              value={role}
              onChange={(e) => onRoleChange(e.target.value)}
              placeholder="Java / Spring Boot / Backend Engineer..."
              className="w-full pl-10 pr-4 py-3 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary text-sm font-sans placeholder:text-ink-muted/60 transition"
            />
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowConstraints(!showConstraints)}
              className={`px-3 py-3 rounded-xl border text-xs font-mono tracking-wide transition flex items-center gap-1.5 ${
                showConstraints || priorityFilter !== 'ALL' || freshnessFilter !== 'ALL'
                  ? 'bg-white/10 border-white/20 text-ink-primary'
                  : 'bg-dark-950 border-white/[0.08] text-ink-muted hover:text-ink-primary'
              }`}
              title="Toggle constraints & filters"
            >
              <SlidersHorizontal className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">CONSTRAINTS</span>
            </button>

            <button
              onClick={onDiscover}
              disabled={isDiscovering}
              className="px-6 py-3 rounded-xl bg-signal-emerald hover:bg-signal-emerald/90 disabled:opacity-50 text-dark-950 font-mono font-bold text-xs tracking-wider transition-all duration-200 flex items-center justify-center gap-2 shadow-lg shadow-signal-emerald/20 flex-shrink-0"
            >
              {isDiscovering ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin text-dark-950" />
                  <span>SEARCHING...</span>
                </>
              ) : (
                <>
                  <Compass className="w-4 h-4 text-dark-950" />
                  <span>DISCOVER</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* Quick Target Chips */}
        <div className="mt-4 flex flex-wrap items-center justify-between gap-2 pt-3 border-t border-white/[0.04]">
          <div className="flex flex-wrap items-center gap-1.5">
            <span className="text-[10px] font-mono text-ink-muted mr-1">QUICK:</span>
            {quickPills.map((p) => {
              const isSelected =
                (p.type === 'loc' && location === p.val) ||
                (p.type === 'mode' && workMode === p.val);

              return (
                <button
                  key={p.label}
                  onClick={() => {
                    if (p.type === 'loc') {
                      onLocationChange(location === p.val ? '' : p.val);
                    } else {
                      onWorkModeChange(workMode === p.val ? '' : p.val);
                    }
                  }}
                  className={`px-2.5 py-1 rounded-md text-[11px] font-mono tracking-wide transition border ${
                    isSelected
                      ? 'bg-signal-emerald/15 text-signal-emerald border-signal-emerald/30'
                      : 'bg-dark-950/60 text-ink-secondary border-white/[0.06] hover:border-white/15'
                  }`}
                >
                  {p.label}
                </button>
              );
            })}
          </div>

          {(role || location || workMode || priorityFilter !== 'ALL' || freshnessFilter !== 'ALL') && (
            <button
              onClick={onReset}
              className="text-[10px] font-mono text-ink-muted hover:text-ink-primary flex items-center gap-1 transition"
            >
              <X className="w-3 h-3" />
              <span>CLEAR FILTERS</span>
            </button>
          )}
        </div>

        {/* Expandable Advanced Constraints Panel */}
        {showConstraints && (
          <div className="mt-4 pt-4 border-t border-white/[0.06] grid grid-cols-1 sm:grid-cols-3 gap-3 animate-in fade-in duration-200">
            <div>
              <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">
                PRIORITY TIER
              </label>
              <select
                value={priorityFilter}
                onChange={(e) => onPriorityFilterChange(e.target.value)}
                className="w-full px-3 py-2 rounded-lg bg-dark-950 border border-white/[0.08] text-xs font-mono text-ink-primary focus:outline-none focus:border-signal-emerald/60"
              >
                <option value="ALL">All Priorities</option>
                <option value="HIGH_PRIORITY">High Priority (Top Matches)</option>
                <option value="MEDIUM_PRIORITY">Medium Priority (Stretch)</option>
                <option value="LOW_PRIORITY">Low Priority</option>
                <option value="NOT_RECOMMENDED">Not Recommended</option>
              </select>
            </div>

            <div>
              <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">
                FRESHNESS
              </label>
              <select
                value={freshnessFilter}
                onChange={(e) => onFreshnessFilterChange(e.target.value)}
                className="w-full px-3 py-2 rounded-lg bg-dark-950 border border-white/[0.08] text-xs font-mono text-ink-primary focus:outline-none focus:border-signal-emerald/60"
              >
                <option value="ALL">All Freshness Profiles</option>
                <option value="NEW">New (&lt; 7 Days)</option>
                <option value="RECENT">Recent (&lt; 21 Days)</option>
                <option value="OLDER">Older (&lt; 60 Days)</option>
              </select>
            </div>

            <div>
              <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">
                SORT RANKING
              </label>
              <select
                value={sortBy}
                onChange={(e) => onSortByChange(e.target.value)}
                className="w-full px-3 py-2 rounded-lg bg-dark-950 border border-white/[0.08] text-xs font-mono text-ink-primary focus:outline-none focus:border-signal-emerald/60"
              >
                <option value="RECOMMENDED">AI Intelligence Rank</option>
                <option value="PRIORITY_SCORE">Priority Score (High $\rightarrow$ Low)</option>
                <option value="POSTING_DATE">Posting Recency (Newest First)</option>
              </select>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
