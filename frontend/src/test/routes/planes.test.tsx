import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { delay, HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import { estado } from '@/mocks/db/estado';
import { planesDe } from '@/mocks/db/planes';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';
import type { PruebaPremium } from '@/types/planes';

const pruebaDeEjemplo = (paginas = 3, vencida = false): PruebaPremium => {
  const ahora = Date.now();
  return {
    inicioEn: new Date(ahora - (vencida ? 8 : 2) * 86400000).toISOString(),
    finEn: new Date(ahora + (vencida ? -1 : 5) * 86400000).toISOString(),
    paginasIaUtilizadas: paginas,
    limitePaginasIa: 10,
  };
};
const esperarGratuito = () =>
  screen.findByRole('heading', { name: 'Gratuito, tu espacio actual' }, ESPERA);
const esperarPrueba = () =>
  screen.findByRole('heading', { name: 'Tu prueba Premium está activa' }, ESPERA);
const botonPrueba = () => screen.getByRole('button', { name: 'Probar Premium por 7 días' });

describe('planes y activación de prueba', () => {
  it('compara límites y muestra el precio aprobado cada treinta días', async () => {
    setupUsuarioConSesion();
    renderApp('/planes');
    await esperarGratuito();

    const premium = screen.getByRole('article', { name: 'Plan Premium' });
    const gratuito = screen.getByRole('article', { name: 'Plan Gratuito' });
    expect(within(gratuito).getByText('1.000')).toBeVisible();
    expect(within(gratuito).getByText('Sin costo')).toBeVisible();
    expect(within(premium).getByText('100')).toBeVisible();
    expect(within(premium).getByText(/14\.900/)).toBeVisible();
    expect(within(premium).getByText('Cada 30 días · renovación automática')).toBeVisible();
    expect(botonPrueba()).toBeEnabled();
    expect(document.title).toBe('Planes · Ensambla');
  });

  it('se puede entrar desde la navegación principal', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/inicio');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Planes' }, ESPERA));
    await esperarGratuito();

    expect(router.state.location.pathname).toBe('/planes');
    expect(screen.getByRole('button', { name: 'Planes' })).toHaveAttribute('aria-current', 'page');
  });

  it('permite cancelar con Escape y devuelve el foco sin activar la prueba', async () => {
    setupUsuarioConSesion();
    renderApp('/planes');
    await esperarGratuito();
    const user = userEvent.setup();

    await user.click(botonPrueba());
    const dialogo = screen.getByRole('dialog', { name: 'Probá Premium durante 7 días' });
    expect(within(dialogo).getByText(/10 páginas de IA en total/)).toBeVisible();
    await user.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(botonPrueba()).toHaveFocus();
    expect(estado.pruebasPremium.size).toBe(0);
  });

  it('activa una vez con CSRF, refresca el estado y protege el doble clic', async () => {
    const usuario = setupUsuarioConSesion();
    let intentos = 0;
    let csrf: string | null = null;
    let cuerpo = '';
    server.use(
      http.post('/api/planes/prueba', async ({ request }) => {
        intentos++;
        csrf = request.headers.get('X-CSRF-TOKEN');
        cuerpo = await request.text();
        await delay(180);
        const prueba = pruebaDeEjemplo(0);
        estado.pruebasPremium.set(usuario.email, prueba);
        return HttpResponse.json(prueba);
      }),
    );
    renderApp('/planes');
    await esperarGratuito();
    const user = userEvent.setup();

    await user.click(botonPrueba());
    await user.dblClick(screen.getByRole('button', { name: 'Activar prueba gratis' }));
    await esperarPrueba();
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull(), ESPERA);

    expect(intentos).toBe(1);
    expect(csrf).toBe('csrf-token-de-prueba');
    expect(cuerpo).toBe('');
    expect(screen.getByRole('button', { name: 'Prueba activa' })).toBeDisabled();
    expect(screen.getByText(/Las diez páginas son para toda la prueba/)).toBeVisible();
    expect(document.getElementById('main')).toHaveFocus();
  });

  it('al agotar diez páginas conserva premium y muestra el aviso del cupo total', async () => {
    const usuario = setupUsuarioConSesion();
    estado.pruebasPremium.set(usuario.email, pruebaDeEjemplo(10));
    renderApp('/planes');

    await esperarPrueba();

    expect(screen.getByText(/Usaste las páginas de IA incluidas/)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Prueba activa' })).toBeDisabled();
    expect(screen.getByRole('progressbar', { name: 'Páginas de IA de la prueba' })).toHaveValue(10);
  });

  it('muestra la prueba finalizada, conserva los datos y no ofrece reiniciarla', async () => {
    const usuario = setupUsuarioConSesion();
    estado.pruebasPremium.set(usuario.email, pruebaDeEjemplo(10, true));
    renderApp('/planes');

    await esperarGratuito();

    expect(screen.getByText(/Tu prueba terminó/)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Prueba ya utilizada' })).toBeDisabled();
    expect(screen.getByText(/conservás tus sistemas, registros y recetas/)).toBeVisible();
  });

  it('muestra premium vigente con cien páginas y sin ofrecer otra prueba', async () => {
    const usuario = setupUsuarioConSesion();
    const datos = planesDe(usuario.email);
    datos.suscripcion = {
      ...datos.suscripcion,
      codigoPlanActual: 'PREMIUM',
      puedeIniciarPrueba: false,
    };
    datos.uso.limitePaginasIa = 100;
    server.use(http.get('/api/planes', () => HttpResponse.json(datos)));
    renderApp('/planes');

    await screen.findByRole('heading', { name: 'Premium, tu espacio actual' }, ESPERA);

    expect(screen.getByRole('button', { name: 'Premium activo' })).toBeDisabled();
    expect(screen.getByRole('progressbar', { name: 'Páginas de IA de este mes' })).toHaveAttribute(
      'max',
      '100',
    );
  });

  it('un error inicial permite reintentar sin mostrar un plan inventado', async () => {
    const usuario = setupUsuarioConSesion();
    setupServerError('get', '/planes', 503, 'No pudimos consultar los planes.');
    renderApp('/planes');

    expect(await screen.findByRole('alert', {}, ESPERA)).toHaveTextContent(
      'No pudimos consultar los planes.',
    );
    expect(screen.queryByRole('article', { name: 'Plan Premium' })).toBeNull();
    server.use(http.get('/api/planes', () => HttpResponse.json(planesDe(usuario.email))));
    await userEvent.setup().click(screen.getByRole('button', { name: 'Reintentar' }));

    await esperarGratuito();
  });

  it('una activación rechazada no habilita premium ni se reintenta como CSRF', async () => {
    setupUsuarioConSesion();
    let intentos = 0;
    server.use(
      http.post('/api/planes/prueba', () => {
        intentos++;
        return HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 403,
            error: 'Forbidden',
            message: 'Este negocio ya utilizó su prueba premium.',
            path: '/api/planes/prueba',
            traceId: 'rechazo',
            codigo: 'PRUEBA_UTILIZADA',
          },
          { status: 403 },
        );
      }),
    );
    renderApp('/planes');
    await esperarGratuito();
    const user = userEvent.setup();

    await user.click(botonPrueba());
    await user.click(screen.getByRole('button', { name: 'Activar prueba gratis' }));
    expect(await screen.findByRole('alert', {}, ESPERA)).toHaveTextContent(/ya utilizó su prueba/);

    expect(intentos).toBe(1);
    expect(screen.queryByRole('heading', { name: 'Tu prueba Premium está activa' })).toBeNull();
    expect(estado.pruebasPremium.size).toBe(0);
  });

  it('la página y la confirmación no introducen problemas de accesibilidad', async () => {
    setupUsuarioConSesion();
    renderApp('/planes');
    await esperarGratuito();
    const opciones = {
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    };

    expect((await axe(document.body, opciones)).violations.map((item) => item.id)).toEqual([]);
    await userEvent.setup().click(botonPrueba());

    expect((await axe(document.body, opciones)).violations.map((item) => item.id)).toEqual([]);
  });
});
