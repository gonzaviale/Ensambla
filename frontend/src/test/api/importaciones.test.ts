import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { ApiError } from '@/api/apiError';
import { pedirLectura } from '@/api/importaciones';
import { makeBlock } from '@/lib/sistema/core';
import { server } from '@/mocks/node';
import { setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';
import type { PedidoDeLectura } from '@/types/importacion';

const pedidoDe = (destinoExistente: boolean): PedidoDeLectura => {
  const bloque = makeBlock('sales');
  return {
    nombre: 'Mis ventas',
    contexto: 'registros',
    tipo: bloque.type,
    campos: bloque.fields,
    vinculos: {},
    destinoExistente,
  };
};

const foto = () => new File(['pixeles'], 'cuaderno.jpg', { type: 'image/jpeg' });

const textoDe = async (parte: FormDataEntryValue | null) =>
  typeof parte === 'string' ? parte : ((await parte?.text()) ?? 'null');

describe('API de importaciones', () => {
  it('manda la foto y el bloque de destino en un multipart', async () => {
    setupUsuarioConSesion();
    let imagen = '';
    let enviado: { nombre: string; destinoExistente: boolean; campos: { key: string }[] } | null =
      null;
    server.use(
      http.post('/api/importaciones/lectura', async ({ request }) => {
        const cuerpo = await request.formData();
        imagen = (cuerpo.get('imagen') as File).name;
        enviado = JSON.parse(await textoDe(cuerpo.get('pedido')));
        return HttpResponse.json({ filas: [{ monto: 1500 }], descripcion: '' });
      }),
    );
    const pedido = pedidoDe(true);

    const lectura = await pedirLectura(pedido, foto());

    expect(lectura.filas).toEqual([{ monto: 1500 }]);
    expect(lectura.camposNuevos).toEqual([]);
    expect(lectura.dudas).toEqual([]);
    expect(imagen).toBe('cuaderno.jpg');
    expect(enviado).toMatchObject({ nombre: 'Mis ventas', destinoExistente: true });
    expect((enviado as unknown as { campos: { key: string }[] }).campos.map((c) => c.key)).toEqual(
      pedido.campos.map((campo) => campo.key),
    );
  });

  it('una imagen sin nombre (la que sale de achicar una foto) se manda como foto.jpg', async () => {
    setupUsuarioConSesion();
    let nombre = '';
    let destinoExistente: boolean | null = null;
    server.use(
      http.post('/api/importaciones/lectura', async ({ request }) => {
        const cuerpo = await request.formData();
        nombre = (cuerpo.get('imagen') as File).name;
        destinoExistente = JSON.parse(await textoDe(cuerpo.get('pedido'))).destinoExistente;
        return HttpResponse.json({ filas: [], descripcion: '' });
      }),
    );

    await pedirLectura({ ...pedidoDe(false), destinoExistente: undefined }, new Blob(['x']));

    expect(nombre).toBe('foto.jpg');
    expect(destinoExistente).toBe(false);
  });

  it('en la simulación devuelve registros, columnas nuevas y dudas de ejemplo', async () => {
    setupUsuarioConSesion();

    const lectura = await pedirLectura(pedidoDe(true), foto());

    expect(lectura.filas.length).toBeGreaterThan(0);
    expect(lectura.camposNuevos.length).toBeGreaterThan(0);
    expect(lectura.dudas.length).toBeGreaterThan(0);
  });

  it('propaga el mensaje del servidor cuando la foto no se puede leer', async () => {
    setupUsuarioConSesion();
    setupServerError(
      'post',
      '/importaciones/lectura',
      400,
      'La estructura leída no es compatible con el bloque de destino',
    );

    const error = await pedirLectura(pedidoDe(true), foto()).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).message).toBe(
      'La estructura leída no es compatible con el bloque de destino',
    );
  });
});
