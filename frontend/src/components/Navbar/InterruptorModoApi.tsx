import { env } from '@/config/env';
import { guardarModoApi } from '@/lib/modoApi';

type Props = {
  /** Recarga la página para arrancar en el otro modo; los tests la reemplazan. */
  recargar?: () => void;
};

// Interruptor casi escondido en el borde superior: pasa entre el backend real y la API simulada en el navegador.
// Es para presentar: si el backend falla, se toca y la app sigue andando con datos de demostración. A propósito es
// diminuto y no sigue la regla de 48 px; se puede enfocar con el teclado y su nombre accesible dice qué hace.
export function InterruptorModoApi({ recargar = () => window.location.reload() }: Props) {
  const simulado = env.mocks === 'completo';

  const cambiar = async () => {
    guardarModoApi(simulado ? 'real' : 'mock');
    // Al volver al real se da de baja el service worker de los mocks, así no queda nada interceptando pedidos.
    if (simulado && 'serviceWorker' in navigator) {
      try {
        const registros = await navigator.serviceWorker.getRegistrations();
        await Promise.all(
          registros
            .filter((registro) => registro.active?.scriptURL.includes('mockServiceWorker'))
            .map((registro) => registro.unregister()),
        );
      } catch {
        // Si no se puede dar de baja, el service worker igual deja pasar los pedidos de una página sin mocks.
      }
    }
    recargar();
  };

  return (
    <button
      type="button"
      onClick={() => void cambiar()}
      aria-label={simulado ? 'Datos simulados. Pasar al backend real' : 'Datos reales. Pasar a datos simulados'}
      title={simulado ? 'Modo mock: tocá para usar el backend real' : 'Modo real: tocá para usar datos simulados'}
      className="fixed top-0 left-0 z-[9999] cursor-pointer rounded-br border-0 bg-transparent px-1 text-[9px] leading-3 text-fg opacity-20 hover:opacity-100 focus-visible:opacity-100"
    >
      {simulado ? 'mock' : 'real'}
    </button>
  );
}
