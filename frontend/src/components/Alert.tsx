export function Alert({
  kind,
  children,
  onClose,
}: {
  kind: 'error' | 'success' | 'info';
  children: React.ReactNode;
  onClose?: () => void;
}) {
  return (
    <div className={`alert alert-${kind}`} role="alert">
      <span>{children}</span>
      {onClose ? (
        <button type="button" className="alert-close" onClick={onClose} aria-label="Dismiss">
          &times;
        </button>
      ) : null}
    </div>
  );
}