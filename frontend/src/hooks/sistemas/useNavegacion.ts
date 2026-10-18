import { useNavigate } from '@tanstack/react-router';
import { useUsuario } from '@/hooks/auth/queries';
import { usePreferencias } from '@/hooks/preferencias/queries';
import type { Sistema } from '@/lib/sistema/tipos';
import { useSistemaStore, type ModoDelEditor } from '@/stores/sistemaStore';

export const MODO_EN_LA_URL = { build: 'construir', use: 'usar' } as const;

/** Cómo se llama la persona en pantalla: el que eligió en el perfil o, si no, el de la cuenta. */
export function useNombre() {
  const usuario = useUsuario();
  return usePreferencias().nombre ?? usuario.nombre;
}

// Los movimientos entre pantallas del mockup (navigate(), openSystem(), saveNew()) hechos con el router.
// Cada uno cierra el modal abierto y deja sin selección, como navigate() del original.
export function useNavegacion() {
  const navigate = useNavigate();

  const limpiar = () => {
    const estado = useSistemaStore.getState();
    estado.cerrarModal();
    estado.seleccionar(null);
    window.scrollTo({ top: 0, behavior: 'instant' });
  };

  const irA = (to: '/inicio' | '/plantillas' | '/taller' | '/zona') => {
    limpiar();
    return navigate({ to });
  };

  const abrirSistema = (id: string, modo: ModoDelEditor = 'use', esNuevo = false) => {
    limpiar();
    useSistemaStore.getState().abrirSistema(id, modo, esNuevo);
    return navigate({
      to: '/sistemas/$sistemaId',
      params: { sistemaId: id },
      search: { modo: MODO_EN_LA_URL[modo] },
    });
  };

  return {
    irAInicio: () => irA('/inicio'),
    irAPlantillas: () => irA('/plantillas'),
    irATaller: () => irA('/taller'),
    irAZona: () => irA('/zona'),
    abrirSistema,
    /** saveNew() del mockup: guarda el sistema nuevo y lo abre. */
    crearYAbrir: (sistema: Sistema, modo: ModoDelEditor = 'use') => {
      useSistemaStore.getState().agregarSistema(sistema);
      return abrirSistema(sistema.id, modo, true);
    },
    /** «Mis sistemas»: vuelve al inicio y baja a la lista, o explica por dónde empezar. */
    irAMisSistemas: async () => {
      await irA('/inicio');
      const estado = useSistemaStore.getState();
      if (estado.sistemas.length) {
        document
          .getElementById('my-systems')
          ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      } else {
        estado.avisar('Tu primer sistema puede empezar con cualquiera de estos tres caminos.');
      }
    },
  };
}
