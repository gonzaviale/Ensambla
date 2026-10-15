import type { ReactNode } from 'react';
import { ChatCircleTextIcon, MagnifyingGlassIcon, TagIcon } from '@phosphor-icons/react';

const PASOS: Array<{ icono: ReactNode; titulo: string; texto: string }> = [
  {
    icono: <ChatCircleTextIcon aria-hidden size={28} />,
    titulo: 'Reenviás la lista',
    texto: 'Cuando tu proveedor te manda precios nuevos, los reenviás o les sacás una foto.',
  },
  {
    icono: <MagnifyingGlassIcon aria-hidden size={28} />,
    titulo: 'Revisás lo que cambió',
    texto: 'Te mostramos qué subió y qué precios te conviene cambiar. Vos confirmás.',
  },
  {
    icono: <TagIcon aria-hidden size={28} />,
    titulo: 'Cambiás las etiquetas',
    texto: 'Te dejamos la lista de lo que cambió para que lo pases al mostrador.',
  },
];

// Mini recorrido de tres tarjetas (T-057): es sólo lectura, así que no frena a nadie.
export function ComoFunciona({ titulo = 'Así funciona, en tres pasos' }: { titulo?: string }) {
  return (
    <section aria-labelledby="como-funciona-titulo" className="flex flex-col gap-3">
      <h2 id="como-funciona-titulo" className="m-0 text-h3">
        {titulo}
      </h2>
      <ol className="m-0 grid list-none gap-3 p-0 sm:grid-cols-3">
        {PASOS.map((paso, indice) => (
          <li
            key={paso.titulo}
            className="flex min-w-0 flex-col gap-2 rounded-lg border border-line/60 bg-surface p-4 shadow-sm"
          >
            <span className="flex items-center gap-3">
              <span
                aria-hidden
                className="flex size-12 shrink-0 items-center justify-center rounded-full bg-sand-100 text-fg"
              >
                {paso.icono}
              </span>
              <span className="text-body font-bold">
                <span className="sr-only">Paso </span>
                {indice + 1}. {paso.titulo}
              </span>
            </span>
            <span className="text-hint text-soft">{paso.texto}</span>
          </li>
        ))}
      </ol>
    </section>
  );
}
