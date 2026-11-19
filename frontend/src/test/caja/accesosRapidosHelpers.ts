import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect } from 'vitest';
import { renderApp } from '@/test/renderApp';
import {
  givenDosNegociosConCaja,
  givenNegocioRecordado,
  whenEligeNegocio,
  ALMACEN_DE_PRUEBA,
} from '@/test/caja/negociosCajaHelpers';
import { ESPERA } from '@/test/testHelpers';

export async function givenInicioConNegocioElegido() {
  const { kiosco } = givenDosNegociosConCaja();
  givenNegocioRecordado(kiosco.id);
  const router = renderApp('/inicio');
  await screen.findByRole('heading', { name: /En qué puedo/ }, ESPERA);
  return { router, kiosco, user: userEvent.setup() };
}
type Caso = Awaited<ReturnType<typeof givenInicioConNegocioElegido>>;
export const whenAbreAccesos = (caso: Caso) =>
  caso.user.click(screen.getByRole('button', { name: 'Accesos rápidos' }));
export const whenEligeVenta = (caso: Caso) =>
  caso.user.click(screen.getByRole('button', { name: 'Nueva venta' }));
export const thenPrimeraAccionEnfocada = () =>
  expect(screen.getByRole('button', { name: 'Nueva venta' })).toHaveFocus();
export const whenEligeInventario = (caso: Caso) =>
  caso.user.click(
    within(screen.getByRole('navigation', { name: 'Acciones de venta y stock' })).getByRole(
      'button',
      { name: /Escanear Inventario/ },
    ),
  );
export async function thenCajaSinFlotante() {
  await screen.findByRole('heading', { name: 'Caja', level: 1 }, ESPERA);
  expect(screen.queryByRole('button', { name: 'Accesos rápidos' })).toBeNull();
}
export async function thenLectorDelNegocioActual(caso: Caso) {
  await screen.findByRole('heading', { name: 'Escaneá, revisá y agregá.' }, ESPERA);
  expect(caso.router.state.location.pathname).toBe(`/sistemas/${caso.kiosco.id}`);
  expect(screen.queryByRole('button', { name: 'Accesos rápidos' })).toBeNull();
}
export const whenCierraConEscape = (caso: Caso) => caso.user.keyboard('{Escape}');
export const whenCambiaNegocioConAccesosAbiertos = () => whenEligeNegocio(ALMACEN_DE_PRUEBA);
export const whenSaleConTeclado = (caso: Caso) => caso.user.keyboard('{Shift>}{Tab}{/Shift}');
export function thenMenuDeAccesosCerrado() {
  expect(screen.getByRole('button', { name: 'Accesos rápidos' })).toHaveAttribute(
    'aria-expanded',
    'false',
  );
  expect(screen.queryByRole('button', { name: 'Nueva venta' })).toBeNull();
}
export function thenRecuperaFoco() {
  expect(screen.getByRole('button', { name: 'Accesos rápidos' })).toHaveFocus();
  expect(screen.getByRole('button', { name: 'Accesos rápidos' })).toHaveAttribute(
    'aria-expanded',
    'false',
  );
  expect(screen.queryByRole('button', { name: 'Nueva venta' })).toBeNull();
}
