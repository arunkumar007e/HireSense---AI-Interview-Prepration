export function StatCard({
  label,
  value,
  hint,
  accent,
}: {
  label: string;
  value: string | number;
  hint?: string;
  accent?: string;
}) {
  return (
    <div className="stat-card">
      <p className="stat-label">{label}</p>
      <p className="stat-value" style={accent ? { color: accent } : undefined}>
        {value}
      </p>
      {hint ? <p className="stat-hint">{hint}</p> : null}
    </div>
  );
}