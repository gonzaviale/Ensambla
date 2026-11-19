import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect } from 'vitest';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';
import { guardarNegocioActivo } from '@/lib/negocios/lista';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupNegocios, setupUsuarioConSesion } from '@/test/testHelpers';

function crearNegocio(nombre: string) {
  const sistema = makeTemplate('shop', {}, false);
  sistema.name = nombre;
  const inventario = allBlocks(sistema).find((bloque) => bloque.type === 'inventory');
  if (!inventario) throw new Error('Falta la sección de stock del escenario');
  inventario.rows = [
    { id: crypto.randomUUID(), name: 'Café del negocio', quantity: 3, price: 1000 },
  ];
  return { sistema, inventario };
}

export function givenSegundoNegocioRecordado() {
  const usuario = setupUsuarioConSesion();
  const primero = crearNegocio('Almacén del centro');
  const elegido = crearNegocio('Kiosco de la plaza');
  setupNegocios(usuario.email, primero.sistema, elegido.sistema);
  guardarNegocioActivo(elegido.sistema.id);
  return elegido;
}

export async function whenAbreLectorDesdeInicio(inventario: { name: string }) {
  const router = renderApp('/inicio');
  const user = userEvent.setup();
  await screen.findByRole('heading', { name: /En qué puedo/ }, ESPERA);
  await user.click(screen.getByRole('button', { name: 'Mi negocio' }));
  await user.click(
    within(await screen.findByRole('region', { name: inventario.name }, ESPERA)).getByRole(
      'button',
      { name: 'Escanear productos' },
    ),
  );
  await screen.findByRole('heading', { name: 'Escaneá, revisá y agregá.' }, ESPERA);
  return { router, user };
}

type NavegacionDelLector = Awaited<ReturnType<typeof whenAbreLectorDesdeInicio>>;
type NegocioDePrueba = ReturnType<typeof givenSegundoNegocioRecordado>;

export function thenLectorDelNegocioElegido(caso: NavegacionDelLector, negocio: NegocioDePrueba) {
  expect(caso.router.state.location.pathname).toBe(`/sistemas/${negocio.sistema.id}`);
  expect(caso.router.state.location.search.escanear).toBe(negocio.inventario.id);
}

export async function whenVuelveAlInventario(caso: NavegacionDelLector, negocio: NegocioDePrueba) {
  await caso.user.click(screen.getByRole('button', { name: 'Volver al stock' }));
  await screen.findByRole('dialog', { name: negocio.inventario.name }, ESPERA);
}

export function thenConservaElNegocioAlVolver(caso: NavegacionDelLector, negocio: NegocioDePrueba) {
  expect(caso.router.state.location.pathname).toBe(`/sistemas/${negocio.sistema.id}`);
  expect(caso.router.state.location.search.escanear).toBeUndefined();
  expect(screen.getByRole('dialog', { name: negocio.inventario.name })).toBeVisible();
}

export async function whenVuelveAlInicio(caso: NavegacionDelLector) {
  const ubicacion = within(screen.getByRole('navigation', { name: 'Ubicación en el stock' }));
  await caso.user.click(ubicacion.getByRole('button', { name: 'Inicio' }));
  await screen.findByRole('heading', { name: /En qué puedo/ }, ESPERA);
}

export function thenInicioDelNegocioElegido(caso: NavegacionDelLector, negocio: NegocioDePrueba) {
  expect(caso.router.state.location.pathname).toBe('/inicio');
  expect(
    screen.getByRole('button', { name: `Negocio activo: ${negocio.sistema.name}` }),
  ).toBeVisible();
}
