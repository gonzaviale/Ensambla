import { act, fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';
import { pedirMostrarFila } from '@/lib/sistema/tandas';
import type { Bloque } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

const FILAS = 25;

type Usuario = ReturnType<typeof userEvent.setup>;

let user: Usuario;
let bloque: HTMLElement;
let inventario: Bloque;

describe('las tablas de la sección se cargan de a poco', () => {
  it('muestran 10 registros con el scroll del bloque', async () => {
    await dadoUnSistemaConMuchosRegistros();

    entoncesSeVenFilas(10);
    entoncesElBloqueTieneScrollYDiceCuantosRegistrosHay();
  });

  it('suman de a 10 hasta el final', async () => {
    await dadoUnSistemaConMuchosRegistros();

    await cuandoSePideMostrarMas(`Mostrar 10 más · quedan ${FILAS - 10}`);
    entoncesSeVenFilas(20);

    await cuandoSePideMostrarMas('Mostrar 5 más · quedan 5');
    entoncesSeVenFilas(FILAS);
    entoncesYaNoSeOfreceMostrarMas();
  });

  it('al llegar al final del scroll del bloque se suma la tanda siguiente', async () => {
    await dadoUnSistemaConMuchosRegistros();

    cuandoSeLlegaAlFinalDelScroll();

    await entoncesSeVenFilasConEspera(20);
  });

  it('la búsqueda filtra sobre todos los registros, también los que todavía no se dibujaron', async () => {
    await dadoUnSistemaConMuchosRegistros();

    await cuandoSeBuscaProducto('Producto 24');

    entoncesSoloSeVeElProducto('Producto 24');
    entoncesYaNoSeOfreceMostrarMas();
  });

  it('al borrar la búsqueda vuelven los primeros 10 registros', async () => {
    await dadoUnSistemaConMuchosRegistros();
    await cuandoSeBuscaProducto('Producto 24');

    await cuandoSeBorraLaBusqueda();

    entoncesSeVenFilas(10);
  });

  it('un registro que todavía no se dibujó aparece cuando se pide llevar la vista a él', async () => {
    await dadoUnSistemaConMuchosRegistros();

    cuandoSePideLlevarLaVistaAlRegistro('fila-17');

    await entoncesApareceElRegistro('fila-17');
    entoncesSeVenFilas(18);
  });
});

/** Un almacén con muchos productos, abierto en su pantalla de siempre. */
async function dadoUnSistemaConMuchosRegistros() {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const sistema = makeTemplate('shop', {}, false);
  sistema.name = 'Almacén de Ana';
  inventario = allBlocks(sistema).find((candidato) => candidato.type === 'inventory') as Bloque;
  const clave = inventario.fields[0]?.key ?? 'name';
  inventario.rows = Array.from({ length: FILAS }, (_, indice) => ({
    id: `fila-${indice}`,
    [clave]: `Producto ${indice}`,
  }));
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp(`/sistemas/${sistema.id}?modo=usar`);
  await screen.findByRole('heading', { level: 1, name: /Almacén de Ana/ }, ESPERA);
  const encontrado = document.querySelector<HTMLElement>(`[data-block="${inventario.id}"]`);
  if (!encontrado) throw new Error('Falta el bloque del inventario');
  bloque = encontrado;
  user = userEvent.setup();
}

const filasVisibles = () => bloque.querySelectorAll('tbody tr[data-record]');

async function cuandoSePideMostrarMas(nombre: string) {
  await user.click(within(bloque).getByRole('button', { name: nombre }));
}

function cuandoSeLlegaAlFinalDelScroll() {
  const scroll = bloque.querySelector<HTMLElement>('.table-scroll');
  if (!scroll) throw new Error('Falta el scroll del bloque');
  Object.defineProperties(scroll, {
    scrollTop: { value: 400, configurable: true },
    clientHeight: { value: 375, configurable: true },
    scrollHeight: { value: 800, configurable: true },
  });
  fireEvent.scroll(scroll);
}

async function cuandoSeBuscaProducto(texto: string) {
  await user.type(within(bloque).getByRole('textbox'), texto);
}

async function cuandoSeBorraLaBusqueda() {
  await user.clear(within(bloque).getByRole('textbox'));
}

function cuandoSePideLlevarLaVistaAlRegistro(id: string) {
  expect(bloque.querySelector(`[data-record="${id}"]`)).toBeNull();
  act(() => pedirMostrarFila(inventario.id, id));
}

function entoncesSeVenFilas(cantidad: number) {
  expect(filasVisibles()).toHaveLength(cantidad);
}

async function entoncesSeVenFilasConEspera(cantidad: number) {
  await waitFor(() => expect(filasVisibles()).toHaveLength(cantidad));
}

function entoncesElBloqueTieneScrollYDiceCuantosRegistrosHay() {
  expect(bloque.querySelector('.table-scroll')).not.toBeNull();
  expect(within(bloque).getByText(`${FILAS} registros`)).toBeVisible();
}

function entoncesYaNoSeOfreceMostrarMas() {
  expect(within(bloque).queryByRole('button', { name: /^Mostrar \d+ más/ })).toBeNull();
}

function entoncesSoloSeVeElProducto(nombre: string) {
  expect(filasVisibles()).toHaveLength(1);
  expect(filasVisibles()[0]).toHaveTextContent(nombre);
}

async function entoncesApareceElRegistro(id: string) {
  await waitFor(() => expect(bloque.querySelector(`[data-record="${id}"]`)).not.toBeNull());
}
