import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, getApiErrorMessage } from '../api/client';
import {
  JobRoleResponse,
  ResumeSummaryResponse,
  SkillGapResponse,
} from '../types';
import { Alert } from '../components/Alert';
import { Pill, gapTone } from '../components/Pill';
import { ScoreBar } from '../components/ScoreBar';
import { InlineSpinner, Spinner } from '../components/Spinner';

export function JobRoles() {
  const [roles, setRoles] = useState<JobRoleResponse[]>([]);
  const [resumes, setResumes] = useState<ResumeSummaryResponse[]>([]);
  const [selectedId, setSelectedId] = useState<number>(0);
  const [resumeId, setResumeId] = useState<number>(0);
  const [manualSkills, setManualSkills] = useState('');
  const [useManual, setUseManual] = useState(false);
  const [gap, setGap] = useState<SkillGapResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const [rolesRes, resumeRes] = await Promise.all([
          api.get<JobRoleResponse[]>('/job-roles'),
          api.get<ResumeSummaryResponse[]>('/resumes'),
        ]);
        if (!active) return;
        setRoles(rolesRes.data);
        setResumes(resumeRes.data);
        if (rolesRes.data.length > 0) setSelectedId(rolesRes.data[0].id);
      } catch (err) {
        if (active) setError(getApiErrorMessage(err, 'Could not load job roles'));
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, []);

  const selectedRole = roles.find((role) => role.id === selectedId) ?? null;

  async function analyze() {
    setError(null);
    setGap(null);
    if (!selectedRole) return;
    let resumeSkills: string[];
    if (useManual) {
      resumeSkills = manualSkills
        .split(',')
        .map((s) => s.trim().toLowerCase())
        .filter(Boolean);
      if (resumeSkills.length === 0) {
        setError('Enter at least one skill separated by commas.');
        return;
      }
    } else {
      const resume = resumes.find((r) => r.id === resumeId) ?? resumes[0];
      if (!resume || resume.parsedSkills.length === 0) {
        setError('No resume skills available. Upload a resume or check the "Enter skills manually" option.');
        return;
      }
      resumeSkills = resume.parsedSkills.map((s) => s.toLowerCase());
    }
    setAnalyzing(true);
    try {
      const response = await api.post<SkillGapResponse>(
        `/job-roles/${selectedRole.id}/skill-gap`,
        { resumeSkills }
      );
      setGap(response.data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Skill gap analysis failed'));
    } finally {
      setAnalyzing(false);
    }
  }

  if (loading) return <Spinner label="Loading job roles..." />;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Job Roles &amp; Skill Gap</h1>
          <p className="muted">Compare your skills against a target role and see what to learn next.</p>
        </div>
      </div>

      {error ? <Alert kind="error" onClose={() => setError(null)}>{error}</Alert> : null}

      <div className="grid-2">
        <section className="panel">
          <div className="panel-head">
            <h2 className="panel-title">Choose a job role</h2>
          </div>
          <ul className="role-list">
            {roles.map((role) => (
              <li key={role.id}>
                <button
                  type="button"
                  className={role.id === selectedId ? 'role-item selected' : 'role-item'}
                  onClick={() => {
                    setSelectedId(role.id);
                    setGap(null);
                  }}
                >
                  <strong>{role.title}</strong>
                  {role.category ? <span className="muted">{role.category}</span> : null}
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section className="panel">
          {selectedRole ? (
            <>
              <h2 className="panel-title">{selectedRole.title}</h2>
              <p className="muted">{selectedRole.description}</p>
              <h3>Required skills</h3>
              <div className="resume-skills">
                {selectedRole.requiredSkills.map((skill) => (
                  <Pill key={skill.id}>{skill.name}</Pill>
                ))}
              </div>

              <div className="gap-inputs">
                <label className="field">
                  <span>Your skills source</span>
                  <select
                    value={useManual ? 'manual' : 'resume'}
                    onChange={(e) => setUseManual(e.target.value === 'manual')}
                    className="select"
                  >
                    <option value="resume">From my resume</option>
                    <option value="manual">Enter manually</option>
                  </select>
                </label>
                {useManual ? (
                  <label className="field">
                    <span>Skills (comma separated)</span>
                    <input
                      value={manualSkills}
                      onChange={(e) => setManualSkills(e.target.value)}
                      placeholder="java, spring boot, sql, docker"
                    />
                  </label>
                ) : (
                  <label className="field">
                    <span>Resume</span>
                    <select
                      value={resumeId}
                      onChange={(e) => setResumeId(Number(e.target.value))}
                      className="select"
                    >
                      {resumes.length === 0 ? (
                        <option value={0}>No resumes available</option>
                      ) : (
                        resumes.map((resume) => (
                          <option key={resume.id} value={resume.id}>
                            {resume.fileName} ({resume.parsedSkills.length} skills)
                          </option>
                        ))
                      )}
                    </select>
                  </label>
                )}
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={analyze}
                  disabled={analyzing || !selectedRole}
                >
                  {analyzing ? <InlineSpinner /> : 'Analyze skill gap'}
                </button>
              </div>
            </>
          ) : (
            <p className="muted">Select a job role to begin.</p>
          )}
        </section>
      </div>

      {gap ? (
        <section className="panel gap-result">
          <div className="panel-head">
            <h2 className="panel-title">Skill gap for {gap.jobRoleTitle}</h2>
          </div>
          <ScoreBar label="Match score" value={Math.round(gap.matchScore)} />
          <p className="gap-summary">{gap.summary}</p>
          <div className="gap-table">
            {gap.skills.map((item) => (
              <div key={item.skill} className="gap-row">
                <div className="gap-skill">
                  <strong>{item.skill}</strong>
                  <span className="muted">
                    {item.currentLevel} &rarr; {item.requiredLevel}
                  </span>
                </div>
                <Pill tone={gapTone(item.gap)}>{item.gap}</Pill>
              </div>
            ))}
          </div>
          <div className="gap-cta">
            <Link to="/learning-plans" className="btn btn-secondary">
              Generate a learning plan for these gaps
            </Link>
          </div>
        </section>
      ) : null}
    </div>
  );
}