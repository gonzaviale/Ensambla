import { PIEZAS } from '@/lib/catalogo/piezas';
import type { RubroId } from '@/types/negocio';
import type { Bloque } from '@/types/proyecto';

/** Rubro de un sistema según cómo se llaman sus tablas («Platos» → gastronomía); por defecto, comercio. */
export function rubroDelSistema(bloques: readonly Bloque[]): RubroId {
  for (const bloque of bloques) {
    for (const pieza of PIEZAS) {
      for (const [rubro, nombre] of Object.entries(pieza.nombrePorRubro)) {
        if (nombre === bloque.nombre) return rubro as RubroId;
      }
    }
  }
  return 'comercio';
}
