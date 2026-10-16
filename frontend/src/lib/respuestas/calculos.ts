import { pesos, porcentaje, plural } from '@/lib/formato';
import type { ResultadoRespuesta } from '@/types/negocio';

// Cálculos puros de las respuestas. Reciben números ya traídos por los hooks (reportes y filas) y
// devuelven lo que se muestra; así se prueban sin React ni red.

const dosDigitos = (n: number) => String(n).padStart(2, '0');
const aFecha = (d: Date) =>
  `${d.getFullYear()}-${dosDigitos(d.getMonth() + 1)}-${dosDigitos(d.getDate())}`;

/** Del 1.º del mes a hoy, y el mes anterior completo (fechas AAAA-MM-DD, las del filtro de reportes). */
export function periodosDelMes(hoy: Date = new Date()) {
  const inicio = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
  const inicioAnterior = new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1);
  const finAnterior = new Date(hoy.getFullYear(), hoy.getMonth(), 0);
  return {
    actual: { desde: aFecha(inicio), hasta: aFecha(hoy) },
    anterior: { desde: aFecha(inicioAnterior), hasta: aFecha(finAnterior) },
  };
}

function variacion(actual: number, anterior: number): number | null {
  if (anterior === 0) return null;
  return ((actual - anterior) / Math.abs(anterior)) * 100;
}

function tendencia(actual: number, anterior: number, subirEsBueno: boolean) {
  const cambio = variacion(actual, anterior);
  if (cambio === null || Math.abs(cambio) < 0.5) return undefined;
  return { porcentaje: cambio, mejor: subirEsBueno ? cambio > 0 : cambio < 0 };
}

const SIN_DATOS: ResultadoRespuesta = { estado: 'sin-datos' };

type Monto = { total: number; cantidad: number };

export function respuestaTotal(
  actual: Monto,
  anterior: Monto,
  opciones: { esGasto: boolean },
): ResultadoRespuesta {
  if (actual.cantidad === 0 && anterior.cantidad === 0) return SIN_DATOS;
  return {
    estado: 'lista',
    titular: pesos(actual.total),
    detalle:
      actual.cantidad === 0
        ? 'Todavía no cargaste nada este mes.'
        : `${plural(actual.cantidad, opciones.esGasto ? 'gasto' : 'venta', opciones.esGasto ? 'gastos' : 'ventas')} este mes.`,
    tendencia: tendencia(actual.total, anterior.total, !opciones.esGasto),
  };
}

export function respuestaTeQuedo(
  ventas: Monto,
  gastos: Monto,
  ventasAnt: Monto,
  gastosAnt: Monto,
): ResultadoRespuesta {
  if (ventas.cantidad === 0 && gastos.cantidad === 0) return SIN_DATOS;
  const quedo = ventas.total - gastos.total;
  const quedoAnt = ventasAnt.total - gastosAnt.total;
  return {
    estado: 'lista',
    titular: pesos(quedo),
    detalle:
      quedo >= 0
        ? `Vendiste ${pesos(ventas.total)} y gastaste ${pesos(gastos.total)}.`
        : `Gastaste ${pesos(gastos.total)} y vendiste ${pesos(ventas.total)}: este mes vas en rojo.`,
    tendencia: tendencia(quedo, quedoAnt, true),
  };
}

export type FilaProducto = {
  id: string;
  nombre: string;
  costo: number | null;
  precio: number | null;
  stock: number | null;
};

export const margenDe = (costo: number, precio: number) =>
  costo > 0 ? ((precio - costo) / costo) * 100 : 0;

export function respuestaMargen(productos: FilaProducto[]): ResultadoRespuesta {
  const conDatos = productos.filter(
    (p): p is FilaProducto & { costo: number; precio: number } =>
      p.costo !== null && p.precio !== null && p.costo > 0,
  );
  if (conDatos.length === 0) return SIN_DATOS;
  const ordenados = [...conDatos].sort(
    (a, b) => b.precio - b.costo - (a.precio - a.costo) || a.nombre.localeCompare(b.nombre),
  );
  const mejor = ordenados[0];
  if (!mejor) return SIN_DATOS;
  return {
    estado: 'lista',
    titular: mejor.nombre,
    detalle: `Te deja ${pesos(mejor.precio - mejor.costo)} por unidad (${porcentaje(margenDe(mejor.costo, mejor.precio))} sobre lo que te cuesta).`,
    items: ordenados.slice(0, 5).map((p) => ({
      nombre: p.nombre,
      valor: pesos(p.precio - p.costo),
      nota: `${porcentaje(margenDe(p.costo, p.precio))} de ganancia`,
    })),
  };
}

export function respuestaPerdida(productos: FilaProducto[]): ResultadoRespuesta {
  const conDatos = productos.filter(
    (p): p is FilaProducto & { costo: number; precio: number } =>
      p.costo !== null && p.precio !== null,
  );
  if (conDatos.length === 0) return SIN_DATOS;
  const perdida = conDatos.filter((p) => p.precio < p.costo);
  if (perdida.length === 0) {
    return {
      estado: 'lista',
      titular: 'Ninguno',
      detalle: 'Todo lo que vendés está por encima de lo que te cuesta.',
    };
  }
  return {
    estado: 'lista',
    titular: plural(perdida.length, 'producto', 'productos'),
    detalle: 'Los cobrás por menos de lo que te cuestan.',
    items: perdida.map((p) => ({
      nombre: p.nombre,
      valor: pesos(p.precio - p.costo),
      nota: `Cuesta ${pesos(p.costo)} y lo vendés a ${pesos(p.precio)}`,
    })),
  };
}

export function respuestaStock(productos: FilaProducto[], umbral = 5): ResultadoRespuesta {
  const conStock = productos.filter((p): p is FilaProducto & { stock: number } => p.stock !== null);
  if (conStock.length === 0) return SIN_DATOS;
  const pocos = conStock.filter((p) => p.stock <= umbral).sort((a, b) => a.stock - b.stock);
  if (pocos.length === 0) {
    return { estado: 'lista', titular: 'Todo en orden', detalle: `Nada con ${umbral} o menos.` };
  }
  return {
    estado: 'lista',
    titular: plural(pocos.length, 'producto', 'productos'),
    detalle: `Quedan ${umbral} o menos de cada uno.`,
    items: pocos.slice(0, 5).map((p) => ({ nombre: p.nombre, valor: `${p.stock}` })),
  };
}

export type FilaDeuda = { cliente: string; monto: number };

export function respuestaDeuda(filas: FilaDeuda[], cantidadTotal: number): ResultadoRespuesta {
  if (cantidadTotal === 0) return SIN_DATOS;
  const total = filas.reduce((suma, fila) => suma + fila.monto, 0);
  const porCliente = new Map<string, number>();
  for (const fila of filas) {
    porCliente.set(fila.cliente, (porCliente.get(fila.cliente) ?? 0) + fila.monto);
  }
  const items = [...porCliente]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([cliente, monto]) => ({ nombre: cliente, valor: pesos(monto) }));
  return {
    estado: 'lista',
    titular: pesos(total),
    detalle:
      porCliente.size === 0
        ? 'Nadie te debe nada.'
        : `Te deben ${plural(porCliente.size, 'cliente', 'clientes')}.`,
    items,
  };
}

export type FilaPedido = { cliente: string; monto: number; sena: number };

export function respuestaPedidos(filas: FilaPedido[]): ResultadoRespuesta {
  if (filas.length === 0)
    return { estado: 'lista', titular: 'Ninguno', detalle: 'No tenés pedidos por entregar.' };
  const falta = filas.reduce((suma, fila) => suma + Math.max(0, fila.monto - fila.sena), 0);
  return {
    estado: 'lista',
    titular: plural(filas.length, 'pedido', 'pedidos'),
    detalle: `Te faltan cobrar ${pesos(falta)} (ya te dieron ${pesos(filas.reduce((s, f) => s + f.sena, 0))} de seña).`,
    items: filas.slice(0, 5).map((fila) => ({
      nombre: fila.cliente,
      valor: pesos(fila.monto),
      nota: fila.sena > 0 ? `Seña ${pesos(fila.sena)}` : undefined,
    })),
  };
}

export type FilaCosto = { nombre: string; costo: number; precio: number | null };

export function respuestaCostoPlato(filas: FilaCosto[]): ResultadoRespuesta {
  if (filas.length === 0) return SIN_DATOS;
  const ordenadas = [...filas].sort((a, b) => b.costo - a.costo);
  const primero = ordenadas[0];
  if (!primero) return SIN_DATOS;
  return {
    estado: 'lista',
    titular: pesos(primero.costo),
    detalle: `Lo que más te cuesta es ${primero.nombre}.`,
    items: ordenadas.slice(0, 5).map((fila) => ({
      nombre: fila.nombre,
      valor: pesos(fila.costo),
      nota: fila.precio !== null ? `Lo vendés a ${pesos(fila.precio)}` : undefined,
    })),
  };
}
