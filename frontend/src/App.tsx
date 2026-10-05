import React, { useState, useEffect } from 'react';
import { SystemHeader, AppTab } from './components/SystemHeader';
import { LandingHero } from './components/LandingHero';
import { DiscoveryDashboard } from './components/DiscoveryDashboard';
import { GovernmentJobsFeed } from './components/government/GovernmentJobsFeed';
import { ApplicationsTracker } from './components/ApplicationsTracker';
import { CandidateProfileHub } from './components/CandidateProfileHub';
import { ResumeUploadModal } from './components/ResumeUploadModal';
import { AuthModal } from './components/AuthModal';
import { apiClient } from './api/client';
import { CandidateProfile, User } from './types';
import { Loader2, UserCheck, CheckSquare } from 'lucide-react';

export const App: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<CandidateProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<AppTab>('landing');
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);

  const fetchProfile = async () => {
    try {
      const data = await apiClient.profile.get();
      setProfile(data);
      setUser({
        id: data.userId,
        email: data.email,
        firstName: data.firstName,
        lastName: data.lastName,
        role: 'ROLE_CANDIDATE',
      });
      setIsAuthModalOpen(false);
    } catch (err: any) {
      console.warn('Session invalid or expired', err);
      localStorage.removeItem('jh_token');
      setUser(null);
      setProfile(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const existingToken = localStorage.getItem('jh_token');
    if (existingToken) {
      fetchProfile();
      setActiveTab('jobs');
    } else {
      setLoading(false);
      setActiveTab('landing');
    }
  }, []);

  // Listen for automatic 401 session expiration from API client
  useEffect(() => {
    const handleExpired = () => {
      localStorage.removeItem('jh_token');
      setUser(null);
      setProfile(null);
      setIsAuthModalOpen(true);
    };

    window.addEventListener('jh_session_expired', handleExpired);
    return () => window.removeEventListener('jh_session_expired', handleExpired);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('jh_token');
    setUser(null);
    setProfile(null);
    setActiveTab('landing');
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-dark-950 flex flex-col items-center justify-center text-ink-muted gap-3 font-mono">
        <Loader2 className="w-7 h-7 text-signal-emerald animate-spin" />
        <p className="text-xs tracking-wider uppercase">INITIALIZING JOBHUNTER OS CORE...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-dark-950 text-ink-primary flex flex-col font-sans selection:bg-signal-emerald selection:text-dark-950 relative">
      <SystemHeader
        user={user}
        onLogout={handleLogout}
        activeTab={activeTab}
        onSelectTab={setActiveTab}
        onOpenAuth={() => setIsAuthModalOpen(true)}
      />

      <main className="flex-1 w-full mx-auto">
        {/* Auth prompt modal */}
        {isAuthModalOpen && !user && (
          <div className="fixed inset-0 z-50 bg-dark-950/80 backdrop-blur-md flex items-center justify-center p-4">
            <div className="bg-dark-900 border border-white/10 rounded-2xl w-full max-w-md p-6 shadow-2xl relative">
              <button
                onClick={() => setIsAuthModalOpen(false)}
                className="absolute top-4 right-4 text-ink-muted hover:text-ink-primary transition"
                aria-label="Close modal"
              >
                ✕
              </button>
              <div className="text-center mb-6">
                <h3 className="text-xl font-semibold text-ink-primary">Sign In to JobHunter</h3>
                <p className="text-xs text-ink-secondary mt-1">
                  Access tailored private matching and save your eligibility profile.
                </p>
              </div>
              <AuthModal onSuccess={() => fetchProfile()} />
            </div>
          </div>
        )}

        {/* 1. Landing View */}
        {activeTab === 'landing' && (
          <LandingHero
            onFindJobs={() => setActiveTab('jobs')}
            onExploreGovJobs={() => setActiveTab('gov-jobs')}
            isAuthenticated={!!user}
          />
        )}

        {/* 2. Private Jobs Discovery Tab */}
        {activeTab === 'jobs' && (
          !user || !profile ? (
            <div className="min-h-[70vh] flex flex-col items-center justify-center px-4 py-12">
              <div className="text-center mb-8 max-w-lg">
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/[0.04] border border-white/[0.08] text-signal-emerald font-mono text-[10px] tracking-widest uppercase mb-3">
                  <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald" />
                  <span>PRIVATE JOBS ENGINE</span>
                </div>
                <h2 className="text-2xl sm:text-3xl font-light text-ink-primary">
                  Sign In for Private Jobs Discovery
                </h2>
                <p className="text-xs sm:text-sm text-ink-secondary mt-2 font-light">
                  Private job discovery matches verified facts from your master resume against live market requirements with ATS scoring.
                </p>
              </div>
              <AuthModal onSuccess={() => fetchProfile()} />
            </div>
          ) : (
            <DiscoveryDashboard onNavigateToProfile={() => setActiveTab('profile')} />
          )
        )}

        {/* 3. Government Jobs Discovery Tab */}
        {activeTab === 'gov-jobs' && <GovernmentJobsFeed />}

        {/* 4. Applications Tracker Tab */}
        {activeTab === 'applications' && (
          !user ? (
            <div className="min-h-[70vh] flex flex-col items-center justify-center px-4 py-12">
              <div className="text-center mb-8 max-w-lg">
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/[0.04] border border-white/[0.08] text-signal-emerald font-mono text-[10px] tracking-widest uppercase mb-3">
                  <CheckSquare className="w-3.5 h-3.5 text-signal-emerald" />
                  <span>APPLICATIONS TRACKER</span>
                </div>
                <h2 className="text-2xl sm:text-3xl font-light text-ink-primary">
                  Sign In to Track Applications
                </h2>
                <p className="text-xs sm:text-sm text-ink-secondary mt-2 font-light">
                  Keep a clean, authentic log of private and government vacancies you have applied for.
                </p>
              </div>
              <AuthModal onSuccess={() => fetchProfile()} />
            </div>
          ) : (
            <ApplicationsTracker
              onNavigateToJobs={() => setActiveTab('jobs')}
              onNavigateToGovJobs={() => setActiveTab('gov-jobs')}
            />
          )
        )}

        {/* 5. Profile Hub Tab */}
        {activeTab === 'profile' && (
          !user || !profile ? (
            <div className="min-h-[70vh] flex flex-col items-center justify-center px-4 py-12">
              <div className="text-center mb-8 max-w-lg">
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/[0.04] border border-white/[0.08] text-signal-emerald font-mono text-[10px] tracking-widest uppercase mb-3">
                  <UserCheck className="w-3.5 h-3.5 text-signal-emerald" />
                  <span>CANDIDATE PROFILE</span>
                </div>
                <h2 className="text-2xl sm:text-3xl font-light text-ink-primary">
                  Sign In to Manage Your Profile
                </h2>
                <p className="text-xs sm:text-sm text-ink-secondary mt-2 font-light">
                  Upload your master resume or set your government domicile and eligibility parameters.
                </p>
              </div>
              <AuthModal onSuccess={() => fetchProfile()} />
            </div>
          ) : (
            <CandidateProfileHub
              profile={profile}
              onRefresh={fetchProfile}
              onOpenUpload={() => setIsUploadOpen(true)}
            />
          )
        )}
      </main>

      <ResumeUploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        onUploadSuccess={() => {
          fetchProfile();
        }}
      />
    </div>
  );
};
