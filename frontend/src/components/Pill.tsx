export function Pill({ children, tone = 'default' }: { children: React.ReactNode; tone?: string }) {
  return <span className={`pill pill-${tone}`}>{children}</span>;
}

export function gapTone(gap: string): string {
  if (gap === 'none') return 'success';
  if (gap === 'minor') return 'warning';
  return 'danger';
}