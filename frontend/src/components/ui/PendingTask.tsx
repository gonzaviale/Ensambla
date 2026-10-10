import type { ReactNode } from 'react';
import { WrenchIcon } from '@phosphor-icons/react';
import { EmptyState } from './States';

type PendingTaskProps = { tarea: string; title: string; children?: ReactNode };

// Marcador de pantallas que el equipo implementa en el MVP2: indica qué tarea del backlog la cubre.
export function PendingTask({ tarea, title, children }: PendingTaskProps) {
  return (
    <EmptyState icon={<WrenchIcon aria-hidden size={32} />} title={title}>
      <p>
        Pantalla pendiente · tarea <strong>{tarea}</strong> del backlog.
      </p>
      {children}
    </EmptyState>
  );
}
