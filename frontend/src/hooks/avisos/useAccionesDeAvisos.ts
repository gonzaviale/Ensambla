import type { AccionDeAviso, Aviso } from '@/lib/sistema/alertas';
import { findBlock, id as nuevoId } from '@/lib/sistema/core';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { useAplicarAviso, useDescartarAviso } from '@/hooks/avisos/mutations';
import { useGuardarPreferencias } from '@/hooks/preferencias/mutations';
import { usePreferencias } from '@/hooks/preferencias/queries';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useSistemaStore } from '@/stores/sistemaStore';

const MAXIMO_DE_DESCARTADOS = 200;

// Lo que se puede hacer con un aviso: aplicarlo, abrirlo, descartarlo o apagar un grupo.
export function useAccionesDeAvisos() {
  const preferencias = usePreferencias();
  const guardarPreferencias = useGuardarPreferencias();
  const aplicarEnServidor = useAplicarAviso();
  const descartarEnServidor = useDescartarAviso();
  const navegacion = useNavegacion();

  // Abre el sistema, va a la sección del bloque, lo resalta y, si hace falta, abre el formulario.
  const irAlBloque = async (
    sistemaId: string,
    bloqueId: string,
    {
      filaId = null,
      nuevoRegistro = false,
    }: { filaId?: string | null; nuevoRegistro?: boolean } = {},
  ) => {
    await navegacion.abrirSistema(sistemaId, 'use');
    const store = useSistemaStore.getState();
    const sistema = store.sistemas.find((item) => item.id === sistemaId);
    const seccion = sistema?.sections.find((item) =>
      item.blocks.some((bloque) => bloque.id === bloqueId),
    );
    if (seccion && seccion.id !== store.sectionId) store.elegirSeccion(seccion.id);
    setTimeout(() => {
      const nodo = document.querySelector(`[data-block="${bloqueId}"]`);
      nodo?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      nodo?.classList.add('destacado');
      setTimeout(() => nodo?.classList.remove('destacado'), 1800);
      if (filaId || nuevoRegistro) store.abrirModal({ tipo: 'registro', bloqueId, filaId });
    }, 80);
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
        void irAlBloque(accion.sistemaId, accion.bloqueId, { nuevoRegistro: true });
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

  const crearDemo = () => {
    void navegacion.crearYAbrir(crearAlmacenDeEjemplo(hoy()), 'use');
    useSistemaStore.getState().avisar('Creamos un almacén de ejemplo. Sami ya lo está mirando.');
  };

  return {
    ejecutar,
    descartar,
    crearDemo,
  };
}
