import React, { useState, useEffect } from 'react';
import { 
  CandidateProfile, CandidateSkill 
} from '../types';
import { 
  Save, Plus, Trash2, FileText, CheckCircle2, Shield, 
  DollarSign, Briefcase, Code, Globe, RefreshCw, AlertTriangle
} from 'lucide-react';
import { apiClient } from '../api/client';

interface CandidateProfileEditorProps {
  profile: CandidateProfile;
  onRefresh: () => void;
  onOpenUpload: () => void;
}

export const CandidateProfileEditor: React.FC<CandidateProfileEditorProps> = ({ 
  profile, 
  onRefresh, 
  onOpenUpload 
}) => {
  const [formData, setFormData] = useState<CandidateProfile>(profile);
  const [skills, setSkills] = useState<CandidateSkill[]>(profile.skills || []);
  const [saving, setSaving] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    setFormData(profile);
    setSkills(profile.skills || []);
  }, [profile]);

  // New skill inline entry state
  const [newSkillName, setNewSkillName] = useState('');
  const [newSkillLevel, setNewSkillLevel] = useState<'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT'>('INTERMEDIATE');
  const [newSkillYears, setNewSkillYears] = useState(2);
  const [newSkillPrimary, setNewSkillPrimary] = useState(true);
  const [newSkillEvidence, setNewSkillEvidence] = useState('');

  const handleTextChange = (field: keyof CandidateProfile, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleAddSkill = () => {
    if (!newSkillName.trim()) return;
    const newSkill: CandidateSkill = {
      skillName: newSkillName.trim(),
      category: 'TECHNICAL',
      proficiencyLevel: newSkillLevel,
      yearsExperience: newSkillYears,
      primary: newSkillPrimary,
      evidenceText: newSkillEvidence.trim() || undefined
    };

    setSkills(prev => [...prev, newSkill]);
    setNewSkillName('');
    setNewSkillEvidence('');
  };

  const handleRemoveSkill = (index: number) => {
    setSkills(prev => prev.filter((_, i) => i !== index));
  };

  const handleSaveAll = async () => {
    setSaving(true);
    setSuccessMessage(null);
    setErrorMessage(null);
    try {
      await apiClient.profile.update(formData);
      await apiClient.profile.updateSkills(skills);
      setSuccessMessage('Candidate profile & verified skills saved successfully.');
      setTimeout(() => setSuccessMessage(null), 4000);
      onRefresh();
    } catch (err: any) {
      console.error(err);
      const msg = err?.response?.data?.message || err?.message || 'Failed to save profile changes';
      setErrorMessage(`Unable to save profile info: ${msg}`);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-8 pb-16 font-sans">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-6 sm:p-8 rounded-2xl bg-dark-900 border border-white/10">
        <div>
          <div className="flex items-center gap-2 mb-1.5 font-mono text-[10px] tracking-widest text-signal-emerald uppercase">
            <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald" />
            <span>VERIFIED CANDIDATE PROFILE // TRUTH BASELINE</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-light text-ink-primary tracking-tight">
            Candidate Intelligence
          </h1>
          <p className="text-xs sm:text-sm text-ink-secondary mt-1 max-w-2xl font-light leading-relaxed">
            This verified profile grounds all semantic job matching, advantage synthesis, and resume tailoring. The system will never fabricate experience outside these factual boundaries.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={onOpenUpload}
            className="px-4 py-2.5 rounded-xl bg-dark-950 hover:bg-white/[0.04] text-ink-secondary hover:text-ink-primary border border-white/10 font-mono text-xs tracking-wider transition flex items-center gap-2"
          >
            <FileText className="w-4 h-4 text-signal-emerald" />
            <span>UPLOAD RESUME</span>
          </button>
          <button
            onClick={handleSaveAll}
            disabled={saving}
            className="px-5 py-2.5 rounded-xl bg-signal-emerald hover:bg-signal-emerald/90 disabled:opacity-50 text-dark-950 font-mono font-bold text-xs tracking-wider transition flex items-center gap-2 shadow-lg shadow-signal-emerald/20"
          >
            {saving ? <RefreshCw className="w-4 h-4 animate-spin text-dark-950" /> : <Save className="w-4 h-4 text-dark-950" />}
            <span>SAVE PROFILE</span>
          </button>
        </div>
      </div>

      {successMessage && (
        <div className="p-4 rounded-xl bg-signal-emerald/10 border border-signal-emerald/30 text-signal-emerald text-xs font-mono flex items-center gap-2.5 animate-in fade-in">
          <CheckCircle2 className="w-4 h-4 text-signal-emerald flex-shrink-0" />
          <span>{successMessage}</span>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 rounded-xl bg-signal-rose/10 border border-signal-rose/30 text-signal-rose text-xs font-mono flex items-center gap-2.5 animate-in fade-in">
          <AlertTriangle className="w-4 h-4 text-signal-rose flex-shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Grid: Core Info & Preferences */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Primary Details */}
        <div className="lg:col-span-2 space-y-6">
          {/* Professional Overview Card */}
          <div className="p-6 rounded-2xl bg-dark-900 border border-white/10 space-y-4">
            <h2 className="text-sm font-mono font-semibold text-ink-primary flex items-center gap-2 uppercase tracking-wider">
              <Briefcase className="w-4 h-4 text-signal-emerald" /> Professional Overview
            </h2>

            <div>
              <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">Headline</label>
              <input
                type="text"
                value={formData.headline || ''}
                onChange={e => handleTextChange('headline', e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary text-sm font-sans"
              />
            </div>

            <div>
              <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">Professional Summary & Focus</label>
              <textarea
                rows={4}
                value={formData.summary || ''}
                onChange={e => handleTextChange('summary', e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary text-sm leading-relaxed font-sans"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">Commercial Experience (Years)</label>
                <input
                  type="number"
                  step="0.5"
                  value={formData.yearsOfExperience || 0}
                  onChange={e => handleTextChange('yearsOfExperience', parseFloat(e.target.value))}
                  className="w-full px-3.5 py-2 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary text-sm font-mono"
                />
              </div>
              <div>
                <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">Current Base Location</label>
                <input
                  type="text"
                  value={formData.currentLocation || ''}
                  onChange={e => handleTextChange('currentLocation', e.target.value)}
                  className="w-full px-3.5 py-2 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary text-sm font-sans"
                />
              </div>
            </div>
          </div>

          {/* Target Compensation & Role Matching Card */}
          <div className="p-6 rounded-2xl bg-dark-900 border border-white/10 space-y-4">
            <h2 className="text-sm font-mono font-semibold text-ink-primary flex items-center gap-2 uppercase tracking-wider">
              <DollarSign className="w-4 h-4 text-signal-emerald" /> Target Compensation & Search Filters
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">
                  Target CTC Baseline (INR / Annum)
                </label>
                <div className="relative">
                  <span className="absolute left-3.5 top-2.5 text-xs text-ink-muted font-bold font-mono">₹</span>
                  <input
                    type="number"
                    value={formData.minSalaryInr || 1000000}
                    onChange={e => handleTextChange('minSalaryInr', parseFloat(e.target.value))}
                    className="w-full pl-8 pr-3.5 py-2 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary text-sm font-mono"
                  />
                </div>
                <p className="text-[10px] font-mono text-ink-muted mt-1">
                  e.g. ₹10,00,000 = 10 LPA. Fully customizable; never hard-coded.
                </p>
              </div>

              <div>
                <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">Target Currency</label>
                <input
                  type="text"
                  value={formData.currency || 'INR'}
                  onChange={e => handleTextChange('currency', e.target.value)}
                  className="w-full px-3.5 py-2 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary text-sm font-mono uppercase"
                />
              </div>
            </div>

            <div>
              <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">Target Roles</label>
              <div className="flex flex-wrap gap-1.5 mb-2">
                {formData.targetRoles?.map((role, idx) => (
                  <span key={idx} className="font-mono text-xs px-2.5 py-0.5 rounded bg-signal-emerald/10 text-signal-emerald border border-signal-emerald/20">
                    {role}
                  </span>
                ))}
              </div>
              <input
                type="text"
                placeholder="Comma-separated roles (e.g. Software Engineer, Backend Engineer, Java Developer)"
                value={formData.targetRoles?.join(', ') || ''}
                onChange={e => handleTextChange('targetRoles', e.target.value.split(',').map(s => s.trim()).filter(Boolean))}
                className="w-full px-3.5 py-2 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary text-xs font-mono"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">Preferred Locations</label>
                <input
                  type="text"
                  value={formData.preferredLocations?.join(', ') || ''}
                  onChange={e => handleTextChange('preferredLocations', e.target.value.split(',').map(s => s.trim()).filter(Boolean))}
                  placeholder="e.g. Bangalore, Hyderabad, Remote"
                  className="w-full px-3.5 py-2 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary text-xs font-mono"
                />
              </div>

              <div>
                <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">Work Modes</label>
                <input
                  type="text"
                  value={formData.workModes?.join(', ') || ''}
                  onChange={e => handleTextChange('workModes', e.target.value.split(',').map(s => s.trim()).filter(Boolean))}
                  placeholder="e.g. REMOTE, HYBRID"
                  className="w-full px-3.5 py-2 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary text-xs font-mono"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Master Resume & Links */}
        <div className="space-y-6">
          {/* Master Resume Card */}
          <div className="p-6 rounded-2xl bg-dark-900 border border-white/10 space-y-4">
            <h2 className="text-sm font-mono font-semibold text-ink-primary flex items-center gap-2 uppercase tracking-wider">
              <FileText className="w-4 h-4 text-signal-emerald" /> Master Resume
            </h2>

            {formData.masterResumeTitle ? (
              <div className="p-4 rounded-xl bg-dark-950 border border-signal-emerald/20 space-y-3">
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-signal-emerald/10 text-signal-emerald">
                    <CheckCircle2 className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="text-xs font-mono font-medium text-ink-primary truncate max-w-[200px]">
                      {formData.masterResumeTitle}
                    </p>
                    <span className="text-[10px] text-signal-emerald font-mono uppercase tracking-wider">
                      Active Master Resume
                    </span>
                  </div>
                </div>
                <button
                  onClick={onOpenUpload}
                  className="w-full py-2 rounded-lg bg-dark-900 hover:bg-white/[0.04] text-xs font-mono text-ink-secondary hover:text-ink-primary transition border border-white/[0.06]"
                >
                  Upload New Version
                </button>
              </div>
            ) : (
              <div className="p-4 rounded-xl bg-dark-950 border border-dashed border-white/15 text-center space-y-2">
                <p className="text-xs font-mono text-ink-muted">No master resume uploaded yet.</p>
                <button
                  onClick={onOpenUpload}
                  className="px-3 py-1.5 rounded-lg bg-signal-emerald hover:bg-signal-emerald/90 text-dark-950 text-xs font-mono font-bold transition"
                >
                  Upload Master Resume PDF
                </button>
              </div>
            )}

            <div className="p-3 rounded-xl bg-dark-950/60 border border-white/[0.06] text-[11px] text-ink-secondary space-y-1 font-light">
              <div className="flex items-center gap-1.5 text-signal-emerald font-mono font-semibold text-[10px] uppercase">
                <Shield className="w-3.5 h-3.5" /> Factual Integrity Invariant
              </div>
              <p>
                JobHunter keeps your Master Resume immutable and generates role-specific tailored versions showing side-by-side diffs.
              </p>
            </div>
          </div>

          {/* Social / Profiles */}
          <div className="p-6 rounded-2xl bg-dark-900 border border-white/10 space-y-3">
            <h2 className="text-sm font-mono font-semibold text-ink-primary flex items-center gap-2 uppercase tracking-wider">
              <Globe className="w-4 h-4 text-signal-emerald" /> Professional Presence
            </h2>

            <div>
              <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">GitHub URL</label>
              <input
                type="text"
                value={formData.githubUrl || ''}
                onChange={e => handleTextChange('githubUrl', e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary text-xs font-mono"
              />
            </div>

            <div>
              <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">LinkedIn URL</label>
              <input
                type="text"
                value={formData.linkedinUrl || ''}
                onChange={e => handleTextChange('linkedinUrl', e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary text-xs font-mono"
              />
            </div>

            <div>
              <label className="block text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1.5">Phone Number</label>
              <input
                type="text"
                value={formData.phoneNumber || ''}
                onChange={e => handleTextChange('phoneNumber', e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary text-xs font-mono"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Technical Skills & Verified Evidence Matrix */}
      <div className="p-6 sm:p-8 rounded-2xl bg-dark-900 border border-white/10 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-white/[0.06] pb-4">
          <div>
            <h2 className="text-base sm:text-lg font-light text-ink-primary flex items-center gap-2">
              <Code className="w-5 h-5 text-signal-emerald" /> Verified Technical Skills Matrix
            </h2>
            <p className="text-xs text-ink-muted mt-0.5 font-light">
              Every skill is associated with a proficiency level, verified commercial duration, and real project evidence.
            </p>
          </div>
          <span className="font-mono text-xs px-3 py-1 rounded bg-dark-950 border border-white/[0.06] text-ink-secondary self-start sm:self-auto">
            {skills.length} VERIFIED SIGNALS
          </span>
        </div>

        {/* Existing Skills Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-white/[0.06] text-[10px] font-mono font-semibold text-ink-muted uppercase tracking-wider">
                <th className="py-2.5 px-3">Skill</th>
                <th className="py-2.5 px-3">Category</th>
                <th className="py-2.5 px-3">Proficiency</th>
                <th className="py-2.5 px-3">Experience</th>
                <th className="py-2.5 px-3">Tier</th>
                <th className="py-2.5 px-3">Truthful Project Evidence</th>
                <th className="py-2.5 px-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.04] font-mono text-xs">
              {skills.map((s, idx) => (
                <tr key={idx} className="hover:bg-white/[0.015] transition">
                  <td className="py-3 px-3 font-semibold text-ink-primary font-sans">{s.skillName}</td>
                  <td className="py-3 px-3 text-ink-muted">{s.category || 'TECHNICAL'}</td>
                  <td className="py-3 px-3">
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      s.proficiencyLevel === 'EXPERT' ? 'bg-signal-cyan/10 text-signal-cyan border border-signal-cyan/20' :
                      s.proficiencyLevel === 'ADVANCED' ? 'bg-signal-emerald/10 text-signal-emerald border border-signal-emerald/20' :
                      'bg-white/[0.04] text-ink-secondary border border-white/10'
                    }`}>
                      {s.proficiencyLevel}
                    </span>
                  </td>
                  <td className="py-3 px-3 text-ink-secondary">{s.yearsExperience} yrs</td>
                  <td className="py-3 px-3">
                    {s.primary ? (
                      <span className="text-[10px] text-signal-emerald font-semibold font-mono">Core</span>
                    ) : (
                      <span className="text-[10px] text-ink-muted font-mono">Secondary</span>
                    )}
                  </td>
                  <td className="py-3 px-3 text-ink-secondary font-sans text-xs max-w-xs truncate" title={s.evidenceText}>
                    {s.evidenceText || 'Verified via candidate profile'}
                  </td>
                  <td className="py-3 px-3 text-right">
                    <button
                      onClick={() => handleRemoveSkill(idx)}
                      className="p-1 rounded hover:bg-signal-rose/10 text-ink-muted hover:text-signal-rose transition"
                      title="Remove skill"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Add Skill Row */}
        <div className="p-4 rounded-xl bg-dark-950 border border-white/[0.06] space-y-3">
          <p className="text-xs font-mono uppercase tracking-wider text-ink-primary flex items-center gap-1.5">
            <Plus className="w-4 h-4 text-signal-emerald" /> ADD VERIFIED SKILL SPECIMEN
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-3">
            <input
              type="text"
              placeholder="Skill Name (e.g. Kafka)"
              value={newSkillName}
              onChange={e => setNewSkillName(e.target.value)}
              className="px-3 py-1.5 rounded-lg bg-dark-900 border border-white/10 text-ink-primary text-xs focus:border-signal-emerald/60 focus:outline-none font-mono"
            />
            <select
              value={newSkillLevel}
              onChange={e => setNewSkillLevel(e.target.value as any)}
              className="px-3 py-1.5 rounded-lg bg-dark-900 border border-white/10 text-ink-primary text-xs focus:border-signal-emerald/60 focus:outline-none font-mono"
            >
              <option value="BEGINNER">BEGINNER</option>
              <option value="INTERMEDIATE">INTERMEDIATE</option>
              <option value="ADVANCED">ADVANCED</option>
              <option value="EXPERT">EXPERT</option>
            </select>
            <input
              type="number"
              step="0.5"
              placeholder="Years"
              value={newSkillYears}
              onChange={e => setNewSkillYears(parseFloat(e.target.value) || 0)}
              className="px-3 py-1.5 rounded-lg bg-dark-900 border border-white/10 text-ink-primary text-xs focus:border-signal-emerald/60 focus:outline-none font-mono"
            />
            <input
              type="text"
              placeholder="Project evidence reference..."
              value={newSkillEvidence}
              onChange={e => setNewSkillEvidence(e.target.value)}
              className="px-3 py-1.5 rounded-lg bg-dark-900 border border-white/10 text-ink-primary text-xs focus:border-signal-emerald/60 focus:outline-none font-sans"
            />
            <div className="flex items-center gap-1.5 px-2 py-1.5 rounded-lg bg-dark-900 border border-white/10">
              <input
                type="checkbox"
                id="primaryToggle"
                checked={newSkillPrimary}
                onChange={e => setNewSkillPrimary(e.target.checked)}
                className="rounded border-white/10 text-signal-emerald focus:ring-signal-emerald bg-dark-950"
              />
              <label htmlFor="primaryToggle" className="text-[11px] text-ink-secondary font-mono cursor-pointer select-none">
                Primary Core
              </label>
            </div>
            <button
              onClick={handleAddSkill}
              className="py-1.5 px-3 rounded-lg bg-white/[0.06] hover:bg-white/[0.1] text-signal-emerald font-mono text-xs border border-white/10 transition flex items-center justify-center gap-1.5"
            >
              <Plus className="w-3.5 h-3.5" /> ADD
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
