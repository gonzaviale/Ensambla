import { normalize } from '@/lib/sistema/core';
import { proveedorDe } from './compras';
import { pesos } from './formato';
import type { DatosDelNegocio, Producto } from './resultado';

// Pedidos por WhatsApp: Sami lee el mensaje del cliente, busca cada cosa en el inventario, resuelve «la de siempre»
// con sus compras anteriores y arma el pedido contra el stock real. El comerciante lo confirma antes de responder.

export type LineaPedida = {
  /** Cómo lo escribió el cliente. */
  texto: string;
  cantidad: number;
  /** La palabra con que se busca en el inventario. */
  busca: string;
  /** «La de siempre»: se elige lo que más le compró ese cliente entre lo que coincide. */
  deSiempre?: boolean;
};

export type MensajeDePedido = {
  id: string;
  cliente: string;
  hora: string;
  /** El mensaje tal cual llegó. */
  texto: string;
  /** Lo que entendió la IA del mensaje. En el mockup viene escrito; en el producto lo arma Sami. */
  lineas: LineaPedida[];
  entrega: 'retira' | 'envio';
  pago: 'cobrado' | 'fiado';
};

export type EstadoDeLinea = 'hay' | 'poco' | 'agotado' | 'no-trabajas';

export type LineaArmada = {
  linea: LineaPedida;
  producto: Producto | null;
  /** Lo que se puede entregar ahora. */
  entrega: number;
  estado: EstadoDeLinea;
  subtotal: number;
  /** Lo que Sami le explica al comerciante sobre esta línea. */
  aviso?: string;
};

export type PedidoArmado = {
  mensaje: MensajeDePedido;
  lineas: LineaArmada[];
  total: number;
  /** Hay algo que no se puede entregar completo. */
  incompleto: boolean;
};

/** Cuántas unidades de cada producto le compró un cliente. */
function comprasDe(datos: DatosDelNegocio, nombreDelCliente: string) {
  const cliente = datos.clientes.find((item) => item.nombre === nombreDelCliente);
  const unidades = new Map<string, number>();
  if (!cliente) return unidades;
  for (const venta of datos.ventas)
    if (venta.clienteId === cliente.id)
      unidades.set(venta.productoId, (unidades.get(venta.productoId) ?? 0) + venta.cantidad);
  return unidades;
}

function buscarProducto(linea: LineaPedida, datos: DatosDelNegocio, compras: Map<string, number>) {
  const palabra = normalize(linea.busca);
  const candidatos = datos.productos.filter((producto) =>
    normalize(producto.nombre).includes(palabra),
  );
  if (!linea.deSiempre) return candidatos[0] ?? null;
  return (
    [...candidatos].sort((a, b) => (compras.get(b.id) ?? 0) - (compras.get(a.id) ?? 0))[0] ?? null
  );
}

export function armarPedido(mensaje: MensajeDePedido, datos: DatosDelNegocio): PedidoArmado {
  const compras = comprasDe(datos, mensaje.cliente);
  const lineas = mensaje.lineas.map((linea): LineaArmada => {
    const producto = buscarProducto(linea, datos, compras);
    if (!producto)
      return {
        linea,
        producto: null,
        entrega: 0,
        estado: 'no-trabajas',
        subtotal: 0,
        aviso: `No tenés «${linea.busca}» en el inventario. Sami lo anota por si querés sumarlo.`,
      };
    const entrega = Math.min(linea.cantidad, producto.stock);
    const proveedor = proveedorDe(producto.nombre);
    const estado: EstadoDeLinea =
      producto.stock === 0 ? 'agotado' : entrega < linea.cantidad ? 'poco' : 'hay';
    return {
      linea,
      producto,
      entrega,
      estado,
      subtotal: entrega * producto.precio,
      aviso:
        estado === 'agotado'
          ? `Se agotó. Llega con ${proveedor.nombre}, que pasa cada ${proveedor.entregaCadaDias} días.`
          : estado === 'poco'
            ? `Pidió ${linea.cantidad} y te quedan ${producto.stock}.`
            : linea.deSiempre && (compras.get(producto.id) ?? 0) > 0
              ? `«La de siempre»: es lo que más te compró (${compras.get(producto.id)} unidades).`
              : undefined,
    };
  });
  return {
    mensaje,
    lineas,
    total: lineas.reduce((total, linea) => total + linea.subtotal, 0),
    incompleto: lineas.some((linea) => linea.estado !== 'hay'),
  };
}

/** La respuesta para el cliente, lista para revisar antes de mandarla. */
export function respuestaAlCliente(pedido: PedidoArmado, negocio: string) {
  const nombre = pedido.mensaje.cliente;
  const separados = pedido.lineas.filter((linea) => linea.entrega > 0 && linea.producto);
  const lineas = [
    `¡Hola, ${nombre}! Te separo en ${negocio}:`,
    ...separados.map((linea) => `• ${linea.entrega} × ${linea.producto?.nombre}`),
    `Total: ${pesos(pedido.total)}${pedido.mensaje.pago === 'fiado' ? ' (lo anoto en tu cuenta)' : ''}.`,
  ];
  for (const linea of pedido.lineas) {
    if (linea.estado === 'poco')
      lineas.push(
        `De ${linea.producto?.nombre} me quedan ${linea.entrega} (pediste ${linea.linea.cantidad}).`,
      );
    if (linea.estado === 'agotado')
      lineas.push(`Se me terminó ${linea.producto?.nombre}: me llega en unos días y te aviso.`);
    if (linea.estado === 'no-trabajas') lineas.push(`Perdón, ${linea.linea.texto} no tengo.`);
  }
  lineas.push(
    pedido.mensaje.entrega === 'envio'
      ? 'Te lo mando en un rato.'
      : 'Te lo dejo listo para que pases.',
  );
  return lineas.join('\n');
}
