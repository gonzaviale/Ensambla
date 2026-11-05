import { proveedorDe } from './compras';
import { MERMA_SOBRE_COSTO, SIN_PERMISO_PARA_OFERTAS } from './datosDeEjemplo';
import { pesos } from './formato';
import {
  COMISION_PROMEDIO,
  diasEntre,
  ventasDelPeriodo,
  type Cliente,
  type DatosDelNegocio,
  type Producto,
} from './resultado';

// Ofertas por WhatsApp: Sami elige qué ofrecer (lo que no se mueve o sobra), hasta qué descuento se sigue ganando
// con la cuenta de la ganancia real, y a quién (sólo a quienes compran eso o algo del mismo rubro y aceptaron).

export type MotivoDeOferta = 'parado' | 'sobra';

/** Un producto sobra si al ritmo de venta del mes le dura más que esto. */
export const DIAS_DE_SOBRA = 90;
/** Sami no propone más que esto por vez: pocas y con plata en juego. */
export const MAXIMO_DE_OFERTAS = 5;
const MINIMO_DE_DESTINATARIOS = 5;

export type OfertaPosible = {
  producto: Producto;
  motivo: MotivoDeOferta;
  /** Días desde la última venta; `null` si nunca se vendió. */
  diasSinVenta: number | null;
  /** Lo que hay invertido en ese stock, a costo. */
  plataParada: number;
  /** El descuento más grande con el que cada venta todavía deja algo. */
  descuentoMaximo: number;
  descuentoSugerido: number;
  precioOferta: number;
  /** Lo que deja cada unidad vendida con la oferta, después de comisiones y pérdidas. */
  dejaPorUnidad: number;
  destinatarios: Cliente[];
  /** Clientes que compran eso pero pidieron no recibir ofertas. */
  sinPermiso: number;
};

/** Lo que deja una unidad a ese precio, con la misma cuenta que la ganancia real. */
const dejaA = (precio: number, costo: number) =>
  precio * (1 - COMISION_PROMEDIO) - costo * (1 + MERMA_SOBRE_COSTO);

/** El descuento que deja la venta en cero: más que eso, se pierde plata. */
export function descuentoMaximo(precio: number, costo: number) {
  if (precio <= 0) return 0;
  return Math.max(0, 1 - (costo * (1 + MERMA_SOBRE_COSTO)) / (precio * (1 - COMISION_PROMEDIO)));
}

/** La mitad del máximo, de a 5 % y hasta 25 %: tienta sin regalar el margen. */
export function descuentoSugerido(maximo: number) {
  const pasos = Math.floor(Math.min(maximo / 2, 0.25) * 20);
  return Math.max(pasos, maximo >= 0.05 ? 1 : 0) / 20;
}

/** Los productos para ofertar, de la plata más parada a la menos. */
export function ofertasPosibles(
  datos: DatosDelNegocio,
  hoy: string,
  sinPermiso: string[] = SIN_PERMISO_PARA_OFERTAS,
): OfertaPosible[] {
  const delMes = ventasDelPeriodo(datos.ventas, hoy, 30);
  const visitas = new Map<string, number>();
  for (const venta of datos.ventas)
    if (venta.clienteId) visitas.set(venta.clienteId, (visitas.get(venta.clienteId) ?? 0) + 1);
  const frecuentes = [...visitas.entries()].sort((a, b) => b[1] - a[1]).map(([id]) => id);
  const lista: OfertaPosible[] = [];
  for (const producto of datos.productos) {
    if (producto.stock <= 0 || producto.precio <= 0) continue;
    const ventas = datos.ventas.filter((venta) => venta.productoId === producto.id);
    const vendidasEnElMes = delMes
      .filter((venta) => venta.productoId === producto.id)
      .reduce((total, venta) => total + venta.cantidad, 0);
    const ultima =
      ventas
        .map((venta) => venta.fecha)
        .sort()
        .pop() ?? null;
    const diasDeStock = vendidasEnElMes > 0 ? producto.stock / (vendidasEnElMes / 30) : Infinity;
    const motivo: MotivoDeOferta | null =
      vendidasEnElMes === 0 ? 'parado' : diasDeStock > DIAS_DE_SOBRA ? 'sobra' : null;
    if (!motivo) continue;

    const maximo = descuentoMaximo(producto.precio, producto.costo);
    const sugerido = descuentoSugerido(maximo);
    if (sugerido === 0) continue;
    const precioOferta = Math.floor((producto.precio * (1 - sugerido)) / 50) * 50;

    // Primero quienes compraron ese producto; si son pocos, quienes compran al mismo proveedor (el rubro) y, si
    // todavía son pocos, los que vienen más seguido.
    const rubro = proveedorDe(producto.nombre).id;
    const compraron = new Set(ventas.map((venta) => venta.clienteId).filter(Boolean));
    if (compraron.size < MINIMO_DE_DESTINATARIOS) {
      const delRubro = new Set(
        datos.productos
          .filter((item) => proveedorDe(item.nombre).id === rubro)
          .map((item) => item.id),
      );
      for (const venta of datos.ventas)
        if (venta.clienteId && delRubro.has(venta.productoId)) compraron.add(venta.clienteId);
    }
    for (const cliente of frecuentes) {
      if (compraron.size >= MINIMO_DE_DESTINATARIOS) break;
      compraron.add(cliente);
    }
    const interesados = datos.clientes.filter((cliente) => compraron.has(cliente.id));
    const destinatarios = interesados.filter((cliente) => !sinPermiso.includes(cliente.nombre));

    lista.push({
      producto,
      motivo,
      diasSinVenta: ultima ? diasEntre(ultima, hoy) : null,
      plataParada: producto.stock * producto.costo,
      descuentoMaximo: maximo,
      descuentoSugerido: sugerido,
      precioOferta,
      dejaPorUnidad: dejaA(precioOferta, producto.costo),
      destinatarios,
      sinPermiso: interesados.length - destinatarios.length,
    });
  }
  return lista.sort((a, b) => b.plataParada - a.plataParada).slice(0, MAXIMO_DE_OFERTAS);
}

/** Recalcula precio y ganancia por unidad para otro descuento. */
export function conDescuento(oferta: OfertaPosible, descuento: number): OfertaPosible {
  const precioOferta = Math.floor((oferta.producto.precio * (1 - descuento)) / 50) * 50;
  return {
    ...oferta,
    descuentoSugerido: descuento,
    precioOferta,
    dejaPorUnidad: dejaA(precioOferta, oferta.producto.costo),
  };
}

export function mensajeDeOferta(oferta: OfertaPosible, cliente: string, negocio: string) {
  return [
    `¡Hola, ${cliente}! Esta semana en ${negocio}:`,
    `${oferta.producto.nombre} a ${pesos(oferta.precioOferta)} (antes ${pesos(oferta.producto.precio)}).`,
    'Respondé LA QUIERO y te la separo.',
    'Si no querés recibir ofertas, respondé BAJA.',
  ].join('\n');
}

export type ResultadoDeOferta = {
  respondieron: number;
  unidades: number;
  vendido: number;
  /** La plata parada que volvió a la caja (a costo). */
  recuperado: number;
  ganancia: number;
};

/**
 * Lo que pasa después de mandar la oferta. En el mockup es una estimación fija (4 de cada 10 responden y se llevan
 * 2); en el producto sale de las respuestas «LA QUIERO» que entran como pedidos.
 */
export function resultadoEstimado(oferta: OfertaPosible): ResultadoDeOferta {
  const respondieron = Math.max(1, Math.round(oferta.destinatarios.length * 0.4));
  const unidades = Math.min(oferta.producto.stock, respondieron * 2);
  return {
    respondieron,
    unidades,
    vendido: unidades * oferta.precioOferta,
    recuperado: unidades * oferta.producto.costo,
    ganancia: unidades * oferta.dejaPorUnidad,
  };
}
