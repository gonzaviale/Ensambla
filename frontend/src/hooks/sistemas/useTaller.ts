import { useMutation } from '@tanstack/react-query';
import { pedirPropuesta } from '@/api/sami';
import { useSistemaStore } from '@/stores/sistemaStore';

const MENSAJE_CORTO = 'Contame un poquito más para encontrar un punto de partida.';
const MENSAJE_FALLO = 'Sami no pudo pensar la propuesta. Probá de nuevo en un momento.';

// Le manda a Sami lo que la persona escribió (o un ejemplo) y deja la propuesta en el store. Sin propuesta
// todavía arma una base; con propuesta la ajusta. Es el `guideSend()` del mockup.
export function useTaller() {
  const propuesta = useMutation({ mutationFn: pedirPropuesta });

  const enviar = async (mensaje?: string) => {
    const store = useSistemaStore.getState();
    const taller = store.taller;
    if (!taller || taller.pensando) return;
    const texto = (mensaje ?? taller.entrada).trim();
    if (texto.length < 5) {
      store.avisar(MENSAJE_CORTO);
      return;
    }
    store.actualizarTaller((actual) => ({
      entrada: '',
      pensando: true,
      mensajes: [...actual.mensajes, { quien: 'you', texto }],
    }));

    // Si mientras tanto empezó otra conversación, la respuesta de esta ya no importa.
    const vigente = () => useSistemaStore.getState().taller?.id === taller.id;
    try {
      const respuesta = await propuesta.mutateAsync(
        taller.borrador
          ? { tipo: 'ajustar', texto, sistema: taller.borrador }
          : { tipo: 'crear', texto, nombre: taller.nombre, actividad: taller.actividad },
      );
      if (!vigente()) return;
      useSistemaStore.getState().actualizarTaller((actual) => {
        const sistema = respuesta.sistema;
        // El nombre que la persona fue escribiendo mientras Sami pensaba no se pisa.
        if (actual.borrador) sistema.name = actual.borrador.name;
        const seccionVigente = sistema.sections.some((seccion) => seccion.id === actual.seccionId);
        return {
          borrador: sistema,
          seccionId: seccionVigente ? actual.seccionId : (sistema.sections[0]?.id ?? null),
          pensando: false,
          mensajes: [...actual.mensajes, { quien: 'sami', texto: respuesta.respuesta }],
        };
      });
    } catch {
      if (!vigente()) return;
      useSistemaStore.getState().actualizarTaller({ pensando: false });
      useSistemaStore.getState().avisar(MENSAJE_FALLO);
    }
  };

  return { enviar };
}
