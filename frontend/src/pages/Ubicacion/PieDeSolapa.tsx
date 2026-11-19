import type { EstadoDeUbicacion } from '@/hooks/ubicacion/useConfiguracionUbicacion';
import type { SeccionUbicacion } from '@/types/ubicacion';
import { AGREGADO_AL_GUARDADO, NO_SE_PUEDE_GUARDAR, SOLAPAS } from './solapas';

type Props = { estado: EstadoDeUbicacion; activa: SeccionUbicacion };

function AvisoDeGuardado({ estado, activa }: Props) {
  if (!estado.guardados[activa]) return null;
  return (
    <p className="ubicacion-guardado" role="status">
      {SOLAPAS[activa].guardado}
      {AGREGADO_AL_GUARDADO[activa](estado)}
    </p>
  );
}

/** El botón de guardar y sus mensajes van adentro del recuadro de cada solapa. */
export function PieDeSolapa({ estado, activa }: Props) {
  const guardando = estado.guardandoEn !== null;
  const noSePuedeGuardar = guardando || NO_SE_PUEDE_GUARDAR[activa](estado);
  return (
    <div className="ubicacion-pie">
      <button
        type="button"
        className="btn primary"
        disabled={noSePuedeGuardar}
        onClick={() => void estado.guardar(activa)}
      >
        {estado.guardandoEn === activa ? 'Guardando…' : SOLAPAS[activa].guardar}
      </button>
      <p className="form-error" role="alert">
        {estado.erroresGuardado[activa]}
      </p>
      <AvisoDeGuardado estado={estado} activa={activa} />
    </div>
  );
}
