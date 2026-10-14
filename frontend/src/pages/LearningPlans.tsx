import { FormEvent, useEffect, useState } from 'react';
import { api, getApiErrorMessage } from '../api/client';
import { JobRoleResponse, LearningPlanResponse, LearningPlanItemResponse, ResumeSummaryResponse, InterviewSessionResponse } from '../types';
import { Alert } from '../components/Alert';
import { Pill } from '../components/Pill';
import { ScoreBar } from '../components/ScoreBar';
import { InlineSpinner, Spinner } from '../components/Spinner';

export function LearningPlans() {
  const [plans, setPlans] = useState<LearningPlanResponse[]>([]);
  const [roles, setRoles] = useState<JobRoleResponse[]>([]);
  const [resumes, setResumes] = useState<ResumeSummaryResponse[]>([]);
  const [sessions, setSessions] = useState<InterviewSessionResponse[]>([]);
  const [jobRoleId, setJobRoleId] = useState<number>(0);
  const [resumeId, setResumeId] = useState<number>(0);
  const [sessionId, setSessionId] = useState<number>(0);
  const [expanded, setExpanded] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [togglingId, setTogglingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const [plansRes, rolesRes, resumeRes, sessionRes] = await Promise.all([
          api.get<LearningPlanResponse[]>('/learning-plans'),
          api.get<JobRoleResponse[]>('/job-roles'),
          api.get<ResumeSummaryResponse[]>('/resumes'),
          api.get<InterviewSessionResponse[]>('/interviews'),
        ]);
        if (!active) return;
        setPlans(plansRes.data);
        setRoles(rolesRes.data);
        setResumes(resumeRes.data);
        setSessions(sessionRes.data);
        if (rolesRes.data.length > 0) setJobRoleId(rolesRes.data[0].id);
      } catch (err) {
        if (active) setError(getApiErrorMessage(err, 'Could not load learning plans'));
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, []);

  async function generate(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (!jobRoleId) {
      setError('Select a job role.');
      return;
    }
    setGenerating(true);
    try {
      const response = await api.post<LearningPlanResponse>('/learning-plans', {
        jobRoleId,
        resumeId: resumeId || null,
        interviewSessionId: sessionId || null,
      });
      setPlans((prev) => [response.data, ...prev]);
      setExpanded(response.data.id);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Could not generate the learning plan'));
    } finally {
      setGenerating(false);
    }
  }

  async function completeItem(planId: number, item: LearningPlanItemResponse) {
    setTogglingId(item.id);
    setError(null);
    try {
      const response = await api.post<LearningPlanResponse>(
        `/learning-plans/${planId}/items/${item.id}/complete`
      );
      setPlans((prev) => prev.map((p) => (p.id === planId ? response.data : p)));
    } catch (err) {
      setError(getApiErrorMessage(err, 'Could not update the item'));
    } finally {
      setTogglingId(null);
    }
  }

  async function deletePlan(planId: number) {
    if (!window.confirm('Delete this learning plan?')) return;
    setDeletingId(planId);
    try {
      await api.delete(`/learning-plans/${planId}`);
      setPlans((prev) => prev.filter((p) => p.id !== planId));
    } catch (err) {
      setError(getApiErrorMessage(err, 'Could not delete the plan'));
    } finally {
      setDeletingId(null);
    }
  }

  if (loading) return <Spinner label="Loading learning plans..." />;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Learning Plans</h1>
          <p className="muted">Generate a personalized study plan and track your progress.</p>
        </div>
      </div>

      {error ? <Alert kind="error" onClose={() => setError(null)}>{error}</Alert> : null}

      <form onSubmit={generate} className="panel form-grid">
        <h2 className="panel-title span-2">Generate a new plan</h2>
        <label className="field">
          <span>Job role</span>
          <select value={jobRoleId} onChange={(e) => setJobRoleId(Number(e.target.value))} className="select">
            <option value={0}>Select...</option>
            {roles.map((role) => (
              <option key={role.id} value={role.id}>{role.title}</option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Resume (optional)</span>
          <select value={resumeId} onChange={(e) => setResumeId(Number(e.target.value))} className="select">
            <option value={0}>None</option>
            {resumes.map((resume) => (
              <option key={resume.id} value={resume.id}>{resume.fileName}</option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Completed interview (optional)</span>
          <select value={sessionId} onChange={(e) => setSessionId(Number(e.target.value))} className="select">
            <option value={0}>None</option>
            {sessions
              .filter((session) => session.status === 'COMPLETED')
              .map((session) => (
                <option key={session.id} value={session.id}>
                  {session.jobRoleTitle ?? 'Interview'} #{session.id}
                </option>
              ))}
          </select>
        </label>
        <div className="field form-actions field-end">
          <button type="submit" className="btn btn-primary" disabled={generating}>
            {generating ? <InlineSpinner /> : 'Generate plan'}
          </button>
        </div>
      </form>

      {plans.length === 0 ? (
        <Alert kind="info">
          No learning plans yet. Generate one above to turn your skill gaps into a study plan.
        </Alert>
      ) : (
        plans.map((plan) => (
          <section key={plan.id} className="panel plan-card">
            <div className="row-between">
              <div>
                <div className="row-gap">
                  <h2 className="panel-title">{plan.title}</h2>
                  <Pill tone={plan.status === 'COMPLETED' ? 'success' : plan.status === 'IN_PROGRESS' ? 'warning' : 'info'}>
                    {plan.progressPercent}%
                  </Pill>
                </div>
                <p className="muted">
                  {plan.completedItems} of {plan.totalItems} items completed
                </p>
              </div>
              <div className="row-gap">
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => deletePlan(plan.id)} disabled={deletingId === plan.id}>
                  {deletingId === plan.id ? 'Deleting...' : 'Delete'}
                </button>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setExpanded(expanded === plan.id ? null : plan.id)}
                >
                  {expanded === plan.id ? 'Collapse' : 'Open'}
                </button>
              </div>
            </div>
            {plan.description ? <p className="muted">{plan.description}</p> : null}
            {plan.focusSkills.length > 0 ? (
              <div className="resume-skills">
                {plan.focusSkills.map((skill) => (
                  <Pill key={skill}>{skill}</Pill>
                ))}
              </div>
            ) : null}
            <ScoreBar label="Plan progress" value={plan.progressPercent} />

            {expanded === plan.id ? (
              <ul className="item-list">
                {plan.items.map((item) => (
                  <li key={item.id} className="plan-item">
                    <div className="item-main">
                      <div className="row-gap">
                        <span className={item.completed ? 'plan-check done' : 'plan-check'} role="button" tabIndex={0}
                          onClick={() => completeItem(plan.id, item)} onKeyDown={(e) => { if (e.key === 'Enter') completeItem(plan.id, item); }}>
                          {togglingId === item.id ? '…' : item.completed ? '✓' : ''}
                        </span>
                        <div>
                          <strong>{item.title}</strong>
                          {item.skillName ? <Pill>{item.skillName}</Pill> : null}
                          {item.description ? <p className="muted">{item.description}</p> : null}
                          {item.targetDays ? (
                            <p className="muted">Target: {item.targetDays} days</p>
                          ) : null}
                        </div>
                      </div>
                    </div>
                    {item.resourceUrl ? (
                      <a href={item.resourceUrl} target="_blank" rel="noreferrer" className="btn btn-ghost btn-sm">
                        Resource
                      </a>
                    ) : null}
                  </li>
                ))}
              </ul>
            ) : null}
          </section>
        ))
      )}
    </div>
  );
}