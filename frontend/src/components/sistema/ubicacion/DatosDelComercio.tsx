type Props = {
  nombre: string;
  localidad: string;
  comparteDatosZona: boolean;
  zona: string | null;
  ubicacionGuardada: boolean;
  localidadDelMapa: string | null;
  errorLocalidad: string;
  verificandoLocalidad: boolean;
  onNombre: (valor: string) => void;
  onLocalidad: (valor: string) => void;
  onUsarLocalidadDelMapa: () => void;
  onReintentarLocalidad: () => void;
  onCompartir: (valor: boolean) => void;
};

export function DatosDelComercio(props: Props) {
  const ayuda = props.verificandoLocalidad
    ? 'Verificando la localidad con el mapa…'
    : props.localidadDelMapa
      ? `Según el mapa: ${props.localidadDelMapa}. Se completa sola al elegir o mover el punto.`
      : 'Se completa sola al elegir o mover el punto en el mapa.';
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
      <div className="field">
        <label htmlFor="negocio-localidad">Localidad del comercio</label>
        <input
          id="negocio-localidad"
          className="input"
          maxLength={120}
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
      <p className="ubicacion-ayuda">
        Podés desactivar tu aporte a la zona. La configuración y la ubicación de tu comercio son
        privadas.
      </p>
    </section>
  );
}
