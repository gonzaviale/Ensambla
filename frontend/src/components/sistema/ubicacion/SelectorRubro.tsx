import type { ReactNode } from 'react';
import type { Rubro } from '@/types/ubicacion';
import { Btn } from '../Btn';

type Props = {
  rubros: Rubro[];
  valor: string;
  onCambiar: (valor: string) => void;
  cargando: boolean;
  error: string;
  onReintentar: () => void;
  /** El botón de guardar de la solapa, al final del recuadro. */
  pie?: ReactNode;
};

export function SelectorRubro({
  rubros,
  valor,
  onCambiar,
  cargando,
  error,
  onReintentar,
  pie,
}: Props) {
  return (
    <section className="ubicacion-panel ubicacion-rubro" aria-labelledby="titulo-rubro">
      <h2 id="titulo-rubro">Elegí tu rubro</h2>
      <div className="field">
        <label htmlFor="rubro-comercio">Rubro del comercio</label>
        <select
          id="rubro-comercio"
          className="input"
          value={valor}
          disabled={cargando || !!error}
          onChange={(evento) => onCambiar(evento.target.value)}
        >
          <option value="">{cargando ? 'Cargando rubros…' : 'Seleccioná un rubro'}</option>
          {rubros.map((rubro) => (
            <option key={rubro.codigo} value={rubro.codigo}>
              {rubro.nombre}
            </option>
          ))}
        </select>
      </div>
      {error && (
        <div role="alert">
          <p>{error}</p>
          <Btn onClick={onReintentar}>Reintentar rubros</Btn>
        </div>
      )}
      {pie}
    </section>
  );
}
