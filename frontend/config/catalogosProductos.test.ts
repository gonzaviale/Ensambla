import type { IncomingMessage, ServerResponse } from 'node:http';
import type * as FileSystem from 'node:fs';
import { gzipSync } from 'node:zlib';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Connect, ViteDevServer } from 'vite';
import { catalogosProductos } from './catalogosProductos';

const archivos = vi.hoisted(() => ({ existe: vi.fn(() => false), leer: vi.fn() }));
vi.mock('node:fs', async (importOriginal) => {
  const original = await importOriginal<typeof FileSystem>();
  const reemplazos = { existsSync: archivos.existe, readFileSync: archivos.leer };
  return { ...original, ...reemplazos, default: { ...original, ...reemplazos } };
});
beforeEach(() => {
  archivos.existe.mockReset().mockReturnValue(false);
  archivos.leer.mockReset();
});

async function middlewareDelCatalogo() {
  let middleware: Connect.NextHandleFunction | undefined;
  const configurar = catalogosProductos().configureServer;
  if (typeof configurar !== 'function') throw new Error('Falta configurar el catálogo');
  await configurar.call(
    {} as never,
    {
      config: { root: '/catalogo-de-prueba', logger: { warn: vi.fn() } },
      middlewares: {
        use: (funcion: Connect.NextHandleFunction) => {
          middleware = funcion;
        },
      },
    } as unknown as ViteDevServer,
  );
  if (!middleware) throw new Error('Falta el middleware');
  return middleware;
}

describe('proxy local de catálogo', () => {
  it('quita credenciales y referencias antes de crear el pedido externo, sin modificar headers ya enviados', async () => {
    const middleware = await middlewareDelCatalogo();
    const pedido = {
      url: '/catalogos/openfoodfacts/77903754',
      method: 'GET',
      headers: {
        cookie: 'sesion-privada',
        authorization: 'Bearer privado',
        origin: 'https://comercio.local',
        referer: 'https://comercio.local/cuadernos',
        accept: 'application/json',
      },
    } as IncomingMessage;
    const siguiente = vi.fn();
    middleware(pedido, {} as ServerResponse, siguiente);
    expect(siguiente).toHaveBeenCalledOnce();
    expect(pedido.headers).toEqual({ accept: 'application/json' });
  });
  it('conserva las credenciales de los pedidos que no son consultas al catálogo', async () => {
    const middleware = await middlewareDelCatalogo();
    const pedido = {
      url: '/api/auth/me',
      headers: { cookie: 'sesion-privada' },
    } as IncomingMessage;
    const siguiente = vi.fn();
    middleware(pedido, {} as ServerResponse, siguiente);
    expect(pedido.headers.cookie).toBe('sesion-privada');
    expect(siguiente).toHaveBeenCalledOnce();
  });
  it.each([undefined, '{', '{"fecha":"2026-11-10"}', '{"productos":null}'])(
    'informa indisponibilidad con un índice ausente o inválido: %s',
    async (contenido) => {
      archivos.existe.mockReturnValue(contenido !== undefined);
      archivos.leer.mockReturnValue(Buffer.from(contenido ?? ''));
      const middleware = await middlewareDelCatalogo();
      const res = { setHeader: vi.fn(), end: vi.fn(), statusCode: 0 };
      const siguiente = vi.fn();
      middleware(
        { url: '/catalogos/sepa/77903754', method: 'GET' } as IncomingMessage,
        res as unknown as ServerResponse,
        siguiente,
      );
      expect(res.statusCode).toBe(503);
      expect(siguiente).not.toHaveBeenCalled();
    },
  );
  it('carga el índice y encuentra códigos equivalentes sin perder ceros iniciales', async () => {
    const producto = {
      codigo: '77903754',
      nombre: 'Alfajor de prueba',
      marca: 'Marca de prueba',
      presentacion: '40 g',
    };
    archivos.existe.mockReturnValue(true);
    archivos.leer.mockReturnValue(
      Buffer.from(
        JSON.stringify({ fecha: '2026-11-10', productos: { '00000077903754': producto } }),
      ),
    );
    const middleware = await middlewareDelCatalogo();
    const res = { setHeader: vi.fn(), end: vi.fn(), statusCode: 0 };
    const siguiente = vi.fn();
    for (const codigo of ['77903754', '00000077903754']) {
      middleware(
        { url: `/catalogos/sepa/${codigo}`, method: 'GET' } as IncomingMessage,
        res as unknown as ServerResponse,
        siguiente,
      );
      expect(res.statusCode).toBe(200);
      expect(JSON.parse(res.end.mock.lastCall?.[0] ?? '{}')).toEqual({
        producto,
        fecha: '2026-11-10',
      });
    }
    expect(siguiente).not.toHaveBeenCalled();
  });
  it('usa el catálogo comprimido incluido cuando no hay una actualización local', async () => {
    archivos.existe.mockImplementation((ruta: unknown) => String(ruta).endsWith('sepa.json.gz'));
    archivos.leer.mockReturnValue(gzipSync(JSON.stringify({ fecha: '2026-11-10', productos: {} })));
    const middleware = await middlewareDelCatalogo();
    const res = { setHeader: vi.fn(), end: vi.fn(), statusCode: 0 };
    middleware(
      { url: '/catalogos/sepa/77903754', method: 'GET' } as IncomingMessage,
      res as unknown as ServerResponse,
      vi.fn(),
    );
    expect(archivos.leer.mock.lastCall?.[0]).toMatch(/datos[\\/]catalogos[\\/]sepa\.json\.gz$/);
    expect(res.statusCode).toBe(200);
    expect(JSON.parse(res.end.mock.lastCall?.[0] ?? '{}')).toEqual({
      producto: null,
      fecha: '2026-11-10',
    });
  });
});
