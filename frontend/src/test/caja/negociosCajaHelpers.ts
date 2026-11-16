import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect } from 'vitest';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';
import { prepararCaja } from '@/lib/caja';
import { guardarNegocioActivo } from '@/lib/negocios/lista';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupNegocios, setupUsuarioConSesion } from '@/test/testHelpers';

const ALMACEN = 'Almacén del centro';
const KIOSCO = 'Kiosco de la plaza';
const GASEOSA = /Gaseosa del almacén.*1\.000/;
const ALFAJOR = /Alfajor del kiosco.*1\.000/;

function crearNegocio(nombre: string, producto: string) {
  const sistema = makeTemplate('shop', {}, false);
  sistema.name = nombre;
  const inventario = allBlocks(sistema).find((bloque) => bloque.type === 'inventory');
  if (!inventario) throw new Error('Falta el inventario del escenario');
  inventario.rows = [
    { id: crypto.randomUUID(), name: producto, quantity: 10, price: 1000, favorite: true },
  ];
  prepararCaja(sistema, inventario.id);
  return sistema;
}

export function givenDosNegociosConCaja() {
  const usuario = setupUsuarioConSesion();
  const almacen = crearNegocio(ALMACEN, 'Gaseosa del almacén');
  const kiosco = crearNegocio(KIOSCO, 'Alfajor del kiosco');
  setupNegocios(usuario.email, almacen, kiosco);
  return { almacen, kiosco };
}

export const givenNegocioRecordado = (id: string) => guardarNegocioActivo(id);

export async function whenEntraDesdeInicio() {
  renderApp('/inicio');
  await screen.findByRole('heading', { name: /En qué puedo/ }, ESPERA);
  await whenAbreCaja();
}

export async function whenAbreCaja() {
  await userEvent.setup().click(screen.getByRole('button', { name: 'Caja' }));
  await screen.findByRole('heading', { name: 'Caja', level: 1 }, ESPERA);
}

async function mostrarFavoritos() {
  await userEvent.setup().click(await screen.findByRole('tab', { name: 'Favoritos' }, ESPERA));
}

export async function whenAgregaGaseosa() {
  await mostrarFavoritos();
  await userEvent.setup().click(screen.getByRole('button', { name: GASEOSA }));
}

export async function whenEligeNegocio(nombre: string) {
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: /Negocio activo:/ }));
  await user.click(
    within(screen.getByRole('menu')).getByRole('menuitem', { name: new RegExp(nombre) }),
  );
  await screen.findByRole('heading', { name: /En qué puedo/ }, ESPERA);
}

export function thenNavegacionConservaCajaYNegocios() {
  const menu = within(screen.getByRole('navigation', { name: 'Principal' }));
  expect(
    menu
      .getAllByRole('button')
      .slice(0, 3)
      .map((boton) => boton.textContent),
  ).toEqual(['Inicio', 'Caja', 'Avisos']);
  expect(menu.getByRole('button', { name: 'Caja' })).toHaveAttribute('aria-current', 'page');
  expect(menu.queryByRole('button', { name: /^(Mi Negocio|Mis cuadernos)$/ })).toBeNull();
  expect(
    screen.getByRole('button', { name: new RegExp(`Negocio activo: ${ALMACEN}`) }),
  ).toBeVisible();
}

export async function thenSoloVeProductosDelKiosco() {
  await mostrarFavoritos();
  expect(await screen.findByRole('button', { name: ALFAJOR }, ESPERA)).toBeVisible();
  expect(screen.queryByRole('button', { name: GASEOSA })).toBeNull();
  expect(screen.queryByLabelText('Cantidad de Gaseosa del almacén')).toBeNull();
}

export function thenRecuperaCompraDelAlmacen() {
  expect(screen.getByLabelText('Cantidad de Gaseosa del almacén')).toHaveValue('1');
  expect(screen.queryByLabelText('Cantidad de Alfajor del kiosco')).toBeNull();
}

export const ALMACEN_DE_PRUEBA = ALMACEN;
export const KIOSCO_DE_PRUEBA = KIOSCO;
