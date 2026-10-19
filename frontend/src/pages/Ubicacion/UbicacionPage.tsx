import { Link } from '@tanstack/react-router';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { PageFooter } from '@/components/sistema/PageFooter';
import { AjusteUbicacion } from '@/components/sistema/ubicacion/AjusteUbicacion';
import { BuscadorDireccion } from '@/components/sistema/ubicacion/BuscadorDireccion';
import { RevisionUbicacion } from '@/components/sistema/ubicacion/RevisionUbicacion';
import { SelectorRubro } from '@/components/sistema/ubicacion/SelectorRubro';
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
          <strong>Vista previa, sin guardar</strong>
          <p>
            Podés probar el flujo completo. Los datos se borran al salir de esta pantalla y no
            modifican tu negocio ni los datos de Mi zona.
          </p>
          {estado.busquedaSimulada && (
            <p>En este modo se muestran direcciones de ejemplo de San Justo.</p>
          )}
        </aside>
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
          <p className="form-error" role="alert">
            {estado.errorRevision}
          </p>
        </div>
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
