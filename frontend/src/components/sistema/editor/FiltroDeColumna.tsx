import type { ComponentType } from 'react';
import {
  claseDeFiltro,
  type ClaseDeFiltro,
  type FiltroDeColumna as ValorDelFiltro,
} from '@/lib/sistema/filtros';
import type { Campo } from '@/lib/sistema/tipos';

type Props = {
  campo: Campo;
  filtro: ValorDelFiltro;
  onCambio: (cambio: Partial<ValorDelFiltro>) => void;
};

type OpcionDelFiltro = [valor: string, etiqueta: string];

const OPCIONES_SI_NO: OpcionDelFiltro[] = [
  ['si', 'Sí'],
  ['no', 'No'],
];

function SelectorDeOpcion({
  campo,
  filtro,
  onCambio,
  opciones,
}: Props & { opciones: OpcionDelFiltro[] }) {
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

function FiltroDeLista(props: Props) {
  const opciones = (props.campo.options ?? []).map((opcion): OpcionDelFiltro => [opcion, opcion]);
  return <SelectorDeOpcion {...props} opciones={opciones} />;
}

function FiltroSiNo(props: Props) {
  return <SelectorDeOpcion {...props} opciones={OPCIONES_SI_NO} />;
}

type RangoProps = Props & { tipo: 'date' | 'number'; minimo: string; maximo: string };

function FiltroDeRango({ campo, filtro, onCambio, tipo, minimo, maximo }: RangoProps) {
  return (
    <div className="filtro-rango">
      <input
        className="input"
        type={tipo}
        aria-label={`${campo.label}: ${minimo.toLowerCase()}`}
        title={minimo}
        placeholder={minimo}
        value={filtro.desde ?? ''}
        onChange={(evento) => onCambio({ desde: evento.target.value })}
      />
      <input
        className="input"
        type={tipo}
        aria-label={`${campo.label}: ${maximo.toLowerCase()}`}
        title={maximo}
        placeholder={maximo}
        value={filtro.hasta ?? ''}
        onChange={(evento) => onCambio({ hasta: evento.target.value })}
      />
    </div>
  );
}

function FiltroDeFechas(props: Props) {
  return <FiltroDeRango {...props} tipo="date" minimo="Desde" maximo="Hasta" />;
}

function FiltroNumerico(props: Props) {
  return <FiltroDeRango {...props} tipo="number" minimo="Mín." maximo="Máx." />;
}

function FiltroDeTexto({ campo, filtro, onCambio }: Props) {
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

const FILTROS: Record<ClaseDeFiltro, ComponentType<Props>> = {
  opcion: FiltroDeLista,
  'si-no': FiltroSiNo,
  fecha: FiltroDeFechas,
  numero: FiltroNumerico,
  texto: FiltroDeTexto,
};

// El control de filtro de una columna, según el tipo de dato: lista, sí/no, rango de fechas o de números, o texto.
export function FiltroDeColumna(props: Props) {
  const Filtro = FILTROS[claseDeFiltro(props.campo)];
  return <Filtro {...props} />;
}
