import { useEffect, useRef, useState } from 'react';
import { api, getApiErrorMessage } from '../api/client';
import {
  JobRoleResponse,
  ResumeAnalysisResponse,
  ResumeSummaryResponse,
} from '../types';
import { Alert } from '../components/Alert';
import { Pill } from '../components/Pill';
import { ScoreBar } from '../components/ScoreBar';
import { InlineSpinner, Spinner } from '../components/Spinner';

export function ResumeUpload() {
  const [resumes, setResumes] = useState<ResumeSummaryResponse[]>([]);
  const [jobRoles, setJobRoles] = useState<JobRoleResponse[]>([]);
  const [selectedRole, setSelectedRole] = useState<Record<number, number>>({});
  const [analysis, setAnalysis] = useState<Record<number, ResumeAnalysisResponse>>({});
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [analyzingId, setAnalyzingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const [resumeRes, roleRes] = await Promise.all([
          api.get<ResumeSummaryResponse[]>('/resumes'),
          api.get<JobRoleResponse[]>('/job-roles'),
        ]);
        if (!active) return;
        setResumes(resumeRes.data);
        setJobRoles(roleRes.data);
      } catch (err) {
        if (active) setError(getApiErrorMessage(err, 'Could not load resumes and job roles'));
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, []);

  async function handleUpload() {
    if (!selectedFile) return;
    setUploading(true);
    setError(null);
    setSuccess(null);
    const formData = new FormData();
    formData.append('file', selectedFile);
    try {
      const response = await api.post<ResumeSummaryResponse>('/resumes/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setResumes((prev) => [response.data, ...prev.filter((r) => r.id !== response.data.id)]);
      setSelectedFile(null);
      if (fileInput.current) fileInput.current.value = '';
      setSuccess(`Resume "${response.data.fileName}" uploaded - extracted ${response.data.parsedSkills.length} skills.`);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Upload failed'));
    } finally {
      setUploading(false);
    }
  }

  async function handleAnalyze(resumeId: number) {
    const roleId = selectedRole[resumeId];
    if (!roleId) {
      setError('Select a job role to analyze against first.');
      return;
    }
    setAnalyzingId(resumeId);
    setError(null);
    try {
      const response = await api.post<ResumeAnalysisResponse>(`/resumes/${resumeId}/analyze`, {
        jobRoleId: roleId,
      });
      setAnalysis((prev) => ({ ...prev, [resumeId]: response.data }));
      setResumes((prev) =>
        prev.map((r) =>
          r.id === resumeId
            ? {
                ...r,
                analysisStatus: response.data.status,
                atsScore: response.data.atsScore,
                keywordMatchScore: response.data.keywordMatchScore,
                analysisSummary: response.data.summary,
                parsedSkills: response.data.extractedSkills,
              }
            : r
        )
      );
    } catch (err) {
      setError(getApiErrorMessage(err, 'Analysis failed'));
    } finally {
      setAnalyzingId(null);
    }
  }

  async function handleDelete(resumeId: number) {
    if (!window.confirm('Delete this resume? This cannot be undone.')) return;
    setDeletingId(resumeId);
    try {
      await api.delete(`/resumes/${resumeId}`);
      setResumes((prev) => prev.filter((r) => r.id !== resumeId));
      setAnalysis((prev) => {
        const next = { ...prev };
        delete next[resumeId];
        return next;
      });
    } catch (err) {
      setError(getApiErrorMessage(err, 'Delete failed'));
    } finally {
      setDeletingId(null);
    }
  }

  if (loading) return <Spinner label="Loading resumes..." />;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Resume Analysis</h1>
          <p className="muted">Upload your resume, then run a resume analysis against a target job role.</p>
        </div>
      </div>

      {error ? <Alert kind="error" onClose={() => setError(null)}>{error}</Alert> : null}
      {success ? <Alert kind="success" onClose={() => setSuccess(null)}>{success}</Alert> : null}

      <section className="panel upload-panel">
        <input
          ref={fileInput}
          type="file"
          accept=".pdf,.docx,.doc,.txt,.md,.rtf,.odt"
          onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
        />
        <p className="muted">PDF, DOCX, DOC, TXT, MD, RTF or ODT up to 10MB.</p>
        <button
          type="button"
          className="btn btn-primary"
          onClick={handleUpload}
          disabled={!selectedFile || uploading}
        >
          {uploading ? <InlineSpinner /> : selectedFile ? `Upload ${selectedFile.name}` : 'Choose a file above'}
        </button>
      </section>

      {resumes.length === 0 ? (
        <Alert kind="info">No resumes uploaded yet. Add one to see your ATS score and skill extraction.</Alert>
      ) : (
        resumes.map((resume) => {
          const resumeResult = analysis[resume.id];
          return (
            <section key={resume.id} className="panel resume-card">
              <div className="row-between">
                <div>
                  <h2 className="panel-title">{resume.fileName}</h2>
                  <p className="muted">
                    Uploaded {new Date(resume.createdAt).toLocaleDateString()} &middot;{' '}
                    {resume.fileType?.toUpperCase()} &middot;{' '}
                    {resume.fileSize ? `${Math.round(resume.fileSize / 1024)} KB` : ''}
                    {' '}&middot; <Pill tone={resume.analysisStatus === 'ANALYZED' ? 'success' : 'info'}>{resume.analysisStatus}</Pill>
                  </p>
                </div>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  onClick={() => handleDelete(resume.id)}
                  disabled={deletingId === resume.id}
                >
                  {deletingId === resume.id ? 'Deleting...' : 'Delete'}
                </button>
              </div>

              {resume.atsScore != null && !resumeResult ? (
                <div className="analysis-summary">
                  <ScoreBar label="ATS score" value={resume.atsScore} />
                  <ScoreBar label="Keyword match" value={resume.keywordMatchScore ?? 0} />
                </div>
              ) : null}

              <div className="analyze-row">
                <select
                  value={selectedRole[resume.id] ?? ''}
                  onChange={(e) =>
                    setSelectedRole((prev) => ({ ...prev, [resume.id]: Number(e.target.value) }))
                  }
                  className="select"
                >
                  <option value="">Select job role...</option>
                  {jobRoles.map((role) => (
                    <option key={role.id} value={role.id}>
                      {role.title}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => handleAnalyze(resume.id)}
                  disabled={analyzingId === resume.id}
                >
                  {analyzingId === resume.id ? <InlineSpinner /> : 'Analyze against role'}
                </button>
              </div>

              {resumeResult ? (
                <div className="analysis-result">
                  <div className="analysis-scores">
                    <ScoreBar label="ATS score" value={resumeResult.atsScore} />
                    <ScoreBar label="Keyword match" value={resumeResult.keywordMatchScore} />
                  </div>
                  <p className="analysis-summary">{resumeResult.summary}</p>
                  <div className="resume-skills">
                    {resumeResult.extractedSkills.map((skill) => (
                      <Pill key={skill}>{skill}</Pill>
                    ))}
                  </div>
                  <div className="lists-2">
                    <div>
                      <h3>Strengths</h3>
                      <ul className="check-list">
                        {resumeResult.strengths.map((item) => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <h3>Improvements</h3>
                      <ul className="check-list warn">
                        {resumeResult.improvements.map((item) => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    </div>
                  </div>
                </div>
              ) : resume.analysisSummary ? (
                <p className="muted">{resume.analysisSummary}</p>
              ) : null}
            </section>
          );
        })
      )}
    </div>
  );
}