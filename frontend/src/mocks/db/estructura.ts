import type { Bloque, Proyecto } from '@/types/proyecto';
import { estado, type Fechas } from './estado';

// Bloques y campos viven dentro de `proyecto.bloques` (la misma estructura que devuelve
// GET /proyectos/{id} y que usan los registros); acá se agregan las fechas que el contrato pide.

export function proyectoDeLaSesion(id: string): Proyecto | undefined {
  const item = estado.proyectos.get(id);
  return item?.usuarioId === estado.sesionUsuarioId ? item.proyecto : undefined;
}

export function bloqueDeLaSesion(id: string): { proyecto: Proyecto; bloque: Bloque } | undefined {
  for (const { usuarioId, proyecto } of estado.proyectos.values()) {
    if (usuarioId !== estado.sesionUsuarioId) continue;
    const bloque = proyecto.bloques.find((item) => item.id === id);
    if (bloque) return { proyecto, bloque };
  }
  return undefined;
}

export function fechasDe(id: string, porDefecto: string): Fechas {
  return estado.fechas.get(id) ?? { creadoEn: porDefecto, actualizadoEn: porDefecto };
}

export function registrarAlta(id: string) {
  const ahora = new Date().toISOString();
  estado.fechas.set(id, { creadoEn: ahora, actualizadoEn: ahora });
}

export function registrarCambio(id: string, porDefecto: string) {
  estado.fechas.set(id, { ...fechasDe(id, porDefecto), actualizadoEn: new Date().toISOString() });
}

export const siguientePosicion = (items: readonly { posicion: number }[]) =>
  items.reduce((maxima, item) => Math.max(maxima, item.posicion + 1), 0);
