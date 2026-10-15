import { CheckCircleIcon, ProhibitIcon } from '@phosphor-icons/react';
import { Panel } from '@/components/ui';
import { useUmbrales } from '@/hooks/red/useRed';

// Qué se comparte y qué nunca: dicho en claro, porque la red sólo funciona si hay confianza.
export function Privacidad() {
  const { minimoComerciosZona } = useUmbrales();

  return (
    <Panel aria-labelledby="cuenta-privacidad" className="flex flex-col gap-4">
      <h2 id="cuenta-privacidad" className="m-0 text-h3">
        Privacidad y datos
      </h2>
      <p className="m-0 text-body text-soft">
        Ensambla compara precios entre comercios de tu zona, pero nadie ve tus datos ni vos los de
        nadie.
      </p>

      <div className="flex flex-col gap-2">
        <h3 className="m-0 flex items-center gap-2 text-body font-bold">
          <CheckCircleIcon aria-hidden size={24} weight="fill" className="shrink-0 text-ok" />
          Lo que se comparte
        </h3>
        <p className="m-0 text-body text-soft">
          Sólo datos agregados y anónimos, y únicamente cuando hay {minimoComerciosZona} comercios o
          más en tu zona. Por ejemplo: «los comercios de tu zona pagan en promedio tanto por este
          producto».
        </p>
      </div>

      <div className="flex flex-col gap-2">
        <h3 className="m-0 flex items-center gap-2 text-body font-bold">
          <ProhibitIcon aria-hidden size={24} weight="fill" className="shrink-0 text-bad" />
          Lo que nunca se comparte
        </h3>
        <ul className="m-0 flex list-disc flex-col gap-1 pl-6 text-body text-soft">
          <li>El nombre de tu comercio ni tus datos de contacto.</li>
          <li>Tus precios de venta, tus ventas y tu stock.</li>
          <li>Tus listas completas ni tus ganancias.</li>
        </ul>
      </div>

      <p className="m-0 text-hint text-muted">
        En esta demostración, todo lo que cargás se guarda sólo en este navegador.
      </p>
    </Panel>
  );
}
