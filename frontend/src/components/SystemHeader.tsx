import React from 'react';
import { User } from '../types';
import { Compass, UserCheck, LogOut, LogIn } from 'lucide-react';

interface SystemHeaderProps {
  user: User | null;
  onLogout: () => void;
  activeTab: 'discovery' | 'profile';
  onSelectTab: (tab: 'discovery' | 'profile') => void;
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
  return (
    <header className="sticky top-0 z-50 w-full backdrop-blur-xl bg-dark-950/75 border-b border-white/[0.06] transition-all duration-300">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand System Identifier */}
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-lg bg-dark-900 border border-white/10 flex items-center justify-center text-signal-emerald shadow-inner">
            <span className="font-mono text-xs font-bold tracking-tighter">JH</span>
          </div>
          <div className="flex flex-col">
            <div className="flex items-center gap-2">
              <span className="text-sm font-semibold tracking-tight text-ink-primary font-mono">
                JOBHUNTER
              </span>
              <span className="text-[10px] font-mono text-ink-muted uppercase tracking-widest hidden sm:inline-block">
                // AGENT OS
              </span>
            </div>
          </div>
        </div>

        {/* Navigation & Mode Controls */}
        <nav className="flex items-center space-x-1 sm:space-x-2">
          <button
            onClick={() => onSelectTab('discovery')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-mono tracking-wide transition-all duration-200 flex items-center gap-2 ${
              activeTab === 'discovery'
                ? 'bg-white/[0.08] text-ink-primary border border-white/15 shadow-sm'
                : 'text-ink-muted hover:text-ink-primary hover:bg-white/[0.03]'
            }`}
          >
            <Compass className={`w-3.5 h-3.5 ${activeTab === 'discovery' ? 'text-signal-emerald' : ''}`} />
            <span>DISCOVERY</span>
            {isSearching && (
              <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald animate-ping" />
            )}
          </button>

          <button
            onClick={() => onSelectTab('profile')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-mono tracking-wide transition-all duration-200 flex items-center gap-2 ${
              activeTab === 'profile'
                ? 'bg-white/[0.08] text-ink-primary border border-white/15 shadow-sm'
                : 'text-ink-muted hover:text-ink-primary hover:bg-white/[0.03]'
            }`}
          >
            <UserCheck className={`w-3.5 h-3.5 ${activeTab === 'profile' ? 'text-signal-emerald' : ''}`} />
            <span>PROFILE</span>
          </button>
        </nav>

        {/* Technical Status & Auth Controls */}
        <div className="flex items-center space-x-3">
          {user ? (
            <div className="flex items-center space-x-3">
              <div className="hidden md:flex items-center space-x-2 px-2.5 py-1 rounded border border-white/[0.06] bg-dark-900/60 font-mono text-[11px] text-ink-secondary">
                <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald" />
                <span className="truncate max-w-[140px]">{user.email}</span>
              </div>
              <button
                onClick={onLogout}
                title="Disconnect session"
                className="p-1.5 text-ink-muted hover:text-ink-primary hover:bg-white/5 rounded transition"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <div className="flex items-center space-x-2">
              <button
                onClick={onOpenAuth}
                className="px-3.5 py-1.5 rounded-lg bg-signal-emerald/10 hover:bg-signal-emerald/20 text-signal-emerald border border-signal-emerald/30 font-mono text-xs tracking-wider transition flex items-center gap-1.5 shadow-sm"
              >
                <LogIn className="w-3.5 h-3.5" />
                <span>SIGN IN / SIGN UP</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
