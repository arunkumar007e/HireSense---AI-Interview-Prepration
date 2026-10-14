import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const features = [
  {
    title: 'ATS Resume Analysis',
    text: 'Upload your resume to get an ATS score, keyword match, and clear strengths and improvement suggestions based on your target role.',
  },
  {
    title: 'Skill Gap Analysis',
    text: 'Compare your skills against real job roles and see exactly what you need to learn next.',
  },
  {
    title: 'Mock Interviews',
    text: 'Practice role-specific technical and behavioral questions and get instant feedback on every answer.',
  },
  {
    title: 'Personalized Learning Plans',
    text: 'Turn weak topics into a prioritized study plan with curated resources and track your progress.',
  },
];

export function Landing() {
  const { token } = useAuth();
  return (
    <div className="landing">
      <header className="landing-nav">
        <div className="landing-inner">
          <span className="brand brand-lg">
            <span className="brand-mark">HS</span>
            <span className="brand-name">HireSense</span>
          </span>
          <div className="landing-actions">
            {token ? (
              <Link to="/dashboard" className="btn btn-primary">Go to Dashboard</Link>
            ) : (
              <>
                <Link to="/login" className="btn btn-ghost">Log in</Link>
                <Link to="/register" className="btn btn-primary">Get Started Free</Link>
              </>
            )}
          </div>
        </div>
      </header>

      <main>
        <section className="hero">
          <div className="hero-inner">
            <p className="hero-eyebrow">Interview preparation toolkit</p>
            <h1 className="hero-title">
              Prepare for your next interview with <span>structured practice</span>
            </h1>
            <p className="hero-subtitle">
              HireSense analyzes your resume, finds your skill gaps, runs realistic mock
              interviews, and builds a personalized learning plan - all in one place.
            </p>
            <div className="hero-cta">
              {token ? (
                <Link to="/dashboard" className="btn btn-primary btn-lg">Open Dashboard</Link>
              ) : (
                <Link to="/register" className="btn btn-primary btn-lg">Create free account</Link>
              )}
            </div>
          </div>
        </section>

        <section className="features">
          <div className="features-grid">
            {features.map((feature) => (
              <div key={feature.title} className="feature-card">
                <h3>{feature.title}</h3>
                <p>{feature.text}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="how-it-works">
          <h2>How it works</h2>
          <div className="steps">
            <div className="step">
              <span className="step-num">1</span>
              <h3>Upload your resume</h3>
              <p>We extract your skills and score your resume against target roles.</p>
            </div>
            <div className="step">
              <span className="step-num">2</span>
              <h3>Pick a job role</h3>
              <p>Choose from developer, DevOps, data and engineering roles.</p>
            </div>
            <div className="step">
              <span className="step-num">3</span>
              <h3>Take a mock interview</h3>
              <p>Answer role-specific questions and get instant feedback.</p>
            </div>
            <div className="step">
              <span className="step-num">4</span>
              <h3>Follow your learning plan</h3>
              <p>Close your skill gaps with a structured, trackable study plan.</p>
            </div>
          </div>
        </section>
      </main>

      <footer className="landing-footer">
        <div className="landing-inner">
          <span>HireSense &middot; Resume analysis, mock interviews, and personalized learning plans</span>
        </div>
      </footer>
    </div>
  );
}