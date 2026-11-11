import type { EstadoDeOrden, OrdenDelServidor } from '@/types/pedidos';

export const ETIQUETA_DE_ESTADO: Record<EstadoDeOrden, string> = {
  PENDIENTE: 'Pendiente',
  ACEPTADA: 'Aceptado',
  RECHAZADA: 'Rechazado',
  EN_PREPARACION: 'En preparación',
  LISTA: 'Lista para retirar',
};

export type FiltroDePedidos = 'todos' | 'pendientes' | 'en-curso' | 'listos' | 'rechazados';

export const FILTROS: {
  clave: FiltroDePedidos;
  etiqueta: string;
  estados: EstadoDeOrden[] | null;
}[] = [
  { clave: 'todos', etiqueta: 'Todos', estados: null },
  { clave: 'pendientes', etiqueta: 'Pendientes', estados: ['PENDIENTE'] },
  { clave: 'en-curso', etiqueta: 'En curso', estados: ['ACEPTADA', 'EN_PREPARACION'] },
  { clave: 'listos', etiqueta: 'Listos', estados: ['LISTA'] },
  { clave: 'rechazados', etiqueta: 'Rechazados', estados: ['RECHAZADA'] },
];

export function filtrarPedidos(pedidos: OrdenDelServidor[], filtro: FiltroDePedidos) {
  const estados = FILTROS.find((posible) => posible.clave === filtro)?.estados;
  return estados ? pedidos.filter((pedido) => estados.includes(pedido.estado)) : pedidos;
}

export type PasoDelDueno = { estado: EstadoDeOrden; etiqueta: string; rechaza?: boolean };

/** Lo que el dueño puede hacer con una orden en cada etapa; rechazada y lista ya no tienen pasos. */
export function pasosPara(estado: EstadoDeOrden): PasoDelDueno[] {
  switch (estado) {
    case 'PENDIENTE':
      return [
        { estado: 'ACEPTADA', etiqueta: 'Aceptar' },
        { estado: 'RECHAZADA', etiqueta: 'Rechazar', rechaza: true },
      ];
    case 'ACEPTADA':
      return [
        { estado: 'EN_PREPARACION', etiqueta: 'Poner en preparación' },
        { estado: 'LISTA', etiqueta: 'Dejar lista para retirar' },
      ];
    case 'EN_PREPARACION':
      return [{ estado: 'LISTA', etiqueta: 'Dejar lista para retirar' }];
    default:
      return [];
  }
}

const cantidades = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 3 });
export const formatoCantidad = (valor: number) => cantidades.format(valor);

const fechaYHora = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' });
export const formatoFechaYHora = (iso: string) => fechaYHora.format(new Date(iso));

/** El WhatsApp con el «+» delante, como se ve en el teléfono; sin número, sólo «Cliente». */
export const clienteLegible = (telefono: string | null) => (telefono ? `+${telefono}` : 'Cliente');
