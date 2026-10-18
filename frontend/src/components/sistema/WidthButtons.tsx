import type { Ancho } from '@/lib/sistema/tipos';

const ANCHOS: { valor: Ancho; etiqueta: string; titulo: string }[] = [
  { valor: 'half', etiqueta: '½', titulo: 'Media fila' },
  { valor: 'wide', etiqueta: '⅔', titulo: 'Dos tercios' },
  { valor: 'full', etiqueta: '1', titulo: 'Fila completa' },
];

type WidthButtonsProps = {
  ancho: Ancho;
  onElegir: (ancho: Ancho) => void;
  /** Los botones del tutorial se reconocen por este atributo. */
  tour?: string;
};

export function WidthButtons({ ancho, onElegir, tour }: WidthButtonsProps) {
  return (
    <div className="width-buttons" role="group" aria-label="Ancho del bloque" data-tour={tour}>
      {ANCHOS.map(({ valor, etiqueta, titulo }) => (
        <button
          key={valor}
          type="button"
          className={ancho === valor ? 'active' : ''}
          aria-pressed={ancho === valor}
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
