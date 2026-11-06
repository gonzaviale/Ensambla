import type { OrdenDeCompra } from './compras';
import { COSTOS_DEL_REMITO, type Renglon } from './datosDeEjemplo';
import { resultadoEstimado, type OfertaPosible } from './ofertas';
import type { MensajeDePedido, PedidoArmado } from './pedidos';
import type { DatosDelNegocio, Producto, Venta } from './resultado';

// Lo que pasó hoy en «Mi negocio», puesto sobre el cuaderno. Las pestañas se pasan los datos a través de esto: un
// pedido confirmado es una venta de hoy y descuenta stock (lo ve el cierre, compras, el fiado y el monotributo); un
// remito recibido suma stock y trae el costo nuevo; un precio nuevo cambia lo que ven la proyección y las ofertas.

export type LoQuePasoHoy = {
  /** Los pedidos confirmados, como quedaron al confirmarlos. */
  confirmados: PedidoArmado[];
  /** Los pedidos a proveedores cuyo remito ya se leyó. */
  recibidos: OrdenDeCompra[];
  /** Precios cambiados hoy, por id de producto. */
  precios: Record<string, number>;
};

export const DIA_VACIO: LoQuePasoHoy = { confirmados: [], recibidos: [], precios: {} };

/** El cuaderno con lo de hoy: stock, costos y precios al día, y las ventas de los pedidos confirmados. */
export function conLoDeHoy(
  datos: DatosDelNegocio,
  hoy: string,
  { confirmados, recibidos, precios }: LoQuePasoHoy,
): DatosDelNegocio {
  const stock = new Map<string, number>();
  const costos = new Map<string, number>();
  const sumar = (id: string, unidades: number) => stock.set(id, (stock.get(id) ?? 0) + unidades);

  for (const orden of recibidos)
    for (const item of orden.items) {
      sumar(item.producto.id, item.pedir);
      const costo = COSTOS_DEL_REMITO[item.producto.nombre];
      if (costo !== undefined) costos.set(item.producto.id, costo);
    }

  const ventas: Venta[] = [];
  for (const pedido of confirmados) {
    const cliente = datos.clientes.find((item) => item.nombre === pedido.mensaje.cliente);
    for (const linea of pedido.lineas) {
      if (!linea.producto || linea.entrega === 0) continue;
      sumar(linea.producto.id, -linea.entrega);
      ventas.push({
        productoId: linea.producto.id,
        cantidad: linea.entrega,
        precio: linea.subtotal / linea.entrega,
        fecha: hoy,
        clienteId: cliente?.id ?? '',
        fiado: pedido.mensaje.pago === 'fiado',
      });
    }
  }

  const productos = datos.productos.map((producto): Producto => ({
    ...producto,
    stock: Math.max(0, producto.stock + (stock.get(producto.id) ?? 0)),
    costo: costos.get(producto.id) ?? producto.costo,
    precio: precios[producto.id] ?? producto.precio,
  }));
  return { ...datos, productos, ventas: [...datos.ventas, ...ventas] };
}

/** Cada pedido confirmado es un renglón del día: ya está en el cuaderno, así que no hace falta leerlo de la foto. */
export function renglonesDePedidos(confirmados: PedidoArmado[]): Renglon[] {
  return confirmados
    .filter((pedido) => pedido.total > 0)
    .map((pedido) => {
      const fiado = pedido.mensaje.pago === 'fiado';
      const cuantos = pedido.lineas.filter((linea) => linea.entrega > 0).length;
      return {
        id: `pedido-${pedido.mensaje.id}`,
        pedidoId: pedido.mensaje.id,
        escrito: `Pedido por WhatsApp · ${pedido.mensaje.hora}`,
        tipo: fiado ? 'fiado' : 'venta',
        importe: pedido.total,
        detalle: `Pedido de ${pedido.mensaje.cliente} (${cuantos === 1 ? '1 producto' : `${cuantos} productos`})`,
        medio: fiado ? undefined : 'cuenta',
        cliente: pedido.mensaje.cliente,
      };
    });
}

/** El prefijo de los pedidos que llegan respondiendo a una oferta. */
export const idDeOferta = (productoId: string) => `oferta-${productoId}`;

/**
 * Las respuestas «LA QUIERO» a una oferta enviada, como pedidos para la bandeja. En el mockup responden los primeros
 * destinatarios según la estimación de `resultadoEstimado`; en el producto son los mensajes que llegan.
 */
export function pedidosDeOferta(oferta: OfertaPosible, hora: string): MensajeDePedido[] {
  const { respondieron, unidades } = resultadoEstimado(oferta);
  const porCliente = Math.max(1, Math.floor(unidades / respondieron));
  return oferta.destinatarios.slice(0, respondieron).map((cliente, indice) => ({
    id: `${idDeOferta(oferta.producto.id)}-${indice + 1}`,
    cliente: cliente.nombre,
    hora,
    texto: `LA QUIERO. Separame ${porCliente === 1 ? 'una' : porCliente}, paso a buscar${porCliente === 1 ? 'la' : 'las'}.`,
    lineas: [
      {
        texto: `${porCliente} ${oferta.producto.nombre}`,
        cantidad: porCliente,
        busca: oferta.producto.nombre,
        precio: oferta.precioOferta,
      },
    ],
    entrega: 'retira',
    pago: 'cobrado',
  }));
}
