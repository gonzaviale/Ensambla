import { ArrowCounterClockwiseIcon } from '@phosphor-icons/react';
import { Button } from '@/components/ui';
import type { Alerta } from '@/types/red';
import { META_AVISO } from './avisoMeta';

type AvisosIgnoradosProps = {
  ignoradas: Alerta[];
  onRecuperar: (id: string) => void;
  onRecuperarTodos: () => void;
};

// «Avisos que ignoraste»: nada se pierde, se puede volver a mostrar cualquiera.
export function AvisosIgnorados({
  ignoradas,
  onRecuperar,
  onRecuperarTodos,
}: AvisosIgnoradosProps) {
  if (ignoradas.length === 0) return null;

  return (
    <details className="rounded-lg border border-line bg-canvas">
      <summary className="flex min-h-12 cursor-pointer items-center px-4 py-2.5 text-base font-semibold text-accent-800 hover:text-accent-900">
        Avisos que ignoraste ({ignoradas.length})
      </summary>
      <div className="flex flex-col gap-3 px-4 pb-4">
        <ul className="m-0 flex list-none flex-col divide-y divide-line p-0">
          {ignoradas.map((alerta) => (
            <li
              key={alerta.id}
              className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 py-3"
            >
              <div className="min-w-0 flex-1 basis-60">
                <p className="m-0 text-hint font-semibold text-muted">
                  {META_AVISO[alerta.tipo].etiqueta}
                </p>
                <p className="m-0 text-body text-soft">{alerta.titulo}</p>
              </div>
              <Button
                variant="primary"
                aria-label={`Volver a mostrar: ${alerta.titulo}`}
                onClick={() => onRecuperar(alerta.id)}
              >
                <ArrowCounterClockwiseIcon aria-hidden size={20} />
                Volver a mostrar
              </Button>
            </li>
          ))}
        </ul>
        {ignoradas.length > 1 && (
          <div>
            <Button variant="secondary" onClick={onRecuperarTodos}>
              Volver a mostrar todos
            </Button>
          </div>
        )}
      </div>
    </details>
  );
}
