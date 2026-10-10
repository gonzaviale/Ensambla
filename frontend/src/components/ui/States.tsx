import type { ReactNode } from 'react';
import { CircleNotchIcon, SmileySadIcon } from '@phosphor-icons/react';
import { Button } from './Button';
import { Panel } from './Panel';

export function Spinner({ label }: { label: string }) {
  return (
    <div role="status" className="flex items-center gap-2.5 text-[13px] text-soft">
      <CircleNotchIcon aria-hidden className="animate-spin" size={18} />
      {label}
    </div>
  );
}

export function PageSpinner() {
  return (
    <div className="flex justify-center py-24">
      <Spinner label="Cargando…" />
    </div>
  );
}

type ErrorStateProps = { title?: string; message: string; onRetry?: () => void };

export function ErrorState({
  title = 'No pudimos cargar esta pantalla',
  message,
  onRetry,
}: ErrorStateProps) {
  return (
    <Panel role="alert" className="mx-auto mt-10 flex max-w-xl flex-col items-start gap-3">
      <SmileySadIcon aria-hidden size={28} className="text-bad" />
      <h2 className="m-0 text-[17px]">{title}</h2>
      <p className="text-soft">{message}</p>
      {onRetry && (
        <Button variant="primary" onClick={onRetry}>
          Reintentar
        </Button>
      )}
    </Panel>
  );
}

type EmptyStateProps = { icon: ReactNode; title: string; children?: ReactNode };

export function EmptyState({ icon, title, children }: EmptyStateProps) {
  return (
    <Panel flat className="flex flex-col items-center gap-2 py-10 text-center">
      <span className="text-muted">{icon}</span>
      <h2 className="m-0 text-[17px]">{title}</h2>
      {children && <div className="text-[13px] text-soft">{children}</div>}
    </Panel>
  );
}
