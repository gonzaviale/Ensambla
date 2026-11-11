import { describe, expect, it } from 'vitest';
import { obtenerPasosObjetivo } from '@/api/objetivos';
import { setupServerResponse, setupUsuarioConSesion } from '@/test/testHelpers';

const PASO = {
  tipo: 'SUBIR_PRECIO',
  nombre: 'Yerba',
  precio: 3600,
  costo: 2700,
  stock: 4,
  vendidas30Dias: 60,
  precioZona: { minimo: 3500, promedio: 3900, maximo: 4300 },
};

describe('API de objetivos', () => {
  it('obtiene los productos elegidos por el servidor con sus datos reales', async () => {
    setupUsuarioConSesion();
    setupServerResponse('get', '/objetivos/pasos', 200, [PASO]);

    const pasos = await obtenerPasosObjetivo();

    expect(pasos).toEqual([PASO]);
  });

  it('admite una lista vacía y un producto sin precio de zona', async () => {
    setupUsuarioConSesion();
    setupServerResponse('get', '/objetivos/pasos', 200, [{ ...PASO, precioZona: null }]);

    expect((await obtenerPasosObjetivo())[0]?.precioZona).toBeNull();
    setupServerResponse('get', '/objetivos/pasos', 200, []);
    expect(await obtenerPasosObjetivo()).toEqual([]);
  });

  it('rechaza una respuesta que no respeta el contrato', async () => {
    setupUsuarioConSesion();
    setupServerResponse('get', '/objetivos/pasos', 200, [{ ...PASO, vendidas30Dias: -1 }]);

    await expect(obtenerPasosObjetivo()).rejects.toMatchObject({ name: 'ZodError' });
  });
});
