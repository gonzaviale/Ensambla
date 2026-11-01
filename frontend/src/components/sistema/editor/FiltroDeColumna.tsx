import { claseDeFiltro, type FiltroDeColumna } from '@/lib/sistema/filtros';
import type { Campo } from '@/lib/sistema/tipos';

type Props = {
  campo: Campo;
  filtro: FiltroDeColumna;
  onCambio: (cambio: Partial<FiltroDeColumna>) => void;
};

// El control de filtro de una columna, según el tipo de dato: lista, sí/no, rango de fechas o de números, o texto.
export function FiltroDeColumna({ campo, filtro, onCambio }: Props) {
  const clase = claseDeFiltro(campo);

  if (clase === 'opcion' || clase === 'si-no') {
    const opciones =
      clase === 'si-no'
        ? [
            ['si', 'Sí'],
            ['no', 'No'],
          ]
        : (campo.options ?? []).map((opcion) => [opcion, opcion]);
    return (
      <select
        className="input"
        aria-label={`Filtrar ${campo.label}`}
        value={filtro.opcion ?? ''}
        onChange={(evento) => onCambio({ opcion: evento.target.value })}
      >
        <option value="">Todos</option>
        {opciones.map(([valor, etiqueta]) => (
          <option key={valor} value={valor}>
            {etiqueta}
          </option>
        ))}
      </select>
    );
  }

  if (clase === 'fecha' || clase === 'numero') {
    const tipo = clase === 'fecha' ? 'date' : 'number';
    const [desde, hasta] = clase === 'fecha' ? ['Desde', 'Hasta'] : ['Mín.', 'Máx.'];
    return (
      <div className="filtro-rango">
        <input
          className="input"
          type={tipo}
          aria-label={`${campo.label}: ${desde.toLowerCase()}`}
          title={desde}
          placeholder={desde}
          value={filtro.desde ?? ''}
          onChange={(evento) => onCambio({ desde: evento.target.value })}
        />
        <input
          className="input"
          type={tipo}
          aria-label={`${campo.label}: ${hasta.toLowerCase()}`}
          title={hasta}
          placeholder={hasta}
          value={filtro.hasta ?? ''}
          onChange={(evento) => onCambio({ hasta: evento.target.value })}
        />
      </div>
    );
  }

  return (
    <input
      className="input"
      type="search"
      aria-label={`Filtrar ${campo.label}`}
      placeholder="Contiene…"
      value={filtro.texto ?? ''}
      onChange={(evento) => onCambio({ texto: evento.target.value })}
    />
  );
}
