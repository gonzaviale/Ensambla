import { describe, expect, it } from 'vitest';
import {
  clienteLegible,
  enlaceParaPedir,
  filtrarPedidos,
  formatoCantidad,
  pasosPara,
} from '@/lib/pedidos';
import type { OrdenDelServidor } from '@/types/pedidos';

const orden = (estado: OrdenDelServidor['estado']): OrdenDelServidor => ({
  id: estado,
  clienteTelefono: null,
  estado,
  items: [],
  total: 0,
  creadoEn: '2026-11-11T12:00:00Z',
  actualizadoEn: '2026-11-11T12:00:00Z',
});

describe('lib/pedidos', () => {
  it('el enlace abre el chat con el número de Sami y el mensaje para pedir, codificado', () => {
    expect(enlaceParaPedir('5491155550000')).toBe(
      'https://wa.me/5491155550000?text=Hola%20Sami%2C%20quiero%20hacer%20un%20pedido',
    );
  });

  it('los pasos del dueño son los que permite el servidor y rechazada y lista no tienen ninguno', () => {
    expect(pasosPara('PENDIENTE').map((paso) => paso.estado)).toEqual(['ACEPTADA', 'RECHAZADA']);
    expect(pasosPara('ACEPTADA').map((paso) => paso.estado)).toEqual(['EN_PREPARACION', 'LISTA']);
    expect(pasosPara('EN_PREPARACION').map((paso) => paso.estado)).toEqual(['LISTA']);
    expect(pasosPara('RECHAZADA')).toEqual([]);
    expect(pasosPara('LISTA')).toEqual([]);
  });

  it('filtra los pedidos por etapa y «todos» no filtra', () => {
    const pedidos = (
      ['PENDIENTE', 'ACEPTADA', 'EN_PREPARACION', 'LISTA', 'RECHAZADA'] as const
    ).map(orden);

    expect(filtrarPedidos(pedidos, 'todos')).toHaveLength(5);
    expect(filtrarPedidos(pedidos, 'en-curso').map((pedido) => pedido.estado)).toEqual([
      'ACEPTADA',
      'EN_PREPARACION',
    ]);
    expect(filtrarPedidos(pedidos, 'pendientes')).toHaveLength(1);
    expect(filtrarPedidos(pedidos, 'listos')).toHaveLength(1);
    expect(filtrarPedidos(pedidos, 'rechazados')).toHaveLength(1);
  });

  it('el cliente se ve con el «+» delante y, sin número, como «Cliente»', () => {
    expect(clienteLegible('5491155550001')).toBe('+5491155550001');
    expect(clienteLegible(null)).toBe('Cliente');
  });

  it('las cantidades salen sin ceros de más y con coma decimal', () => {
    expect(formatoCantidad(2)).toBe('2');
    expect(formatoCantidad(0.5)).toBe('0,5');
  });
});
