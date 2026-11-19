import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  negocioDePrueba,
  setupNegocios,
  setupServerError,
  setupUsuarioConSesion,
} from '@/test/testHelpers';

const ALMACEN = {
  ...crearAlmacenCargado(hoy()),
  id: '10000000-0000-4000-8000-000000000001',
  name: 'Almacén San Justo',
};
const KIOSCO = negocioDePrueba('20000000-0000-4000-8000-000000000002', 'Kiosco de la estación');

const givenDosNegocios = () => {
  const usuario = setupUsuarioConSesion();
  setupNegocios(usuario.email, ALMACEN, KIOSCO);
};

const tabla = () => screen.findByRole('table', { name: 'Negocio por negocio' }, ESPERA);

const filaDe = async (nombre: string) => {
  const fila = within(await tabla())
    .getAllByRole('row')
    .find((candidata) => candidata.textContent?.includes(nombre));
  if (!fila) throw new Error(`No hay una fila de ${nombre}`);
  return fila;
};

describe('comparar mis negocios', () => {
  it('muestra un negocio por cuaderno, con los totales y la nota de la zona', async () => {
    givenDosNegocios();

    renderApp('/negocios/comparar');

    expect(within(await tabla()).getByText('Almacén San Justo')).toBeVisible();
    expect(within(await tabla()).getByText('Kiosco de la estación')).toBeVisible();
    expect(screen.getByRole('region', { name: 'Todos tus negocios juntos' })).toHaveTextContent('2 negocios');
    expect(screen.getByText(/últimos 30 días/)).toBeVisible();
    expect(screen.getByText(/entrando a cada negocio/)).toBeVisible();
    expect(document.title).toBe('Comparar mis negocios · Ensambla');
  });

  it('cada negocio trae los números de su propio cuaderno', async () => {
    givenDosNegocios();

    renderApp('/negocios/comparar');

    expect(await filaDe('Almacén San Justo')).toHaveTextContent('$');
    expect(await filaDe('Kiosco de la estación')).toHaveTextContent('Sin ventas en estos 30 días');
  });

  it('si el panel de un negocio falla avisa y deja probar de nuevo', async () => {
    givenDosNegocios();
    setupServerError('get', '/negocio/panel', 500, 'Error interno');

    renderApp('/negocios/comparar');

    expect(await screen.findByRole('alert', {}, ESPERA)).toHaveTextContent(
      'No pudimos traer el resumen de tus negocios.',
    );
    server.resetHandlers();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Probar de nuevo' }));
    expect(await tabla()).toBeVisible();
  });
});
