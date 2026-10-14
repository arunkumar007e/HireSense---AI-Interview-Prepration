import { useEffect, useState } from 'react';
import { api, getApiErrorMessage } from '../api/client';
import { ProgressResponse, ProgressSummaryResponse } from '../types';
import { Alert } from '../components/Alert';
import { Pill } from '../components/Pill';
import { StatCard } from '../components/StatCard';
import { Spinner } from '../components/Spinner';

const metricMeta: Record<string, { label: string; tone: string }> = {
  INTERVIEW_SCORE: { label: 'Interview score', tone: 'success' },
  QUESTIONS_ANSWERED: { label: 'Questions answered', tone: 'info' },
  SKILL_GAP: { label: 'Skill gap', tone: 'warning' },
  PLAN_COMPLETION: { label: 'Plan completion', tone: 'success' },
  RESUME_ANALYSIS: { label: 'Resume analysis', tone: 'info' },
};

export function ProgressPage() {
  const [summary, setSummary] = useState<ProgressSummaryResponse | null>(null);
  const [entries, setEntries] = useState<ProgressResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const [summaryRes, entriesRes] = await Promise.all([
          api.get<ProgressSummaryResponse>('/progress/summary'),
          api.get<ProgressResponse[]>('/progress'),
        ]);
        if (!active) return;
        setSummary(summaryRes.data);
        setEntries(entriesRes.data);
      } catch (err) {
        if (active) setError(getApiErrorMessage(err, 'Could not load progress'));
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, []);

  if (loading) return <Spinner label="Loading progress..." />;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>My Progress</h1>
          <p className="muted">Track your interview preparation over time.</p>
        </div>
      </div>

      {error ? <Alert kind="error" onClose={() => setError(null)}>{error}</Alert> : null}

      {summary && (
        <div className="stats-grid">
          <StatCard label="Interviews completed" value={summary.interviewsCompleted} />
          <StatCard label="Average score" value={`${summary.interviewScoreAverage}%`} accent="var(--primary)" />
          <StatCard label="Questions answered" value={summary.questionsAnswered} />
          <StatCard label="Plan completion" value={`${summary.learningPlanCompletionPercent}%`} />
        </div>
      )}

      <section className="panel">
        <h2 className="panel-title">Activity log</h2>
        {entries.length === 0 ? (
          <Alert kind="info">No activity yet. Complete an interview or analyze your resume to see progress here.</Alert>
        ) : (
          <ul className="item-list">
            {entries.map((entry) => {
              const meta = metricMeta[entry.metricType];
              return (
                <li key={entry.id} className="item-row">
                  <div className="item-main">
                    <span className="row-gap">
                      <Pill tone={meta?.tone ?? 'info'}>{meta?.label ?? entry.metricType}</Pill>
                      {entry.relatedTitle ? <strong>{entry.relatedTitle}</strong> : null}
                    </span>
                    <span className="muted">
                      {new Date(entry.recordedAt).toLocaleString()} &middot; {entry.note ?? ''}
                    </span>
                  </div>
                  <span className="metric-value">{Math.round(entry.metricValue)}</span>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </div>
  );
}