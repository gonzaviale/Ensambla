import { allBlocks, numero } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import {
  CAMBIO_INICIAL,
  COBROS_DE_FIADO,
  GASTOS_FIJOS,
  MEDIOS_DE_COBRO,
  MERMA_SOBRE_COSTO,
  type Renglon,
} from './datosDeEjemplo';

// La ganancia de verdad del comercio. No es «ventas menos costos»: a lo vendido se le resta lo que cuesta
// cobrarlo, el costo de la mercadería que salió (no la que se compró), lo que se pierde y los gastos fijos.
// La caja va aparte: el fiado se vende pero no entra, y pagar mercadería saca plata pero no es una pérdida.

export type Producto = {
  id: string;
  nombre: string;
  stock: number;
  precio: number;
  costo: number;
  minimo: number;
};

export type Venta = {
  productoId: string;
  cantidad: number;
  precio: number;
  fecha: string;
  clienteId: string;
  fiado: boolean;
};

export type Cliente = { id: string; nombre: string; telefono: string };

export type DatosDelNegocio = {
  nombre: string;
  productos: Producto[];
  ventas: Venta[];
  clientes: Cliente[];
};

const DIA_MS = 24 * 60 * 60 * 1000;

const aFecha = (iso: string) => new Date(`${iso}T12:00:00`);

/** Días entre dos fechas «aaaa-mm-dd» (positivo si `hasta` es posterior). */
export const diasEntre = (desde: string, hasta: string) =>
  Math.round((aFecha(hasta).getTime() - aFecha(desde).getTime()) / DIA_MS);

/** El cuaderno con inventario y ventas que mira «Mi negocio»: el almacén de la demo o el primero que tenga datos. */
export function buscarNegocio(sistemas: Sistema[]): Sistema | undefined {
  const tieneDatos = (sistema: Sistema) => {
    const bloques = allBlocks(sistema);
    const conFilas = (tipo: string) =>
      bloques.some((bloque) => bloque.type === tipo && bloque.rows.some((fila) => !fila.__example));
    return conFilas('inventory') && conFilas('sales');
  };
  return (
    sistemas.find((sistema) => sistema.demoAlmacen && tieneDatos(sistema)) ??
    sistemas.find(tieneDatos)
  );
}

/** Lee del cuaderno los productos, las ventas y los clientes; las filas de ejemplo no cuentan. */
export function datosDelNegocio(sistema: Sistema): DatosDelNegocio {
  const bloques = allBlocks(sistema);
  const filasDe = (tipo: string) =>
    bloques
      .filter((bloque) => bloque.type === tipo)
      .flatMap((bloque) => bloque.rows.filter((fila) => !fila.__example));

  return {
    nombre: sistema.name,
    productos: filasDe('inventory').map((fila) => ({
      id: fila.id,
      nombre: String(fila.name ?? ''),
      stock: numero(fila.quantity),
      precio: numero(fila.price),
      costo: numero(fila.cost),
      minimo: numero(fila.minimum),
    })),
    ventas: filasDe('sales').map((fila) => ({
      productoId: String(fila.product ?? ''),
      cantidad: numero(fila.quantity),
      precio: numero(fila.price),
      fecha: String(fila.date ?? ''),
      clienteId: String(fila.customer ?? ''),
      fiado: Boolean(fila.credit),
    })),
    clientes: filasDe('clients').map((fila) => ({
      id: fila.id,
      nombre: String(fila.name ?? ''),
      telefono: String(fila.phone ?? ''),
    })),
  };
}

/** Lo que se queda quien cobra, como parte de las ventas (promedio ponderado de los medios de cobro). */
export const COMISION_PROMEDIO = MEDIOS_DE_COBRO.reduce(
  (total, medio) => total + medio.parte * medio.comision,
  0,
);

export const GASTOS_FIJOS_MENSUALES = GASTOS_FIJOS.reduce(
  (total, gasto) => total + gasto.mensual,
  0,
);

/** Las ventas de los últimos `dias` días, hoy incluido. */
export const ventasDelPeriodo = (ventas: Venta[], hoy: string, dias: number) =>
  ventas.filter((venta) => {
    const atras = diasEntre(venta.fecha, hoy);
    return atras >= 0 && atras < dias;
  });

export type Cuenta = {
  ventas: number;
  comisiones: number;
  ventasNetas: number;
  costoDeLoVendido: number;
  margenBruto: number;
  mermas: number;
  gastosFijos: number;
  ganancia: number;
  /** Lo que queda de cada peso vendido para pagar los gastos fijos. */
  contribucion: number;
  /** Ventas por mes necesarias para cubrir los gastos fijos; `null` si lo vendido no deja margen. */
  equilibrioMensual: number | null;
  /** Qué día del mes se terminan de cubrir los gastos fijos a este ritmo; `null` si no se llega. */
  diaDeEquilibrio: number | null;
};

/** La cuenta de un mes a partir de lo vendido y su costo. Es la base de la proyección. */
export function cuentaDelMes(ventas: number, costo: number, gastosFijos: number): Cuenta {
  const comisiones = ventas * COMISION_PROMEDIO;
  const mermas = costo * MERMA_SOBRE_COSTO;
  const margenBruto = ventas - costo;
  const queda = margenBruto - comisiones - mermas;
  const contribucion = ventas > 0 ? queda / ventas : 0;
  return {
    ventas,
    comisiones,
    ventasNetas: ventas - comisiones,
    costoDeLoVendido: costo,
    margenBruto,
    mermas,
    gastosFijos,
    ganancia: queda - gastosFijos,
    contribucion,
    equilibrioMensual: contribucion > 0 ? gastosFijos / contribucion : null,
    diaDeEquilibrio: queda > 0 ? Math.ceil((30 * gastosFijos) / queda) : null,
  };
}

export type Resultado = Cuenta & {
  unidades: number;
  vendidoAFiado: number;
  /** Lo que la app mostraba antes como ganancia: ventas menos el costo de lo vendido. */
  cuentaVieja: number;
};

/** El resultado de los últimos 30 días con los gastos fijos de un mes. */
export function resultadoDelMes(datos: DatosDelNegocio, hoy: string): Resultado {
  const porId = new Map(datos.productos.map((producto) => [producto.id, producto]));
  const delMes = ventasDelPeriodo(datos.ventas, hoy, 30);
  let ventas = 0;
  let costo = 0;
  let unidades = 0;
  let vendidoAFiado = 0;
  for (const venta of delMes) {
    const importe = venta.precio * venta.cantidad;
    ventas += importe;
    costo += (porId.get(venta.productoId)?.costo ?? 0) * venta.cantidad;
    unidades += venta.cantidad;
    if (venta.fiado) vendidoAFiado += importe;
  }
  const cuenta = cuentaDelMes(ventas, costo, GASTOS_FIJOS_MENSUALES);
  return { ...cuenta, unidades, vendidoAFiado, cuentaVieja: cuenta.margenBruto };
}

export type Riesgo = 'bajo' | 'medio' | 'alto';

export type CuentaCorriente = {
  cliente: Cliente;
  debe: number;
  /** Días desde la compra fiada más vieja que todavía no pagó. */
  dias: number;
  riesgo: Riesgo;
};

/** Lo que debe cada cliente: lo fiado menos lo cobrado, aplicando los pagos a lo más viejo primero. */
export function cuentasCorrientes(
  datos: DatosDelNegocio,
  hoy: string,
  cobros: Record<string, number> = COBROS_DE_FIADO,
): CuentaCorriente[] {
  const resultado: CuentaCorriente[] = [];
  for (const cliente of datos.clientes) {
    const fiados = datos.ventas
      .filter((venta) => venta.fiado && venta.clienteId === cliente.id)
      .sort((a, b) => a.fecha.localeCompare(b.fecha));
    let pagado = cobros[cliente.nombre] ?? 0;
    let debe = 0;
    let masVieja: string | null = null;
    for (const venta of fiados) {
      const importe = venta.precio * venta.cantidad;
      const cubre = Math.min(pagado, importe);
      pagado -= cubre;
      if (importe - cubre > 0) {
        debe += importe - cubre;
        masVieja ??= venta.fecha;
      }
    }
    if (debe <= 0 || !masVieja) continue;
    const dias = diasEntre(masVieja, hoy);
    resultado.push({
      cliente,
      debe,
      dias,
      riesgo: dias > 45 ? 'alto' : dias > 20 ? 'medio' : 'bajo',
    });
  }
  return resultado.sort((a, b) => b.debe - a.debe);
}

export type Cierre = {
  vendido: number;
  cobrado: number;
  fiadoNuevo: number;
  cobrosDeFiado: number;
  costoDeLoVendido: number;
  margenBruto: number;
  comisiones: number;
  mermas: number;
  gastos: number;
  fijosDelDia: number;
  ganancia: number;
  pagosAProveedores: number;
  ventasEnEfectivo: number;
  /** Lo que tendría que haber en el cajón al cerrar. */
  cajon: number;
  /** Lo que entró a la cuenta (QR, transferencias), ya sin comisiones. */
  enLaCuenta: number;
};

/**
 * El cierre del día a partir de los renglones de la hoja. El costo de lo vendido sale del margen del mes
 * (la hoja dice cuánto se vendió, no qué producto), y los gastos fijos se reparten en 30 días.
 */
export function cierreDelDia(renglones: Renglon[], margenDelMes: number): Cierre {
  const suma = (filtro: (renglon: Renglon) => boolean) =>
    renglones.filter(filtro).reduce((total, renglon) => total + renglon.importe, 0);
  const cobrado = suma((renglon) => renglon.tipo === 'venta');
  const fiadoNuevo = suma((renglon) => renglon.tipo === 'fiado');
  const vendido = cobrado + fiadoNuevo;
  const costoDeLoVendido = vendido * (1 - margenDelMes);
  const margenBruto = vendido - costoDeLoVendido;
  const ventasPorCuenta = suma((renglon) => renglon.tipo === 'venta' && renglon.medio === 'cuenta');
  const comisionDeCuenta =
    MEDIOS_DE_COBRO.find((medio) => medio.medio.startsWith('QR'))?.comision ?? 0;
  const comisiones = ventasPorCuenta * comisionDeCuenta;
  const mermas = suma((renglon) => renglon.tipo === 'merma');
  const gastos = suma((renglon) => renglon.tipo === 'gasto');
  const fijosDelDia = GASTOS_FIJOS_MENSUALES / 30;
  const enEfectivo = (tipo: Renglon['tipo']) =>
    suma((renglon) => renglon.tipo === tipo && renglon.medio === 'efectivo');
  const pagosAProveedores = suma((renglon) => renglon.tipo === 'pago');

  return {
    vendido,
    cobrado,
    fiadoNuevo,
    cobrosDeFiado: suma((renglon) => renglon.tipo === 'cobro'),
    costoDeLoVendido,
    margenBruto,
    comisiones,
    mermas,
    gastos,
    fijosDelDia,
    ganancia: margenBruto - comisiones - mermas - gastos - fijosDelDia,
    pagosAProveedores,
    ventasEnEfectivo: enEfectivo('venta'),
    cajon:
      CAMBIO_INICIAL +
      enEfectivo('venta') +
      enEfectivo('cobro') -
      enEfectivo('pago') -
      enEfectivo('gasto'),
    enLaCuenta:
      ventasPorCuenta -
      comisiones +
      suma((renglon) => renglon.tipo === 'cobro' && renglon.medio === 'cuenta'),
  };
}

/** Suma a las cuentas corrientes lo que la hoja del día fió y descuenta lo que cobró. */
export function fiadoConLaHoja(
  cuentas: CuentaCorriente[],
  renglones: Renglon[],
): CuentaCorriente[] {
  const lista = cuentas.map((cuenta) => ({ ...cuenta }));
  for (const renglon of renglones) {
    if (!renglon.cliente || (renglon.tipo !== 'fiado' && renglon.tipo !== 'cobro')) continue;
    const signo = renglon.tipo === 'fiado' ? 1 : -1;
    const cuenta = lista.find((item) => item.cliente.nombre === renglon.cliente);
    if (cuenta) cuenta.debe += signo * renglon.importe;
    else if (signo > 0)
      lista.push({
        cliente: { id: `hoja-${renglon.id}`, nombre: renglon.cliente, telefono: '' },
        debe: renglon.importe,
        dias: 0,
        riesgo: 'bajo',
      });
  }
  return lista.filter((cuenta) => cuenta.debe > 0).sort((a, b) => b.debe - a.debe);
}
