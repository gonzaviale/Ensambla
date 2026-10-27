import { useRef, useState, type KeyboardEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { AjusteUbicacion } from '@/components/sistema/ubicacion/AjusteUbicacion';
import { BuscadorDireccion } from '@/components/sistema/ubicacion/BuscadorDireccion';
import { SelectorRubro } from '@/components/sistema/ubicacion/SelectorRubro';
import {
  DatosDelComercio,
  LocalidadDelComercio,
} from '@/components/sistema/ubicacion/DatosDelComercio';
import {
  useConfiguracionUbicacion,
  type SeccionUbicacion,
} from '@/hooks/ubicacion/useConfiguracionUbicacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';

const SOLAPAS: Record<SeccionUbicacion, { nombre: string; guardar: string; guardado: string }> = {
  comercio: {
    nombre: 'Tu comercio',
    guardar: 'Guardar datos del comercio',
    guardado: 'Guardamos los datos de tu comercio.',
  },
  ubicacion: {
    nombre: 'Ubicación',
    guardar: 'Guardar ubicación',
    guardado: 'Guardamos la ubicación de tu comercio.',
  },
  rubro: {
    nombre: 'Rubro',
    guardar: 'Guardar rubro',
    guardado: 'Guardamos el rubro de tu comercio.',
  },
};
const ORDEN: SeccionUbicacion[] = ['comercio', 'ubicacion', 'rubro'];

// La configuración del comercio en solapas: se entra a cualquiera en el orden que quieras y cada una
// se guarda sola. Lo cargado en una solapa queda en pantalla aunque vayas a otra.
export function UbicacionPage() {
  useTituloPagina('Ubicación y rubro');
  const estado = useConfiguracionUbicacion();
  const [activa, setActiva] = useState<SeccionUbicacion>('comercio');
  const botones = useRef<Partial<Record<SeccionUbicacion, HTMLButtonElement | null>>>({});
  const solapa = SOLAPAS[activa];
  const guardando = estado.guardandoEn !== null;
  const noSePuedeGuardar =
    guardando ||
    (activa === 'ubicacion' && (estado.buscando || estado.localidadBloquea)) ||
    (activa === 'rubro' && (estado.cargandoRubros || !!estado.errorRubros));

  // Flechas izquierda y derecha para moverse entre solapas, como en cualquier lista de pestañas.
  const moverConTeclado = (evento: KeyboardEvent<HTMLButtonElement>) => {
    const salto = evento.key === 'ArrowRight' ? 1 : evento.key === 'ArrowLeft' ? -1 : 0;
    if (!salto) return;
    evento.preventDefault();
    const indice = ORDEN.indexOf(activa);
    const siguiente = ORDEN[(indice + salto + ORDEN.length) % ORDEN.length] ?? activa;
    setActiva(siguiente);
    botones.current[siguiente]?.focus();
  };

  // El botón de guardar y sus mensajes van adentro del recuadro de cada solapa.
  const pie = (
    <div className="ubicacion-pie">
      <button
        type="button"
        className="btn primary"
        disabled={noSePuedeGuardar}
        onClick={() => void estado.guardar(activa)}
      >
        {estado.guardandoEn === activa ? 'Guardando…' : solapa.guardar}
      </button>
      <p className="form-error" role="alert">
        {estado.erroresGuardado[activa]}
      </p>
      {estado.guardados[activa] && (
        <p className="ubicacion-guardado" role="status">
          {solapa.guardado}
          {activa === 'ubicacion' &&
            (estado.zona ? ` Tu zona: ${estado.zona}.` : ' Todavía no hay una zona que la cubra.')}
        </p>
      )}
    </div>
  );

  return (
    <>
      <CabeceraComun active="zone" />
      <main id="main" tabIndex={-1} className="page-wrap ubicacion-page reveal">
        <Link to="/zona" className="text-link">
          ← Volver a Mi zona
        </Link>
        <div className="page-title">
          <h1>Ubicación y rubro</h1>
          <p>Así comparamos tu comercio con los de tu zona.</p>
        </div>
        {estado.busquedaSimulada && (
          <p className="ubicacion-aviso">
            Modo de prueba: direcciones de ejemplo de San Justo y guardado simulado.
          </p>
        )}
        <div className="ubicacion-solapas" role="tablist" aria-label="Configuración del comercio">
          {ORDEN.map((clave) => {
            const seleccionada = clave === activa;
            const pendiente = estado.sinGuardar[clave];
            return (
              <button
                key={clave}
                ref={(boton) => {
                  botones.current[clave] = boton;
                }}
                id={`solapa-${clave}`}
                type="button"
                role="tab"
                aria-selected={seleccionada}
                aria-controls={seleccionada ? `panel-${clave}` : undefined}
                tabIndex={seleccionada ? 0 : -1}
                className={seleccionada ? 'activa' : ''}
                onClick={() => setActiva(clave)}
                onKeyDown={moverConTeclado}
              >
                {SOLAPAS[clave].nombre}
                {pendiente ? (
                  <span className="ubicacion-solapa-estado pendiente">Sin guardar</span>
                ) : (
                  estado.conDatosGuardados[clave] && (
                    <span className="ubicacion-solapa-estado guardada">
                      <Icon name="check" />
                      <span className="sr-only">Guardado</span>
                    </span>
                  )
                )}
              </button>
            );
          })}
        </div>
        {estado.cargandoConfiguracion && (
          <p role="status">Cargando la configuración del comercio…</p>
        )}
        {estado.errorConfiguracion && (
          <div role="alert">
            <p>{estado.errorConfiguracion}</p>
            <button className="btn secondary" onClick={estado.reintentarConfiguracion}>
              Reintentar configuración
            </button>
          </div>
        )}
        <div
          id={`panel-${activa}`}
          role="tabpanel"
          aria-labelledby={`solapa-${activa}`}
          className="ubicacion-solapa-panel"
        >
          <fieldset
            className="ubicacion-formulario"
            disabled={estado.cargandoConfiguracion || !!estado.errorConfiguracion || guardando}
          >
            <legend className="sr-only">Configuración del comercio: {solapa.nombre}</legend>
            {activa === 'comercio' && (
              <DatosDelComercio
                nombre={estado.nombre}
                comparteDatosZona={estado.comparteDatosZona}
                zona={estado.zona}
                ubicacionGuardada={estado.ubicacionGuardada}
                onNombre={estado.cambiarNombre}
                onCompartir={estado.cambiarCompartir}
                pie={pie}
              />
            )}
            {/* Sólo se monta la solapa visible: el mapa no se dibuja bien estando oculto. */}
            {activa === 'ubicacion' && (
              <div className="ubicacion-grid">
                <BuscadorDireccion
                  consulta={estado.consulta}
                  onConsulta={estado.cambiarConsulta}
                  localidad={
                    <LocalidadDelComercio
                      localidad={estado.localidad}
                      localidadDelMapa={estado.localidadDelMapa}
                      errorLocalidad={estado.errorLocalidad}
                      verificandoLocalidad={estado.verificandoLocalidad}
                      onUsarLocalidadDelMapa={estado.usarLocalidadDelMapa}
                      onReintentarLocalidad={estado.reintentarLocalidad}
                    />
                  }
                  onBuscar={estado.buscar}
                  buscando={estado.buscando}
                  resultados={estado.resultados}
                  elegida={estado.direccion}
                  onElegir={estado.elegirDireccion}
                  error={estado.errorBusqueda}
                  pie={pie}
                />
                <AjusteUbicacion punto={estado.punto} onMover={estado.moverPunto} />
              </div>
            )}
            {activa === 'rubro' && (
              <SelectorRubro
                rubros={estado.rubros}
                valor={estado.rubro}
                onCambiar={estado.cambiarRubro}
                cargando={estado.cargandoRubros}
                error={estado.errorRubros}
                onReintentar={estado.reintentarRubros}
                pie={pie}
              />
            )}
          </fieldset>
        </div>
        <PageFooter />
      </main>
    </>
  );
}
