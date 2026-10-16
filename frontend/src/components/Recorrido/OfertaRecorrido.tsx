import { useId } from 'react';
import { CompassIcon } from '@phosphor-icons/react';
import { Button, Panel } from '@/components/ui';
import { useRecorrido } from '@/hooks/recorrido/useRecorrido';

// La primera vez que se entra a «Inicio» (la monta AppLayout sólo ahí): una invitación al recorrido guiado, que se puede descartar.
export function OfertaRecorrido() {
  const { visto, activo, iniciar, descartarOferta } = useRecorrido();
  const tituloId = useId();
  if (visto || activo) return null;

  return (
    <Panel
      flat
      role="region"
      aria-labelledby={tituloId}
      className="mb-6 flex flex-wrap items-center justify-between gap-4"
    >
      <div className="flex min-w-0 flex-1 basis-64 items-start gap-3">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-accent-100 text-accent-800">
          <CompassIcon aria-hidden size={26} weight="duotone" />
        </span>
        <div className="flex min-w-0 flex-col gap-0.5">
          <p id={tituloId} className="m-0 text-body font-bold">
            ¿Querés que te mostremos cómo usar Ensambla?
          </p>
          <p className="m-0 text-body text-soft">
            Dura unos 3 minutos y lo podés dejar cuando quieras.
          </p>
        </div>
      </div>
      <div className="flex flex-wrap gap-3">
        <Button variant="primary" onClick={iniciar}>
          Empezar el recorrido
        </Button>
        <Button variant="ghost" onClick={descartarOferta}>
          Ahora no
        </Button>
      </div>
    </Panel>
  );
}
