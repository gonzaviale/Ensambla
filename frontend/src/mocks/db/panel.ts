import { allBlocks } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import type { PanelDelNegocio, SemanaDeCaja } from '@/types/panel';

// El panel de la home calculado con las mismas reglas que el servidor (ver docs/panel-negocio.md): las ventas
// fiadas no son una entrada, la mercadería es lo comprado (tabla de entradas) al costo actual del producto, y
// sin ventas en el período no hay ni métricas ni semanas.

const DIA_MS = 86_400_000;
const DIAS_POR_SEMANA = 7;

const numero = (valor: unknown) =>
  typeof valor === 'number' && Number.isFinite(valor) ? valor : null;
const positivo = (valor: unknown) => {
  const dato = numero(valor);
  return dato !== null && dato > 0 ? dato : null;
};
const dia = (texto: string) => Math.floor(Date.parse(`${texto}T12:00:00Z`) / DIA_MS);
const aTexto = (diaDelCalendario: number) =>
  new Date(diaDelCalendario * DIA_MS + DIA_MS / 2).toISOString().slice(0, 10);
const redondear = (valor: number, decimales: number) => {
  const factor = 10 ** decimales;
  return Math.round(valor * factor) / factor;
};
const suma = (valores: number[]) => valores.reduce((total, valor) => total + valor, 0);

type Movimiento = { dia: number; importe: number };

export function panelDe(sistema: Sistema, hoy: string, dias: number): PanelDelNegocio {
  const hasta = dia(hoy);
  const desde = hasta - (dias - 1);
  const enElPeriodo = (fecha: unknown, inicio: number, fin: number) =>
    typeof fecha === 'string' && dia(fecha) >= inicio && dia(fecha) <= fin;

  const bloques = allBlocks(sistema);
  const productos = new Map(
    bloques
      .filter((bloque) => bloque.type === 'inventory')
      .flatMap((bloque) => bloque.rows)
      .map((fila) => [fila.id, positivo(fila.cost)]),
  );
  const ventas = bloques
    .filter((bloque) => bloque.type === 'sales')
    .flatMap((bloque) => bloque.rows)
    .filter(
      (fila) =>
        !fila.__example && positivo(fila.quantity) !== null && typeof fila.date === 'string',
    )
    .map((fila) => ({
      dia: dia(String(fila.date)),
      producto: String(fila.product ?? ''),
      cantidad: positivo(fila.quantity) ?? 0,
      precio: positivo(fila.price),
      fiado: fila.credit === true,
    }));
  const importe = (venta: (typeof ventas)[number]) => (venta.precio ?? 0) * venta.cantidad;

  const delPeriodo = ventas.filter((venta) => venta.dia >= desde && venta.dia <= hasta);
  if (delPeriodo.length === 0)
    return {
      desde: aTexto(desde),
      hasta: aTexto(hasta),
      dias,
      hayDatos: false,
      metricas: null,
      flujoDeCaja: [],
    };

  const vendiste = suma(delPeriodo.map(importe));
  const anterior = suma(
    ventas.filter((venta) => venta.dia >= desde - dias && venta.dia < desde).map(importe),
  );
  const ganancias = delPeriodo
    .filter((venta) => venta.precio !== null && productos.get(venta.producto))
    .map((venta) => ((venta.precio ?? 0) - (productos.get(venta.producto) ?? 0)) * venta.cantidad);

  const cobradas: Movimiento[] = delPeriodo
    .filter((venta) => !venta.fiado && venta.precio !== null)
    .map((venta) => ({ dia: venta.dia, importe: importe(venta) }));
  const bloquesDeGastos = bloques.filter((bloque) => bloque.type === 'expenses');
  const gastos: Movimiento[] = bloquesDeGastos
    .flatMap((bloque) => bloque.rows)
    .filter(
      (fila) =>
        !fila.__example && numero(fila.total) !== null && enElPeriodo(fila.date, desde, hasta),
    )
    .map((fila) => ({ dia: dia(String(fila.date)), importe: numero(fila.total) ?? 0 }));
  const bloquesDeEntradas = bloques.filter((bloque) => bloque.type === 'entries');
  const mercaderia: Movimiento[] = bloquesDeEntradas
    .flatMap((bloque) => bloque.rows)
    .filter(
      (fila) =>
        !fila.__example &&
        productos.get(String(fila.product ?? '')) &&
        positivo(fila.quantity) !== null &&
        enElPeriodo(fila.date, desde, hasta),
    )
    .map((fila) => ({
      dia: dia(String(fila.date)),
      importe: (positivo(fila.quantity) ?? 0) * (productos.get(String(fila.product)) ?? 0),
    }));

  const flujoDeCaja: SemanaDeCaja[] = [];
  let acumulado = 0;
  for (let inicio = desde; inicio <= hasta; inicio += DIAS_POR_SEMANA) {
    const fin = Math.min(inicio + DIAS_POR_SEMANA - 1, hasta);
    const en = (movimientos: Movimiento[]) =>
      suma(
        movimientos
          .filter((movimiento) => movimiento.dia >= inicio && movimiento.dia <= fin)
          .map((m) => m.importe),
      );
    const entradas = en(cobradas);
    const salidas = en(gastos) + en(mercaderia);
    acumulado += entradas - salidas;
    flujoDeCaja.push({
      desde: aTexto(inicio),
      hasta: aTexto(fin),
      entradas,
      salidas,
      saldo: entradas - salidas,
      saldoAcumulado: acumulado,
    });
  }

  return {
    desde: aTexto(desde),
    hasta: aTexto(hasta),
    dias,
    hayDatos: true,
    metricas: {
      vendiste,
      mercaderia: bloquesDeEntradas.length === 0 ? null : suma(mercaderia.map((m) => m.importe)),
      gastos: bloquesDeGastos.some((bloque) => bloque.fields.some((campo) => campo.key === 'total'))
        ? suma(gastos.map((m) => m.importe))
        : null,
      queda: acumulado,
      teDeben: suma(delPeriodo.filter((venta) => venta.fiado).map(importe)),
      margen: ganancias.length === 0 ? null : redondear((suma(ganancias) * 100) / vendiste, 1),
      ticketPromedio: redondear(vendiste / delPeriodo.length, 2),
      variacion: anterior > 0 ? Math.round(((vendiste - anterior) * 100) / anterior) : null,
    },
    flujoDeCaja,
  };
}
