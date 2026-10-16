import type { PiezaPresente } from '@/hooks/negocio/useMiNegocio';
import { PIEZAS, sinTildes } from '@/lib/catalogo/piezas';
import type { PiezaId, RespuestaId, RubroId } from '@/types/negocio';

// Orden en que Inicio elige las respuestas que muestra (las que más importan, primero).
export const PRIORIDAD: RespuestaId[] = [
  'te-quedo',
  'vendiste',
  'gastaste',
  'margen-producto',
  'quien-te-debe',
  'pedidos-pendientes',
  'que-se-termina',
  'producto-perdida',
  'costo-plato',
];

export const prioridadDe = (id: RespuestaId) => {
  const indice = PRIORIDAD.indexOf(id);
  return indice === -1 ? PRIORIDAD.length : indice;
};

/** Rubro del sistema según cómo se llaman sus tablas («Platos» es gastronomía). */
export function rubroDelSistema(presentes: Map<PiezaId, PiezaPresente>): RubroId {
  for (const [id, presente] of presentes) {
    const definicion = PIEZAS.find((pieza) => pieza.id === id);
    for (const [rubro, nombre] of Object.entries(definicion?.nombrePorRubro ?? {})) {
      if (nombre && sinTildes(nombre) === sinTildes(presente.bloque.nombre)) {
        return rubro as RubroId;
      }
    }
  }
  return 'comercio';
}

/** «Gastos», «Gastos y Productos», «Gastos, Productos e Insumos». */
export function unir(nombres: string[]): string {
  if (nombres.length <= 1) return nombres.join('');
  return `${nombres.slice(0, -1).join(', ')} y ${nombres[nombres.length - 1]}`;
}

/** Número de un valor ya formateado («$ 12.000», «3»): sirve para el largo de las barras. */
export function numeroDe(valor: string): number | null {
  const limpio = valor.replace(/[^\d,]/g, '').replace(',', '.');
  if (limpio === '') return null;
  const numero = Number(limpio);
  return Number.isFinite(numero) ? numero : null;
}
