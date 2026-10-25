import { guardarSistema } from '@/api/sistemas';
import { ApiError } from '@/api/apiError';
import { useNavigate } from '@tanstack/react-router';
import { useUsuario } from '@/hooks/auth/queries';
import { usePreferencias } from '@/hooks/preferencias/queries';
import type { SeccionDeCuenta } from '@/lib/cuenta';
import type { Sistema } from '@/lib/sistema/tipos';
import { useSistemaStore, type ModoDelEditor } from '@/stores/sistemaStore';

export const MODO_EN_LA_URL = { build: 'construir', use: 'usar' } as const;

/** Cómo se llama la persona en pantalla: el que eligió en el perfil o, si no, el de la cuenta. */
export function useNombre() {
  const usuario = useUsuario();
  return usePreferencias().nombre ?? usuario.nombre;
}

export const saludoDeSami = (nombre: string) =>
  `Hola, ${nombre}. Contame qué hacés y cómo te organizás hoy. Yo pienso una base y vos me decís si te sirve.`;

/** Cómo arranca Sami cuando se abre el taller sobre un sistema que ya existe. */
export const saludoParaAjustar = (nombreDelSistema: string) =>
  `Estoy mirando «${nombreDelSistema}». ¿Qué querés cambiar? Te muestro cómo queda antes de guardar.`;

// Los movimientos entre pantallas del mockup (navigate(), openSystem(), saveNew()) hechos con el router.
// Cada uno cierra el modal abierto y deja sin selección, como navigate() del original.
export function useNavegacion() {
  const navigate = useNavigate();
  const nombre = useNombre();

  const limpiar = () => {
    const estado = useSistemaStore.getState();
    estado.cerrarModal();
    estado.seleccionar(null);
    window.scrollTo({ top: 0, behavior: 'instant' });
  };

  const irA = (to: '/inicio' | '/plantillas' | '/taller' | '/zona' | '/planes' | '/sami') => {
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
    /** «Armémoslo con Sami»: cada vez empieza una conversación nueva. */
    irATaller: () => {
      useSistemaStore.getState().iniciarTaller(saludoDeSami(nombre));
      return irA('/taller');
    },
    /** «Ajustar con Sami»: una conversación nueva sobre una copia del sistema; nada cambia hasta guardar. */
    ajustarConSami: (id: string) => {
      const estado = useSistemaStore.getState();
      const sistema = estado.sistemas.find((item) => item.id === id);
      if (sistema) estado.iniciarTaller(saludoParaAjustar(sistema.name), sistema);
      limpiar();
      return navigate({ to: '/taller', search: { sistema: id } });
    },
    irAZona: () => irA('/zona'),
    /** El avatar: la configuración de la cuenta, en la sección que se pida (el perfil, si no). */
    irACuenta: (seccion?: SeccionDeCuenta) => {
      limpiar();
      return navigate({ to: '/cuenta', search: { seccion } });
    },
    irAPlanes: () => irA('/planes'),
    /** «Armalo con Sami»: la conversación que crea un proyecto real en el backend. */
    irASami: () => irA('/sami'),
    abrirSistema,
    /** saveNew() del mockup: guarda el sistema nuevo y lo abre. */
    crearYAbrir: async (sistema: Sistema, modo: ModoDelEditor = 'use') => {
      try {
        const guardado = await guardarSistema(sistema);
        useSistemaStore.getState().agregarSistema(guardado);
        useSistemaStore.getState().marcarGuardados([guardado.id], []);
        await abrirSistema(guardado.id, modo, true);
      } catch (error) {
        useSistemaStore
          .getState()
          .avisar(
            error instanceof ApiError
              ? `${error.message}${error.status === 403 && error.codigo ? ' Revisá los beneficios de Premium en Planes.' : ''}`
              : 'No pudimos crear tu sistema. Tus datos todavía no se guardaron. Probá de nuevo.',
          );
      }
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
