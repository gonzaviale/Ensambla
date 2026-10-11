import type { ReactNode } from 'react';
import { CircleNotchIcon, SmileySadIcon } from '@phosphor-icons/react';
import { Button } from './Button';
import { Panel } from './Panel';

export function Spinner({ label }: { label: string }) {
  return (
    <div role="status" className="flex items-center gap-2.5 text-hint text-soft">
      <CircleNotchIcon aria-hidden className="animate-spin" size={22} />
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
      <span className="flex size-12 items-center justify-center rounded-full bg-coral-100 text-bad">
        <SmileySadIcon aria-hidden size={30} />
      </span>
      <h2 className="m-0 text-h3">{title}</h2>
      <p className="m-0 text-body text-soft">{message}</p>
      {onRetry && (
        <Button variant="solid" onClick={onRetry}>
          Reintentar
        </Button>
      )}
    </Panel>
  );
}

type EmptyStateProps = { icon: ReactNode; title: string; children?: ReactNode };

// Vacío con ilustración: el ícono va sobre un círculo arena y el texto explica qué hacer.
export function EmptyState({ icon, title, children }: EmptyStateProps) {
  return (
    <Panel flat className="flex flex-col items-center gap-3 py-10 text-center">
      <span className="flex size-16 items-center justify-center rounded-full bg-sand-100 text-fg">
        {icon}
      </span>
      <h2 className="m-0 text-h3">{title}</h2>
      {children && <div className="max-w-md text-body text-soft">{children}</div>}
    </Panel>
  );
}
