import React from 'react';
import { Search, MapPin, Briefcase, Filter, X } from 'lucide-react';

interface JobFiltersProps {
  role: string;
  setRole: (r: string) => void;
  location: string;
  setLocation: (l: string) => void;
  workMode: string;
  setWorkMode: (m: string) => void;
  technology: string;
  setTechnology: (t: string) => void;
  source: string;
  setSource: (s: string) => void;
  onReset: () => void;
}

export const JobFilters: React.FC<JobFiltersProps> = ({
  role,
  setRole,
  location,
  setLocation,
  workMode,
  setWorkMode,
  technology,
  setTechnology,
  source,
  setSource,
  onReset,
}) => {
  const hasActiveFilters = Boolean(role || location || workMode || technology || source);

  return (
    <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-slate-400">
          <Filter className="w-3.5 h-3.5 text-emerald-400" />
          <span>Filter Discovered Jobs</span>
        </div>
        {hasActiveFilters && (
          <button
            onClick={onReset}
            className="text-xs text-rose-400 hover:text-rose-300 flex items-center gap-1 transition"
          >
            <X className="w-3.5 h-3.5" />
            <span>Reset Filters</span>
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
        {/* Role search */}
        <div className="relative">
          <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-500" />
          <input
            type="text"
            value={role}
            onChange={(e) => setRole(e.target.value)}
            placeholder="Search role..."
            className="w-full pl-9 pr-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500"
          />
        </div>

        {/* Location search */}
        <div className="relative">
          <MapPin className="w-4 h-4 absolute left-3 top-2.5 text-slate-500" />
          <input
            type="text"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            placeholder="Location (e.g. Bangalore)..."
            className="w-full pl-9 pr-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500"
          />
        </div>

        {/* Work mode */}
        <div>
          <select
            value={workMode}
            onChange={(e) => setWorkMode(e.target.value)}
            className="w-full px-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-300 focus:outline-none focus:border-emerald-500"
          >
            <option value="">All Work Modes</option>
            <option value="REMOTE">Remote</option>
            <option value="HYBRID">Hybrid</option>
            <option value="ON_SITE">On-Site</option>
          </select>
        </div>

        {/* Technology filter */}
        <div className="relative">
          <Briefcase className="w-4 h-4 absolute left-3 top-2.5 text-slate-500" />
          <input
            type="text"
            value={technology}
            onChange={(e) => setTechnology(e.target.value)}
            placeholder="Skill (e.g. Java, Spring)..."
            className="w-full pl-9 pr-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500"
          />
        </div>

        {/* Source ATS */}
        <div>
          <select
            value={source}
            onChange={(e) => setSource(e.target.value)}
            className="w-full px-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-300 focus:outline-none focus:border-emerald-500"
          >
            <option value="">All ATS Sources</option>
            <option value="FIRECRAWL_GREENHOUSE">Greenhouse</option>
            <option value="FIRECRAWL_LEVER">Lever</option>
            <option value="FIRECRAWL_ASHBY">Ashby</option>
            <option value="FIRECRAWL_WORKDAY">Workday</option>
          </select>
        </div>
      </div>
    </div>
  );
};
