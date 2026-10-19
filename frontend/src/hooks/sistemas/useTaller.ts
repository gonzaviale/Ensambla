import { useMutation } from '@tanstack/react-query';
import { conversarEnTaller } from '@/api/samiTaller';
import {
  aplicarAcciones,
  entiendeElNegocio,
  esOrdenSimple,
  mismoSistema,
  resumirSistema,
} from '@/lib/sistema/acciones';
import { allBlocks, clone, createGuided, makeSystem, refineDraft } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { useSistemaStore, type Taller } from '@/stores/sistemaStore';
import type { OpcionesDeSami } from '@/types/samiTaller';

const MENSAJE_CORTO = 'Contame un poquito más para encontrar un punto de partida.';
const MENSAJE_FALLO = 'Sami no pudo pensar la propuesta. Probá de nuevo en un momento.';
const MENSAJE_DESHECHO = 'Listo, volví atrás. ¿Probamos otra cosa?';
const MENSAJES_DE_HISTORIAL = 6;
const LARGO_MAXIMO_DE_MENSAJE = 1000;

// Sami piensa un momento: en la pantalla se ve el globo con los puntos. En los tests no se espera.
const DEMORA_MS = import.meta.env.MODE === 'test' ? 0 : 500;
const esperar = () => new Promise((resolver) => setTimeout(resolver, DEMORA_MS));

type Turno = {
  borrador: Sistema | null;
  respuesta: string;
  opciones: OpcionesDeSami | null;
  /** La propuesta antes de este cambio, si cambió algo. */
  anterior: Sistema | null;
};

// La charla con Sami en el taller. Gasta lo menos posible: lo que las reglas del taller entienden (armar una base
// con una descripción clara, «sumá gastos», «separá los clientes») se resuelve en el navegador, sin IA. Lo demás va
// a POST /api/sami/taller: el modelo conversa y pide herramientas, el backend las valida y acá se aplican al
// borrador. Cada cambio de Sami se puede deshacer.
export function useTaller() {
  const consulta = useMutation({ mutationFn: conversarEnTaller });

  const resolverTurno = async (
    texto: string,
    taller: Taller,
    usarReglas: boolean,
  ): Promise<Turno> => {
    const { borrador } = taller;
    if (!borrador && (taller.actividad || entiendeElNegocio(texto))) {
      await esperar();
      const { system } = createGuided(texto, taller.nombre, taller.actividad);
      const piezas = allBlocks(system)
        .map((bloque) => bloque.name.toLowerCase())
        .join(', ');
      return {
        borrador: system,
        respuesta: `Te propongo esta base: ${piezas}. Ya podés verla a la derecha. ¿Qué te falta? ¿Cambiarías algo de lugar?`,
        opciones: null,
        anterior: null,
      };
    }
    if (borrador && usarReglas) {
      await esperar();
      const copia = clone(borrador);
      const respuesta = refineDraft(copia, texto);
      if (!mismoSistema(copia, borrador)) {
        return { borrador: copia, respuesta, opciones: null, anterior: borrador };
      }
    }

    const base = borrador ?? makeSystem(taller.nombre || 'Mi sistema', [], true);
    const historial = taller.mensajes.slice(-MENSAJES_DE_HISTORIAL).map((mensaje) => ({
      quien: mensaje.quien,
      texto: mensaje.texto.slice(0, LARGO_MAXIMO_DE_MENSAJE),
    }));
    const respuesta = await consulta.mutateAsync({
      mensaje: texto,
      estado: resumirSistema(base),
      historial,
    });
    const copia = clone(base);
    aplicarAcciones(copia, respuesta.acciones);
    const cambio = !mismoSistema(copia, base);
    return {
      borrador: borrador || cambio ? copia : null,
      respuesta: respuesta.respuesta,
      opciones: respuesta.opciones ?? null,
      anterior: borrador && cambio ? borrador : null,
    };
  };

  /**
   * `esOpcion`: lo que se manda es una respuesta rápida de la botonera, que puede ser corta («Sí»).
   * `soloReglas`: es uno de los ajustes fijos de la pantalla; se prueba primero con las reglas del taller (gratis).
   */
  const enviar = async (mensaje?: string, { esOpcion = false, soloReglas = false } = {}) => {
    const store = useSistemaStore.getState();
    const taller = store.taller;
    if (!taller || taller.pensando) return;
    const texto = (mensaje ?? taller.entrada).trim();
    if (texto.length < (esOpcion ? 1 : 5)) {
      store.avisar(MENSAJE_CORTO);
      return;
    }
    store.actualizarTaller((actual) => ({
      entrada: '',
      pensando: true,
      opciones: null,
      mensajes: [...actual.mensajes, { quien: 'you', texto }],
    }));

    // Si mientras tanto empezó otra conversación, la respuesta de esta ya no importa.
    const vigente = () => useSistemaStore.getState().taller?.id === taller.id;
    const nombreAlEnviar = taller.borrador?.name;
    try {
      const turno = await resolverTurno(texto, taller, soloReglas || esOrdenSimple(texto));
      if (!vigente()) return;
      useSistemaStore.getState().actualizarTaller((actual) => {
        const sistema = turno.borrador;
        // El nombre que la persona fue escribiendo mientras Sami pensaba no se pisa.
        if (sistema && actual.borrador && actual.borrador.name !== nombreAlEnviar) {
          sistema.name = actual.borrador.name;
        }
        const seccionVigente = sistema?.sections.some((seccion) => seccion.id === actual.seccionId);
        return {
          borrador: sistema,
          seccionId: seccionVigente ? actual.seccionId : (sistema?.sections[0]?.id ?? null),
          pensando: false,
          opciones: turno.opciones,
          anterior: turno.anterior,
          mensajes: [...actual.mensajes, { quien: 'sami', texto: turno.respuesta }],
        };
      });
    } catch (error) {
      if (!vigente()) return;
      useSistemaStore.getState().actualizarTaller({ pensando: false });
      useSistemaStore.getState().avisar(error instanceof Error ? error.message : MENSAJE_FALLO);
    }
  };

  /** Vuelve la propuesta a como estaba antes del último cambio de Sami. */
  const deshacer = () => {
    const store = useSistemaStore.getState();
    if (!store.taller?.anterior || store.taller.pensando) return;
    store.actualizarTaller((actual) => ({
      borrador: actual.anterior,
      seccionId: actual.anterior?.sections.some((seccion) => seccion.id === actual.seccionId)
        ? actual.seccionId
        : (actual.anterior?.sections[0]?.id ?? null),
      anterior: null,
      opciones: null,
      mensajes: [...actual.mensajes, { quien: 'sami', texto: MENSAJE_DESHECHO }],
    }));
  };

  return { enviar, deshacer };
}
