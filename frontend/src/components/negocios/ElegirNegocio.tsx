import { InicialDelNegocio } from '@/components/negocios/InicialDelNegocio';
import { Icon } from '@/components/sistema/Icon';
import { textoDeAvisos, type NegocioParaElegir } from '@/lib/negocios/presentacion';

export type EstadoDeLaEleccion =
  | { tipo: 'cargando' }
  | { tipo: 'error'; reintentar: () => void }
  | { tipo: 'listo'; negocios: NegocioParaElegir[]; activoId: string | null };

type Props = {
  estado: EstadoDeLaEleccion;
  onElegir: (negocioId: string) => void;
};

// Lo que se ve en el pop-up que aparece al iniciar sesión con más de un negocio (T-204): con qué negocio se va a
// trabajar. Sólo lo justo para decidir (nombre y, si se saben, sus avisos), sin montos ni juicios sobre cómo viene. No sabe de consultas ni del diálogo: recibe el estado y avisa cuál se eligió.
export function ElegirNegocio({ estado, onElegir }: Props) {
  if (estado.tipo === 'cargando') {
    return (
      <div className="elegir-negocio-cargando">
        <span className="sr-only" role="status">
          Buscando tus negocios…
        </span>
        <span />
        <span />
      </div>
    );
  }

  if (estado.tipo === 'error') {
    return (
      <p className="tablero-aviso" role="status">
        No pudimos traer tus negocios ahora.{' '}
        <button type="button" onClick={estado.reintentar}>
          Probar de nuevo
        </button>
      </p>
    );
  }

  return (
    <ul className="elegir-negocio">
      {estado.negocios.map((negocio, posicion) => {
        const nombreId = `elegir-negocio-${negocio.id}`;
        const detalleId = `${nombreId}-detalle`;
        const esElActivo = negocio.id === estado.activoId;
        return (
          <li key={negocio.id}>
            <button
              type="button"
              className="elegir-negocio-opcion"
              aria-labelledby={nombreId}
              aria-describedby={detalleId}
              onClick={() => onElegir(negocio.id)}
            >
              <InicialDelNegocio
                className="elegir-negocio-icono"
                nombre={negocio.nombre}
                posicion={posicion}
              />
              <span className="elegir-negocio-texto">
                <strong id={nombreId}>{negocio.nombre}</strong>
                <span id={detalleId} className="elegir-negocio-detalle">
                  {negocio.avisosPendientes !== null && (
                    <span data-hay={negocio.avisosPendientes > 0 || undefined}>
                      {textoDeAvisos(negocio.avisosPendientes)}
                    </span>
                  )}
                  {esElActivo && (
                    <span className="elegir-negocio-ultimo">
                      {negocio.avisosPendientes !== null && ' · '}El último que usaste
                    </span>
                  )}
                </span>
              </span>
              <span className="elegir-negocio-flecha" aria-hidden="true">
                <Icon name="arrow" />
              </span>
            </button>
          </li>
        );
      })}
    </ul>
  );
}
