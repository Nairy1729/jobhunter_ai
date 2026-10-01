import React, { useState } from 'react';
import { UploadCloud, X, CheckCircle, AlertCircle, FileText, Loader2 } from 'lucide-react';
import { apiClient } from '../api/client';
import { ResumeUploadResponse } from '../types';

interface ResumeUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUploadSuccess: (data: ResumeUploadResponse) => void;
}

export const ResumeUploadModal: React.FC<ResumeUploadModalProps> = ({ isOpen, onClose, onUploadSuccess }) => {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ResumeUploadResponse | null>(null);

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selected = e.target.files[0];
      if (selected.size > 10 * 1024 * 1024) {
        setError('File exceeds 10MB limit.');
        return;
      }
      setFile(selected);
      setError(null);
    }
  };

  const handleUpload = async () => {
    if (!file) return;
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.resumes.upload(file);
      setResult(res);
      onUploadSuccess(res);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to upload and parse resume.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-dark-950/80 backdrop-blur-xl animate-in fade-in duration-200">
      <div className="w-full max-w-lg rounded-2xl bg-dark-900 border border-white/10 p-6 sm:p-8 shadow-2xl relative font-sans">
        <button
          onClick={onClose}
          className="absolute top-5 right-5 text-ink-muted hover:text-ink-primary p-1.5 rounded-lg hover:bg-white/[0.04] transition"
        >
          <X className="w-4 h-4" />
        </button>

        <div className="flex items-center space-x-3 mb-5">
          <div className="p-2.5 rounded-xl bg-signal-emerald/10 text-signal-emerald border border-signal-emerald/20">
            <UploadCloud className="w-5 h-5" />
          </div>
          <div>
            <div className="font-mono text-[10px] text-ink-muted uppercase tracking-widest">
              VERIFIED INPUT
            </div>
            <h3 className="text-lg font-light text-ink-primary tracking-tight">Upload Master Resume</h3>
          </div>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-xl bg-signal-rose/10 border border-signal-rose/20 text-signal-rose text-xs flex items-center gap-2 font-mono">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {result ? (
          <div className="space-y-4 py-2">
            <div className="p-4 rounded-xl bg-signal-emerald/10 border border-signal-emerald/20 text-signal-emerald flex items-start gap-3">
              <CheckCircle className="w-4 h-4 text-signal-emerald flex-shrink-0 mt-0.5" />
              <div className="space-y-1">
                <p className="font-semibold text-xs font-mono">{result.message}</p>
                <div className="flex flex-wrap gap-2 text-[11px] text-signal-emerald/80 font-mono mt-2">
                  <span className="px-2 py-0.5 rounded bg-dark-950/60 border border-signal-emerald/20">
                    Source: {result.extractionSource || 'AI Orchestrator'}
                  </span>
                  {result.yearsOfExperience !== undefined && (
                    <span className="px-2 py-0.5 rounded bg-dark-950/60 border border-signal-emerald/20">
                      Exp: {result.yearsOfExperience} yrs
                    </span>
                  )}
                  {result.experiencesCount !== undefined && (
                    <span className="px-2 py-0.5 rounded bg-dark-950/60 border border-signal-emerald/20">
                      Roles: {result.experiencesCount}
                    </span>
                  )}
                  {result.projectsCount !== undefined && (
                    <span className="px-2 py-0.5 rounded bg-dark-950/60 border border-signal-emerald/20">
                      Projects: {result.projectsCount}
                    </span>
                  )}
                </div>
              </div>
            </div>

            {result.headline && (
              <div className="p-3 rounded-xl bg-dark-950 border border-white/[0.08]">
                <p className="text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-1">
                  AUTO-FILLED HEADLINE:
                </p>
                <p className="text-xs font-sans text-ink-primary font-medium">
                  {result.headline}
                </p>
              </div>
            )}

            {result.detectedSkills.length > 0 && (
              <div>
                <p className="text-[10px] font-mono text-ink-muted uppercase tracking-widest mb-2">
                  VERIFIED SKILLS EXTRACTED & SYNCED ({result.detectedSkills.length}):
                </p>
                <div className="flex flex-wrap gap-1.5 max-h-32 overflow-y-auto">
                  {result.detectedSkills.map((s, idx) => (
                    <span
                      key={idx}
                      className="px-2 py-0.5 rounded bg-dark-950 border border-white/[0.06] text-ink-secondary text-[11px] font-mono"
                    >
                      {s}
                    </span>
                  ))}
                </div>
              </div>
            )}

            <button
              onClick={onClose}
              className="w-full mt-4 py-2.5 rounded-xl bg-signal-emerald hover:bg-signal-emerald/90 text-dark-950 font-mono font-bold text-xs tracking-wider transition"
            >
              CONTINUE TO WORKSTATION
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            <label className="border-2 border-dashed border-white/10 hover:border-signal-emerald/40 rounded-xl p-8 flex flex-col items-center justify-center cursor-pointer transition bg-dark-950/60 group">
              <input type="file" accept=".pdf" onChange={handleFileChange} className="hidden" />
              <FileText className="w-8 h-8 text-ink-muted group-hover:text-signal-emerald mb-2 transition" />
              <span className="text-xs font-mono text-ink-secondary group-hover:text-ink-primary transition">
                {file ? file.name : 'Select or drop master PDF resume'}
              </span>
              <span className="text-[10px] font-mono text-ink-muted mt-1">PDF format up to 10MB</span>
            </label>

            <button
              onClick={handleUpload}
              disabled={!file || loading}
              className="w-full py-2.5 rounded-xl bg-signal-emerald hover:bg-signal-emerald/90 disabled:opacity-40 text-dark-950 font-mono font-bold text-xs tracking-wider transition flex items-center justify-center gap-2 shadow-lg shadow-signal-emerald/20"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin text-dark-950" />
                  <span>PARSING AND VERIFYING...</span>
                </>
              ) : (
                <span>INGEST MASTER RESUME</span>
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
