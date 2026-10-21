import type { ReactNode } from 'react';
import { CircleNotchIcon } from '@phosphor-icons/react';
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
    <div className="flex min-h-[70vh] items-center justify-center px-4">
      <div
        role="alert"
        className="flex w-full max-w-xl flex-col items-center gap-3 rounded-3xl border-t-[6px] border-accent bg-white px-8 py-10 text-center shadow-xl"
      >
        <img src="/mockup/sami-error.webp" alt="" className="w-36" />
        <h2 className="m-0 text-h3">{title}</h2>
        <p className="m-0 text-body text-soft">{message}</p>
        {onRetry && (
          <Button variant="solid" className="mt-2 text-white hover:text-white" onClick={onRetry}>
            Reintentar
          </Button>
        )}
      </div>
    </div>
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
