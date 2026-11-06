import { create } from 'zustand';
import type { OrdenDeCompra } from '@/lib/gestion/compras';
import { CAJA_PARA_COMPRAS, HOJA_DEL_DIA, type Renglon } from '@/lib/gestion/datosDeEjemplo';
import type { FacturaC } from '@/lib/gestion/monotributo';
import type { OfertaPosible } from '@/lib/gestion/ofertas';
import type { MensajeDePedido, PedidoArmado } from '@/lib/gestion/pedidos';
import type { TipoBloque } from '@/lib/sistema/tipos';

// Lo que pasó hoy en «Mi negocio»: lo que la persona confirmó, mandó, recibió y facturó. Vive mientras dura la
// sesión, así no se pierde al cambiar de pestaña, y `useNegocio` lo pone sobre el cuaderno para que todas las
// pantallas vean el mismo stock, las mismas ventas y el mismo fiado. En el mockup no va al servidor.

export type PasoDelCierre = 'foto' | 'leyendo' | 'revisar' | 'cerrado';
export type RecepcionDeOrden = 'enviado' | 'leyendo' | 'recibido';

export type OfertaEnviada = {
  /** La oferta como salió (con el descuento elegido). */
  oferta: OfertaPosible;
  /** Las respuestas «LA QUIERO», que entran como pedidos. */
  pedidos: MensajeDePedido[];
};

type Estado = {
  /** Pedidos confirmados, por id de mensaje, como quedaron al confirmarlos. */
  confirmados: Record<string, PedidoArmado>;
  /** Ofertas enviadas, por id de producto. */
  ofertas: Record<string, OfertaEnviada>;
  caja: number;
  ordenesEnviadas: OrdenDeCompra[];
  /** En qué va cada pedido a proveedor, por id de proveedor. */
  recepcion: Record<string, RecepcionDeOrden>;
  /** Precios cambiados hoy, por id de producto. */
  precios: Record<string, number>;
  hoja: Renglon[];
  pasoDelCierre: PasoDelCierre;
  bloquesCreados: TipoBloque[];
  /** Facturas emitidas, por id de pedido. */
  facturas: Record<string, FacturaC>;
};

type Acciones = {
  confirmarPedido: (pedido: PedidoArmado) => void;
  enviarOferta: (enviada: OfertaEnviada) => void;
  cambiarCaja: (caja: number) => void;
  enviarOrden: (orden: OrdenDeCompra) => void;
  cambiarRecepcion: (proveedorId: string, recepcion: RecepcionDeOrden) => void;
  cambiarPrecio: (productoId: string, precio: number) => void;
  cambiarHoja: (hoja: Renglon[]) => void;
  cambiarPasoDelCierre: (paso: PasoDelCierre) => void;
  crearBloque: (tipo: TipoBloque) => void;
  emitirFactura: (pedidoId: string, factura: FacturaC) => void;
  /** Vuelve al principio del día de la demo. */
  reiniciar: () => void;
};

const INICIAL: Estado = {
  confirmados: {},
  ofertas: {},
  caja: CAJA_PARA_COMPRAS,
  ordenesEnviadas: [],
  recepcion: {},
  precios: {},
  hoja: HOJA_DEL_DIA,
  pasoDelCierre: 'foto',
  bloquesCreados: [],
  facturas: {},
};

export const useNegocioStore = create<Estado & Acciones>()((set) => ({
  ...INICIAL,
  confirmarPedido: (pedido) =>
    set((estado) => ({ confirmados: { ...estado.confirmados, [pedido.mensaje.id]: pedido } })),
  enviarOferta: (enviada) =>
    set((estado) => ({ ofertas: { ...estado.ofertas, [enviada.oferta.producto.id]: enviada } })),
  cambiarCaja: (caja) => set({ caja }),
  enviarOrden: (orden) =>
    set((estado) => ({
      ordenesEnviadas: [...estado.ordenesEnviadas, { ...orden, enviada: true }],
      recepcion: { ...estado.recepcion, [orden.proveedor.id]: 'enviado' },
    })),
  cambiarRecepcion: (proveedorId, recepcion) =>
    set((estado) => ({ recepcion: { ...estado.recepcion, [proveedorId]: recepcion } })),
  cambiarPrecio: (productoId, precio) =>
    set((estado) => ({ precios: { ...estado.precios, [productoId]: precio } })),
  cambiarHoja: (hoja) => set({ hoja }),
  cambiarPasoDelCierre: (pasoDelCierre) => set({ pasoDelCierre }),
  crearBloque: (tipo) =>
    set((estado) => ({ bloquesCreados: [...new Set([...estado.bloquesCreados, tipo])] })),
  emitirFactura: (pedidoId, factura) =>
    set((estado) => ({ facturas: { ...estado.facturas, [pedidoId]: factura } })),
  reiniciar: () => set(INICIAL),
}));
