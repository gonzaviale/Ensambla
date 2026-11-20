import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect } from 'vitest';
import { renderApp } from '@/test/renderApp';
import {
  dadoDosNegociosConCaja,
  dadoNegocioRecordado,
  cuandoEligeNegocio,
  ALMACEN_DE_PRUEBA,
} from '@/test/caja/negociosCajaHelpers';
import { ESPERA } from '@/test/testHelpers';

export async function dadoInicioConNegocioElegido() {
  const { kiosco } = dadoDosNegociosConCaja();
  dadoNegocioRecordado(kiosco.id);
  const router = renderApp('/inicio');
  await screen.findByRole('heading', { name: /En qué puedo/ }, ESPERA);
  return { router, kiosco, user: userEvent.setup() };
}
type Caso = Awaited<ReturnType<typeof dadoInicioConNegocioElegido>>;
export const cuandoAbreAccesos = (caso: Caso) =>
  caso.user.click(screen.getByRole('button', { name: 'Accesos rápidos' }));
export const cuandoEligeVenta = (caso: Caso) =>
  caso.user.click(screen.getByRole('button', { name: 'Nueva venta' }));
export const entoncesPrimeraAccionEnfocada = () =>
  expect(screen.getByRole('button', { name: 'Nueva venta' })).toHaveFocus();
export const cuandoEligeInventario = (caso: Caso) =>
  caso.user.click(
    within(screen.getByRole('navigation', { name: 'Acciones de venta y stock' })).getByRole(
      'button',
      { name: /^Escanear / },
    ),
  );
export const cuandoCierraConLaCruz = (caso: Caso) =>
  caso.user.click(screen.getByRole('button', { name: 'Cerrar accesos rápidos' }));
export function entoncesNoHayUnBotonDeTextoParaCerrar() {
  expect(screen.queryByRole('button', { name: 'Cerrar accesos' })).toBeNull();
}
export function entoncesLaCruzDeCerrarEstaVisible() {
  expect(screen.getByRole('button', { name: 'Cerrar accesos rápidos' })).toBeVisible();
}
export async function entoncesCajaSinFlotante() {
  await screen.findByRole('heading', { name: 'Caja', level: 1 }, ESPERA);
  expect(screen.queryByRole('button', { name: 'Accesos rápidos' })).toBeNull();
}
export async function entoncesLectorDelNegocioActual(caso: Caso) {
  await screen.findByRole('heading', { name: 'Escaneá, revisá y agregá.' }, ESPERA);
  expect(caso.router.state.location.pathname).toBe(`/sistemas/${caso.kiosco.id}`);
  expect(screen.queryByRole('button', { name: 'Accesos rápidos' })).toBeNull();
}
export const cuandoCierraConEscape = (caso: Caso) => caso.user.keyboard('{Escape}');
export const cuandoCambiaNegocioConAccesosAbiertos = () => cuandoEligeNegocio(ALMACEN_DE_PRUEBA);
export const cuandoSaleConTeclado = (caso: Caso) => caso.user.keyboard('{Shift>}{Tab}{/Shift}');
export function entoncesMenuDeAccesosCerrado() {
  expect(screen.getByRole('button', { name: 'Accesos rápidos' })).toHaveAttribute(
    'aria-expanded',
    'false',
  );
  expect(screen.queryByRole('button', { name: 'Nueva venta' })).toBeNull();
}
export function entoncesRecuperaFoco() {
  expect(screen.getByRole('button', { name: 'Accesos rápidos' })).toHaveFocus();
  expect(screen.getByRole('button', { name: 'Accesos rápidos' })).toHaveAttribute(
    'aria-expanded',
    'false',
  );
  expect(screen.queryByRole('button', { name: 'Nueva venta' })).toBeNull();
}
