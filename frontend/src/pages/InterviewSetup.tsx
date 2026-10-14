import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, getApiErrorMessage } from '../api/client';
import { DifficultyLevel, InterviewSessionResponse, JobRoleResponse } from '../types';
import { Alert } from '../components/Alert';
import { InlineSpinner, Spinner } from '../components/Spinner';

const difficultyOptions: DifficultyLevel[] = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];

export function InterviewSetup() {
  const navigate = useNavigate();
  const [roles, setRoles] = useState<JobRoleResponse[]>([]);
  const [jobRoleId, setJobRoleId] = useState<number>(0);
  const [numberOfQuestions, setNumberOfQuestions] = useState(5);
  const [difficulty, setDifficulty] = useState<DifficultyLevel>('INTERMEDIATE');
  const [timeLimitSeconds, setTimeLimitSeconds] = useState<number>(900);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    api
      .get<JobRoleResponse[]>('/job-roles')
      .then((response) => {
        if (!active) return;
        setRoles(response.data);
        if (response.data.length > 0) setJobRoleId(response.data[0].id);
      })
      .catch((err) => {
        if (active) setError(getApiErrorMessage(err, 'Could not load job roles'));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!jobRoleId) {
      setError('Select a job role first.');
      return;
    }
    setCreating(true);
    setError(null);
    try {
      const response = await api.post<InterviewSessionResponse>('/interviews/setup', {
        jobRoleId,
        numberOfQuestions,
        difficulty,
        timeLimitSeconds,
      });
      navigate(`/interviews/${response.data.id}`);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Could not set up the interview'));
    } finally {
      setCreating(false);
    }
  }

  if (loading) return <Spinner label="Loading job roles..." />;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>New Mock Interview</h1>
          <p className="muted">Configure your interview and start practicing.</p>
        </div>
      </div>

      {error ? <Alert kind="error" onClose={() => setError(null)}>{error}</Alert> : null}

      <form onSubmit={handleSubmit} className="panel form-grid">
        <label className="field span-2">
          <span>Job role</span>
          <select
            value={jobRoleId}
            onChange={(e) => setJobRoleId(Number(e.target.value))}
            className="select"
          >
            <option value={0}>Select a job role...</option>
            {roles.map((role) => (
              <option key={role.id} value={role.id}>
                {role.title}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Number of questions</span>
          <input
            type="number"
            min={3}
            max={20}
            value={numberOfQuestions}
            onChange={(e) => setNumberOfQuestions(Number(e.target.value))}
          />
        </label>
        <label className="field">
          <span>Difficulty</span>
          <select
            value={difficulty}
            onChange={(e) => setDifficulty(e.target.value as DifficultyLevel)}
            className="select"
          >
            {difficultyOptions.map((option) => (
              <option key={option} value={option}>
                {option.charAt(0) + option.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Time limit (seconds, optional)</span>
          <input
            type="number"
            min={1}
            value={timeLimitSeconds}
            onChange={(e) => setTimeLimitSeconds(Number(e.target.value))}
            placeholder="e.g. 900"
          />
        </label>
        <div className="span-2 form-actions">
          <button type="submit" className="btn btn-primary btn-lg" disabled={creating}>
            {creating ? <InlineSpinner /> : 'Generate interview'}
          </button>
        </div>
      </form>
    </div>
  );
}