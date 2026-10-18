import { useEffect } from 'react';
import { eliminarSistema, guardarSistema } from '@/api/sistemas';
import { useSistemaStore } from '@/stores/sistemaStore';

const ESPERA_MS = 500;
const REINTENTO_MS = 4000;

// Lleva al servidor lo que la persona va cambiando. Los cambios se juntan medio segundo, así que mover
// un bloque o escribir un nombre no dispara un pedido por cada tecla; si falla, se reintenta solo.
export function useSincronizarSistemas() {
  useEffect(() => {
    let temporizador: ReturnType<typeof setTimeout> | undefined;
    let volcando = false;
    let avisoDeErrorDado = false;

    const programar = (ms: number) => {
      clearTimeout(temporizador);
      temporizador = setTimeout(() => {
        temporizador = undefined;
        void volcar();
      }, ms);
    };

    async function volcar() {
      if (volcando) return;
      const { pendientes, eliminados, sistemas, marcarGuardados, avisar } =
        useSistemaStore.getState();
      const aGuardar = sistemas.filter((sistema) => pendientes.includes(sistema.id));
      if (aGuardar.length === 0 && eliminados.length === 0) return;
      volcando = true;
      try {
        await Promise.all([
          ...aGuardar.map((sistema) => guardarSistema(sistema)),
          ...eliminados.map((id) => eliminarSistema(id)),
        ]);
        // Si se volvió a cambiar mientras viajaba, ese sistema sigue pendiente.
        const actuales = useSistemaStore.getState().sistemas;
        const sinCambios = aGuardar
          .filter(
            (enviado) =>
              actuales.find((actual) => actual.id === enviado.id)?.updatedAt === enviado.updatedAt,
          )
          .map((sistema) => sistema.id);
        marcarGuardados(sinCambios, eliminados);
        avisoDeErrorDado = false;
      } catch {
        if (!avisoDeErrorDado) {
          avisar('No pudimos guardar tus cambios. Lo vamos a intentar de nuevo.');
          avisoDeErrorDado = true;
        }
        programar(REINTENTO_MS);
      } finally {
        volcando = false;
        const { pendientes: quedan, eliminados: porBorrar } = useSistemaStore.getState();
        if ((quedan.length > 0 || porBorrar.length > 0) && temporizador === undefined) {
          programar(ESPERA_MS);
        }
      }
    }

    const desuscribir = useSistemaStore.subscribe((estado, anterior) => {
      if (estado.pendientes === anterior.pendientes && estado.eliminados === anterior.eliminados)
        return;
      if (estado.pendientes.length > 0 || estado.eliminados.length > 0) programar(ESPERA_MS);
    });
    // Al salir de la pantalla o cerrar la pestaña no se queda nada sin mandar.
    const alSalir = () => void volcar();
    window.addEventListener('pagehide', alSalir);

    return () => {
      desuscribir();
      window.removeEventListener('pagehide', alSalir);
      clearTimeout(temporizador);
      void volcar();
    };
  }, []);
}
