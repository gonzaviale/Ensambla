import { describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { consultarCatalogos } from '@/api/catalogosProductos';
import { server } from '@/mocks/node';

describe('comparación de catálogos públicos', () => {
  it('mantiene las coincidencias separadas y no envía credenciales del negocio', async () => {
    server.use(
      http.get('/catalogos/sepa/77903754', ({ request }) => {
        expect(request.credentials).toBe('omit');
        return HttpResponse.json({
          producto: {
            codigo: '77903754',
            nombre: 'Guaymallén',
            marca: 'Guaymallén',
            presentacion: '',
          },
        });
      }),
      http.get('/catalogos/openfoodfacts/77903754', () =>
        HttpResponse.json({
          product: {
            code: '77903754',
            product_name: 'Guaymallén de oro',
            brands: 'Guaymallén',
            image_front_url: 'https://images.openfoodfacts.org/images/products/test.jpg',
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
      http.get('/catalogos/sepa/3017620422003', () => HttpResponse.json({ producto: null })),
      http.get(
        '/catalogos/openfoodfacts/3017620422003',
        () => new HttpResponse(null, { status: 503 }),
      ),
    );
    const resultado = await consultarCatalogos('3017620422003', new AbortController().signal);
    expect(resultado.fuentes.map((r) => r.estado)).toEqual(['no_encontrado', 'no_disponible']);
  });
  it('rechaza una ficha con otro código e imágenes ajenas al catálogo', async () => {
    server.use(
      http.get('/catalogos/sepa/77905741', () =>
        HttpResponse.json({
          producto: { codigo: '77903754', nombre: 'Otro', marca: '', presentacion: '' },
        }),
      ),
      http.get('/catalogos/openfoodfacts/77905741', () =>
        HttpResponse.json({
          product: {
            code: '77905741',
            product_name: 'Jorgito',
            image_front_url: 'https://un-sitio-ajeno.test/image',
          },
        }),
      ),
    );
    const resultado = await consultarCatalogos('77905741', new AbortController().signal);
    expect(resultado.fuentes[0]?.estado).toBe('no_disponible');
    expect(resultado.fuentes[1]?.producto?.imagen).toBeUndefined();
  });
});
