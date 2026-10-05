import React, { useEffect, useState } from 'react';
import { GovernmentCoverageMetrics } from '../../types';
import { apiClient } from '../../api/client';
import {
  Server,
  Building2,
  Layers,
  RefreshCw,
  Loader2,
} from 'lucide-react';

export const GovernmentCoverageDashboard: React.FC = () => {
  const [metrics, setMetrics] = useState<GovernmentCoverageMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchMetrics = async () => {
    try {
      const data = await apiClient.government.getCoverage();
      setMetrics(data);
    } catch (err) {
      console.error('Failed to load coverage metrics', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchMetrics();
  }, []);

  const handleRefresh = () => {
    setRefreshing(true);
    fetchMetrics();
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-3 font-mono text-xs text-ink-muted">
        <Loader2 className="w-6 h-6 text-signal-emerald animate-spin" />
        <span>QUERYING OFFICIAL SOURCE REGISTRY METRICS...</span>
      </div>
    );
  }

  if (!metrics) {
    return (
      <div className="text-center py-16 text-xs font-mono text-rose-400">
        Unable to load Government Source Registry telemetry from database.
      </div>
    );
  }

  const formatTimestamp = (ts?: string) => {
    if (!ts) return 'Never';
    try {
      return new Date(ts).toLocaleString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return ts;
    }
  };

  const sourceCategories = [
    { label: 'Central Government & Commissions (UPSC/SSC/RRB)', count: metrics.centralGovernment, color: 'text-blue-400' },
    { label: 'State Governments & PSCs (UPPSC, MPSC, etc.)', count: metrics.stateGovernment, color: 'text-indigo-400' },
    { label: 'District Administrations (NIC Portals)', count: metrics.districtAdministration, color: 'text-emerald-400' },
    { label: 'Municipal Corporations (BMC, MCD, etc.)', count: metrics.municipal, color: 'text-cyan-400' },
    { label: 'Panchayati Raj & Rural Development', count: metrics.panchayat, color: 'text-amber-400' },
    { label: 'Health Missions & Hospitals (NHM, AIIMS)', count: metrics.health, color: 'text-rose-400' },
    { label: 'Women & Child Development (Anganwadi / ICDS)', count: metrics.womenChildDevelopment, color: 'text-pink-400' },
    { label: 'State & Central PSUs (BHEL, ONGC, etc.)', count: metrics.psus, color: 'text-teal-400' },
    { label: 'Government Universities & Colleges', count: metrics.universities, color: 'text-purple-400' },
    { label: 'Education Boards & Departments', count: metrics.education, color: 'text-yellow-400' },
    { label: 'Ministries & Specialized Departments', count: metrics.departments, color: 'text-orange-400' },
    { label: 'Other Missions, Schemes & Boards', count: metrics.other, color: 'text-gray-400' },
  ];

  return (
    <div className="max-w-6xl mx-auto py-6 space-y-6">
      {/* Title & Philosophy Banner */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-signal-emerald/10 border border-signal-emerald/20 text-signal-emerald font-mono text-xs mb-2">
            <Server className="w-3.5 h-3.5" />
            <span>SOURCE REGISTRY TELEMETRY (LIVE DATABASE METRICS)</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-semibold text-ink-primary">
            Government Source Coverage Dashboard
          </h1>
          <p className="text-xs sm:text-sm text-ink-secondary mt-1">
            Real-time coverage metrics across India's administrative hierarchy. Zero hardcoded or marketing numbers.
          </p>
        </div>

        <button
          onClick={handleRefresh}
          disabled={refreshing}
          className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-white/[0.06] hover:bg-white/[0.1] text-xs font-mono text-ink-primary border border-white/10 transition"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin text-signal-emerald' : ''}`} />
          <span>Refresh Telemetry</span>
        </button>
      </div>

      {/* Top 4 Metric KPI Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-dark-900/80 border border-white/[0.08] rounded-xl p-4">
          <span className="text-[11px] font-mono text-ink-muted uppercase tracking-wider block mb-1">
            Total Monitored Sources
          </span>
          <span className="text-2xl sm:text-3xl font-bold font-mono text-ink-primary">
            {metrics.totalSources}
          </span>
          <span className="text-[10px] text-ink-muted block mt-1 font-mono">
            Across Central, State & District tiers
          </span>
        </div>

        <div className="bg-dark-900/80 border border-emerald-500/25 rounded-xl p-4">
          <span className="text-[11px] font-mono text-emerald-400 uppercase tracking-wider block mb-1">
            Active Sources
          </span>
          <span className="text-2xl sm:text-3xl font-bold font-mono text-emerald-400">
            {metrics.activeSources}
          </span>
          <span className="text-[10px] text-ink-muted block mt-1 font-mono">
            Healthy crawler heartbeats
          </span>
        </div>

        <div className="bg-dark-900/80 border border-white/[0.08] rounded-xl p-4">
          <span className="text-[11px] font-mono text-ink-muted uppercase tracking-wider block mb-1">
            Verified Official Jobs
          </span>
          <span className="text-2xl sm:text-3xl font-bold font-mono text-signal-emerald">
            {metrics.verifiedOfficialJobs}
          </span>
          <span className="text-[10px] text-ink-muted block mt-1 font-mono">
            Out of {metrics.totalJobs} total discovered jobs
          </span>
        </div>

        <div className="bg-dark-900/80 border border-white/[0.08] rounded-xl p-4">
          <span className="text-[11px] font-mono text-ink-muted uppercase tracking-wider block mb-1">
            Last Successful Crawl
          </span>
          <span className="text-xs sm:text-sm font-semibold font-mono text-amber-300 block truncate mt-1">
            {formatTimestamp(metrics.lastSuccessfulCrawl)}
          </span>
          <span className="text-[10px] text-ink-muted block mt-1 font-mono">
            Failed sources: {metrics.failedSources}
          </span>
        </div>
      </div>

      {/* Breakdown by Administrative Hierarchy (Section 33 Table) */}
      <div className="bg-dark-900/80 border border-white/[0.08] rounded-xl p-5 sm:p-6 shadow-sm">
        <h3 className="text-sm font-semibold uppercase font-mono tracking-wider text-ink-primary flex items-center gap-2 mb-4 pb-2 border-b border-white/[0.06]">
          <Building2 className="w-4 h-4 text-signal-emerald" />
          Government Sources Registry by Administrative Tier
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {sourceCategories.map((cat, idx) => (
            <div
              key={idx}
              className="flex items-center justify-between p-3 rounded-lg bg-dark-950/50 border border-white/[0.04] text-xs font-mono"
            >
              <span className="text-ink-secondary">{cat.label}</span>
              <span className={`font-bold ${cat.color}`}>{cat.count}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Employment Type Distribution in Current Database */}
      <div className="bg-dark-900/80 border border-white/[0.08] rounded-xl p-5 sm:p-6 shadow-sm">
        <h3 className="text-sm font-semibold uppercase font-mono tracking-wider text-ink-primary flex items-center gap-2 mb-4 pb-2 border-b border-white/[0.06]">
          <Layers className="w-4 h-4 text-signal-emerald" />
          Recruitment Opportunities by Appointment Category
        </h3>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs font-mono">
          <div className="p-3 rounded-lg bg-dark-950/50 border border-white/[0.04]">
            <span className="text-ink-muted block text-[10px] uppercase">Regular / Permanent</span>
            <span className="text-lg font-bold text-ink-primary mt-1 block">
              {metrics.employmentTypeBreakdown['REGULAR'] || metrics.employmentTypeBreakdown['PERMANENT'] || 0}
            </span>
          </div>

          <div className="p-3 rounded-lg bg-dark-950/50 border border-amber-500/20">
            <span className="text-amber-400 block text-[10px] uppercase">Samvida & Contractual</span>
            <span className="text-lg font-bold text-amber-300 mt-1 block">
              {metrics.contractualSamvidaJobs}
            </span>
          </div>

          <div className="p-3 rounded-lg bg-dark-950/50 border border-pink-500/20">
            <span className="text-pink-400 block text-[10px] uppercase">Honorarium (Anganwadi)</span>
            <span className="text-lg font-bold text-pink-300 mt-1 block">
              {metrics.employmentTypeBreakdown['HONORARIUM'] || 0}
            </span>
          </div>

          <div className="p-3 rounded-lg bg-dark-950/50 border border-blue-500/20">
            <span className="text-blue-400 block text-[10px] uppercase">Local / Panchayat / Block</span>
            <span className="text-lg font-bold text-blue-300 mt-1 block">
              {metrics.smallLocalJobs}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
