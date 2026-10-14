import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, getApiErrorMessage } from '../api/client';
import { InterviewSessionResponse } from '../types';
import { Alert } from '../components/Alert';
import { Pill } from '../components/Pill';
import { Spinner } from '../components/Spinner';

function toneFor(status: string): string {
  if (status === 'COMPLETED') return 'success';
  if (status === 'IN_PROGRESS') return 'warning';
  if (status === 'CONFIGURED') return 'info';
  return 'danger';
}

export function Interviews() {
  const [sessions, setSessions] = useState<InterviewSessionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    api
      .get<InterviewSessionResponse[]>('/interviews')
      .then((response) => {
        if (active) setSessions(response.data);
      })
      .catch((err) => {
        if (active) setError(getApiErrorMessage(err, 'Could not load interviews'));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  if (loading) return <Spinner label="Loading interviews..." />;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Interviews</h1>
          <p className="muted">Your mock interview sessions and results.</p>
        </div>
        <div className="page-actions">
          <Link to="/interviews/setup" className="btn btn-primary">New Interview</Link>
        </div>
      </div>

      {error ? <Alert kind="error" onClose={() => setError(null)}>{error}</Alert> : null}
      {sessions.length === 0 ? (
        <Alert kind="info">
          No interviews yet. <Link to="/interviews/setup">Set one up</Link> to start practicing.
        </Alert>
      ) : (
        <div className="panel">
          <ul className="item-list">
            {sessions.map((session) => (
              <li key={session.id} className="item-row">
                <div className="item-main">
                  <strong>{session.jobRoleTitle ?? 'General interview'}</strong>
                  <span className="muted">
                    {new Date(session.createdAt).toLocaleDateString()} &middot;{' '}
                    {session.numberOfQuestions} questions &middot; {session.difficulty}
                  </span>
                </div>
                <div className="item-side">
                  <Pill tone={toneFor(session.status)}>{session.status}</Pill>
                  {session.status === 'IN_PROGRESS' || session.status === 'CONFIGURED' ? (
                    <Link to={`/interviews/${session.id}`} className="btn btn-secondary btn-sm">
                      Continue
                    </Link>
                  ) : session.status === 'COMPLETED' ? (
                    <Link to={`/interviews/${session.id}/result`} className="btn btn-secondary btn-sm">
                      View result
                    </Link>
                  ) : null}
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}