import type { FormEvent } from 'react';
import type { Direccion } from '@/types/ubicacion';

const TEXTOS_DEL_COMERCIO = {
  paso: 'PASO 1',
  titulo: 'Encontrá tu comercio',
  etiqueta: 'Dirección del comercio',
  ayuda: 'Calle, altura y localidad. Buscamos direcciones de Argentina.',
  ejemplo: 'Ej.: Florencio Varela 1903, San Justo',
};

type Props = {
  consulta: string;
  onConsulta: (valor: string) => void;
  onBuscar: (evento: FormEvent) => void;
  buscando: boolean;
  resultados: Direccion[] | null;
  elegida: Direccion | null;
  onElegir: (direccion: Direccion) => void;
  error: string;
  /** Textos propios de cada pantalla; sin ellos, los del paso «Encontrá tu comercio». */
  textos?: Partial<typeof TEXTOS_DEL_COMERCIO>;
  /** Con otra acción principal en la pantalla, el botón de buscar pasa a secundario. */
  botonSecundario?: boolean;
};

export function BuscadorDireccion(props: Props) {
  const textos = { ...TEXTOS_DEL_COMERCIO, ...props.textos };
  return (
    <section className="ubicacion-panel ubicacion-direccion" aria-labelledby="titulo-direccion">
      <div className="eyebrow">{textos.paso}</div>
      <h2 id="titulo-direccion">{textos.titulo}</h2>
      <form onSubmit={props.onBuscar} noValidate>
        <div className="field">
          <label htmlFor="direccion-comercial">{textos.etiqueta}</label>
          <p id="ayuda-direccion">{textos.ayuda}</p>
          <div className="ubicacion-buscador">
            <input
              id="direccion-comercial"
              className="input"
              type="text"
              autoComplete="off"
              maxLength={200}
              placeholder={textos.ejemplo}
              aria-describedby="ayuda-direccion error-direccion"
              aria-invalid={!!props.error}
              value={props.consulta}
              onChange={(evento) => props.onConsulta(evento.target.value)}
            />
            <button
              type="submit"
              className={props.botonSecundario ? 'btn secondary' : 'btn primary'}
              disabled={props.buscando}
            >
              {props.buscando ? 'Buscando…' : 'Buscar dirección'}
            </button>
          </div>
        </div>
        <p className="ubicacion-ayuda">
          Al buscar, enviamos esa dirección comercial a Nominatim / OpenStreetMap. No ingreses datos
          personales.
        </p>
        <div id="error-direccion" className="form-error" role="alert">
          {props.error}
        </div>
      </form>
      <div aria-live="polite" aria-busy={props.buscando}>
        {props.buscando && <p>Buscando direcciones…</p>}
        {props.resultados?.length === 0 && (
          <p>No encontramos esa dirección. Probá con otra altura o agregá la localidad.</p>
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
    </section>
  );
}
