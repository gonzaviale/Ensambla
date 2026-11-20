import { Link } from '@tanstack/react-router';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { PageFooter } from '@/components/sistema/PageFooter';
import {
  useConfiguracionUbicacion,
  type EstadoDeUbicacion,
} from '@/hooks/ubicacion/useConfiguracionUbicacion';
import { useSolapaActiva } from '@/hooks/ubicacion/useSolapaActiva';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { ContenidoDeSolapa } from './ContenidoDeSolapa';
import { PieDeSolapa } from './PieDeSolapa';
import { SolapasDeUbicacion } from './SolapasDeUbicacion';
import { SOLAPAS } from './solapas';

function EstadoDeLaConfiguracion({ estado }: { estado: EstadoDeUbicacion }) {
  return (
    <>
      {estado.cargandoConfiguracion && <p role="status">Cargando la configuración del comercio…</p>}
      {estado.errorConfiguracion && (
        <div role="alert">
          <p>{estado.errorConfiguracion}</p>
          <button className="btn secondary" onClick={estado.reintentarConfiguracion}>
            Reintentar configuración
          </button>
        </div>
      )}
    </>
  );
}

const noSePuedeEditar = (estado: EstadoDeUbicacion) =>
  estado.cargandoConfiguracion || !!estado.errorConfiguracion || estado.guardandoEn !== null;

// La configuración del comercio en solapas: se entra a cualquiera en el orden que quieras y cada una
// se guarda sola. Lo cargado en una solapa queda en pantalla aunque vayas a otra.
export function UbicacionPage() {
  useTituloPagina('Ubicación y rubro');
  const estado = useConfiguracionUbicacion();
  const solapa = useSolapaActiva();
  const { activa } = solapa;
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
        <SolapasDeUbicacion solapa={solapa} estado={estado} />
        <EstadoDeLaConfiguracion estado={estado} />
        <div
          id={`panel-${activa}`}
          role="tabpanel"
          aria-labelledby={`solapa-${activa}`}
          className="ubicacion-solapa-panel"
        >
          <fieldset className="ubicacion-formulario" disabled={noSePuedeEditar(estado)}>
            <legend className="sr-only">
              Configuración del comercio: {SOLAPAS[activa].nombre}
            </legend>
            <ContenidoDeSolapa
              activa={activa}
              estado={estado}
              pie={<PieDeSolapa estado={estado} activa={activa} />}
            />
          </fieldset>
        </div>
        <PageFooter />
      </main>
    </>
  );
}
