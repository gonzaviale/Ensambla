import { useEffect, useRef, useState } from 'react';
import { ListIcon, XIcon } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';

const SECCIONES = [
  { id: 'como-funciona', nombre: 'Cómo te ayuda' },
  { id: 'a-tu-manera', nombre: 'A tu manera' },
  { id: 'tus-datos', nombre: 'Tus datos' },
  { id: 'tus-dudas', nombre: 'Tus dudas' },
] as const;

const enlace =
  'inline-flex min-h-12 items-center rounded-md px-3 py-2 text-hint font-semibold text-soft no-underline hover:bg-canvas hover:text-fg';

// Anclas a las secciones de la landing, en línea desde md.
export function LandingLinks() {
  return (
    <nav aria-label="Secciones" className="ml-4 hidden items-center gap-1 xl:flex">
      {SECCIONES.map(({ id, nombre }) => (
        <a key={id} href={`#${id}`} className={enlace}>
          {nombre}
        </a>
      ))}
    </nav>
  );
}

// Las mismas anclas en un menú desplegable para pantallas chicas.
export function LandingMenu() {
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef<HTMLDivElement>(null);
  const boton = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!abierto) return;
    const alTeclear = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setAbierto(false);
        boton.current?.focus();
      }
    };
    const alTocarFuera = (e: PointerEvent) => {
      if (e.target instanceof Node && !contenedor.current?.contains(e.target)) setAbierto(false);
    };
    document.addEventListener('keydown', alTeclear);
    document.addEventListener('pointerdown', alTocarFuera);
    return () => {
      document.removeEventListener('keydown', alTeclear);
      document.removeEventListener('pointerdown', alTocarFuera);
    };
  }, [abierto]);

  return (
    <div ref={contenedor} className="xl:hidden">
      <button
        ref={boton}
        type="button"
        aria-label={abierto ? 'Cerrar menú' : 'Abrir menú'}
        aria-expanded={abierto}
        aria-controls="menu-landing"
        onClick={() => setAbierto((v) => !v)}
        className="flex size-12 items-center justify-center rounded-md border border-line bg-surface text-fg"
      >
        {abierto ? <XIcon aria-hidden size={22} /> : <ListIcon aria-hidden size={22} />}
      </button>
      <nav
        id="menu-landing"
        aria-label="Menú de secciones"
        hidden={!abierto}
        className={cn(
          'absolute inset-x-4 top-full mt-2 flex-col gap-1 rounded-lg border border-line bg-surface p-2 shadow-md',
          abierto && 'flex',
        )}
      >
        {SECCIONES.map(({ id, nombre }) => (
          <a key={id} href={`#${id}`} className={enlace} onClick={() => setAbierto(false)}>
            {nombre}
          </a>
        ))}
      </nav>
    </div>
  );
}
