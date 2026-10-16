import { useState } from 'react';
import { ArrowCounterClockwiseIcon } from '@phosphor-icons/react';
import { Badge, Button, ConfirmDialog, Panel } from '@/components/ui';
import { BotonRecorrido } from '@/components/Recorrido/BotonRecorrido';
import { useAccionesPlan } from '@/hooks/negocio/usePlan';

type ModoDemostracionProps = { onHecho: (mensaje: string) => void };

// Los planes y los pagos son simulados: acá se puede volver a empezar la prueba y el recorrido.
export function ModoDemostracion({ onHecho }: ModoDemostracionProps) {
  const { reiniciar } = useAccionesPlan();
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
        <Badge tone="warn">Simulado</Badge>
      </div>
      <p className="m-0 text-body text-soft">
        Los planes y los pagos de Ensambla son una simulación: no se cobra nada de verdad. Podés
        probar todo sin miedo y volver a empezar cuando quieras.
      </p>

      <div className="flex flex-col gap-2">
        <Button variant="danger" className="self-start" onClick={() => setConfirmando(true)}>
          <ArrowCounterClockwiseIcon aria-hidden size={22} />
          Volver a empezar la prueba
        </Button>
        <p className="m-0 text-hint text-muted">
          Vuelven los 7 días de prueba con todo desbloqueado. Tus datos no se tocan.
        </p>
      </div>

      <ConfirmDialog
        open={confirmando}
        titulo="¿Volver a empezar la prueba?"
        confirmar="Sí, volver a empezar"
        conservar="No, dejar todo como está"
        onConservar={() => setConfirmando(false)}
        onConfirmar={() => {
          reiniciar();
          setConfirmando(false);
          onHecho('Listo, volvió a empezar la prueba de 7 días.');
        }}
      >
        <p className="m-0">Esto cambia tu plan de demostración:</p>
        <ul className="m-0 list-disc pl-6">
          <li>se borran tus pagos y tu suscripción simulados,</li>
          <li>vuelve la prueba de 7 días con todo desbloqueado.</li>
        </ul>
        <p className="m-0">Los bloques y los datos de tu negocio no se tocan.</p>
      </ConfirmDialog>

      <BotonRecorrido etiqueta="Mirar el recorrido otra vez" className="self-start" />
    </Panel>
  );
}
