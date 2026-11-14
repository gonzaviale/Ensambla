import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

const esperarPantalla = () =>
  screen.findByRole('heading', { name: 'Comparar mis negocios', level: 1 }, ESPERA);

describe('comparar mis negocios', () => {
  it('muestra los negocios de la cuenta con sus totales y la nota de la zona', async () => {
    const usuario = setupUsuarioConSesion();
    estado.negociosDeLaCuenta.set(usuario.email, {
      activoId: 'almacen',
      negocios: [
        {
          id: 'almacen',
          nombre: 'Almacén San Justo',
          rubro: 'ALMACEN',
          creadoEn: '2026-10-02T14:03:11Z',
        },
        {
          id: 'kiosco',
          nombre: 'Kiosco de la estación',
          rubro: 'KIOSCO',
          creadoEn: '2026-11-05T09:41:57Z',
        },
      ],
    });
    renderApp('/negocios/comparar');
    await esperarPantalla();

    const tabla = await screen.findByRole('table', { name: 'Negocio por negocio' }, ESPERA);
    expect(within(tabla).getByText('Almacén San Justo')).toBeVisible();
    expect(within(tabla).getByText('Kiosco de la estación')).toBeVisible();
    expect(screen.getByRole('region', { name: 'Toda la cuenta' })).toHaveTextContent('2 negocios');
    expect(screen.getByText(/últimos 30 días/)).toBeVisible();
    expect(screen.getByText(/se miran dentro de cada negocio/)).toBeVisible();
    expect(document.title).toBe('Comparar mis negocios · Ensambla');
  });

  it('pide el resumen de los últimos 30 días', async () => {
    setupUsuarioConSesion();
    let dias: string | null = null;
    server.use(
      http.get('/api/negocios/resumen', ({ request }) => {
        dias = new URL(request.url).searchParams.get('dias');
        return HttpResponse.json({
          desde: '2026-10-15',
          hasta: '2026-11-13',
          dias: 30,
          total: {
            negocios: 1,
            vendiste: 0,
            gastos: 0,
            queda: 0,
            teDeben: 0,
            avisosPendientes: 0,
          },
          negocios: [
            {
              id: 'almacen',
              nombre: 'Almacén San Justo',
              rubro: 'ALMACEN',
              activo: true,
              hayDatos: false,
              vendiste: null,
              gastos: null,
              queda: null,
              teDeben: null,
              avisosPendientes: 0,
            },
          ],
        });
      }),
    );
    renderApp('/negocios/comparar');

    expect(
      await screen.findByText(
        'Del 15 de octubre al 13 de noviembre: los últimos 30 días.',
        {},
        ESPERA,
      ),
    ).toBeVisible();
    expect(dias).toBe('30');
  });

  it('si el resumen falla avisa y deja probar de nuevo', async () => {
    setupUsuarioConSesion();
    setupServerError('get', '/negocios/resumen', 500, 'Error interno');
    renderApp('/negocios/comparar');
    await esperarPantalla();

    expect(await screen.findByRole('alert', {}, ESPERA)).toHaveTextContent(
      'No pudimos traer el resumen de tus negocios.',
    );
    server.resetHandlers();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Probar de nuevo' }));

    expect(await screen.findByRole('table', { name: 'Negocio por negocio' }, ESPERA)).toBeVisible();
  });
});
