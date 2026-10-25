import { allBlocks } from './core';
import type { Fila, Sistema } from './tipos';
import type { NombreDeIcono } from './iconos';

export type MetricaInicio = {
  clave: string;
  titulo: string;
  valor: number | null;
  detalle: string;
  objetivo: string;
  direccion: 'minimo' | 'maximo';
  moneda?: boolean;
  icono: NombreDeIcono;
};
const numero = (valor: unknown): number | null => {
  if (typeof valor !== 'number' && typeof valor !== 'string') return null;
  if (String(valor).trim() === '') return null;
  const n = Number(valor);
  return Number.isFinite(n) && n >= 0 ? n : null;
};
const propias = (filas: Fila[]) => filas.filter((fila) => !fila.__example);

export function metricasInicio(sistema: Sistema): MetricaInicio[] {
  const bloques = allBlocks(sistema).map((bloque) =>
    sistema.demoAlmacen ? { ...bloque, rows: [] } : bloque,
  );
  const metricas: MetricaInicio[] = [];
  const ventas = bloques.filter((b) => b.type === 'sales');
  if (ventas.length) {
    let importe = 0;
    let validas = 0;
    let incompletas = 0;
    for (const bloque of ventas)
      for (const fila of propias(bloque.rows)) {
        const cantidad = numero(fila.quantity);
        const precio = numero(fila.price);
        const total = cantidad !== null && precio !== null ? cantidad * precio : NaN;
        if (Number.isFinite(total)) {
          importe += total;
          validas++;
        } else incompletas++;
      }
    metricas.push({
      clave: 'ventas',
      titulo: 'Ventas registradas',
      valor: (!validas && incompletas) || !Number.isFinite(importe) ? null : importe,
      detalle: `Acumulado de Ventas: cantidad × precio. No representa ganancia ni cobros.${incompletas ? ` Hay ${incompletas} filas sin cantidad o precio válidos.` : ''}`,
      objetivo: 'Alcanzar un importe de ventas registradas',
      direccion: 'minimo',
      moneda: true,
      icono: 'wallet',
    });
  }
  const pedidos = bloques.filter((b) => b.type === 'orders');
  if (pedidos.length)
    metricas.push({
      clave: 'pendientes',
      titulo: 'Pedidos pendientes',
      valor: pedidos
        .flatMap((b) => propias(b.rows))
        .filter(
          (f) =>
            !['entregado', 'cancelado', 'completado'].includes(
              String(f.status ?? '')
                .trim()
                .toLowerCase(),
            ),
        ).length,
      detalle: 'Pedidos que todavía no están entregados, completados ni cancelados. Estado actual.',
      objetivo: 'Bajar los pedidos pendientes a un máximo',
      direccion: 'maximo',
      icono: 'orders',
    });
  const inventarios = bloques.filter((b) => b.type === 'inventory');
  if (inventarios.length)
    metricas.push({
      clave: 'stock-bajo',
      titulo: 'Productos con stock bajo',
      valor: inventarios
        .flatMap((b) => propias(b.rows))
        .filter((f) => {
          const cantidad = numero(f.quantity);
          const minimo = numero(f.minimum);
          return cantidad !== null && minimo !== null && cantidad <= minimo;
        }).length,
      detalle:
        'Inventario: productos con cantidad igual o menor al stock mínimo definido. Sin mínimo no se evalúan.',
      objetivo: 'Bajar los productos con stock bajo a un máximo',
      direccion: 'maximo',
      icono: 'box',
    });
  const clientes = bloques.filter((b) => b.type === 'clients');
  if (clientes.length)
    metricas.push({
      clave: 'clientes',
      titulo: 'Clientes registrados',
      valor: clientes.reduce((n, b) => n + propias(b.rows).length, 0),
      detalle:
        'Filas propias de tus tablas de Clientes. Acumulado registrado, sin deduplicar contactos.',
      objetivo: 'Alcanzar una cantidad de clientes registrados',
      direccion: 'minimo',
      icono: 'users',
    });
  metricas.push({
    clave: 'filas',
    titulo: 'Filas cargadas',
    valor: bloques
      .filter((b) => b.type !== 'summary' && b.type !== 'note')
      .reduce((n, b) => n + propias(b.rows).length, 0),
    detalle:
      'Datos propios de las tablas de este sistema. No incluye filas de ejemplo, notas ni resúmenes.',
    objetivo: 'Alcanzar una cantidad de filas cargadas',
    direccion: 'minimo',
    icono: 'grid',
  });
  return metricas;
}

export function validarMeta(texto: string, moneda = false): number | null {
  if (!texto.trim()) return null;
  const n = Number(texto);
  if (!Number.isFinite(n) || n < 0 || (!moneda && !Number.isSafeInteger(n))) return null;
  return n;
}
