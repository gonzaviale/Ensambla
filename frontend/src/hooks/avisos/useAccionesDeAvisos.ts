import type { AccionDeAviso, Aviso } from '@/lib/sistema/alertas';
import type { Sistema } from '@/lib/sistema/tipos';
import { findBlock, id as nuevoId } from '@/lib/sistema/core';
import { crearAlmacenCargado, crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { destinoDeAviso } from '@/lib/sistema/destinoDeAviso';
import { resaltar } from '@/lib/sistema/resaltar';
import { pedirMostrarFila } from '@/lib/sistema/tandas';
import { useAplicarAviso, useDescartarAviso } from '@/hooks/avisos/mutations';
import { useGuardarPreferencias } from '@/hooks/preferencias/mutations';
import { usePreferencias } from '@/hooks/preferencias/queries';
import { useAbrirAvisos } from '@/hooks/avisos/useAbrirAvisos';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useSistemaStore } from '@/stores/sistemaStore';

type AccionDeTipo<T extends AccionDeAviso['tipo']> = Extract<AccionDeAviso, { tipo: T }>;
type AccionDelServidor = AccionDeTipo<'servidor'>;
type ManejadoresDeAccion = {
  [T in AccionDeAviso['tipo']]: (accion: AccionDeTipo<T>) => void;
};

const MAXIMO_DE_DESCARTADOS = 200;
/** Lo que se espera a que la tabla dibuje el registro pedido antes de marcarlo. */
const ESPERA_DE_LA_TANDA = 60;

const ocultarElAvisoNuevo = () => useSistemaStore.getState().ocultarAvisoNuevo();

const avisarAlUsuario = (mensaje: string) => useSistemaStore.getState().avisar(mensaje);

const actualizarElSistema = (sistemaId: string, cambio: (sistema: Sistema) => void) =>
  useSistemaStore.getState().actualizarSistema(sistemaId, cambio);

function aplicarElCambioDelAviso(accion: AccionDeTipo<'aplicar'>) {
  actualizarElSistema(accion.sistemaId, (sistema) => {
    const fila = findBlock(sistema, accion.bloqueId)?.rows.find(
      (item) => item.id === accion.filaId,
    );
    if (fila) Object.assign(fila, accion.valores);
  });
  avisarAlUsuario(accion.confirmacion);
}

function crearElRegistroDelAviso(accion: AccionDeTipo<'crear'>) {
  actualizarElSistema(accion.sistemaId, (sistema) => {
    findBlock(sistema, accion.bloqueId)?.rows.push({
      id: nuevoId(),
      ...accion.valores,
      __source: 'Aviso de Sami',
    });
  });
  avisarAlUsuario(accion.confirmacion);
}

// Lo que se puede hacer con un aviso: aplicarlo, abrirlo, descartarlo o apagar un grupo.
export function useAccionesDeAvisos() {
  const preferencias = usePreferencias();
  const guardarPreferencias = useGuardarPreferencias();
  const aplicarEnServidor = useAplicarAviso();
  const descartarEnServidor = useDescartarAviso();
  const navegacion = useNavegacion();
  const abrirAvisos = useAbrirAvisos();

  // Abre el sistema, va a la pestaña del bloque y marca con el marco animado el registro (o la tabla, si no se
  // sabe cuál). Si hace falta, abre el formulario de un registro nuevo.
  const irAlBloque = async (
    sistemaId: string,
    bloqueId: string,
    {
      filaId = null,
      nuevoRegistro = false,
      avisoId,
    }: { filaId?: string | null; nuevoRegistro?: boolean; avisoId?: string } = {},
  ) => {
    const abierto = useSistemaStore.getState();
    if (abierto.activeId !== sistemaId || abierto.modo !== 'use') {
      await navegacion.abrirSistema(sistemaId, 'use');
    }
    const store = useSistemaStore.getState();
    const sistema = store.sistemas.find((item) => item.id === sistemaId);
    const seccion = sistema?.sections.find((item) =>
      item.blocks.some((bloque) => bloque.id === bloqueId),
    );
    if (seccion && seccion.id !== store.sectionId) store.elegirSeccion(seccion.id);
    // La tabla dibuja de a tandas: con la pestaña ya a la vista se le pide el registro y, cuando lo dibujó, se lo marca.
    setTimeout(() => {
      if (filaId) pedirMostrarFila(bloqueId, filaId);
      setTimeout(
        () => {
          const bloque = document.querySelector(`[data-block="${CSS.escape(bloqueId)}"]`);
          const fila = filaId
            ? bloque?.querySelector(`[data-record="${CSS.escape(filaId)}"]`)
            : null;
          const objetivo = fila ?? bloque;
          objetivo?.scrollIntoView?.({ behavior: 'smooth', block: 'center' });
          resaltar(objetivo);
          if (nuevoRegistro) store.abrirModal({ tipo: 'registro', bloqueId, filaId, avisoId });
        },
        filaId ? ESPERA_DE_LA_TANDA : 0,
      );
    }, 120);
  };

  const manejadores: ManejadoresDeAccion = {
    aplicar: (accion) => aplicarElCambioDelAviso(accion),
    crear: (accion) => crearElRegistroDelAviso(accion),
    registrar: (accion) => {
      ocultarElAvisoNuevo();
      void irAlBloque(accion.sistemaId, accion.bloqueId, {
        nuevoRegistro: true,
        avisoId: accion.avisoId,
      });
    },
    abrir: (accion) => {
      ocultarElAvisoNuevo();
      void irAlBloque(accion.sistemaId, accion.bloqueId, { filaId: accion.filaId });
    },
    zona: () => {
      ocultarElAvisoNuevo();
      void navegacion.irAZona();
    },
    servidor: (accion) => aplicarEnElServidor(accion),
  };

  const aplicarEnElServidor = (accion: AccionDelServidor) =>
    aplicarEnServidor.mutate(accion.avisoId, {
      onSuccess: () => avisarAlUsuario(accion.confirmacion),
      onError: () => avisarAlUsuario('No pudimos aplicar el cambio. Probá de nuevo.'),
    });

  const ejecutar = (accion: AccionDeAviso) => {
    const manejar = manejadores[accion.tipo] as (accion: AccionDeAviso) => void;
    manejar(accion);
  };

  const descartar = (aviso: Aviso) => {
    // Los avisos del servidor se descartan allá: el motor no los vuelve a crear mientras no cambien sus datos.
    if (aviso.servidorId) {
      descartarEnServidor.mutate(aviso.servidorId);
      return;
    }
    guardarPreferencias.mutate({
      ...preferencias,
      descartadas: [...new Set([...preferencias.descartadas, aviso.id])].slice(
        -MAXIMO_DE_DESCARTADOS,
      ),
    });
  };

  /** «Ver»: lleva al objeto que representa el aviso y lo marca; si no apunta a ninguno, abre sus detalles. */
  const ver = (aviso: Aviso) => {
    const destino = destinoDeAviso(aviso);
    if (destino) {
      useSistemaStore.getState().ocultarAvisoNuevo();
      void irAlBloque(destino.sistemaId, destino.bloqueId, { filaId: destino.filaId });
      return;
    }
    const zona = aviso.acciones.find((accion) => accion.tipo === 'zona');
    if (zona) ejecutar(zona);
    else void abrirAvisos(aviso.id);
  };

  const crearDemo = () => {
    void navegacion.crearYAbrir(crearAlmacenDeEjemplo(hoy()), 'use');
    useSistemaStore.getState().avisar('Creamos un almacén de ejemplo. Sami ya lo está mirando.');
  };

  const crearDemoCargado = () => {
    void navegacion.crearYAbrir(crearAlmacenCargado(hoy()), 'use');
    useSistemaStore
      .getState()
      .avisar('Cargamos un almacén con muchos datos. Sami ya lo está mirando.');
  };

  /** Lleva a un registro (o a una tabla) de la sección y lo marca. */
  const irAlObjeto = (destino: { sistemaId: string; bloqueId: string; filaId?: string }) =>
    irAlBloque(destino.sistemaId, destino.bloqueId, { filaId: destino.filaId });

  return {
    ejecutar,
    irAlObjeto,
    descartar,
    ver,
    crearDemo,
    crearDemoCargado,
  };
}
