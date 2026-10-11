import { FileIcon, FrameCornersIcon, SunIcon } from '@phosphor-icons/react';
import type { ComponentType } from 'react';

const CONSEJOS: {
  Icono: ComponentType<{ size?: number; 'aria-hidden'?: boolean }>;
  texto: string;
}[] = [
  { Icono: SunIcon, texto: 'Buena luz, sin sombras sobre la hoja.' },
  { Icono: FileIcon, texto: 'La hoja bien plana, apoyada sobre una mesa.' },
  { Icono: FrameCornersIcon, texto: 'Que se vea la hoja entera y nada más alrededor.' },
];

// Tres consejos con ícono, antes de los botones de la cámara: lo justo para sacar una buena foto.
export function ConsejosFoto() {
  return (
    <section aria-labelledby="consejos-titulo" className="flex flex-col gap-3">
      <h2 id="consejos-titulo" className="m-0 text-h3">
        Consejos para una buena foto
      </h2>
      <ul className="m-0 grid list-none gap-3 p-0 sm:grid-cols-3">
        {CONSEJOS.map(({ Icono, texto }) => (
          <li
            key={texto}
            className="flex items-center gap-3 rounded-lg bg-sand-100 px-4 py-3 text-body sm:flex-col sm:items-start"
          >
            <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-surface text-accent-800">
              <Icono size={28} aria-hidden />
            </span>
            <span className="min-w-0">{texto}</span>
          </li>
        ))}
      </ul>
      <p className="m-0 text-hint text-muted">
        Una foto por hoja, con el celular derecho y quieto. Si la letra se ve borrosa, sacala de
        nuevo.
      </p>
    </section>
  );
}
