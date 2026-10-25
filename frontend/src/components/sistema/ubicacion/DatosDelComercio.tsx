type PropsDatos = {
  nombre: string;
  comparteDatosZona: boolean;
  zona: string | null;
  ubicacionGuardada: boolean;
  onNombre: (valor: string) => void;
  onCompartir: (valor: boolean) => void;
};

// El primer paso: cómo se llama el comercio y si aporta sus datos a la zona.
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
    </section>
  );
}

type PropsLocalidad = {
  localidad: string;
  localidadDelMapa: string | null;
  errorLocalidad: string;
  verificandoLocalidad: boolean;
  onLocalidad: (valor: string) => void;
  onUsarLocalidadDelMapa: () => void;
  onReintentarLocalidad: () => void;
};

// La localidad va en el bloque de la dirección: sirve para buscar y se completa sola con el punto del mapa.
export function LocalidadDelComercio(props: PropsLocalidad) {
  const ayuda = props.verificandoLocalidad
    ? 'Verificando la localidad con el mapa…'
    : props.localidadDelMapa
      ? `Según el mapa: ${props.localidadDelMapa}.`
      : 'Se completa sola con el punto del mapa.';
  return (
    <div className="field">
      <label htmlFor="negocio-localidad">Localidad</label>
      <input
        id="negocio-localidad"
        className="input"
        maxLength={120}
        placeholder="Ej.: San Justo"
        value={props.localidad}
        aria-invalid={props.errorLocalidad ? true : undefined}
        aria-describedby="ayuda-localidad error-localidad"
        onChange={(evento) => props.onLocalidad(evento.target.value)}
      />
      <p id="ayuda-localidad" className="ubicacion-ayuda">
        {ayuda}
      </p>
      <p id="error-localidad" className="form-error" role="alert">
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
