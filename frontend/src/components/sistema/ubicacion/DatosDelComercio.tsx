import type { ReactNode } from 'react';

type PropsDatos = {
  nombre: string;
  comparteDatosZona: boolean;
  zona: string | null;
  ubicacionGuardada: boolean;
  onNombre: (valor: string) => void;
  onCompartir: (valor: boolean) => void;
  /** El botón de guardar de la solapa, al final del recuadro. */
  pie?: ReactNode;
};

// La solapa "Tu comercio": cómo se llama el comercio y si aporta sus datos a la zona.
export function DatosDelComercio(props: PropsDatos) {
  return (
    <section className="ubicacion-panel" aria-labelledby="titulo-datos-comercio">
      <h2 id="titulo-datos-comercio">Datos de tu comercio</h2>
      <div className="field">
        <label htmlFor="negocio-nombre">Nombre del comercio</label>
        <input
          id="negocio-nombre"
          className="input"
          maxLength={120}
          value={props.nombre}
          onChange={(evento) => props.onNombre(evento.target.value)}
        />
      </div>
      {props.ubicacionGuardada && (
        <p className="ubicacion-ayuda" role="status">
          {props.zona
            ? `Tu zona: ${props.zona}`
            : 'Todavía no hay una zona que cubra la ubicación de tu comercio.'}
        </p>
      )}
      <label className="check-row">
        <input
          type="checkbox"
          checked={props.comparteDatosZona}
          onChange={(evento) => props.onCompartir(evento.target.checked)}
        />
        Compartir datos agregados y anónimos de mi comercio con la zona
      </label>
      {props.pie}
    </section>
  );
}

type PropsLocalidad = {
  localidad: string;
  localidadDelMapa: string | null;
  errorLocalidad: string;
  verificandoLocalidad: boolean;
  onUsarLocalidadDelMapa: () => void;
  onReintentarLocalidad: () => void;
};

// La localidad sale de la dirección elegida y se verifica con el mapa; no se escribe a mano.
export function LocalidadDelComercio(props: PropsLocalidad) {
  const ayuda = props.verificandoLocalidad ? 'Verificando la localidad con el mapa…' : '';
  return (
    <div className="ubicacion-localidad-elegida">
      <p>
        Localidad: <strong>{props.localidad || 'sin definir'}</strong>
      </p>
      <p className="ubicacion-ayuda">{ayuda}</p>
      <p className="form-error" role="alert">
        {props.errorLocalidad}
      </p>
      {props.errorLocalidad && props.localidadDelMapa && (
        <button type="button" className="btn secondary" onClick={props.onUsarLocalidadDelMapa}>
          Usar {props.localidadDelMapa}
        </button>
      )}
      {props.errorLocalidad && !props.localidadDelMapa && !props.verificandoLocalidad && (
        <button type="button" className="btn secondary" onClick={props.onReintentarLocalidad}>
          Reintentar verificación
        </button>
      )}
    </div>
  );
}
