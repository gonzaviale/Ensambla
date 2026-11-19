import { screen, waitFor, within } from '@testing-library/react';
import { expect } from 'vitest';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';
import type { Bloque, Fila, Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';
import userEvent from '@testing-library/user-event';

const FILAS_GUARDADAS = 25;
const FILAS_NUEVAS = 3;
const TOTAL_DE_FILAS = FILAS_GUARDADAS + FILAS_NUEVAS;
const PESTANAS = 'Pestañas de la sección';

let user: ReturnType<typeof userEvent.setup>;
let clave = 'name';

const persona = () => user;

const inventarioDe = (sistema: Sistema | undefined): Bloque => {
  const bloque = allBlocks(sistema).find((item) => item.type === 'inventory');
  if (!bloque) throw new Error('Falta el inventario');
  return bloque;
};

const borrador = () => useSistemaStore.getState().taller?.borrador ?? undefined;

const bloqueDeInventario = () => {
  const nombre = inventarioDe(borrador()).name;
  const bloque = screen
    .getByRole('button', { name: new RegExp(`^${nombre}`) })
    .closest<HTMLElement>('.block');
  if (!bloque) throw new Error(`Falta el bloque ${nombre}`);
  return bloque;
};

const filasDe = (bloque: HTMLElement) => bloque.querySelectorAll('tbody tr[data-record]');

const botonParaMostrarMas = /^Mostrar \d+ más/;

const esperarLosRegistrosNuevos = () =>
  waitFor(() => expect(bloqueDeInventario().querySelector('tr.fila-nueva')).not.toBeNull());

/** Lo que Sami cargaría de una foto: registros reales que no estaban en la sección guardada. */
const cargarComoSami = () =>
  useSistemaStore.getState().editarBorrador((propuesta) => {
    const bloque = inventarioDe(propuesta);
    const nuevas: Fila[] = Array.from({ length: FILAS_NUEVAS }, (_, indice) => ({
      id: `nueva-${indice}`,
      [clave]: `Producto de la foto ${indice}`,
    }));
    bloque.rows = [...bloque.rows, ...nuevas];
  });

/* dado */

/** Un almacén ya creado con muchos productos guardados, abierto en el taller de Sami. */
export const dadoUnAlmacenConMuchosRegistrosEnElTaller = async () => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const sistema = makeTemplate('shop', {}, false);
  sistema.name = 'Almacén de Ana';
  const inventario = inventarioDe(sistema);
  clave = inventario.fields[0]?.key ?? 'name';
  inventario.rows = Array.from({ length: FILAS_GUARDADAS }, (_, indice) => ({
    id: `guardada-${indice}`,
    [clave]: `Producto guardado ${indice}`,
  }));
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp(`/taller?sistema=${sistema.id}`);
  await verifyHeadingPresentAsync('Construyamos juntos');
  await screen.findByLabelText('Nombre de tu sección', {}, ESPERA);
  user = userEvent.setup();
};

export const cuandoSeAbreUnAlmacenConMuchosRegistros = dadoUnAlmacenConMuchosRegistrosEnElTaller;

export const dadoQueSamiCargoRegistrosNuevosDeUnaFoto = async () => {
  cargarComoSami();
  await esperarLosRegistrosNuevos();
};

export const dadoQueSeMostraronDiezRegistrosMas = () =>
  persona().click(within(bloqueDeInventario()).getByRole('button', { name: botonParaMostrarMas }));

export const dadoQueLaSeccionTieneOtraPestana = async () => {
  useSistemaStore.getState().editarBorrador((propuesta) => {
    propuesta.sections.push({ id: 'otra', name: 'Ventas', blocks: [] });
  });
  await screen.findByRole('navigation', { name: PESTANAS });
};

/* cuando */

export const cuandoSamiCargaRegistrosNuevosDeUnaFoto = dadoQueSamiCargoRegistrosNuevosDeUnaFoto;

export const cuandoSePideMostrarMasRegistros = dadoQueSeMostraronDiezRegistrosMas;

export const cuandoSePideMostrarMasRegistrosDosVeces = async () => {
  await dadoQueSeMostraronDiezRegistrosMas();
  await dadoQueSeMostraronDiezRegistrosMas();
};

export const cuandoSeBuscaEnElBloque = (texto: string) =>
  persona().type(within(bloqueDeInventario()).getByRole('textbox'), texto);

export const cuandoSeAgregaLaPestana = async (nombre: string) => {
  await persona().click(screen.getByRole('button', { name: 'Agregar y organizar' }));
  await persona().click(screen.getByRole('button', { name: 'Agregar una pestaña' }));
  const dialogo = await screen.findByRole('dialog');
  await persona().type(within(dialogo).getByLabelText('Nombre de la pestaña'), nombre);
  await persona().click(within(dialogo).getByRole('button', { name: 'Agregar pestaña' }));
};

export const cuandoSeQuitaLaPestanaSobrante = async () => {
  await persona().click(screen.getByRole('button', { name: 'Agregar y organizar' }));
  await persona().click(screen.getByRole('button', { name: 'Pestañas' }));
  const dialogo = await screen.findByRole('dialog');
  const quitar = within(dialogo).getAllByRole('button', {
    name: 'Quitar pestaña y conservar bloques',
  });
  await persona().click(quitar[1] as HTMLElement);
  await waitFor(() => expect(borrador()?.sections).toHaveLength(1));
  await clickButton(persona(), 'Guardar nombres');
};

/* entonces */

export const entoncesLosRegistrosNuevosVanPrimeroResaltadosYSeAvisaCuantosSon = async () => {
  await esperarLosRegistrosNuevos();
  const bloque = bloqueDeInventario();
  const primeras = [...filasDe(bloque)].slice(0, FILAS_NUEVAS);
  expect(primeras.map((fila) => fila.getAttribute('data-record'))).toEqual([
    'nueva-0',
    'nueva-1',
    'nueva-2',
  ]);
  expect(within(bloque).getAllByText('Nuevo')).toHaveLength(FILAS_NUEVAS);
  expect(within(bloque).getByText(`${FILAS_NUEVAS} nuevos`)).toBeVisible();
  expect(bloque.querySelectorAll('tr.fila-nueva')).toHaveLength(FILAS_NUEVAS);
};

export const entoncesSeMuestranDiezRegistrosConScrollDentroDelBloque = () => {
  const bloque = bloqueDeInventario();
  expect(filasDe(bloque)).toHaveLength(10);
  expect(bloque.querySelector('.table-scroll')).not.toBeNull();
  expect(
    within(bloque).getByRole('button', { name: `Mostrar 10 más · quedan ${TOTAL_DE_FILAS - 10}` }),
  ).toBeVisible();
};

export const entoncesSeMuestranVeinteRegistros = () => {
  expect(filasDe(bloqueDeInventario())).toHaveLength(20);
};

export const entoncesSeMuestranTodosLosRegistrosYDesapareceElBoton = () => {
  const bloque = bloqueDeInventario();
  expect(filasDe(bloque)).toHaveLength(TOTAL_DE_FILAS);
  expect(within(bloque).queryByRole('button', { name: botonParaMostrarMas })).toBeNull();
};

export const entoncesElBuscadorFiltraSobreTodosLosRegistros = () => {
  const bloque = bloqueDeInventario();
  expect(filasDe(bloque)).toHaveLength(1);
  expect(within(bloque).queryByRole('button', { name: botonParaMostrarMas })).toBeNull();
};

export const entoncesNoSeMuestranLasPestanas = () => {
  expect(screen.queryByRole('navigation', { name: PESTANAS })).toBeNull();
  expect(screen.queryByText('Vista general')).toBeNull();
};

export const entoncesSeMuestranLasDosPestanasSoloEnLaPropuesta = async () => {
  const pestanas = await screen.findByRole('navigation', { name: PESTANAS });
  expect(within(pestanas).getAllByRole('button')).toHaveLength(2);
  expect(borrador()?.sections).toHaveLength(2);
  expect(useSistemaStore.getState().sistemas[0]?.sections).toHaveLength(1);
};

export const entoncesLaPestanaSeQuitaDeLaPropuesta = () => {
  expect(screen.queryByRole('navigation', { name: PESTANAS })).toBeNull();
  expect(useSistemaStore.getState().sistemas[0]?.sections).toHaveLength(1);
};

export const entoncesYaNoSeMuestranLosCartelesViejos = () => {
  expect(screen.queryByText('Distribuí tus piezas')).toBeNull();
  expect(screen.queryByText(/Sumar una pieza/)).toBeNull();
  expect(screen.queryByText(/sin guardar/i)).toBeNull();
  expect(screen.getByRole('button', { name: 'Distribución de los bloques' })).toBeVisible();
};
