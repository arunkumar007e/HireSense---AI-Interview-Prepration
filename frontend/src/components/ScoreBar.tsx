export function ScoreBar({ label, value, hint }: { label: string; value: number; hint?: string }) {
  const clamped = Math.max(0, Math.min(100, value));
  const color = clamped >= 80 ? 'var(--success)' : clamped >= 60 ? 'var(--warning)' : 'var(--danger)';
  return (
    <div className="score-bar">
      <div className="score-bar-head">
        <span className="score-bar-label">{label}</span>
        <span className="score-bar-value">{clamped}%</span>
      </div>
      <div className="score-track">
        <div className="score-fill" style={{ width: `${clamped}%`, backgroundColor: color }} />
      </div>
      {hint ? <p className="score-bar-hint">{hint}</p> : null}
    </div>
  );
}