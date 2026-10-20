import { Link } from '@tanstack/react-router';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { PageFooter } from '@/components/sistema/PageFooter';
import { AjusteUbicacion } from '@/components/sistema/ubicacion/AjusteUbicacion';
import { BuscadorDireccion } from '@/components/sistema/ubicacion/BuscadorDireccion';
import { RevisionUbicacion } from '@/components/sistema/ubicacion/RevisionUbicacion';
import { SelectorRubro } from '@/components/sistema/ubicacion/SelectorRubro';
import { DatosDelComercio } from '@/components/sistema/ubicacion/DatosDelComercio';
import { useConfiguracionUbicacion } from '@/hooks/ubicacion/useConfiguracionUbicacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';

export function UbicacionPage() {
  useTituloPagina('Ubicación y rubro');
  const estado = useConfiguracionUbicacion();
  return (
    <>
      <CabeceraComun active="zone" />
      <main id="main" tabIndex={-1} className="page-wrap ubicacion-page reveal">
        <Link to="/zona" className="text-link">
          ← Volver a Mi zona
        </Link>
        <div className="page-title">
          <div className="eyebrow">TU COMERCIO, EN SU LUGAR</div>
          <h1>Ubicación y rubro</h1>
          <p>
            Encontrá tu dirección y contanos qué vendés para preparar las comparaciones con tu zona.
          </p>
        </div>
        <aside className="ubicacion-aviso">
          <strong>Configuración privada de tu comercio</strong>
          <p>
            Los cambios se aplican al confirmar Guardar configuración. Al volver a entrar vas a ver
            los datos que guardaste.
          </p>
          {estado.busquedaSimulada && (
            <p>
              Modo de prueba: direcciones de ejemplo de San Justo y guardado simulado en este
              navegador.
            </p>
          )}
        </aside>
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
        <fieldset
          className="ubicacion-formulario"
          disabled={estado.cargandoConfiguracion || !!estado.errorConfiguracion || estado.guardando}
        >
          <legend className="sr-only">Configuración del comercio</legend>
          <DatosDelComercio
            nombre={estado.nombre}
            localidad={estado.localidad}
            comparteDatosZona={estado.comparteDatosZona}
            zona={estado.zona}
            ubicacionGuardada={estado.ubicacionGuardada}
            onNombre={estado.cambiarNombre}
            onLocalidad={estado.cambiarLocalidad}
            onCompartir={estado.cambiarCompartir}
          />
          <div className="ubicacion-grid">
            <BuscadorDireccion
              consulta={estado.consulta}
              onConsulta={estado.cambiarConsulta}
              onBuscar={estado.buscar}
              buscando={estado.buscando}
              resultados={estado.resultados}
              elegida={estado.direccion}
              onElegir={estado.elegirDireccion}
              error={estado.errorBusqueda}
            />
            <AjusteUbicacion
              elegida={!!estado.direccion}
              punto={estado.punto}
              latitud={estado.latitud}
              longitud={estado.longitud}
              onCoordenada={estado.cambiarCoordenada}
              onMover={estado.moverPunto}
            />
            <SelectorRubro
              rubros={estado.rubros}
              valor={estado.rubro}
              onCambiar={estado.cambiarRubro}
              cargando={estado.cargandoRubros}
              error={estado.errorRubros}
              onReintentar={estado.reintentarRubros}
            />
          </div>
          <div className="ubicacion-acciones">
            <button
              type="button"
              className="btn primary"
              disabled={estado.buscando || estado.cargandoRubros || !!estado.errorRubros}
              onClick={estado.revisar}
            >
              Revisar configuración
            </button>
            <button
              type="button"
              className="btn primary"
              disabled={estado.buscando || estado.cargandoRubros || !!estado.errorRubros}
              onClick={() => void estado.guardar()}
            >
              {estado.guardando ? 'Guardando…' : 'Guardar configuración'}
            </button>
            <p className="form-error" role="alert">
              {estado.errorRevision}
            </p>
            <p className="form-error" role="alert">
              {estado.errorGuardado}
            </p>
          </div>
        </fieldset>
        {estado.guardado && (
          <p className="ubicacion-revision" role="status">
            La configuración de tu comercio se guardó correctamente.
          </p>
        )}
        {estado.revision && <RevisionUbicacion configuracion={estado.revision} />}
        <p className="ubicacion-ayuda">
          El mapa muestra sólo tu punto. Los datos de otros comercios se presentan agregados y
          anónimos en Mi zona.
        </p>
      </main>
      <PageFooter />
    </>
  );
}
