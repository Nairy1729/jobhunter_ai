import React, { useState, useEffect } from 'react';
import { SystemHeader } from './components/SystemHeader';
import { CandidateProfileEditor } from './components/CandidateProfileEditor';
import { ResumeUploadModal } from './components/ResumeUploadModal';
import { DiscoveryDashboard } from './components/DiscoveryDashboard';
import { AuthModal } from './components/AuthModal';
import { apiClient } from './api/client';
import { CandidateProfile, User } from './types';
import { Loader2 } from 'lucide-react';

export const App: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<CandidateProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<'profile' | 'discovery'>('discovery');

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
    } catch (err: any) {
      console.warn('Authentication token invalid or expired', err);
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
    } else {
      setLoading(false);
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('jh_token');
    setUser(null);
    setProfile(null);
  };

  if (loading && !profile) {
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
        onOpenAuth={() => {
          setUser(null);
          setProfile(null);
        }}
      />

      <main className="flex-1 w-full mx-auto">
        {!user || !profile ? (
          <div className="min-h-[80vh] flex flex-col items-center justify-center px-4 py-12">
            <div className="text-center mb-8 max-w-lg">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/[0.04] border border-white/[0.08] text-signal-emerald font-mono text-[10px] tracking-widest uppercase mb-3">
                <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald" />
                <span>INTELLIGENT CANDIDATE IDENTITY</span>
              </div>
              <h1 className="text-3xl sm:text-4xl font-light tracking-tight text-ink-primary">
                Access JobHunter AI
              </h1>
              <p className="text-xs sm:text-sm text-ink-secondary font-light mt-2 leading-relaxed">
                Authenticate to access autonomous job discovery, semantic matching, The Edge application strategy, and factual resume tailoring.
              </p>
            </div>

            <AuthModal onSuccess={() => fetchProfile()} />
          </div>
        ) : activeTab === 'discovery' ? (
          <DiscoveryDashboard onNavigateToProfile={() => setActiveTab('profile')} />
        ) : (
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <CandidateProfileEditor
              profile={profile}
              onRefresh={fetchProfile}
              onOpenUpload={() => setIsUploadOpen(true)}
            />
          </div>
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
