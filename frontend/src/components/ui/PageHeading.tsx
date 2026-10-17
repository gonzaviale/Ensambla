import type { ReactNode } from 'react';
import { useTituloPagina } from '@/hooks/useTituloPagina';

type PageHeadingProps = {
  kicker?: ReactNode;
  title: string;
  lead?: ReactNode;
  /** Acciones de la pantalla (un solo botón sólido), alineadas a la derecha en escritorio. */
  actions?: ReactNode;
};

// Un título por pantalla: todas las páginas (también Auth y Landing) lo usan para compartir escala
// y para poner el título de la pestaña.
export function PageHeading({ kicker, title, lead, actions }: PageHeadingProps) {
  useTituloPagina(title);

  return (
    <header className="flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
      <div className="flex min-w-0 flex-1 basis-80 flex-col gap-1.5">
        {kicker && (
          <p className="m-0 text-caption font-bold tracking-widest text-petrol uppercase">
            {kicker}
          </p>
        )}
        <h1 className="m-0 text-h2 break-words text-petrol sm:text-h1">{title}</h1>
        {lead && <p className="m-0 max-w-3xl text-lead text-soft">{lead}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-3">{actions}</div>}
    </header>
  );
}
