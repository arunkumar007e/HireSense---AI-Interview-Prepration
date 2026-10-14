import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, getApiErrorMessage } from '../api/client';
import { AnswerEvaluationResponse, InterviewQuestionResponse, InterviewSessionResponse } from '../types';
import { Alert } from '../components/Alert';
import { ScoreBar } from '../components/ScoreBar';
import { Pill } from '../components/Pill';
import { InlineSpinner, Spinner } from '../components/Spinner';

export function MockInterview() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const [session, setSession] = useState<InterviewSessionResponse | null>(null);
  const [questions, setQuestions] = useState<InterviewQuestionResponse[]>([]);
  const [index, setIndex] = useState(0);
  const [answer, setAnswer] = useState('');
  const [evaluation, setEvaluation] = useState<AnswerEvaluationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const [sessionRes, questionRes] = await Promise.all([
          api.get<InterviewSessionResponse>(`/interviews/${sessionId}`),
          api.get<InterviewQuestionResponse[]>(`/interviews/${sessionId}/questions`),
        ]);
        if (!active) return;
        setSession(sessionRes.data);
        setQuestions(questionRes.data);
        const startIndex = Math.min(sessionRes.data.currentQuestionIndex, questionRes.data.length - 1);
        setIndex(Math.max(0, startIndex));
        if (
          sessionRes.data.timeLimitSeconds &&
          sessionRes.data.status === 'IN_PROGRESS' &&
          sessionRes.data.startedAt
        ) {
          const elapsedSeconds = Math.floor(
            (Date.now() - new Date(sessionRes.data.startedAt).getTime()) / 1000
          );
          setSecondsLeft(Math.max(0, sessionRes.data.timeLimitSeconds - elapsedSeconds));
        }
      } catch (err) {
        if (active) setError(getApiErrorMessage(err, 'Could not load the interview'));
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [sessionId]);

  useEffect(() => {
    if (!session || session.status !== 'IN_PROGRESS' || secondsLeft === null) return;
    timerRef.current = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev === null || prev <= 1) {
          if (timerRef.current) clearInterval(timerRef.current);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [session?.status, secondsLeft]);

  const currentQuestion = questions[index];

  async function startInterview() {
    setStarting(true);
    setError(null);
    try {
      const response = await api.post<InterviewSessionResponse>(`/interviews/${sessionId}/start`);
      setSession(response.data);
      setIndex(Math.max(0, response.data.currentQuestionIndex));
      if (response.data.timeLimitSeconds) setSecondsLeft(response.data.timeLimitSeconds);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Could not start the interview'));
    } finally {
      setStarting(false);
    }
  }

  async function submitAnswer() {
    if (!session || !currentQuestion) return;
    setSubmitting(true);
    setError(null);
    try {
      const response = await api.post<AnswerEvaluationResponse>(
        `/interviews/${session.id}/questions/${currentQuestion.id}/answer`,
        { text: answer, timeTakenSeconds: null }
      );
      setEvaluation(response.data);
      const refreshed = await api.get<InterviewSessionResponse>(`/interviews/${session.id}`);
      setSession(refreshed.data);
      setIndex(Math.min(index + 1, questions.length - 1));
    } catch (err) {
      setError(getApiErrorMessage(err, 'Could not submit the answer'));
    } finally {
      setSubmitting(false);
    }
  }

  function nextQuestion() {
    setAnswer('');
    setEvaluation(null);
    setIndex((prev) => Math.min(prev + 1, questions.length - 1));
  }

  if (loading) return <Spinner label="Loading interview..." />;
  if (error && !session) {
    return (
      <div className="page">
        <Alert kind="error">{error}</Alert>
        <Link to="/interviews" className="btn btn-secondary">Back to interviews</Link>
      </div>
    );
  }
  if (!session || !currentQuestion) {
    return (
      <div className="page">
        <Alert kind="info">No questions available for this interview.</Alert>
      </div>
    );
  }

  const isConfigured = session.status === 'CONFIGURED';
  const isCompleted = session.status === 'COMPLETED';
  const answeredCount = questions.filter((q) => q.answered).length;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>{session.jobRoleTitle ?? 'Interview'} &middot; Mock Interview</h1>
          <p className="muted">
            Question {index + 1} of {questions.length} &middot; Difficulty {session.difficulty} &middot; Status{' '}
            <Pill tone={session.status === 'IN_PROGRESS' ? 'warning' : session.status === 'COMPLETED' ? 'success' : 'info'}>
              {session.status}
            </Pill>
          </p>
        </div>
        {secondsLeft !== null ? (
          <div className="timer">
            {Math.floor(secondsLeft / 60)}:{(secondsLeft % 60).toString().padStart(2, '0')}
          </div>
        ) : null}
      </div>

      {error ? <Alert kind="error" onClose={() => setError(null)}>{error}</Alert> : null}

      {isConfigured ? (
        <section className="panel centered">
          <h2 className="panel-title">Ready to begin?</h2>
          <p className="muted">
            You&apos;ll answer {session.numberOfQuestions} role-specific questions and receive structured
            feedback on each answer.
          </p>
          <button type="button" className="btn btn-primary btn-lg" onClick={startInterview} disabled={starting}>
            {starting ? <InlineSpinner /> : 'Start interview'}
          </button>
        </section>
      ) : isCompleted ? (
        <section className="panel centered">
          <h2 className="panel-title">Interview completed</h2>
          <p className="muted">You answered {answeredCount} questions. View your detailed results below.</p>
          <Link to={`/interviews/${session.id}/result`} className="btn btn-primary btn-lg">
            View results
          </Link>
        </section>
      ) : evaluation ? (
        <section className="panel">
          <div className="panel-head">
            <h2 className="panel-title">Your feedback</h2>
          </div>
          <div className="eval-scores">
            <ScoreBar label="Overall" value={evaluation.score} />
            <ScoreBar label="Relevance" value={evaluation.relevanceScore} />
            <ScoreBar label="Clarity" value={evaluation.clarityScore} />
            <ScoreBar label="Depth" value={evaluation.depthScore} />
          </div>
          <p className="eval-feedback">{evaluation.feedback}</p>
          <div className="lists-2">
            <div>
              <h3>Strengths</h3>
              <ul className="check-list">
                {evaluation.strengths.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </div>
            <div>
              <h3>To improve</h3>
              <ul className="check-list warn">
                {evaluation.improvements.length > 0
                  ? evaluation.improvements.map((item) => <li key={item}>{item}</li>)
                  : <li>Keep practicing to refine your answers.</li>}
              </ul>
            </div>
          </div>
          {evaluation.missingKeywords.length > 0 ? (
            <div>
              <h3>Keywords you missed</h3>
              <div className="resume-skills">
                {evaluation.missingKeywords.map((keyword) => (
                  <Pill key={keyword}>{keyword}</Pill>
                ))}
              </div>
            </div>
          ) : null}
          {evaluation.suggestedAnswer ? (
            <details className="suggested">
              <summary>See a suggested answer</summary>
              <p>{evaluation.suggestedAnswer}</p>
            </details>
          ) : null}
          <div className="form-actions">
            {index < questions.length - 1 ? (
              <button type="button" className="btn btn-primary" onClick={nextQuestion}>
                Next question
              </button>
            ) : (
              <Link to={`/interviews/${session.id}/result`} className="btn btn-primary">
                View final results
              </Link>
            )}
          </div>
        </section>
      ) : (
        <section className="panel">
          <div className="question-card">
            <p className="question-category">
              {currentQuestion.category} &middot; {currentQuestion.difficulty}
            </p>
            <h2 className="question-text">{currentQuestion.content}</h2>
          </div>
          <label className="field">
            <span>Your answer</span>
            <textarea
              value={answer}
              onChange={(e) => setAnswer(e.target.value)}
              rows={8}
              minLength={5}
              placeholder="Type your answer here. Share your experience, steps, and reasoning..."
            />
          </label>
          <div className="form-actions">
            <button
              type="button"
              className="btn btn-primary btn-lg"
              onClick={submitAnswer}
              disabled={submitting || answer.trim().length < 5}
            >
              {submitting ? <InlineSpinner /> : 'Submit answer &amp; get feedback'}
            </button>
          </div>
        </section>
      )}

      <div className="progress-track">
        {questions.map((question, i) => (
          <span
            key={question.id}
            className={`progress-dot ${i < index ? 'done' : ''} ${i === index ? 'current' : ''}`}
            title={`Question ${i + 1}`}
          />
        ))}
      </div>
    </div>
  );
}