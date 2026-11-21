import { afterEach, describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { consultarCatalogos } from '@/api/catalogosProductos';
import { server } from '@/mocks/node';
import { env } from '@/config/env';

type Comparacion = Awaited<ReturnType<typeof consultarCatalogos>>;
type FichaDeCatalogo = Record<string, string>;

let comparacion: Comparacion;
let credencialesEnviadas: string[] = [];
const urlOriginal = env.apiUrl;

describe('comparación de catálogos públicos', () => {
  afterEach(() => {
    env.apiUrl = urlOriginal;
  });

  it('respeta la URL configurada del backend y envía la sesión a ese servidor', async () => {
    dadoUnBackendEnOtraUrl();

    await cuandoSeConsultaElCodigo('7790895000997');

    entoncesLasFuentesSon('no_encontrado', 'no_encontrado');
    entoncesSeEnvioLaSesion();
  });

  it('mantiene las coincidencias separadas', async () => {
    dadoQueAmbasFuentesConocenElProducto('77903754');

    await cuandoSeConsultaElCodigo('77903754');

    entoncesLasFuentesSon('encontrado', 'encontrado');
  });

  it('manda la sesión sólo al backend de Ensambla y conserva la imagen del catálogo público', async () => {
    dadoQueAmbasFuentesConocenElProducto('77903761');

    await cuandoSeConsultaElCodigo('77903761');

    entoncesSeEnvioLaSesion();
    entoncesLaImagenViene('images.openfoodfacts.org');
  });

  it('una fuente caída no significa que el producto no exista y permite usar la otra', async () => {
    dadoQueUnaFuenteNoEncuentraYLaOtraEstaCaida();

    await cuandoSeConsultaElCodigo('3017620422003');

    entoncesLasFuentesSon('no_encontrado', 'no_disponible');
  });

  it('rechaza una ficha con otro código', async () => {
    dadoUnaFichaConOtroCodigoEImagenAjena();

    await cuandoSeConsultaElCodigo('77905741');

    entoncesLaPrimeraFuenteNoEstaDisponible();
  });

  it('descarta las imágenes ajenas al catálogo', async () => {
    dadoUnaFichaConOtroCodigoEImagenAjena();

    await cuandoSeConsultaElCodigo('77905741');

    entoncesNoHayImagen();
  });
});

function ficha(extra: FichaDeCatalogo) {
  return { codigo: '77903754', nombre: 'Guaymallén', marca: '', presentacion: '', ...extra };
}

const fichaDe = (codigo: string, extra: FichaDeCatalogo = {}) => ficha({ codigo, ...extra });

function dadoUnBackendEnOtraUrl() {
  env.apiUrl = 'https://backend.example/api';
  server.use(
    http.get('https://backend.example/api/catalogos/:fuente/7790895000997', ({ request }) => {
      credencialesEnviadas.push(request.credentials);
      return HttpResponse.json({ producto: null });
    }),
  );
}

function dadoQueAmbasFuentesConocenElProducto(codigo: string) {
  server.use(
    http.get(`/api/catalogos/sepa/${codigo}`, ({ request }) => {
      credencialesEnviadas.push(request.credentials);
      return HttpResponse.json({ producto: fichaDe(codigo, { marca: 'Guaymallén' }) });
    }),
    http.get(`/api/catalogos/openfoodfacts/${codigo}`, () =>
      HttpResponse.json({
        producto: fichaDe(codigo, {
          nombre: 'Guaymallén de oro',
          marca: 'Guaymallén',
          imagen: 'https://images.openfoodfacts.org/images/products/test.jpg',
        }),
      }),
    ),
  );
}

function dadoQueUnaFuenteNoEncuentraYLaOtraEstaCaida() {
  server.use(
    http.get('/api/catalogos/sepa/3017620422003', () => HttpResponse.json({ producto: null })),
    http.get(
      '/api/catalogos/openfoodfacts/3017620422003',
      () => new HttpResponse(null, { status: 503 }),
    ),
  );
}

function dadoUnaFichaConOtroCodigoEImagenAjena() {
  server.use(
    http.get('/api/catalogos/sepa/77905741', () =>
      HttpResponse.json({ producto: ficha({ nombre: 'Otro' }) }),
    ),
    http.get('/api/catalogos/openfoodfacts/77905741', () =>
      HttpResponse.json({
        producto: ficha({
          codigo: '77905741',
          nombre: 'Jorgito',
          imagen: 'https://un-sitio-ajeno.test/image',
        }),
      }),
    ),
  );
}

async function cuandoSeConsultaElCodigo(codigo: string) {
  credencialesEnviadas = [];
  comparacion = await consultarCatalogos(codigo, new AbortController().signal);
}

function entoncesLasFuentesSon(...estados: string[]) {
  expect(comparacion.fuentes.map((fuente) => fuente.estado)).toEqual(estados);
}

function entoncesSeEnvioLaSesion() {
  expect(credencialesEnviadas.length).toBeGreaterThan(0);
  expect(new Set(credencialesEnviadas)).toEqual(new Set(['include']));
}

function entoncesLaImagenViene(servidor: string) {
  expect(comparacion.fuentes[1]?.producto?.imagen).toContain(servidor);
}

function entoncesLaPrimeraFuenteNoEstaDisponible() {
  expect(comparacion.fuentes[0]?.estado).toBe('no_disponible');
}

function entoncesNoHayImagen() {
  expect(comparacion.fuentes[1]?.producto?.imagen).toBeUndefined();
}
