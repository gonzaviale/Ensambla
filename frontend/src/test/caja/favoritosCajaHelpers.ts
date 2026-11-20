import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect } from 'vitest';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';
import { prepararCaja } from '@/lib/caja';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

const PRODUCTOS = [
  { id: crypto.randomUUID(), name: 'Coca-Cola', quantity: 10, price: 2700, favorite: true },
  { id: crypto.randomUUID(), name: 'Yerba', quantity: 4, price: 3500, favorite: false },
];

export async function dadoLaCajaConUnFavoritoYUnoQueNoLoEs() {
  const usuario = setupUsuarioConSesion();
  const sistema = makeTemplate('shop', {}, false);
  const inventario = allBlocks(sistema).find((bloque) => bloque.type === 'inventory');
  if (!inventario) throw new Error('Falta la sección de stock');
  inventario.rows = PRODUCTOS.map((producto) => ({ ...producto }));
  prepararCaja(sistema, inventario.id);
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp('/caja');
  await screen.findByRole('heading', { name: 'Caja', level: 1 }, ESPERA);
  return userEvent.setup();
}

export async function cuandoBuscaEnElStock(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('tab', { name: 'Buscar' }));
}

export function entoncesElFavoritoSeMarcaComoElegido(nombre: string) {
  expect(screen.getByRole('button', { name: `Quitar de favoritos: ${nombre}` })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
}

export function entoncesElQueNoEsFavoritoSePuedeAgregar(nombre: string) {
  expect(screen.getByRole('button', { name: `Agregar a favoritos: ${nombre}` })).toHaveAttribute(
    'aria-pressed',
    'false',
  );
}

const muestraElIconoDeAgregar = (boton: HTMLElement) =>
  boton.querySelector('use')?.getAttribute('href') === '#i-plus';

export function entoncesNingunaTarjetaMuestraElIconoDeAgregar() {
  const tarjetas = within(screen.getByRole('region', { name: 'Buscar productos' }));
  const favoritos = tarjetas.getAllByRole('button', { name: /favoritos:/ });
  expect(favoritos.filter(muestraElIconoDeAgregar)).toEqual([]);
}
