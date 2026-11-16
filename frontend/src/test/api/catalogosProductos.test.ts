import { describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { consultarCatalogos } from '@/api/catalogosProductos';
import { server } from '@/mocks/node';
import { env } from '@/config/env';

describe('comparación de catálogos públicos', () => {
  it('respeta la URL configurada del backend y envía la sesión a ese servidor', async () => {
    const anterior = env.apiUrl;
    env.apiUrl = 'https://backend.example/api';
    server.use(
      http.get('https://backend.example/api/catalogos/:fuente/7790895000997', ({ request }) => {
        expect(request.credentials).toBe('include');
        return HttpResponse.json({ producto: null });
      }),
    );
    try {
      const resultado = await consultarCatalogos('7790895000997', new AbortController().signal);
      expect(resultado.fuentes.map((fuente) => fuente.estado)).toEqual([
        'no_encontrado',
        'no_encontrado',
      ]);
    } finally {
      env.apiUrl = anterior;
    }
  });
  it('mantiene las coincidencias separadas y usa la sesión sólo con el backend de Ensambla', async () => {
    server.use(
      http.get('/api/catalogos/sepa/77903754', ({ request }) => {
        expect(request.credentials).toBe('include');
        return HttpResponse.json({
          producto: {
            codigo: '77903754',
            nombre: 'Guaymallén',
            marca: 'Guaymallén',
            presentacion: '',
          },
        });
      }),
      http.get('/api/catalogos/openfoodfacts/77903754', () =>
        HttpResponse.json({
          producto: {
            codigo: '77903754',
            nombre: 'Guaymallén de oro',
            presentacion: '',
            marca: 'Guaymallén',
            imagen: 'https://images.openfoodfacts.org/images/products/test.jpg',
          },
        }),
      ),
    );
    const resultado = await consultarCatalogos('77903754', new AbortController().signal);
    expect(resultado.fuentes.map((r) => r.estado)).toEqual(['encontrado', 'encontrado']);
    expect(resultado.fuentes[1]?.producto?.imagen).toContain('images.openfoodfacts.org');
  });
  it('una fuente caída no significa que el producto no exista y permite usar la otra', async () => {
    server.use(
      http.get('/api/catalogos/sepa/3017620422003', () => HttpResponse.json({ producto: null })),
      http.get(
        '/api/catalogos/openfoodfacts/3017620422003',
        () => new HttpResponse(null, { status: 503 }),
      ),
    );
    const resultado = await consultarCatalogos('3017620422003', new AbortController().signal);
    expect(resultado.fuentes.map((r) => r.estado)).toEqual(['no_encontrado', 'no_disponible']);
  });
  it('rechaza una ficha con otro código e imágenes ajenas al catálogo', async () => {
    server.use(
      http.get('/api/catalogos/sepa/77905741', () =>
        HttpResponse.json({
          producto: { codigo: '77903754', nombre: 'Otro', marca: '', presentacion: '' },
        }),
      ),
      http.get('/api/catalogos/openfoodfacts/77905741', () =>
        HttpResponse.json({
          producto: {
            codigo: '77905741',
            nombre: 'Jorgito',
            marca: '',
            presentacion: '',
            imagen: 'https://un-sitio-ajeno.test/image',
          },
        }),
      ),
    );
    const resultado = await consultarCatalogos('77905741', new AbortController().signal);
    expect(resultado.fuentes[0]?.estado).toBe('no_disponible');
    expect(resultado.fuentes[1]?.producto?.imagen).toBeUndefined();
  });
});
