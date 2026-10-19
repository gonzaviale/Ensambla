import type { Ancho } from '@/lib/sistema/tipos';

const ANCHOS: { valor: Ancho; etiqueta: string; titulo: string }[] = [
  { valor: 'half', etiqueta: '½', titulo: 'Media fila' },
  { valor: 'wide', etiqueta: '⅔', titulo: 'Dos tercios' },
  { valor: 'full', etiqueta: '1', titulo: 'Fila completa' },
];

type WidthButtonsProps = {
  ancho: Ancho;
  onElegir: (ancho: Ancho) => void;
  /** El nombre de la acción de los botones: el tutorial decide con él qué deja tocar. */
  accion?: string;
};

export function WidthButtons({ ancho, onElegir, accion = 'set-width' }: WidthButtonsProps) {
  return (
    <div className="width-buttons" role="group" aria-label="Ancho del bloque">
      {ANCHOS.map(({ valor, etiqueta, titulo }) => (
        <button
          key={valor}
          type="button"
          className={ancho === valor ? 'active' : ''}
          aria-pressed={ancho === valor}
          data-accion={accion}
          title={titulo}
          aria-label={titulo}
          onClick={() => onElegir(valor)}
        >
          {etiqueta}
        </button>
      ))}
    </div>
  );
}
