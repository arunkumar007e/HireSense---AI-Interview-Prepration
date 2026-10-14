import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, getApiErrorMessage } from '../api/client';
import { InterviewResultResponse } from '../types';
import { Alert } from '../components/Alert';
import { Pill } from '../components/Pill';
import { Spinner } from '../components/Spinner';

export function InterviewResult() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const [result, setResult] = useState<InterviewResultResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    api
      .get<InterviewResultResponse>(`/interviews/${sessionId}/result`)
      .then((response) => {
        if (active) setResult(response.data);
      })
      .catch((err) => {
        if (active) setError(getApiErrorMessage(err, 'Could not load the result'));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [sessionId]);

  if (loading) return <Spinner label="Loading results..." />;

  if (error || !result) {
    return (
      <div className="page">
        <Alert kind="error">{error ?? 'No result available.'}</Alert>
        <Link to="/interviews" className="btn btn-secondary">Back to interviews</Link>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Interview Results</h1>
          <p className="muted">{result.jobRoleTitle} &middot; {result.questions.length} questions</p>
        </div>
        <div className="page-actions">
          <Link to="/interviews/setup" className="btn btn-primary">Take another interview</Link>
          <Link to="/learning-plans" className="btn btn-secondary">Generate learning plan</Link>
        </div>
      </div>

      <section className="panel score-hero">
        <div className="score-hero-number" style={{ color: heroColor(result.overallScore) }}>
          {Math.round(result.overallScore)}%
        </div>
        <div className="score-hero-text">
          <h2>Overall score</h2>
          <p>{result.summary}</p>
        </div>
      </section>

      <section className="panel">
        <h2 className="panel-title">Question breakdown</h2>
        <ul className="item-list">
          {result.questions.map((question) => (
            <li key={question.questionId} className="question-result">
              <div className="row-between">
                <div>
                  <p className="muted">{question.category}</p>
                  <strong className="question-text">{question.question}</strong>
                </div>
                <span className="question-score" style={{ color: heroColor(question.score) }}>
                  {Math.round(question.score)}%
                </span>
              </div>
              <details>
                <summary>Feedback &amp; suggested answer</summary>
                <div className="answer-details">
                  <h4>Your answer</h4>
                  <p className="muted">{question.userAnswer}</p>
                  <h4>Feedback</h4>
                  <p>{question.feedback}</p>
                  {question.strengths.length > 0 ? (
                    <>
                      <h4>Strengths</h4>
                      <ul className="check-list">
                        {question.strengths.map((item) => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    </>
                  ) : null}
                  {question.missingKeywords.length > 0 ? (
                    <>
                      <h4>Missing keywords</h4>
                      <div className="resume-skills">
                        {question.missingKeywords.map((keyword) => (
                          <Pill key={keyword}>{keyword}</Pill>
                        ))}
                      </div>
                    </>
                  ) : null}
                  <h4>Suggested answer</h4>
                  <p>{question.suggestedAnswer}</p>
                </div>
              </details>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

function heroColor(score: number): string {
  if (score >= 80) return 'var(--success)';
  if (score >= 60) return 'var(--warning)';
  return 'var(--danger)';
}