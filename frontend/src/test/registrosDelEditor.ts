import { screen, waitFor, within } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';
import { expect } from 'vitest';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';
import { verifyToastAsync } from '@/test/testHelpers';

const PRODUCTO = 'Alfajor Jorgito';

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

const filaDelProducto = () => {
  const fila = within(bloquePorNombre('Inventario')).getByText(PRODUCTO).closest('tr');
  if (!fila) throw new Error('Falta la fila');
  return fila;
};

const cantidadDelProducto = () =>
  tipoDeBloque('inventory').rows.find((fila) => fila.name === PRODUCTO)?.quantity;

export const dadoUnProductoRegistrado = async (user: UserEvent) => {
  await cuandoSeRegistraElProducto(user);
  await verifyToastAsync('Registro guardado.');
};

export const cuandoSeRegistraElProducto = async (user: UserEvent) => {
  await user.click(
    within(bloquePorNombre('Inventario')).getByRole('button', { name: /Registrar/ }),
  );
  const modal = await screen.findByRole('dialog');
  await user.type(within(modal).getByLabelText(/Producto/), PRODUCTO);
  await user.type(within(modal).getByLabelText(/Cantidad/), '12');
  await user.type(within(modal).getByLabelText('Precio'), '900');
  await user.click(within(modal).getByRole('button', { name: 'Guardar' }));
};

export const cuandoSeCorrigeLaCantidadA = async (user: UserEvent, cantidad: string) => {
  await user.click(within(filaDelProducto()).getByRole('button', { name: 'Editar registro' }));
  const edicion = await screen.findByRole('dialog');
  const campo = within(edicion).getByLabelText(/Cantidad/);
  await user.clear(campo);
  await user.type(campo, cantidad);
  await user.click(within(edicion).getByRole('button', { name: 'Guardar' }));
};

export const cuandoSeEliminaElProducto = async (user: UserEvent) => {
  await user.click(within(filaDelProducto()).getByRole('button', { name: 'Editar registro' }));
  await user.click(await screen.findByRole('button', { name: /Eliminar/ }));
  await user.click(
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
