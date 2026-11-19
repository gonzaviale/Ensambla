import type { EstadoDeUbicacion } from '@/hooks/ubicacion/useConfiguracionUbicacion';
import type { SeccionUbicacion } from '@/types/ubicacion';

export const SOLAPAS: Record<
  SeccionUbicacion,
  { nombre: string; guardar: string; guardado: string }
> = {
  comercio: {
    nombre: 'Tu comercio',
    guardar: 'Guardar datos del comercio',
    guardado: 'Guardamos los datos de tu comercio.',
  },
  ubicacion: {
    nombre: 'Ubicación',
    guardar: 'Guardar ubicación',
    guardado: 'Guardamos la ubicación de tu comercio.',
  },
  rubro: {
    nombre: 'Rubro',
    guardar: 'Guardar rubro',
    guardado: 'Guardamos el rubro de tu comercio.',
  },
};

/** Cuándo una solapa todavía no se puede guardar. */
export const NO_SE_PUEDE_GUARDAR: Record<SeccionUbicacion, (estado: EstadoDeUbicacion) => boolean> =
  {
    comercio: () => false,
    ubicacion: (estado) => estado.buscando || estado.localidadBloquea,
    rubro: (estado) => estado.cargandoRubros || !!estado.errorRubros,
  };

/** Lo que se suma al aviso de «guardado»: la ubicación cuenta a qué zona quedó asignada. */
export const AGREGADO_AL_GUARDADO: Record<SeccionUbicacion, (estado: EstadoDeUbicacion) => string> =
  {
    comercio: () => '',
    rubro: () => '',
    ubicacion: (estado) =>
      estado.zona ? ` Tu zona: ${estado.zona}.` : ' Todavía no hay una zona que la cubra.',
  };
