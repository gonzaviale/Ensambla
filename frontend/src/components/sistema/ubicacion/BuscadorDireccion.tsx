import type { FormEvent, ReactNode } from 'react';
import type { Direccion } from '@/types/ubicacion';

const TEXTOS_DEL_COMERCIO = {
  /** Sin paso, no se muestra la marca de arriba del título. */
  paso: '',
  titulo: 'Encontrá tu comercio',
  etiqueta: 'Dirección',
  ayuda: 'Calle, altura y localidad, todo junto.',
  ejemplo: 'Ej.: Florencio Varela 1903, San Justo',
};

type Props = {
  consulta: string;
  onConsulta: (valor: string) => void;
  /** La localidad que quedó para la dirección elegida: se completa sola con el mapa. */
  localidad?: ReactNode;
  onBuscar: (evento: FormEvent) => void;
  buscando: boolean;
  resultados: Direccion[] | null;
  elegida: Direccion | null;
  onElegir: (direccion: Direccion) => void;
  error: string;
  /** El botón de guardar de la solapa, al final del recuadro. */
  pie?: ReactNode;
  /** Textos propios de cada pantalla; sin ellos, los del paso «Encontrá tu comercio». */
  textos?: Partial<typeof TEXTOS_DEL_COMERCIO>;
  /** Con otra acción principal en la pantalla, el botón de buscar pasa a secundario. */
  botonSecundario?: boolean;
};

export function BuscadorDireccion(props: Props) {
  const textos = { ...TEXTOS_DEL_COMERCIO, ...props.textos };
  return (
    <section className="ubicacion-panel ubicacion-direccion" aria-labelledby="titulo-direccion">
      {textos.paso && <div className="eyebrow">{textos.paso}</div>}
      <h2 id="titulo-direccion">{textos.titulo}</h2>
      <form onSubmit={props.onBuscar} noValidate>
        <div className="field">
          <label htmlFor="direccion-completa">{textos.etiqueta}</label>
          <input
            id="direccion-completa"
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
          <p id="ayuda-direccion" className="ubicacion-ayuda">
            {textos.ayuda}
          </p>
        </div>
        <button
          type="submit"
          className={props.botonSecundario ? 'btn secondary' : 'btn primary'}
          disabled={props.buscando}
        >
          {props.buscando ? 'Buscando…' : 'Buscar dirección'}
        </button>
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
      {props.elegida && props.localidad}
      {props.pie}
    </section>
  );
}
