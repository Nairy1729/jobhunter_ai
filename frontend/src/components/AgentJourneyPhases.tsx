import React from 'react';
import { Compass, FileSearch, CheckCircle2, TrendingUp, FileText } from 'lucide-react';

interface AgentJourneyPhasesProps {
  currentStep?: number; // 0: Idle, 1..6: Active search steps
  isDiscovering?: boolean;
}

export const AgentJourneyPhases: React.FC<AgentJourneyPhasesProps> = ({
  currentStep = 0,
  isDiscovering = false,
}) => {
  const phases = [
    {
      num: '01',
      tag: 'DISCOVER',
      label: 'SEARCHING 47 SOURCES',
      desc: 'Autonomous crawler queries ATS domains (Greenhouse, Lever, Ashby, Workday).',
      icon: Compass,
      activeStepThreshold: 2,
    },
    {
      num: '02',
      tag: 'UNDERSTAND',
      label: 'ANALYZING REQUIREMENTS',
      desc: 'Parses raw markdown into structured qualification, responsibility, and domain specs.',
      icon: FileSearch,
      activeStepThreshold: 3,
    },
    {
      num: '03',
      tag: 'MATCH',
      label: 'COMPARING EVIDENCE',
      desc: 'Grounding Verification Gate tests claims against verified commercial & project record.',
      icon: CheckCircle2,
      activeStepThreshold: 4,
    },
    {
      num: '04',
      tag: 'PRIORITIZE',
      label: 'RANKING RELEVANCE',
      desc: 'Evaluates hard constraints, seniority deltas, and freshness decay into prioritized tiers.',
      icon: TrendingUp,
      activeStepThreshold: 5,
    },
    {
      num: '05',
      tag: 'PREPARE',
      label: 'TAILORING APPLICATION',
      desc: 'Synthesizes The Edge advantage report and generates ATS-tailored resume with zero hallucination.',
      icon: FileText,
      activeStepThreshold: 6,
    },
  ];

  return (
    <section className="relative w-full py-12 border-y border-white/[0.06] bg-dark-900/40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header with technical label */}
        <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-8 gap-4">
          <div>
            <div className="font-mono text-[11px] text-signal-emerald uppercase tracking-widest mb-1.5 flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald" />
              <span>THE AGENT JOURNEY</span>
            </div>
            <h2 className="text-xl sm:text-2xl font-light tracking-tight text-ink-primary">
              Five-Phase Intelligence Pipeline
            </h2>
          </div>
          <div className="font-mono text-[10px] text-ink-muted uppercase tracking-widest">
            {isDiscovering ? (
              <span className="text-signal-emerald flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald animate-ping" />
                PIPELINE RUNNING · PHASE {Math.min(currentStep, 5)} OF 5
              </span>
            ) : (
              <span>STANDBY · READY TO DEPLOY</span>
            )}
          </div>
        </div>

        {/* 5-Phase Horizontal Editorial Layout */}
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4 lg:gap-6">
          {phases.map((p, idx) => {
            const isActive = isDiscovering && currentStep >= p.activeStepThreshold;
            const isCurrent = isDiscovering && (
              (idx === 0 && currentStep <= 2) ||
              (idx === 1 && currentStep === 3) ||
              (idx === 2 && currentStep === 4) ||
              (idx === 3 && currentStep === 5) ||
              (idx === 4 && currentStep >= 6)
            );

            return (
              <div
                key={p.num}
                className={`relative p-5 rounded-xl border transition-all duration-300 flex flex-col justify-between ${
                  isCurrent
                    ? 'bg-dark-850 border-signal-emerald/40 shadow-lg shadow-signal-emerald/5'
                    : isActive
                    ? 'bg-dark-900/80 border-white/10'
                    : 'bg-dark-950/40 border-white/[0.04] hover:border-white/10'
                }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <span className="font-mono text-sm font-bold text-ink-muted">
                      {p.num}
                    </span>
                    <span
                      className={`text-[9px] font-mono tracking-widest px-2 py-0.5 rounded ${
                        isCurrent
                          ? 'bg-signal-emerald/20 text-signal-emerald border border-signal-emerald/30 font-semibold'
                          : 'bg-white/[0.04] text-ink-muted border border-white/[0.05]'
                      }`}
                    >
                      {p.tag}
                    </span>
                  </div>

                  <h3 className="font-mono text-xs font-semibold text-ink-primary tracking-wide mb-2 uppercase">
                    {p.label}
                  </h3>

                  <p className="text-xs text-ink-secondary leading-relaxed font-light">
                    {p.desc}
                  </p>
                </div>

                {/* Bottom Status Hairline */}
                <div className="mt-5 pt-3 border-t border-white/[0.06] flex items-center justify-between">
                  <span className="text-[10px] font-mono text-ink-muted">
                    {isCurrent ? 'PROCESSING...' : isActive ? 'COMPLETED' : 'AWAITING'}
                  </span>
                  <p.icon
                    className={`w-3.5 h-3.5 ${
                      isCurrent
                        ? 'text-signal-emerald animate-pulse'
                        : isActive
                        ? 'text-ink-secondary'
                        : 'text-ink-faint'
                    }`}
                  />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
