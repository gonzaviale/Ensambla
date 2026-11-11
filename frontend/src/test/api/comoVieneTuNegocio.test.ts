import { describe, expect, it } from 'vitest';
import { obtenerComoVieneTuNegocio } from '@/api/comoVieneTuNegocio';
import { negocioDe } from '@/lib/objetivosNegocio';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { comoVieneTuNegocioDe } from '@/mocks/handlers/comoVieneTuNegocio';
import { setupUsuarioConSesion } from '@/test/testHelpers';

const HOY = '2026-11-08';

describe('Cómo viene tu negocio', () => {
  it('sin cuadernos el servidor responde sin ventas, en cero y sin semanas', async () => {
    setupUsuarioConSesion();

    expect(await obtenerComoVieneTuNegocio()).toEqual({
      hayVentas: false,
      vendiste: 0,
      mercaderia: 0,
      gastos: 0,
      queda: 0,
      teDeben: 0,
      semanas: [],
    });
  });

  it('con el almacén cargado devuelve pesos enteros y seis semanas que terminan en Hoy', () => {
    const almacen = crearAlmacenCargado(HOY);

    const negocio = comoVieneTuNegocioDe([almacen], HOY);

    expect(negocio.hayVentas).toBe(true);
    expect(negocio.vendiste).toBe(Math.round(negocioDe([almacen], HOY).vendiste));
    expect(negocio.queda).toBe(negocio.vendiste - negocio.mercaderia - negocio.gastos);
    expect(negocio.semanas.map((semana) => semana.semana)).toEqual([
      'Hace 5 sem.',
      'Hace 4 sem.',
      'Hace 3 sem.',
      'Hace 2 sem.',
      'Hace 1 sem.',
      'Hoy',
    ]);
    expect(negocio.semanas.at(-1)?.queda).toBe(negocio.queda);
    for (const { queda } of negocio.semanas) expect(Number.isInteger(queda)).toBe(true);
  });
});
