import { Link } from '@tanstack/react-router';
import { CheckCircleIcon, CircleIcon } from '@phosphor-icons/react';
import { Panel, buttonClasses } from '@/components/ui';

type Paso = {
  texto: string;
  hecho: boolean;
  enlace?: { to: '/subir' | '/bloques'; texto: string };
};

// Tres primeros pasos derivados de los datos. Se oculta cuando están los tres.
export function PrimerosPasos({
  hayNegocio,
  cargoDato,
  sumoBloque,
}: {
  hayNegocio: boolean;
  cargoDato: boolean;
  sumoBloque: boolean;
}) {
  const pasos: Paso[] = [
    { texto: 'Armaste tu sistema', hecho: hayNegocio },
    {
      texto: 'Cargaste tu primer dato',
      hecho: cargoDato,
      enlace: { to: '/subir', texto: 'Contá qué pasó' },
    },
    {
      texto: 'Sumaste un bloque más',
      hecho: sumoBloque,
      enlace: { to: '/bloques', texto: 'Ver mis bloques' },
    },
  ];
  if (pasos.every((paso) => paso.hecho)) return null;

  return (
    <Panel
      flat
      aria-labelledby="primeros-pasos"
      data-recorrido="inicio-checklist"
      className="flex flex-col gap-3"
    >
      <h2 id="primeros-pasos" className="m-0 text-h3">
        Tus primeros pasos
      </h2>
      <ul className="m-0 flex list-none flex-col gap-3 p-0">
        {pasos.map((paso) => (
          <li key={paso.texto} className="flex flex-wrap items-center gap-x-3 gap-y-1">
            {paso.hecho ? (
              <CheckCircleIcon aria-hidden size={28} weight="fill" className="shrink-0 text-ok" />
            ) : (
              <CircleIcon aria-hidden size={28} className="shrink-0 text-soft" />
            )}
            <span className={paso.hecho ? 'text-body text-soft' : 'text-body font-semibold'}>
              <span className="sr-only">{paso.hecho ? 'Hecho: ' : 'Falta: '}</span>
              {paso.texto}
            </span>
            {!paso.hecho && paso.enlace && (
              <Link
                to={paso.enlace.to}
                className={buttonClasses({ variant: 'ghost', size: 'compact' })}
              >
                {paso.enlace.texto}
              </Link>
            )}
          </li>
        ))}
      </ul>
    </Panel>
  );
}
