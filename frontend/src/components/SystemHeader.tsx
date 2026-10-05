import React, { useState } from 'react';
import { User } from '../types';
import { Briefcase, Building2, CheckSquare, UserCheck, LogOut, LogIn, Menu, X } from 'lucide-react';

export type AppTab = 'jobs' | 'gov-jobs' | 'applications' | 'profile' | 'landing';

interface SystemHeaderProps {
  user: User | null;
  onLogout: () => void;
  activeTab: AppTab;
  onSelectTab: (tab: AppTab) => void;
  isSearching?: boolean;
  onOpenAuth?: () => void;
}

export const SystemHeader: React.FC<SystemHeaderProps> = ({
  user,
  onLogout,
  activeTab,
  onSelectTab,
  isSearching = false,
  onOpenAuth,
}) => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleTabSelect = (tab: AppTab) => {
    onSelectTab(tab);
    setMobileMenuOpen(false);
  };

  return (
    <header className="sticky top-0 z-50 w-full backdrop-blur-xl bg-dark-950/85 border-b border-white/[0.06] transition-all duration-300">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand System Identifier */}
        <div
          className="flex items-center space-x-3 cursor-pointer group"
          onClick={() => handleTabSelect('landing')}
          role="button"
          tabIndex={0}
          aria-label="JobHunter Home"
        >
          <div className="w-8 h-8 rounded-lg bg-dark-900 border border-white/10 flex items-center justify-center text-signal-emerald shadow-inner group-hover:border-signal-emerald/40 transition">
            <span className="font-mono text-xs font-bold tracking-tighter">JH</span>
          </div>
          <div className="flex flex-col">
            <div className="flex items-center gap-2">
              <span className="text-sm font-semibold tracking-tight text-ink-primary font-mono group-hover:text-signal-emerald transition">
                JOBHUNTER
              </span>
              <span className="text-[10px] font-mono text-signal-emerald uppercase tracking-widest hidden sm:inline-block">
                // INDIA & GLOBAL
              </span>
            </div>
          </div>
        </div>

        {/* Desktop Navigation Controls: Strict 4-item IA */}
        <nav className="hidden md:flex items-center space-x-1.5 py-1" aria-label="Main Navigation">
          {/* 1. Private Jobs Discovery */}
          <button
            onClick={() => handleTabSelect('jobs')}
            className={`px-3.5 py-2 rounded-xl text-xs font-mono tracking-wide transition-all duration-200 flex items-center gap-2 min-h-[40px] ${
              activeTab === 'jobs'
                ? 'bg-white/[0.08] text-ink-primary border border-white/15 shadow-sm font-semibold'
                : 'text-ink-secondary hover:text-ink-primary hover:bg-white/[0.03]'
            }`}
          >
            <Briefcase className={`w-3.5 h-3.5 ${activeTab === 'jobs' ? 'text-signal-emerald' : 'text-ink-muted'}`} />
            <span>JOBS</span>
            {isSearching && (
              <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald animate-ping" />
            )}
          </button>

          {/* 2. Government Jobs Discovery */}
          <button
            onClick={() => handleTabSelect('gov-jobs')}
            className={`px-3.5 py-2 rounded-xl text-xs font-mono tracking-wide transition-all duration-200 flex items-center gap-2 min-h-[40px] ${
              activeTab === 'gov-jobs'
                ? 'bg-signal-emerald/15 text-signal-emerald border border-signal-emerald/40 font-semibold shadow-sm'
                : 'text-ink-secondary hover:text-ink-primary hover:bg-white/[0.03]'
            }`}
          >
            <Building2 className={`w-3.5 h-3.5 ${activeTab === 'gov-jobs' ? 'text-signal-emerald' : 'text-ink-muted'}`} />
            <span>GOVERNMENT JOBS</span>
            <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald" />
          </button>

          {/* 3. Applications Tracker */}
          <button
            onClick={() => handleTabSelect('applications')}
            className={`px-3.5 py-2 rounded-xl text-xs font-mono tracking-wide transition-all duration-200 flex items-center gap-2 min-h-[40px] ${
              activeTab === 'applications'
                ? 'bg-white/[0.08] text-ink-primary border border-white/15 shadow-sm font-semibold'
                : 'text-ink-secondary hover:text-ink-primary hover:bg-white/[0.03]'
            }`}
          >
            <CheckSquare className={`w-3.5 h-3.5 ${activeTab === 'applications' ? 'text-signal-emerald' : 'text-ink-muted'}`} />
            <span>APPLICATIONS</span>
          </button>

          {/* 4. Unified Profile Hub */}
          <button
            onClick={() => handleTabSelect('profile')}
            className={`px-3.5 py-2 rounded-xl text-xs font-mono tracking-wide transition-all duration-200 flex items-center gap-2 min-h-[40px] ${
              activeTab === 'profile'
                ? 'bg-white/[0.08] text-ink-primary border border-white/15 shadow-sm font-semibold'
                : 'text-ink-secondary hover:text-ink-primary hover:bg-white/[0.03]'
            }`}
          >
            <UserCheck className={`w-3.5 h-3.5 ${activeTab === 'profile' ? 'text-signal-emerald' : 'text-ink-muted'}`} />
            <span>PROFILE</span>
          </button>
        </nav>

        {/* Technical Status & Auth Controls */}
        <div className="hidden md:flex items-center space-x-3">
          {user ? (
            <div className="flex items-center space-x-3">
              <div className="flex items-center space-x-2 px-2.5 py-1.5 rounded-lg border border-white/[0.06] bg-dark-900/60 font-mono text-[11px] text-ink-secondary">
                <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald" />
                <span className="truncate max-w-[140px]">{user.email}</span>
              </div>
              <button
                onClick={onLogout}
                title="Disconnect session"
                aria-label="Log out"
                className="p-2 text-ink-muted hover:text-ink-primary hover:bg-white/5 rounded-lg transition min-h-[40px] min-w-[40px] flex items-center justify-center"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <div className="flex items-center space-x-2">
              <button
                onClick={onOpenAuth}
                className="px-4 py-2 rounded-xl bg-signal-emerald/10 hover:bg-signal-emerald/20 text-signal-emerald border border-signal-emerald/30 font-mono text-xs tracking-wider transition flex items-center gap-2 shadow-sm min-h-[40px]"
              >
                <LogIn className="w-3.5 h-3.5" />
                <span>SIGN IN</span>
              </button>
            </div>
          )}
        </div>

        {/* Mobile Menu Toggle Button (>= 44px touch target) */}
        <div className="md:hidden flex items-center gap-2">
          {user ? (
            <button
              onClick={onLogout}
              title="Log out"
              aria-label="Log out"
              className="p-2.5 text-ink-muted hover:text-ink-primary rounded-lg transition min-w-[44px] min-h-[44px] flex items-center justify-center"
            >
              <LogOut className="w-4 h-4" />
            </button>
          ) : (
            <button
              onClick={onOpenAuth}
              className="px-3 py-2 rounded-lg bg-signal-emerald/15 text-signal-emerald text-xs font-mono tracking-wider min-h-[44px] flex items-center gap-1.5"
            >
              <LogIn className="w-3.5 h-3.5" />
              <span>LOGIN</span>
            </button>
          )}

          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle Navigation Menu"
            className="p-2.5 text-ink-primary hover:bg-white/5 rounded-lg transition min-w-[44px] min-h-[44px] flex items-center justify-center"
          >
            {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Mobile Drawer (Accessible, full touch-friendly targets) */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-dark-900 border-b border-white/10 px-4 pt-3 pb-6 space-y-2 animate-in slide-in-from-top-2 duration-200">
          <button
            onClick={() => handleTabSelect('jobs')}
            className={`w-full py-3 px-4 rounded-xl text-xs font-mono tracking-wider flex items-center gap-3 transition min-h-[48px] ${
              activeTab === 'jobs'
                ? 'bg-white/10 text-ink-primary font-semibold border border-white/15'
                : 'text-ink-secondary hover:text-ink-primary hover:bg-white/[0.04]'
            }`}
          >
            <Briefcase className={`w-4 h-4 ${activeTab === 'jobs' ? 'text-signal-emerald' : 'text-ink-muted'}`} />
            <span>JOBS</span>
          </button>

          <button
            onClick={() => handleTabSelect('gov-jobs')}
            className={`w-full py-3 px-4 rounded-xl text-xs font-mono tracking-wider flex items-center gap-3 transition min-h-[48px] ${
              activeTab === 'gov-jobs'
                ? 'bg-signal-emerald/15 text-signal-emerald font-semibold border border-signal-emerald/30'
                : 'text-ink-secondary hover:text-ink-primary hover:bg-white/[0.04]'
            }`}
          >
            <Building2 className={`w-4 h-4 ${activeTab === 'gov-jobs' ? 'text-signal-emerald' : 'text-ink-muted'}`} />
            <span>GOVERNMENT JOBS</span>
            <span className="w-2 h-2 rounded-full bg-signal-emerald ml-auto" />
          </button>

          <button
            onClick={() => handleTabSelect('applications')}
            className={`w-full py-3 px-4 rounded-xl text-xs font-mono tracking-wider flex items-center gap-3 transition min-h-[48px] ${
              activeTab === 'applications'
                ? 'bg-white/10 text-ink-primary font-semibold border border-white/15'
                : 'text-ink-secondary hover:text-ink-primary hover:bg-white/[0.04]'
            }`}
          >
            <CheckSquare className={`w-4 h-4 ${activeTab === 'applications' ? 'text-signal-emerald' : 'text-ink-muted'}`} />
            <span>APPLICATIONS</span>
          </button>

          <button
            onClick={() => handleTabSelect('profile')}
            className={`w-full py-3 px-4 rounded-xl text-xs font-mono tracking-wider flex items-center gap-3 transition min-h-[48px] ${
              activeTab === 'profile'
                ? 'bg-white/10 text-ink-primary font-semibold border border-white/15'
                : 'text-ink-secondary hover:text-ink-primary hover:bg-white/[0.04]'
            }`}
          >
            <UserCheck className={`w-4 h-4 ${activeTab === 'profile' ? 'text-signal-emerald' : 'text-ink-muted'}`} />
            <span>PROFILE (CAREER & GOV ELIGIBILITY)</span>
          </button>

          {user && (
            <div className="pt-3 border-t border-white/[0.06] flex items-center justify-between text-xs font-mono text-ink-muted px-2">
              <span className="truncate max-w-[200px]">{user.email}</span>
              <span className="text-signal-emerald text-[11px]">ACTIVE SESSION</span>
            </div>
          )}
        </div>
      )}
    </header>
  );
};
