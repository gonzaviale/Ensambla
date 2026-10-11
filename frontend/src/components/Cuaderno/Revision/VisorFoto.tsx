import { useState } from 'react';
import { MagnifyingGlassMinusIcon, MagnifyingGlassPlusIcon } from '@phosphor-icons/react';
import { Button } from '@/components/ui';
import { cn } from '@/lib/cn';

export type PaginaFoto = { numero: number; url: string };

type Props = {
  paginas: PaginaFoto[];
  /** Número de la hoja que se muestra (desde 1). */
  actual: number;
  onElegir: (numero: number) => void;
  className?: string;
  /** Alto máximo del área de la foto (por defecto 60 % de la pantalla). */
  altoClassName?: string;
};

const ZOOM_MINIMO = 1;
const ZOOM_MAXIMO = 4;
const PASO = 0.5;

// La foto del cuaderno, ampliable, para cotejarla con lo que leyó la IA. Con varias hojas deja elegir.
export function VisorFoto({ paginas, actual, onElegir, className, altoClassName }: Props) {
  const [zoom, setZoom] = useState(ZOOM_MINIMO);
  const [fallo, setFallo] = useState<number>();
  const pagina = paginas.find((item) => item.numero === actual) ?? paginas[0];

  if (!pagina) return null;
  const cambiarZoom = (cambio: number) =>
    setZoom((valor) => Math.min(ZOOM_MAXIMO, Math.max(ZOOM_MINIMO, valor + cambio)));

  return (
    <figure aria-label="Foto del cuaderno" className={cn('m-0 flex flex-col gap-3', className)}>
      <div className="flex flex-wrap items-center gap-2">
        <Button
          aria-label="Acercar la foto"
          disabled={zoom >= ZOOM_MAXIMO}
          onClick={() => cambiarZoom(PASO)}
        >
          <MagnifyingGlassPlusIcon aria-hidden size={20} />
          Acercar
        </Button>
        <Button
          aria-label="Alejar la foto"
          disabled={zoom <= ZOOM_MINIMO}
          onClick={() => cambiarZoom(-PASO)}
        >
          <MagnifyingGlassMinusIcon aria-hidden size={20} />
          Alejar
        </Button>
        <Button
          variant="ghost"
          disabled={zoom === ZOOM_MINIMO}
          onClick={() => setZoom(ZOOM_MINIMO)}
        >
          Ver entera
        </Button>
        <span className="ml-auto text-hint font-semibold text-soft" aria-live="polite">
          Hoja {pagina.numero} de {paginas.length}
        </span>
      </div>

      {/* tabIndex: con zoom la foto se desplaza con el teclado. */}
      <div
        tabIndex={0}
        role="group"
        aria-label={`Hoja ${pagina.numero} del cuaderno, con desplazamiento`}
        className={cn(
          'overflow-auto rounded-md border border-line bg-canvas',
          altoClassName ?? 'max-h-[60dvh]',
        )}
      >
        {fallo === pagina.numero ? (
          <p role="alert" className="m-4 font-semibold text-bad">
            No pudimos cargar la foto de esta hoja.
          </p>
        ) : (
          <img
            src={pagina.url}
            alt={`Hoja ${pagina.numero} del cuaderno`}
            style={{ width: `${zoom * 100}%`, maxWidth: 'none' }}
            className="block h-auto"
            onError={() => setFallo(pagina.numero)}
          />
        )}
      </div>

      {paginas.length > 1 && (
        <div role="group" aria-label="Elegir hoja" className="flex flex-wrap gap-2">
          {paginas.map((item) => (
            <Button
              key={item.numero}
              aria-pressed={item.numero === pagina.numero}
              variant={item.numero === pagina.numero ? 'primary' : 'secondary'}
              className={cn(item.numero === pagina.numero && 'border-2 bg-accent-100')}
              onClick={() => onElegir(item.numero)}
            >
              Hoja {item.numero}
            </Button>
          ))}
        </div>
      )}
    </figure>
  );
}
