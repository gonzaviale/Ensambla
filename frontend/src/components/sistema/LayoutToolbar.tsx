import type { Distribucion } from '@/lib/sistema/tipos';
import { Icon } from './Icon';

const DISTRIBUCIONES: { valor: Distribucion; etiqueta: string }[] = [
  { valor: 'mosaic', etiqueta: 'Mosaico' },
  { valor: 'two', etiqueta: '2 columnas' },
  { valor: 'single', etiqueta: '1 columna' },
];

export function LayoutToolbar({
  distribucion,
  onElegir,
}: {
  distribucion: Distribucion | undefined;
  onElegir: (distribucion: Distribucion) => void;
}) {
  return (
    <div className="layout-toolbar">
      <div>
        <strong>
          <Icon name="grid" />
          Distribuí tus piezas
        </strong>
        <span>Probá una base y después acomodá cada bloque.</span>
      </div>
      <div className="layout-presets">
        {DISTRIBUCIONES.map(({ valor, etiqueta }) => (
          <button
            key={valor}
            type="button"
            className={`layout-choice ${distribucion === valor ? 'active' : ''}`.trim()}
            aria-pressed={distribucion === valor}
            onClick={() => onElegir(valor)}
          >
            <span className={`layout-symbol ${valor}`} aria-hidden="true">
              <i />
              <i />
              <i />
            </span>
            {etiqueta}
          </button>
        ))}
      </div>
    </div>
  );
}
