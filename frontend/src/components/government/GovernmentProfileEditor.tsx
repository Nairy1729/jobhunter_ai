import React, { useState, useEffect } from 'react';
import { CandidateGovernmentProfile } from '../../types';
import { apiClient } from '../../api/client';
import {
  UserCheck,
  ShieldCheck,
  Save,
  CheckCircle2,
  MapPin,
  GraduationCap,
  Layers,
  Wrench,
  Loader2,
} from 'lucide-react';

const INDIAN_STATES = [
  'All-India',
  'Andhra Pradesh',
  'Arunachal Pradesh',
  'Assam',
  'Bihar',
  'Chhattisgarh',
  'Goa',
  'Gujarat',
  'Haryana',
  'Himachal Pradesh',
  'Jharkhand',
  'Karnataka',
  'Kerala',
  'Madhya Pradesh',
  'Maharashtra',
  'Manipur',
  'Meghalaya',
  'Mizoram',
  'Nagaland',
  'Odisha',
  'Punjab',
  'Rajasthan',
  'Sikkim',
  'Tamil Nadu',
  'Telangana',
  'Tripura',
  'Uttar Pradesh',
  'Uttarakhand',
  'West Bengal',
  'Andaman and Nicobar Islands',
  'Chandigarh',
  'Dadra and Nagar Haveli and Daman and Diu',
  'Delhi',
  'Jammu and Kashmir',
  'Ladakh',
  'Lakshadweep',
  'Puducherry',
];

const EDUCATION_LEVELS = [
  '8th Pass',
  '10th Pass / Matriculation',
  '12th Pass / Intermediate',
  'ITI',
  'Diploma / Polytechnic',
  'Graduate (B.A / B.Sc / B.Com / General)',
  'B.Tech / B.E. (Engineering)',
  'Post Graduate / Master\'s',
  'Ph.D. / Doctorate',
];

const EMPLOYMENT_TYPES = [
  { id: 'PERMANENT', label: 'Permanent / Regular' },
  { id: 'CONTRACTUAL', label: 'Contractual' },
  { id: 'SAMVIDA', label: 'Samvida (संविदा)' },
  { id: 'TEMPORARY', label: 'Temporary / Ad-hoc' },
  { id: 'SCHEME_BASED', label: 'Scheme / Mission-based (NHM, etc.)' },
  { id: 'HONORARIUM', label: 'Honorarium-based (Anganwadi, etc.)' },
  { id: 'APPRENTICESHIP', label: 'Apprenticeships' },
  { id: 'DISTRICT_LEVEL', label: 'District-level positions' },
  { id: 'PANCHAYAT_LEVEL', label: 'Panchayat & Block-level' },
  { id: 'MUNICIPAL', label: 'Municipal Corporation' },
];

export const GovernmentProfileEditor: React.FC = () => {
  const [profile, setProfile] = useState<CandidateGovernmentProfile>({
    age: 24,
    gender: 'FEMALE',
    state: 'Uttar Pradesh',
    district: 'Kannauj',
    domicileState: 'Uttar Pradesh',
    domicileDistrict: 'Kannauj',
    highestEducation: 'Graduate',
    degrees: ['B.Tech Computer Science', '12th Pass'],
    passingYear: 2024,
    yearsOfExperience: 2.5,
    category: 'UR/GEN',
    pwd: false,
    exServiceman: false,
    preferredStates: ['Uttar Pradesh', 'Delhi'],
    preferredDistricts: ['Kannauj', 'Lucknow'],
    preferredEmploymentTypes: ['PERMANENT', 'CONTRACTUAL', 'SAMVIDA', 'HONORARIUM'],
    skills: ['Computer Typing', 'Data Entry', 'Office Assistant'],
  });

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [degreeInput, setDegreeInput] = useState('');
  const [skillInput, setSkillInput] = useState('');

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const data = await apiClient.government.getProfile();
      if (data && (data.state || data.gender || data.age)) {
        setProfile(data);
      }
    } catch (err) {
      console.warn('Failed to load candidate government profile', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setSaveSuccess(false);
    try {
      const updated = await apiClient.government.updateProfile(profile);
      setProfile(updated);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 4000);
    } catch (err) {
      console.error('Failed to update candidate government profile', err);
      alert('Failed to save profile. Please ensure you are logged in.');
    } finally {
      setSaving(false);
    }
  };

  const toggleEmploymentType = (typeId: string) => {
    const current = profile.preferredEmploymentTypes || [];
    if (current.includes(typeId)) {
      setProfile({
        ...profile,
        preferredEmploymentTypes: current.filter((t) => t !== typeId),
      });
    } else {
      setProfile({
        ...profile,
        preferredEmploymentTypes: [...current, typeId],
      });
    }
  };

  const addDegree = () => {
    if (degreeInput.trim() && !(profile.degrees || []).includes(degreeInput.trim())) {
      setProfile({
        ...profile,
        degrees: [...(profile.degrees || []), degreeInput.trim()],
      });
      setDegreeInput('');
    }
  };

  const removeDegree = (deg: string) => {
    setProfile({
      ...profile,
      degrees: (profile.degrees || []).filter((d) => d !== deg),
    });
  };

  const addSkill = () => {
    if (skillInput.trim() && !(profile.skills || []).includes(skillInput.trim())) {
      setProfile({
        ...profile,
        skills: [...(profile.skills || []), skillInput.trim()],
      });
      setSkillInput('');
    }
  };

  const removeSkill = (sk: string) => {
    setProfile({
      ...profile,
      skills: (profile.skills || []).filter((s) => s !== sk),
    });
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-3 font-mono text-xs text-ink-muted">
        <Loader2 className="w-6 h-6 text-signal-emerald animate-spin" />
        <span>LOADING GOVERNMENT ELIGIBILITY PROFILE...</span>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto py-6">
      {/* Page Title & Philosophy Alert */}
      <div className="mb-6">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-signal-emerald/10 border border-signal-emerald/20 text-signal-emerald font-mono text-xs mb-2">
          <ShieldCheck className="w-3.5 h-3.5" />
          <span>DETERMINISTIC ELIGIBILITY PROFILE</span>
        </div>
        <h1 className="text-2xl sm:text-3xl font-semibold text-ink-primary">
          Government Jobs Eligibility Profile
        </h1>
        <p className="text-xs sm:text-sm text-ink-secondary mt-1 max-w-2xl leading-relaxed">
          Government recruitment in India matches directly on factual age limits, domicile, educational certificates, and category criteria.
          <strong className="text-ink-primary font-medium ml-1">No resume is required or generated for this section.</strong>
        </p>
      </div>

      {saveSuccess && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs font-mono flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>Profile saved successfully. All government vacancies now reflect updated deterministic eligibility status.</span>
        </div>
      )}

      <form onSubmit={handleSave} className="space-y-6">
        {/* Section 1: Personal Demographics */}
        <div className="bg-dark-900/70 border border-white/[0.08] rounded-xl p-5 sm:p-6 shadow-sm">
          <h3 className="text-sm font-semibold uppercase font-mono tracking-wider text-ink-primary flex items-center gap-2 mb-4 pb-2 border-b border-white/[0.06]">
            <UserCheck className="w-4 h-4 text-signal-emerald" />
            1. Age, Gender & Social Category
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-mono text-ink-secondary mb-1">
                Age (Years) *
              </label>
              <input
                type="number"
                min="16"
                max="65"
                required
                value={profile.age || ''}
                onChange={(e) => setProfile({ ...profile, age: parseInt(e.target.value) || undefined })}
                className="w-full px-3 py-2 bg-dark-950 border border-white/10 rounded-lg text-sm text-ink-primary focus:outline-none focus:border-signal-emerald font-mono"
                placeholder="e.g. 24"
              />
            </div>

            <div>
              <label className="block text-xs font-mono text-ink-secondary mb-1">
                Gender * (Strict matching)
              </label>
              <select
                required
                value={profile.gender || 'FEMALE'}
                onChange={(e) => setProfile({ ...profile, gender: e.target.value })}
                className="w-full px-3 py-2 bg-dark-950 border border-white/10 rounded-lg text-sm text-ink-primary focus:outline-none focus:border-signal-emerald font-mono"
              >
                <option value="FEMALE">Female</option>
                <option value="MALE">Male</option>
                <option value="TRANSGENDER">Transgender</option>
                <option value="OTHER">Other / Prefer not to say</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-mono text-ink-secondary mb-1">
                Social Category *
              </label>
              <select
                required
                value={profile.category || 'UR/GEN'}
                onChange={(e) => setProfile({ ...profile, category: e.target.value })}
                className="w-full px-3 py-2 bg-dark-950 border border-white/10 rounded-lg text-sm text-ink-primary focus:outline-none focus:border-signal-emerald font-mono"
              >
                <option value="UR/GEN">Unreserved / General (UR)</option>
                <option value="EWS">Economically Weaker Section (EWS)</option>
                <option value="OBC_NCL">Other Backward Class (OBC-Non Creamy Layer)</option>
                <option value="OBC_CL">Other Backward Class (OBC-Creamy Layer)</option>
                <option value="SC">Scheduled Caste (SC)</option>
                <option value="ST">Scheduled Tribe (ST)</option>
              </select>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-6 mt-4 pt-3 border-t border-white/[0.04] text-xs font-mono">
            <label className="flex items-center gap-2 cursor-pointer text-ink-secondary hover:text-ink-primary">
              <input
                type="checkbox"
                checked={profile.pwd || false}
                onChange={(e) => setProfile({ ...profile, pwd: e.target.checked })}
                className="rounded border-white/20 bg-dark-950 text-signal-emerald focus:ring-0"
              />
              <span>Person with Benchmark Disability (PwD)</span>
            </label>

            <label className="flex items-center gap-2 cursor-pointer text-ink-secondary hover:text-ink-primary">
              <input
                type="checkbox"
                checked={profile.exServiceman || false}
                onChange={(e) => setProfile({ ...profile, exServiceman: e.target.checked })}
                className="rounded border-white/20 bg-dark-950 text-signal-emerald focus:ring-0"
              />
              <span>Ex-Serviceman (ESM)</span>
            </label>
          </div>
        </div>

        {/* Section 2: Domicile & Location (Crucial for State/District/Anganwadi/Panchayat jobs) */}
        <div className="bg-dark-900/70 border border-white/[0.08] rounded-xl p-5 sm:p-6 shadow-sm">
          <h3 className="text-sm font-semibold uppercase font-mono tracking-wider text-ink-primary flex items-center gap-2 mb-4 pb-2 border-b border-white/[0.06]">
            <MapPin className="w-4 h-4 text-signal-emerald" />
            2. State, District & Domicile Residence
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-mono text-ink-secondary mb-1">
                State of Residence *
              </label>
              <select
                value={profile.state || 'Uttar Pradesh'}
                onChange={(e) => setProfile({ ...profile, state: e.target.value, domicileState: e.target.value })}
                className="w-full px-3 py-2 bg-dark-950 border border-white/10 rounded-lg text-sm text-ink-primary focus:outline-none focus:border-signal-emerald font-mono"
              >
                {INDIAN_STATES.map((st) => (
                  <option key={st} value={st}>
                    {st}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-mono text-ink-secondary mb-1">
                District of Residence *
              </label>
              <input
                type="text"
                required
                value={profile.district || ''}
                onChange={(e) => setProfile({ ...profile, district: e.target.value, domicileDistrict: e.target.value })}
                className="w-full px-3 py-2 bg-dark-950 border border-white/10 rounded-lg text-sm text-ink-primary focus:outline-none focus:border-signal-emerald font-mono"
                placeholder="e.g. Kannauj, Pune, Patna"
              />
            </div>
          </div>
        </div>

        {/* Section 3: Educational Qualifications */}
        <div className="bg-dark-900/70 border border-white/[0.08] rounded-xl p-5 sm:p-6 shadow-sm">
          <h3 className="text-sm font-semibold uppercase font-mono tracking-wider text-ink-primary flex items-center gap-2 mb-4 pb-2 border-b border-white/[0.06]">
            <GraduationCap className="w-4 h-4 text-signal-emerald" />
            3. Educational Qualifications & Certificates
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-xs font-mono text-ink-secondary mb-1">
                Highest Education Level *
              </label>
              <select
                value={profile.highestEducation || 'Graduate'}
                onChange={(e) => setProfile({ ...profile, highestEducation: e.target.value })}
                className="w-full px-3 py-2 bg-dark-950 border border-white/10 rounded-lg text-sm text-ink-primary focus:outline-none focus:border-signal-emerald font-mono"
              >
                {EDUCATION_LEVELS.map((ed) => (
                  <option key={ed} value={ed}>
                    {ed}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-mono text-ink-secondary mb-1">
                Passing Year
              </label>
              <input
                type="number"
                min="1980"
                max="2030"
                value={profile.passingYear || ''}
                onChange={(e) => setProfile({ ...profile, passingYear: parseInt(e.target.value) || undefined })}
                className="w-full px-3 py-2 bg-dark-950 border border-white/10 rounded-lg text-sm text-ink-primary focus:outline-none focus:border-signal-emerald font-mono"
                placeholder="e.g. 2024"
              />
            </div>
          </div>

          {/* Completed Degrees Pill Box */}
          <div>
            <label className="block text-xs font-mono text-ink-secondary mb-1">
              Degrees / Diplomas / Certificates Attained
            </label>
            <div className="flex gap-2 mb-2">
              <input
                type="text"
                value={degreeInput}
                onChange={(e) => setDegreeInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    addDegree();
                  }
                }}
                className="flex-1 px-3 py-2 bg-dark-950 border border-white/10 rounded-lg text-xs text-ink-primary focus:outline-none focus:border-signal-emerald font-mono"
                placeholder="e.g. 10th Pass, 12th Pass, B.Tech, GNM Nursing, CCC Certificate (Press Enter to add)"
              />
              <button
                type="button"
                onClick={addDegree}
                className="px-3 py-2 bg-white/[0.08] hover:bg-white/[0.12] rounded-lg text-xs font-mono text-ink-primary transition"
              >
                Add
              </button>
            </div>

            <div className="flex flex-wrap gap-2">
              {(profile.degrees || []).map((deg) => (
                <span
                  key={deg}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-white/[0.05] border border-white/10 text-xs font-mono text-ink-primary"
                >
                  <span>{deg}</span>
                  <button
                    type="button"
                    onClick={() => removeDegree(deg)}
                    className="text-ink-muted hover:text-rose-400"
                  >
                    ×
                  </button>
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* Section 4: Employment Type Preferences (Section 4 & 15) */}
        <div className="bg-dark-900/70 border border-white/[0.08] rounded-xl p-5 sm:p-6 shadow-sm">
          <h3 className="text-sm font-semibold uppercase font-mono tracking-wider text-ink-primary flex items-center gap-2 mb-4 pb-2 border-b border-white/[0.06]">
            <Layers className="w-4 h-4 text-signal-emerald" />
            4. Employment & Engagement Types Considered
          </h3>
          <p className="text-xs text-ink-secondary mb-3 font-mono">
            Check all types of government appointments you are open to:
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            {EMPLOYMENT_TYPES.map((t) => {
              const isChecked = (profile.preferredEmploymentTypes || []).includes(t.id);
              return (
                <label
                  key={t.id}
                  onClick={() => toggleEmploymentType(t.id)}
                  className={`flex items-center gap-2.5 p-2.5 rounded-lg border cursor-pointer text-xs font-mono transition ${
                    isChecked
                      ? 'bg-signal-emerald/10 border-signal-emerald/40 text-emerald-300 font-medium'
                      : 'bg-dark-950/40 border-white/[0.06] text-ink-secondary hover:border-white/15'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={isChecked}
                    readOnly
                    className="rounded border-white/20 bg-dark-950 text-signal-emerald focus:ring-0"
                  />
                  <span>{t.label}</span>
                </label>
              );
            })}
          </div>
        </div>

        {/* Section 5: Skills & Certificates */}
        <div className="bg-dark-900/70 border border-white/[0.08] rounded-xl p-5 sm:p-6 shadow-sm">
          <h3 className="text-sm font-semibold uppercase font-mono tracking-wider text-ink-primary flex items-center gap-2 mb-4 pb-2 border-b border-white/[0.06]">
            <Wrench className="w-4 h-4 text-signal-emerald" />
            5. Technical Skills & Government Certifications
          </h3>

          <div>
            <div className="flex gap-2 mb-2">
              <input
                type="text"
                value={skillInput}
                onChange={(e) => setSkillInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    addSkill();
                  }
                }}
                className="flex-1 px-3 py-2 bg-dark-950 border border-white/10 rounded-lg text-xs text-ink-primary focus:outline-none focus:border-signal-emerald font-mono"
                placeholder="e.g. Hindi Typing, English Typing 30 WPM, CCC Computer Certificate, First Aid (Press Enter to add)"
              />
              <button
                type="button"
                onClick={addSkill}
                className="px-3 py-2 bg-white/[0.08] hover:bg-white/[0.12] rounded-lg text-xs font-mono text-ink-primary transition"
              >
                Add Skill
              </button>
            </div>

            <div className="flex flex-wrap gap-2">
              {(profile.skills || []).map((sk) => (
                <span
                  key={sk}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-white/[0.05] border border-white/10 text-xs font-mono text-ink-primary"
                >
                  <span>{sk}</span>
                  <button
                    type="button"
                    onClick={() => removeSkill(sk)}
                    className="text-ink-muted hover:text-rose-400"
                  >
                    ×
                  </button>
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* Submit Button */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <button
            type="submit"
            disabled={saving}
            className="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl bg-signal-emerald hover:bg-emerald-400 text-dark-950 font-mono text-xs font-bold transition shadow-lg shadow-signal-emerald/20 disabled:opacity-50"
          >
            {saving ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>SAVING PROFILE...</span>
              </>
            ) : (
              <>
                <Save className="w-4 h-4" />
                <span>SAVE GOVERNMENT ELIGIBILITY PROFILE</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};
