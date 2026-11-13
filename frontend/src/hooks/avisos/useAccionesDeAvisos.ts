import type { AccionDeAviso, Aviso } from '@/lib/sistema/alertas';
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

const MAXIMO_DE_DESCARTADOS = 200;
/** Lo que se espera a que la tabla dibuje el registro pedido antes de marcarlo. */
const ESPERA_DE_LA_TANDA = 60;

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

  const ejecutar = (accion: AccionDeAviso) => {
    const store = useSistemaStore.getState();
    switch (accion.tipo) {
      case 'aplicar':
        store.actualizarSistema(accion.sistemaId, (sistema) => {
          const fila = findBlock(sistema, accion.bloqueId)?.rows.find(
            (item) => item.id === accion.filaId,
          );
          if (fila) Object.assign(fila, accion.valores);
        });
        store.avisar(accion.confirmacion);
        break;
      case 'crear':
        store.actualizarSistema(accion.sistemaId, (sistema) => {
          findBlock(sistema, accion.bloqueId)?.rows.push({
            id: nuevoId(),
            ...accion.valores,
            __source: 'Aviso de Sami',
          });
        });
        store.avisar(accion.confirmacion);
        break;
      case 'registrar':
        store.ocultarAvisoNuevo();
        void irAlBloque(accion.sistemaId, accion.bloqueId, {
          nuevoRegistro: true,
          avisoId: accion.avisoId,
        });
        break;
      case 'abrir':
        store.ocultarAvisoNuevo();
        void irAlBloque(accion.sistemaId, accion.bloqueId, { filaId: accion.filaId });
        break;
      case 'zona':
        store.ocultarAvisoNuevo();
        void navegacion.irAZona();
        break;
      case 'servidor':
        aplicarEnServidor.mutate(accion.avisoId, {
          onSuccess: () => store.avisar(accion.confirmacion),
          onError: () => store.avisar('No pudimos aplicar el cambio. Probá de nuevo.'),
        });
        break;
    }
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
