import { screen, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import type * as ConfigEnv from '@/config/env';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';
import { avisoDelServidor } from '@/test/avisosData';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

// Con el backend real (VITE_API_MOCKS distinto de «completo») el motor del servidor es el dueño de los avisos de
// estancado. Los tests corren con todo simulado, así que acá sólo se cambia ese modo.
vi.mock('@/config/env', async (importOriginal) => {
  const original = await importOriginal<typeof ConfigEnv>();
  return { ...original, env: { ...original.env, mocks: 'ninguno' as const } };
});

let email = '';
let rutaDelAlmacen = '';
let liberarLaRespuesta: () => void = () => {};

describe('avisos con el motor del servidor activo', () => {
  it('un aviso de estancado descartado no vuelve como copia del navegador', async () => {
    dadoUnAlmacenConUnAvisoDeEstancadoDescartado();

    await cuandoSeAbreElAlmacen();

    entoncesNoHayCopiasDelNavegadorDeEstancado();
  });

  it('un aviso de estancado descartado tampoco trae copias de stock', async () => {
    dadoUnAlmacenConUnAvisoDeEstancadoDescartado();

    await cuandoSeAbreElAlmacen();

    entoncesNoHayCopiasDelNavegadorDe('stock');
  });

  it('mientras el servidor todavía no contestó, tampoco aparecen copias del navegador', async () => {
    dadoUnAlmacen();
    dadoUnServidorQueRetieneLaRespuestaDeAvisos();

    await cuandoSeAbreElAlmacen();

    entoncesNoHayCopiasDelNavegadorDeEstancado();
  });

  it('cuando el servidor contesta siguen sin aparecer copias del navegador', async () => {
    dadoUnAlmacen();
    dadoUnServidorQueRetieneLaRespuestaDeAvisos();
    await cuandoSeAbreElAlmacen();

    await cuandoElServidorContesta();

    entoncesNoHayCopiasDelNavegadorDeEstancado();
  });

  it('si el servidor no responde, siguen los avisos de estancado del navegador', async () => {
    dadoUnAlmacen();
    dadoUnServidorQueFalla();

    await cuandoSeAbreElAlmacenSinEsperarAlAcceso();

    await entoncesHayCopiasDelNavegadorDeEstancado();
  });
});

const delNavegador = (tipo: string) =>
  useSistemaStore.getState().avisos.some((aviso) => aviso.tipo === tipo && !aviso.servidorId);

// Con el motor activo el servidor genera todos los tipos y el navegador no muestra ninguno propio: se espera a que el
// plan haya habilitado los avisos (el acceso se resolvió) para saber que los del navegador ya tuvieron su oportunidad.
const hayDelNavegador = () => useSistemaStore.getState().accesoAvisos !== null;

const hayDelServidor = () => useSistemaStore.getState().avisos.some((aviso) => aviso.servidorId);

function dadoUnAlmacen() {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  email = usuario.email;
  estado.pruebasPremium.set(email, {
    inicioEn: new Date(Date.now() - 1000).toISOString(),
    finEn: new Date(Date.now() + 7 * 86400000).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });
  const sistema = crearAlmacenDeEjemplo(hoy());
  estado.sistemas.set(email, [sistema]);
  rutaDelAlmacen = `/sistemas/${sistema.id}?modo=usar`;
}

function dadoUnAlmacenConUnAvisoDeEstancadoDescartado() {
  dadoUnAlmacen();
  // El servidor respondió bien pero ya no tiene nada pendiente: el aviso se descartó.
  estado.avisosDelServidor.set(email, [
    avisoDelServidor({ tipo: 'ESTANCADO', grupo: 'VENTAS', estado: 'DESCARTADO' }),
  ]);
}

function dadoUnServidorQueRetieneLaRespuestaDeAvisos() {
  // La respuesta del servidor queda retenida hasta que el test la libera.
  const retenida = new Promise<void>((resolver) => {
    liberarLaRespuesta = resolver;
  });
  server.use(
    http.get('/api/avisos', async () => {
      await retenida;
      return HttpResponse.json([avisoDelServidor({ tipo: 'STOCK', grupo: 'STOCK' })]);
    }),
  );
}

function dadoUnServidorQueFalla() {
  setupServerError('get', '/avisos', 500, 'Error interno');
}

async function cuandoSeAbreElAlmacenSinEsperarAlAcceso() {
  renderApp(rutaDelAlmacen);
  await screen.findByRole('heading', { level: 1, name: /Almacén de ejemplo/ }, ESPERA);
}

async function cuandoSeAbreElAlmacen() {
  await cuandoSeAbreElAlmacenSinEsperarAlAcceso();
  // Los del navegador llegan cuando el plan los habilita: se espera a uno que sí se muestra.
  await waitFor(() => expect(hayDelNavegador()).toBe(true), ESPERA);
}

async function cuandoElServidorContesta() {
  liberarLaRespuesta();
  await waitFor(() => expect(hayDelServidor()).toBe(true), ESPERA);
}

function entoncesNoHayCopiasDelNavegadorDeEstancado() {
  entoncesNoHayCopiasDelNavegadorDe('estancado');
}

function entoncesNoHayCopiasDelNavegadorDe(tipo: string) {
  expect(delNavegador(tipo)).toBe(false);
}

async function entoncesHayCopiasDelNavegadorDeEstancado() {
  await waitFor(() => expect(delNavegador('estancado')).toBe(true), ESPERA);
}
