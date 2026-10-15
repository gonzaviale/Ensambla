import { useState } from 'react';
import { ArrowCounterClockwiseIcon, EyeIcon } from '@phosphor-icons/react';
import { Badge, Button, ConfirmDialog, Panel } from '@/components/ui';
import { useAccionesRed, useAlertas } from '@/hooks/red/useRed';
import { plural } from '@/lib/red/formato';

type ModoDemostracionProps = { onHecho: (mensaje: string) => void };

// Todo el estado de la red proactiva es de demostración: acá se puede volver al comienzo.
export function ModoDemostracion({ onHecho }: ModoDemostracionProps) {
  const { reiniciarDemo, restaurarAlertas } = useAccionesRed();
  const { ignoradas } = useAlertas();
  const [confirmando, setConfirmando] = useState(false);

  return (
    <Panel
      aria-labelledby="cuenta-demo"
      data-recorrido="cuenta-demo"
      className="flex flex-col gap-4"
    >
      <div className="flex flex-wrap items-center gap-3">
        <h2 id="cuenta-demo" className="m-0 text-h3">
          Modo demostración
        </h2>
        <Badge tone="warn">Datos de ejemplo</Badge>
      </div>
      <p className="m-0 text-body text-soft">
        Estás viendo un comercio de ejemplo con datos inventados. Podés probar todo sin miedo y
        volver a empezar cuando quieras.
      </p>

      <div className="flex flex-col gap-2">
        <Button
          variant="secondary"
          className="self-start"
          disabled={ignoradas.length === 0}
          onClick={() => {
            restaurarAlertas();
            onHecho('Listo, volvieron a aparecer los avisos que habías ignorado.');
          }}
        >
          <EyeIcon aria-hidden size={22} />
          Mostrar de nuevo los avisos que ignoré
        </Button>
        <p className="m-0 text-hint text-muted">
          {ignoradas.length === 0
            ? 'No ignoraste ningún aviso.'
            : `Hoy tenés ${plural(ignoradas.length, 'aviso ignorado', 'avisos ignorados')}.`}
        </p>
      </div>

      <Button variant="danger" className="self-start" onClick={() => setConfirmando(true)}>
        <ArrowCounterClockwiseIcon aria-hidden size={22} />
        Volver a los datos de ejemplo
      </Button>

      <ConfirmDialog
        open={confirmando}
        titulo="¿Volver a los datos de ejemplo?"
        confirmar="Sí, volver a los datos de ejemplo"
        conservar="No, dejar todo como está"
        onConservar={() => setConfirmando(false)}
        onConfirmar={() => {
          reiniciarDemo();
          setConfirmando(false);
          onHecho('Listo, volvimos a los datos de ejemplo.');
        }}
      >
        <p className="m-0">Vas a perder todo lo que hiciste en esta demostración:</p>
        <ul className="m-0 list-disc pl-6">
          <li>los precios que cambiaste y las listas que revisaste,</li>
          <li>los avisos que resolviste o ignoraste y tu pedido,</li>
          <li>tus reglas de precio, tus datos y tu plan.</li>
        </ul>
        <p className="m-0">Esto no se puede deshacer.</p>
      </ConfirmDialog>
    </Panel>
  );
}
