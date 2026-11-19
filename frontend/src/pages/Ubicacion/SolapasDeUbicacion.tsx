import { Icon } from '@/components/sistema/Icon';
import type { EstadoDeUbicacion } from '@/hooks/ubicacion/useConfiguracionUbicacion';
import type { SolapaActiva } from '@/hooks/ubicacion/useSolapaActiva';
import { SECCIONES_DE_UBICACION, type SeccionUbicacion } from '@/types/ubicacion';
import { SOLAPAS } from './solapas';

type Props = { solapa: SolapaActiva; estado: EstadoDeUbicacion };

function MarcaDeEstado({ clave, estado }: { clave: SeccionUbicacion; estado: EstadoDeUbicacion }) {
  if (estado.sinGuardar[clave]) {
    return <span className="ubicacion-solapa-estado pendiente">Sin guardar</span>;
  }
  if (!estado.conDatosGuardados[clave]) return null;
  return (
    <span className="ubicacion-solapa-estado guardada">
      <Icon name="check" />
      <span className="sr-only">Guardado</span>
    </span>
  );
}

function BotonDeSolapa({ clave, solapa, estado }: Props & { clave: SeccionUbicacion }) {
  const seleccionada = clave === solapa.activa;
  return (
    <button
      ref={solapa.registrarBoton(clave)}
      id={`solapa-${clave}`}
      type="button"
      role="tab"
      aria-selected={seleccionada}
      aria-controls={seleccionada ? `panel-${clave}` : undefined}
      tabIndex={seleccionada ? 0 : -1}
      className={seleccionada ? 'activa' : ''}
      onClick={() => solapa.elegir(clave)}
      onKeyDown={solapa.moverConTeclado}
    >
      {SOLAPAS[clave].nombre}
      <MarcaDeEstado clave={clave} estado={estado} />
    </button>
  );
}

export function SolapasDeUbicacion({ solapa, estado }: Props) {
  return (
    <div className="ubicacion-solapas" role="tablist" aria-label="Configuración del comercio">
      {SECCIONES_DE_UBICACION.map((clave) => (
        <BotonDeSolapa key={clave} clave={clave} solapa={solapa} estado={estado} />
      ))}
    </div>
  );
}
