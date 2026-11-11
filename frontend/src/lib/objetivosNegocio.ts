import { allBlocks } from './sistema/core';
import type { Sistema } from './sistema/tipos';

// Cómo viene el negocio, con lo que la persona anotó en sus cuadernos: los últimos 30 días, dicho en pesos y sin
// términos contables. La pantalla usa lo que calcula el servidor (GET /objetivos/negocio); esta cuenta es la que
// replica el modo de demostración.

export type Negocio = {
  hayVentas: boolean;
  vendiste: number;
  mercaderia: number;
  gastos: number;
  /** Lo que queda de lo vendido después de pagar la mercadería y los gastos. */
  queda: number;
  /** Lo vendido «fiado» que todavía no se cobró. */
  teDeben: number;
};

const DIA_MS = 86_400_000;
const numero = (valor: unknown) => Number(valor ?? 0) || 0;
const enLosUltimos30 = (hoy: string, fecha: string) => {
  const dias = Math.floor(
    (Date.parse(`${hoy}T12:00:00`) - Date.parse(`${fecha}T12:00:00`)) / DIA_MS,
  );
  return dias >= 0 && dias < 30;
};

/** La categoría de la compra de mercadería, escrita como venga («Mercadería», «mercaderia»…). */
const esMercaderia = (categoria: unknown) =>
  String(categoria ?? '')
    .trim()
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase() === 'mercaderia';

export function negocioDe(sistemas: Sistema[], hoy: string): Negocio {
  const negocio: Negocio = {
    hayVentas: false,
    vendiste: 0,
    mercaderia: 0,
    gastos: 0,
    queda: 0,
    teDeben: 0,
  };
  for (const sistema of sistemas) {
    const bloques = allBlocks(sistema);
    const costos = new Map(
      bloques
        .filter((bloque) => bloque.type === 'inventory')
        .flatMap((bloque) => bloque.rows)
        .map((fila) => [fila.id, { costo: numero(fila.cost), precio: numero(fila.price) }]),
    );
    for (const venta of bloques
      .filter((bloque) => bloque.type === 'sales')
      .flatMap((bloque) => bloque.rows)) {
      const producto = costos.get(String(venta.product ?? ''));
      const cantidad = numero(venta.quantity);
      const importe = (numero(venta.price) || producto?.precio || 0) * cantidad;
      if (venta.credit === true) negocio.teDeben += importe;
      if (!venta.date || !enLosUltimos30(hoy, String(venta.date))) continue;
      negocio.hayVentas = true;
      negocio.vendiste += importe;
      negocio.mercaderia += (producto?.costo ?? 0) * cantidad;
    }
    // La mercadería comprada ya se descuenta al venderla: sumarla otra vez como gasto la contaría dos veces.
    for (const gasto of bloques
      .filter((bloque) => bloque.type === 'expenses')
      .flatMap((bloque) => bloque.rows)) {
      if (
        esMercaderia(gasto.category) ||
        !gasto.date ||
        !enLosUltimos30(hoy, String(gasto.date))
      )
        continue;
      negocio.gastos += numero(gasto.total);
    }
  }
  negocio.queda = negocio.vendiste - negocio.mercaderia - negocio.gastos;
  return negocio;
}
