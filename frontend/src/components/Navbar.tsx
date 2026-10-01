import React from 'react';
import { Target, UserCheck, ShieldCheck, LogOut, Sparkles, Compass } from 'lucide-react';
import { User } from '../types';

interface NavbarProps {
  user: User | null;
  onLogout: () => void;
  activeTab: 'profile' | 'discovery';
  onSelectTab: (tab: 'profile' | 'discovery') => void;
}

export const Navbar: React.FC<NavbarProps> = ({ user, onLogout, activeTab, onSelectTab }) => {
  return (
    <header className="sticky top-0 z-40 border-b border-slate-800 bg-slate-950/80 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div className="flex items-center space-x-3 cursor-pointer" onClick={() => onSelectTab('discovery')}>
          <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-emerald-600 to-teal-400 flex items-center justify-center shadow-lg shadow-emerald-500/20">
            <Target className="h-5 w-5 text-white" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="text-lg font-bold tracking-tight text-white">JobHunter</span>
              <span className="text-xs px-2 py-0.5 rounded-full font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center gap-1">
                <Sparkles className="w-3 h-3" /> AI Copilot
              </span>
            </div>
            <p className="text-xs text-slate-400">High-Conviction Software Engineering Job Engine</p>
          </div>
        </div>

        <nav className="hidden md:flex items-center space-x-6 text-sm">
          <button
            onClick={() => onSelectTab('profile')}
            className={`font-medium transition pb-1 flex items-center gap-1.5 ${
              activeTab === 'profile'
                ? 'text-emerald-400 border-b-2 border-emerald-400'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <UserCheck className="w-4 h-4" /> Candidate Profile
          </button>

          <button
            onClick={() => onSelectTab('discovery')}
            className={`font-medium transition pb-1 flex items-center gap-1.5 ${
              activeTab === 'discovery'
                ? 'text-emerald-400 border-b-2 border-emerald-400'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Compass className="w-4 h-4" /> Discovered Jobs
            <span className="text-[10px] px-1.5 py-0.2 rounded font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
              Live
            </span>
          </button>

          <span className="text-slate-500 hover:text-slate-400 transition cursor-not-allowed flex items-center gap-1 opacity-60">
            Application Optimizer <span className="text-[10px] px-1.5 py-0.2 rounded bg-slate-800 text-slate-400">M4</span>
          </span>
        </nav>

        <div className="flex items-center space-x-4">
          {user && (
            <div className="flex items-center space-x-3 text-sm">
              <div className="hidden sm:flex items-center space-x-2 px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-300">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                <span className="font-medium text-slate-200">{user.firstName} {user.lastName}</span>
                <span className="text-xs text-slate-500 font-mono">({user.email})</span>
              </div>
              <button
                onClick={onLogout}
                title="Sign Out"
                className="p-2 rounded-lg bg-slate-900 hover:bg-rose-950/40 text-slate-400 hover:text-rose-400 border border-slate-800 hover:border-rose-900/50 transition"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
