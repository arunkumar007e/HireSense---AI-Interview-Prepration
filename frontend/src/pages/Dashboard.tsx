import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, getApiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { InterviewSessionResponse, ProgressSummaryResponse, ResumeSummaryResponse } from '../types';
import { StatCard } from '../components/StatCard';
import { Spinner } from '../components/Spinner';
import { Alert } from '../components/Alert';
import { Pill } from '../components/Pill';

export function Dashboard() {
  const { user } = useAuth();
  const [summary, setSummary] = useState<ProgressSummaryResponse | null>(null);
  const [resumes, setResumes] = useState<ResumeSummaryResponse[]>([]);
  const [interviews, setInterviews] = useState<InterviewSessionResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const [summaryRes, resumeRes, interviewRes] = await Promise.all([
          api.get<ProgressSummaryResponse>('/progress/summary'),
          api.get<ResumeSummaryResponse[]>('/resumes'),
          api.get<InterviewSessionResponse[]>('/interviews'),
        ]);
        if (!active) return;
        setSummary(summaryRes.data);
        setResumes(resumeRes.data);
        setInterviews(interviewRes.data);
      } catch (err) {
        if (active) setError(getApiErrorMessage(err, 'Could not load your dashboard'));
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, []);

  if (loading) return <Spinner label="Loading your dashboard..." />;

  const latestResume = resumes[0];
  const inProgressInterviews = interviews.filter((i) => i.status === 'IN_PROGRESS');
  const recentInterviews = interviews.slice(0, 5);

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Welcome back, {user?.fullName?.split(' ')[0]}</h1>
          <p className="muted">Here is an overview of your interview preparation.</p>
        </div>
        <div className="page-actions">
          <Link to="/interviews/setup" className="btn btn-primary">Start Interview</Link>
        </div>
      </div>

      {error ? <Alert kind="error" onClose={() => setError(null)}>{error}</Alert> : null}

      {summary && (
        <div className="stats-grid">
          <StatCard label="Interviews completed" value={summary.interviewsCompleted} />
          <StatCard label="Average interview score" value={`${summary.interviewScoreAverage}%`} accent="var(--primary)" />
          <StatCard label="Questions answered" value={summary.questionsAnswered} />
          <StatCard label="Learning plans" value={summary.learningPlansCreated} />
        </div>
      )}

      {inProgressInterviews.length > 0 && (
        <section className="panel">
          <h2 className="panel-title">Continue your interview</h2>
          {inProgressInterviews.map((session) => (
            <div key={session.id} className="row-between">
              <div>
                <strong>{session.jobRoleTitle ?? 'General interview'}</strong>
                <p className="muted">
                  Question {session.currentQuestionIndex} of {session.numberOfQuestions}
                </p>
              </div>
              <Link to={`/interviews/${session.id}`} className="btn btn-secondary">Continue</Link>
            </div>
          ))}
        </section>
      )}

      <div className="grid-2">
        <section className="panel">
          <div className="panel-head">
            <h2 className="panel-title">Recent interviews</h2>
            <Link to="/interviews" className="link">View all</Link>
          </div>
          {recentInterviews.length === 0 ? (
            <EmptyState
              text="No interviews yet. Start one to get structured feedback on your skills."
              action={<Link to="/interviews/setup" className="btn btn-secondary btn-sm">Set up an interview</Link>}
            />
          ) : (
            <ul className="item-list">
              {recentInterviews.map((session) => (
                <li key={session.id} className="item-row">
                  <div className="item-main">
                    <strong>{session.jobRoleTitle ?? 'General interview'}</strong>
                    <span className="muted">{new Date(session.createdAt).toLocaleDateString()}</span>
                  </div>
                  <span>
                    <Pill tone={statusTone(session.status)}>{session.status}</Pill>
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="panel">
          <div className="panel-head">
            <h2 className="panel-title">Latest resume</h2>
            <Link to="/resumes" className="link">Manage resumes</Link>
          </div>
          {latestResume ? (
            <div>
              <div className="row-between">
                <strong>{latestResume.fileName}</strong>
                <Pill tone={latestResume.analysisStatus === 'ANALYZED' ? 'success' : 'info'}>
                  {latestResume.analysisStatus}
                </Pill>
              </div>
              {latestResume.atsScore != null ? (
                <>
                  <p className="muted">ATS score: {latestResume.atsScore}% &middot; Keyword match: {latestResume.keywordMatchScore}%</p>
                  <div className="resume-skills">
                    {latestResume.parsedSkills.slice(0, 8).map((skill) => (
                      <Pill key={skill}>{skill}</Pill>
                    ))}
                  </div>
                </>
              ) : (
                <p className="muted">Upload a resume and run an analysis to see your ATS score.</p>
              )}
            </div>
          ) : (
            <EmptyState
              text="No resume uploaded yet."
              action={<Link to="/resumes" className="btn btn-secondary btn-sm">Upload resume</Link>}
            />
          )}
        </section>
      </div>
    </div>
  );
}

function statusTone(status: string): string {
  if (status === 'COMPLETED') return 'success';
  if (status === 'IN_PROGRESS') return 'warning';
  if (status === 'CONFIGURED') return 'info';
  return 'danger';
}

function EmptyState({ text, action }: { text: string; action: React.ReactNode }) {
  return (
    <div className="empty">
      <p className="muted">{text}</p>
      {action}
    </div>
  );
}