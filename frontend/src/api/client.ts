import axios from 'axios';
import { ApiResponse, AuthResponse, CandidateProfile, CandidateSkill, ResumeUploadResponse } from '../types';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('jh_token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const apiClient = {
  auth: {
    login: async (email: string, password: string): Promise<AuthResponse> => {
      const res = await api.post<ApiResponse<AuthResponse>>('/auth/login', { email, password });
      return res.data.data;
    },
    register: async (payload: { email: string; password: string; firstName: string; lastName: string }): Promise<AuthResponse> => {
      const res = await api.post<ApiResponse<AuthResponse>>('/auth/register', payload);
      return res.data.data;
    },
  },
  profile: {
    get: async (): Promise<CandidateProfile> => {
      const res = await api.get<ApiResponse<CandidateProfile>>('/profile');
      return res.data.data;
    },
    update: async (data: Partial<CandidateProfile>): Promise<CandidateProfile> => {
      const res = await api.put<ApiResponse<CandidateProfile>>('/profile', data);
      return res.data.data;
    },
    updateSkills: async (skills: CandidateSkill[]): Promise<CandidateSkill[]> => {
      const res = await api.put<ApiResponse<CandidateSkill[]>>('/profile/skills', skills);
      return res.data.data;
    },
    getReadiness: async (): Promise<import('../types').ProfileReadinessReport> => {
      const res = await api.get<ApiResponse<import('../types').ProfileReadinessReport>>('/profile/readiness');
      return res.data.data;
    },
  },
  resumes: {
    upload: async (file: File): Promise<ResumeUploadResponse> => {
      const formData = new FormData();
      formData.append('file', file);
      const res = await api.post<ApiResponse<ResumeUploadResponse>>('/resumes/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      return res.data.data;
    },
  },
  jobs: {
    discover: async (request?: { maxQueries?: number; searchLimitPerQuery?: number; sources?: string[] }): Promise<import('../types').DiscoverySummary> => {
      const res = await api.post<ApiResponse<import('../types').DiscoverySummary>>('/jobs/discover', request || {});
      return res.data.data;
    },
    list: async (filters?: {
      role?: string;
      location?: string;
      workMode?: string;
      technology?: string;
      source?: string;
      minSalary?: number;
      priority?: string;
      freshness?: string;
      sortBy?: string;
      page?: number;
      size?: number;
    }): Promise<import('../types').PageResponse<import('../types').Job>> => {
      const res = await api.get<ApiResponse<import('../types').PageResponse<import('../types').Job>>>('/jobs', {
        params: filters,
      });
      return res.data.data;
    },
    getById: async (id: string): Promise<import('../types').Job> => {
      const res = await api.get<ApiResponse<import('../types').Job>>(`/jobs/${id}`);
      return res.data.data;
    },
    getSearchRuns: async (): Promise<import('../types').SearchRun[]> => {
      const res = await api.get<ApiResponse<import('../types').SearchRun[]>>('/jobs/search-runs');
      return res.data.data;
    },
    setApplied: async (id: string, applied: boolean): Promise<{ jobId: string; applied: boolean; appliedAt?: string }> => {
      const res = await api.post<ApiResponse<{ jobId: string; applied: boolean; appliedAt?: string }>>(`/jobs/${id}/applied?applied=${applied}`);
      return res.data.data;
    },
  },
  matching: {
    analyze: async (jobId: string): Promise<import('../types').MatchAnalysisResponse> => {
      const res = await api.post<ApiResponse<import('../types').MatchAnalysisResponse>>(`/jobs/${jobId}/analyze`);
      return res.data.data;
    },
    getMatch: async (jobId: string): Promise<import('../types').MatchAnalysisResponse> => {
      const res = await api.get<ApiResponse<import('../types').MatchAnalysisResponse>>(`/jobs/${jobId}/match`);
      return res.data.data;
    },
    getRequirements: async (jobId: string): Promise<import('../types').JobRequirementResponse[]> => {
      const res = await api.get<ApiResponse<import('../types').JobRequirementResponse[]>>(`/jobs/${jobId}/requirements`);
      return res.data.data;
    },
    getAdvantage: async (jobId: string): Promise<import('../types').ApplicationAdvantageReport> => {
      const res = await api.get<ApiResponse<import('../types').ApplicationAdvantageReport>>(`/jobs/${jobId}/advantage`);
      return res.data.data;
    },
    getTailoringRecommendations: async (jobId: string): Promise<import('../types').TailoringRecommendation> => {
      const res = await api.get<ApiResponse<import('../types').TailoringRecommendation>>(`/jobs/${jobId}/tailoring`);
      return res.data.data;
    },
  },
  tailoring: {
    tailorResume: async (jobId: string): Promise<import('../types').TailoredResume> => {
      const res = await api.post<ApiResponse<import('../types').TailoredResume>>(`/jobs/${jobId}/tailor`);
      return res.data.data;
    },
    getTailoringPlan: async (jobId: string): Promise<import('../types').TailoringPlan> => {
      const res = await api.get<ApiResponse<import('../types').TailoringPlan>>(`/jobs/${jobId}/tailor/plan`);
      return res.data.data;
    },
    getTailoredResume: async (id: string): Promise<import('../types').TailoredResume> => {
      const res = await api.get<ApiResponse<import('../types').TailoredResume>>(`/tailored-resumes/${id}`);
      return res.data.data;
    },
    getTailoredResumesForJob: async (jobId: string): Promise<import('../types').TailoredResume[]> => {
      const res = await api.get<ApiResponse<import('../types').TailoredResume[]>>(`/tailored-resumes/job/${jobId}`);
      return res.data.data;
    },
    getDiff: async (id: string): Promise<import('../types').TailoringDiff> => {
      const res = await api.get<ApiResponse<import('../types').TailoringDiff>>(`/tailored-resumes/${id}/changes`);
      return res.data.data;
    },
    validate: async (id: string): Promise<import('../types').ResumeValidationReport> => {
      const res = await api.post<ApiResponse<import('../types').ResumeValidationReport>>(`/tailored-resumes/${id}/validate`);
      return res.data.data;
    },
    getPdfDownloadUrl: (id: string): string => {
      const token = localStorage.getItem('jh_token');
      return `/api/tailored-resumes/${id}/download${token ? `?token=${encodeURIComponent(token)}` : ''}`;
    },
    downloadPdf: async (id: string, filename = 'tailored_resume.pdf'): Promise<void> => {
      const token = localStorage.getItem('jh_token');
      const res = await api.get(`/tailored-resumes/${id}/download`, {
        responseType: 'blob',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename.endsWith('.pdf') ? filename : `${filename}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    },
    downloadLatex: async (id: string, filename = 'tailored-resume.tex'): Promise<void> => {
      const token = localStorage.getItem('jh_token');
      const res = await api.get(`/v1/tailored-resumes/${id}/download-latex`, {
        responseType: 'blob',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      const blob = new Blob([res.data], { type: 'application/x-tex' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename.endsWith('.tex') ? filename : `${filename}.tex`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    },
    renderLatex: async (id: string): Promise<any> => {
      const res = await api.post(`/v1/tailored-resumes/${id}/render-latex`);
      return res.data.data;
    },
  },
};
