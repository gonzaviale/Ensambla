import { useState } from 'react';
import { MagnifyingGlassMinusIcon, MagnifyingGlassPlusIcon } from '@phosphor-icons/react';
import { Button } from '@/components/ui';
import { cn } from '@/lib/cn';

export type PaginaFoto = { numero: number; url: string };

type Props = {
  paginas: PaginaFoto[];
  /** Número de la página que se muestra (desde 1). */
  actual: number;
  onElegir: (numero: number) => void;
  className?: string;
};

const ZOOM_MINIMO = 1;
const ZOOM_MAXIMO = 4;
const PASO = 0.5;

// La foto del cuaderno, ampliable, para cotejarla con lo que leyó la IA. Con varias páginas deja elegir.
export function VisorFoto({ paginas, actual, onElegir, className }: Props) {
  const [zoom, setZoom] = useState(ZOOM_MINIMO);
  const [fallo, setFallo] = useState<number>();
  const pagina = paginas.find((item) => item.numero === actual) ?? paginas[0];

  if (!pagina) return null;
  const cambiarZoom = (cambio: number) =>
    setZoom((valor) => Math.min(ZOOM_MAXIMO, Math.max(ZOOM_MINIMO, valor + cambio)));

  return (
    <figure aria-label="Foto del cuaderno" className={cn('m-0 flex flex-col gap-2', className)}>
      <div className="flex flex-wrap items-center gap-2">
        <Button
          aria-label="Acercar la foto"
          disabled={zoom >= ZOOM_MAXIMO}
          onClick={() => cambiarZoom(PASO)}
        >
          <MagnifyingGlassPlusIcon aria-hidden size={16} />
          Acercar
        </Button>
        <Button
          aria-label="Alejar la foto"
          disabled={zoom <= ZOOM_MINIMO}
          onClick={() => cambiarZoom(-PASO)}
        >
          <MagnifyingGlassMinusIcon aria-hidden size={16} />
          Alejar
        </Button>
        <Button
          variant="ghost"
          disabled={zoom === ZOOM_MINIMO}
          onClick={() => setZoom(ZOOM_MINIMO)}
        >
          Ver entera
        </Button>
        <span className="ml-auto text-hint text-soft" aria-live="polite">
          Página {pagina.numero} de {paginas.length}
        </span>
      </div>

      {/* tabIndex: con zoom la foto se desplaza con el teclado. */}
      <div
        tabIndex={0}
        role="group"
        aria-label={`Página ${pagina.numero} del cuaderno, con desplazamiento`}
        className="max-h-[60vh] overflow-auto rounded-md border border-line bg-canvas focus-visible:outline-2 focus-visible:outline-accent"
      >
        {fallo === pagina.numero ? (
          <p role="alert" className="m-4 text-bad">
            No pudimos cargar la foto de esta página.
          </p>
        ) : (
          <img
            src={pagina.url}
            alt={`Página ${pagina.numero} del cuaderno`}
            style={{ width: `${zoom * 100}%`, maxWidth: 'none' }}
            className="block h-auto"
            onError={() => setFallo(pagina.numero)}
          />
        )}
      </div>

      {paginas.length > 1 && (
        <div role="group" aria-label="Elegir página" className="flex flex-wrap gap-2">
          {paginas.map((item) => (
            <Button
              key={item.numero}
              aria-pressed={item.numero === pagina.numero}
              className={cn(item.numero === pagina.numero && 'border-accent bg-accent/10')}
              onClick={() => onElegir(item.numero)}
            >
              Página {item.numero}
            </Button>
          ))}
        </div>
      )}
    </figure>
  );
}
