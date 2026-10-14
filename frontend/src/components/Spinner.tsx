export function Spinner({ label }: { label?: string }) {
  return (
    <div className="spinner-wrap">
      <span className="spinner" aria-hidden="true" />
      {label ? <p className="spinner-label">{label}</p> : null}
    </div>
  );
}

export function InlineSpinner({ size = 18 }: { size?: number }) {
  return <span className="spinner" style={{ width: size, height: size, borderWidth: 2 }} />;
}