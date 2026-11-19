import { screen, waitFor, within } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { expect } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyToastAsync } from '@/test/testHelpers';

const PRODUCTO = 'Alfajor Jorgito';

let persona: UserEvent;

export const abrirEditor = async (
  sistema: Sistema = makeTemplate('shop', {}, true),
  modo: 'construir' | 'usar' = 'construir',
) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  estado.sistemas.set(usuario.email, [sistema]);
  const router = renderApp(`/sistemas/${sistema.id}?modo=${modo}`);
  await screen.findByRole('heading', { level: 1, name: new RegExp(sistema.name) }, ESPERA);
  return { router, user: userEvent.setup(), sistema, usuario };
};

export const activo = () => {
  const sistema = seleccionarSistemaActivo(useSistemaStore.getState());
  if (!sistema) throw new Error('No hay un sistema activo');
  return sistema;
};

export const bloquePorNombre = (nombre: string) => {
  const nodo = [...document.querySelectorAll<HTMLElement>('.block')].find(
    (bloque) => bloque.getAttribute('aria-label') === nombre,
  );
  if (!nodo) throw new Error(`Falta el bloque ${nombre}`);
  return nodo;
};

export const tipoDeBloque = (tipo: string) => {
  const bloque = activo()
    .sections.flatMap((seccion) => seccion.blocks)
    .find((item) => item.type === tipo);
  if (!bloque) throw new Error(`Falta el bloque de tipo ${tipo}`);
  return bloque;
};

export const dadoUnCuadernoDeTiendaEnElEditor = async () => {
  persona = (await abrirEditor()).user;
};

export const dadoUnProductoRegistrado = async () => {
  await cuandoSeRegistraUnProductoNuevo();
  await verifyToastAsync('Registro guardado.');
};

export const cuandoSeRegistraUnProductoNuevo = async () => {
  await persona.click(
    within(bloquePorNombre('Inventario')).getByRole('button', { name: /Registrar/ }),
  );
  const modal = await screen.findByRole('dialog');
  await persona.type(within(modal).getByLabelText(/Producto/), PRODUCTO);
  await persona.type(within(modal).getByLabelText(/Cantidad/), '12');
  await persona.type(within(modal).getByLabelText('Precio'), '900');
  await persona.click(within(modal).getByRole('button', { name: 'Guardar' }));
};

export const cuandoSeCorrigeLaCantidadDelProductoA = async (cantidad: string) => {
  await persona.click(within(filaDelProducto()).getByRole('button', { name: 'Editar registro' }));
  const edicion = await screen.findByRole('dialog');
  const campo = within(edicion).getByLabelText(/Cantidad/);
  await persona.clear(campo);
  await persona.type(campo, cantidad);
  await persona.click(within(edicion).getByRole('button', { name: 'Guardar' }));
};

export const cuandoSeEliminaElProducto = async () => {
  await persona.click(within(filaDelProducto()).getByRole('button', { name: 'Editar registro' }));
  await persona.click(await screen.findByRole('button', { name: /Eliminar/ }));
  await persona.click(
    within(await screen.findByRole('dialog')).getByRole('button', { name: 'Eliminar' }),
  );
};

export const entoncesElInventarioMuestraElProducto = async () => {
  expect(await within(bloquePorNombre('Inventario')).findByText(PRODUCTO)).toBeVisible();
  await verifyToastAsync('Registro guardado.');
};

export const entoncesLaCantidadDelProductoEs = async (cantidad: number) => {
  await verifyToastAsync('Registro actualizado.');
  expect(cantidadDelProducto()).toBe(cantidad);
};

export const entoncesElInventarioYaNoMuestraElProducto = () =>
  waitFor(() => expect(within(bloquePorNombre('Inventario')).queryByText(PRODUCTO)).toBeNull());

const filaDelProducto = () => {
  const fila = within(bloquePorNombre('Inventario')).getByText(PRODUCTO).closest('tr');
  if (!fila) throw new Error('Falta la fila');
  return fila;
};

const cantidadDelProducto = () =>
  tipoDeBloque('inventory').rows.find((fila) => fila.name === PRODUCTO)?.quantity;
