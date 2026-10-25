import type { FormEvent, ReactNode } from 'react';
import type { Direccion } from '@/types/ubicacion';

type Props = {
  calle: string;
  altura: string;
  onCalle: (valor: string) => void;
  onAltura: (valor: string) => void;
  /** El campo de la localidad: va en el mismo bloque y también se usa para buscar. */
  localidad: ReactNode;
  onBuscar: (evento: FormEvent) => void;
  buscando: boolean;
  resultados: Direccion[] | null;
  elegida: Direccion | null;
  onElegir: (direccion: Direccion) => void;
  error: string;
};

export function BuscadorDireccion(props: Props) {
  return (
    <section className="ubicacion-panel ubicacion-direccion" aria-labelledby="titulo-direccion">
      <h2 id="titulo-direccion">Encontrá tu comercio</h2>
      <form onSubmit={props.onBuscar} noValidate>
        <div className="ubicacion-calle-altura">
          <div className="field">
            <label htmlFor="direccion-calle">Calle</label>
            <input
              id="direccion-calle"
              className="input"
              type="text"
              autoComplete="off"
              maxLength={150}
              placeholder="Ej.: Florencio Varela"
              aria-describedby="error-direccion"
              aria-invalid={!!props.error}
              value={props.calle}
              onChange={(evento) => props.onCalle(evento.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="direccion-altura">Altura</label>
            <input
              id="direccion-altura"
              className="input"
              type="text"
              inputMode="numeric"
              autoComplete="off"
              maxLength={10}
              placeholder="Ej.: 1903"
              aria-describedby="error-direccion"
              aria-invalid={!!props.error}
              value={props.altura}
              onChange={(evento) => props.onAltura(evento.target.value)}
            />
          </div>
        </div>
        {props.localidad}
        <button type="submit" className="btn primary" disabled={props.buscando}>
          {props.buscando ? 'Buscando…' : 'Buscar dirección'}
        </button>
        <p className="ubicacion-ayuda">Se busca en OpenStreetMap. No pongas datos personales.</p>
        <div id="error-direccion" className="form-error" role="alert">
          {props.error}
        </div>
      </form>
      <div aria-live="polite" aria-busy={props.buscando}>
        {props.buscando && <p>Buscando direcciones…</p>}
        {props.resultados?.length === 0 && (
          <p>No encontramos esa dirección. Probá con otra altura o revisá la localidad.</p>
        )}
      </div>
      {!!props.resultados?.length && (
        <fieldset className="ubicacion-resultados">
          <legend>Elegí la dirección que corresponde</legend>
          {props.resultados.map((direccion, indice) => (
            <label
              key={`${direccion.latitud}-${direccion.longitud}-${indice}`}
              className="ubicacion-opcion"
            >
              <input
                type="radio"
                name="direccion-elegida"
                checked={props.elegida === direccion}
                onChange={() => props.onElegir(direccion)}
              />
              <span>{direccion.direccion}</span>
            </label>
          ))}
        </fieldset>
      )}
      {props.elegida && !props.resultados?.length && (
        <p className="ubicacion-elegida">Dirección elegida: {props.elegida.direccion}</p>
      )}
    </section>
  );
}
