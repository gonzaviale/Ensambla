import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { PIEZAS, piezaDeNombre, sinTildes } from '@/lib/catalogo/piezas';
import type { CampoPieza, Pieza, PiezaId, RolCampo } from '@/types/negocio';
import type { Bloque, Campo, Proyecto } from '@/types/proyecto';
import { proyectoQuery, proyectosQuery } from '../proyectos/queries';

// «Mi negocio» es el sistema (proyecto) más reciente de la persona. Cada tabla (bloque) que coincide
// con una pieza del catálogo —por nombre— es una pieza presente; las respuestas leen sus columnas por rol.

export type PiezaPresente = {
  pieza: Pieza;
  bloque: Bloque;
  /** Columna de la tabla para un rol (por el nombre que define la pieza). */
  campo: (rol: RolCampo) => Campo | undefined;
};

export function campoDeRol(bloque: Bloque, pieza: Pieza, rol: RolCampo): Campo | undefined {
  const definicion: CampoPieza | undefined = pieza.campos.find((c) => c.rol === rol);
  if (!definicion) return undefined;
  const buscado = sinTildes(definicion.nombre);
  return bloque.campos.find((campo) => sinTildes(campo.nombre) === buscado);
}

/** Qué piezas del catálogo tiene un sistema (por el nombre de sus tablas). */
export function ubicarPiezas(sistema: Proyecto | undefined): Map<PiezaId, PiezaPresente> {
  const presentes = new Map<PiezaId, PiezaPresente>();
  if (!sistema) return presentes;
  for (const bloque of sistema.bloques) {
    const pieza = piezaDeNombre(bloque.nombre);
    if (!pieza || presentes.has(pieza.id)) continue;
    presentes.set(pieza.id, {
      pieza,
      bloque,
      campo: (rol) => campoDeRol(bloque, pieza, rol),
    });
  }
  return presentes;
}

/** Piezas del catálogo que todavía no están en el sistema. */
export function piezasQueFaltan(presentes: Map<PiezaId, PiezaPresente>): Pieza[] {
  return PIEZAS.filter((pieza) => !presentes.has(pieza.id));
}

export function useMiNegocio() {
  const lista = useQuery(proyectosQuery());
  const masReciente = useMemo(
    () => [...(lista.data ?? [])].sort((a, b) => b.actualizadoEn.localeCompare(a.actualizadoEn))[0],
    [lista.data],
  );
  const detalle = useQuery({
    ...proyectoQuery(masReciente?.id ?? ''),
    enabled: Boolean(masReciente),
  });
  const sistema = detalle.data;
  const presentes = useMemo(() => ubicarPiezas(sistema), [sistema]);

  return {
    /** Se está pidiendo la lista de sistemas o el sistema elegido. */
    cargando: lista.isPending || (Boolean(masReciente) && detalle.isPending),
    error: lista.isError || detalle.isError,
    /** El sistema más reciente (undefined si todavía no armó ninguno). */
    sistema,
    sistemaId: masReciente?.id,
    /** Hay al menos un sistema: si no, hay que pasar por la bienvenida. */
    hayNegocio: Boolean(masReciente),
    presentes,
    piezasPresentes: [...presentes.keys()],
  };
}
