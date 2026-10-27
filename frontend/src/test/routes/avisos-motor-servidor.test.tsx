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
// estancado y de combo. Los tests corren con todo simulado, así que acá sólo se cambia ese modo.
vi.mock('@/config/env', async (importOriginal) => {
  const original = await importOriginal<typeof ConfigEnv>();
  return { ...original, env: { ...original.env, mocks: 'ninguno' as const } };
});

const abrirAlmacen = async () => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  estado.pruebasPremium.set(usuario.email, {
    inicioEn: new Date(Date.now() - 1000).toISOString(),
    finEn: new Date(Date.now() + 7 * 86400000).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });
  const sistema = crearAlmacenDeEjemplo(hoy());
  estado.sistemas.set(usuario.email, [sistema]);
  return { usuario, sistema };
};

const delNavegador = (tipo: string) =>
  useSistemaStore.getState().avisos.some((aviso) => aviso.tipo === tipo && !aviso.servidorId);

describe('avisos con el motor del servidor activo', () => {
  it('un aviso de estancado descartado no vuelve como copia del navegador', async () => {
    const { usuario, sistema } = await abrirAlmacen();
    // El servidor respondió bien pero ya no tiene nada pendiente: el aviso se descartó.
    estado.avisosDelServidor.set(usuario.email, [
      avisoDelServidor({ tipo: 'ESTANCADO', grupo: 'VENTAS', estado: 'DESCARTADO' }),
    ]);
    renderApp(`/sistemas/${sistema.id}?modo=usar`);
    await screen.findByRole('heading', { level: 1, name: /Almacén de ejemplo/ }, ESPERA);
    // Los del navegador llegan cuando el plan los habilita: se espera a uno que sí se muestra.
    await waitFor(() => expect(delNavegador('stock')).toBe(true), ESPERA);

    expect(delNavegador('estancado')).toBe(false);
    expect(delNavegador('combo')).toBe(false);
  });

  it('mientras el servidor todavía no contestó, tampoco aparecen copias del navegador', async () => {
    const { sistema } = await abrirAlmacen();
    // La respuesta del servidor queda retenida hasta que el test la libera.
    let liberar: () => void = () => {};
    const retenida = new Promise<void>((resolver) => {
      liberar = resolver;
    });
    server.use(
      http.get('/api/avisos', async () => {
        await retenida;
        return HttpResponse.json([]);
      }),
    );
    renderApp(`/sistemas/${sistema.id}?modo=usar`);
    await screen.findByRole('heading', { level: 1, name: /Almacén de ejemplo/ }, ESPERA);
    await waitFor(() => expect(delNavegador('stock')).toBe(true), ESPERA);

    expect(delNavegador('estancado')).toBe(false);
    expect(delNavegador('combo')).toBe(false);

    liberar();
    await new Promise((resolver) => setTimeout(resolver, 150));
    expect(delNavegador('estancado')).toBe(false);
    expect(delNavegador('combo')).toBe(false);
  });

  it('si el servidor no responde, siguen los avisos de estancado y de combo del navegador', async () => {
    const { sistema } = await abrirAlmacen();
    setupServerError('get', '/avisos', 500, 'Error interno');
    renderApp(`/sistemas/${sistema.id}?modo=usar`);
    await screen.findByRole('heading', { level: 1, name: /Almacén de ejemplo/ }, ESPERA);

    await waitFor(() => expect(delNavegador('combo')).toBe(true), ESPERA);
    expect(delNavegador('estancado')).toBe(true);
  });
});
