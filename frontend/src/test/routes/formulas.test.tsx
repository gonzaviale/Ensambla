import { screen, waitFor, within } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { ApiError } from '@/api/apiError';
import { crearBloque } from '@/api/bloques';
import { crearCampo, editarCampo, listarCampos } from '@/api/campos';
import { crearProyecto } from '@/api/proyectos';
import { listarRegistros } from '@/api/registros';
import { estado } from '@/mocks/db/estado';
import { agregarRegistro } from '@/mocks/db/registros';
import type { GuardarCampo } from '@/types/campo';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';

const columna = (datos: Partial<GuardarCampo> & Pick<GuardarCampo, 'nombre'>): GuardarCampo => ({
  tipo: 'NUMERO',
  obligatorio: false,
  valorInicial: null,
  opciones: [],
  bloqueRelacionadoId: null,
  formula: null,
  ...datos,
});

/* setup: Ventas con Producto (texto), Cantidad y Precio (números) */

async function setupVentas() {
  setupUsuarioConSesion();
  const proyecto = await crearProyecto({ nombre: 'Mi almacén' });
  const ventas = await crearBloque({ proyectoId: proyecto.id, nombre: 'Ventas' });
  const producto = await crearCampo({
    bloqueId: ventas.id,
    datos: columna({ nombre: 'Producto', tipo: 'TEXTO' }),
  });
  const cantidad = await crearCampo({ bloqueId: ventas.id, datos: columna({ nombre: 'Cantidad' }) });
  const precio = await crearCampo({ bloqueId: ventas.id, datos: columna({ nombre: 'Precio' }) });
  return { proyecto, ventas, producto, cantidad, precio };
}

async function setupVentasConTotal() {
  const base = await setupVentas();
  const total = await crearCampo({
    bloqueId: base.ventas.id,
    datos: columna({
      nombre: 'Total',
      tipo: 'FORMULA',
      formula: `{${base.cantidad.id}} * {${base.precio.id}}`,
    }),
  });
  return { ...base, total };
}

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

async function abrirNuevaColumna(proyectoId: string, bloqueId: string, user: UserEvent) {
  renderApp(`/proyectos/${proyectoId}/construir?bloque=${bloqueId}`);
  await verifyHeadingPresentAsync('Tus tablas');
  await user.click(await screen.findByRole('button', { name: 'Agregar columna' }, ESPERA));
  return screen.getByRole('form', { name: 'Nueva columna' });
}

describe('ENS-166: la API simulada de columnas de cálculo', () => {
  it('guarda la fórmula y rechaza lo que el backend rechaza', async () => {
    const { ventas, producto, cantidad } = await setupVentas();
    const error = (formula: string | null, valorInicial: string | null = null) =>
      crearCampo({
        bloqueId: ventas.id,
        datos: columna({ nombre: 'Total', tipo: 'FORMULA', formula, valorInicial }),
      }).catch((e: unknown) => e);

    const creada = await crearCampo({
      bloqueId: ventas.id,
      datos: columna({ nombre: 'Doble', tipo: 'FORMULA', formula: `{${cantidad.id}} * 2` }),
    });
    expect(creada).toMatchObject({ tipo: 'FORMULA', formula: `{${cantidad.id}} * 2` });

    expect(await error(null)).toMatchObject({ status: 400, fieldErrors: { formula: 'Armá la fórmula' } });
    expect(await error(`{${cantidad.id}} *`)).toMatchObject({
      fieldErrors: { formula: 'La fórmula está incompleta' },
    });
    expect(await error(`{${producto.id}} * 2`)).toMatchObject({
      fieldErrors: { formula: 'Usá sólo campos numéricos de esta tabla' },
    });
    expect(await error('1 + 1', '3')).toMatchObject({
      fieldErrors: { valorInicial: 'Una fórmula no tiene valor inicial' },
    });
    expect(await error('1 +')).toBeInstanceOf(ApiError);
  });

  it('calcula el total al listar y no lo guarda en la fila', async () => {
    const { ventas, cantidad, precio, total } = await setupVentasConTotal();
    const fila = agregarRegistro(ventas.id, { [cantidad.id]: 3, [precio.id]: 12.5 });

    const pagina = await listarRegistros(ventas.id, {});

    expect(pagina.content[0]?.valores[total.id]).toBe(37.5);
    expect(estado.registros.get(fila.id)?.valores).not.toHaveProperty(total.id);
  });

  it('deja sin valor el cálculo al que le falta un dato', async () => {
    const { ventas, cantidad, total } = await setupVentasConTotal();
    agregarRegistro(ventas.id, { [cantidad.id]: 3 });

    const pagina = await listarRegistros(ventas.id, {});

    expect(pagina.content[0]?.valores).not.toHaveProperty(total.id);
  });

  it('acepta cambiar el cálculo de una columna existente', async () => {
    const { total, cantidad } = await setupVentasConTotal();

    const editada = await editarCampo({
      campoId: total.id,
      datos: columna({ nombre: 'Total', tipo: 'FORMULA', formula: `{${cantidad.id}} + 1` }),
    });

    expect(editada.formula).toBe(`{${cantidad.id}} + 1`);
  });
});

describe('ENS-166: armar el cálculo en el constructor', () => {
  it('arma «Cantidad × Precio» con botones y lo guarda', async () => {
    const { proyecto, ventas, cantidad, precio } = await setupVentas();
    const user = userEvent.setup();
    const form = await abrirNuevaColumna(proyecto.id, ventas.id, user);

    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Total');
    await user.click(within(form).getByRole('radio', { name: 'Cálculo' }));
    expect(within(form).queryByLabelText('Valor inicial (opcional)')).not.toBeInTheDocument();
    expect(within(form).getByText('Todavía no armaste el cálculo.')).toBeVisible();

    const columnas = within(form).getByLabelText('Agregar una columna');
    await waitFor(() => expect(columnas).toBeEnabled(), ESPERA);
    expect(within(columnas).queryByRole('option', { name: 'Producto' })).not.toBeInTheDocument();
    await user.selectOptions(columnas, 'Cantidad');
    await user.click(within(form).getByRole('button', { name: 'por' }));
    await user.selectOptions(columnas, 'Precio');
    expect(within(form).getByText('«Cantidad» × «Precio»')).toBeVisible();
    await sinViolaciones();

    await user.click(within(form).getByRole('button', { name: 'Guardar columna' }));
    await waitFor(() => expect(screen.queryByRole('form')).not.toBeInTheDocument(), ESPERA);

    const lista = await screen.findByRole('list', { name: 'Columnas de la tabla' }, ESPERA);
    expect(
      await within(lista).findByText('Calcula «Cantidad» × «Precio»', {}, ESPERA),
    ).toBeVisible();
    expect((await listarCampos(ventas.id)).at(-1)).toMatchObject({
      nombre: 'Total',
      tipo: 'FORMULA',
      formula: `{${cantidad.id}} * {${precio.id}}`,
      valorInicial: null,
    });
  });

  it('agrega números, borra lo último y empieza de nuevo', async () => {
    const { proyecto, ventas } = await setupVentas();
    const user = userEvent.setup();
    const form = await abrirNuevaColumna(proyecto.id, ventas.id, user);
    await user.click(within(form).getByRole('radio', { name: 'Cálculo' }));
    const columnas = within(form).getByLabelText('Agregar una columna');
    await waitFor(() => expect(columnas).toBeEnabled(), ESPERA);

    await user.selectOptions(columnas, 'Cantidad');
    await user.click(within(form).getByRole('button', { name: 'más' }));
    await user.type(within(form).getByLabelText('Agregar un número'), 'x');
    await user.click(within(form).getByRole('button', { name: 'Agregar número' }));
    expect(within(form).getByText('Escribí un número, por ejemplo 2 o 0,5.')).toBeVisible();

    await user.clear(within(form).getByLabelText('Agregar un número'));
    await user.type(within(form).getByLabelText('Agregar un número'), '0,5{Enter}');
    expect(within(form).getByText('«Cantidad» + 0,5')).toBeVisible();

    await user.click(within(form).getByRole('button', { name: 'Borrar lo último' }));
    expect(within(form).getByText('«Cantidad» +')).toBeVisible();
    await user.click(within(form).getByRole('button', { name: 'Empezar de nuevo' }));
    expect(within(form).getByText('Todavía no armaste el cálculo.')).toBeVisible();
  });

  it('pide armar el cálculo antes de enviar', async () => {
    const { proyecto, ventas } = await setupVentas();
    const user = userEvent.setup();
    const form = await abrirNuevaColumna(proyecto.id, ventas.id, user);

    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Total');
    await user.click(within(form).getByRole('radio', { name: 'Cálculo' }));
    await user.click(within(form).getByRole('button', { name: 'Guardar columna' }));

    expect(
      await within(form).findByText('Armá el cálculo con al menos una columna.'),
    ).toBeVisible();
  });

  it('muestra junto al editor el error que devuelve el servidor', async () => {
    const { proyecto, ventas } = await setupVentas();
    const user = userEvent.setup();
    const form = await abrirNuevaColumna(proyecto.id, ventas.id, user);

    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Total');
    await user.click(within(form).getByRole('radio', { name: 'Cálculo' }));
    const columnas = within(form).getByLabelText('Agregar una columna');
    await waitFor(() => expect(columnas).toBeEnabled(), ESPERA);
    await user.selectOptions(columnas, 'Cantidad');
    await user.click(within(form).getByRole('button', { name: 'por' }));
    await user.click(within(form).getByRole('button', { name: 'Guardar columna' }));

    expect(await within(form).findByText('La fórmula está incompleta', {}, ESPERA)).toBeVisible();
  });

  it('al cambiar de tipo se vacía el cálculo armado', async () => {
    const { proyecto, ventas } = await setupVentas();
    const user = userEvent.setup();
    const form = await abrirNuevaColumna(proyecto.id, ventas.id, user);
    await user.click(within(form).getByRole('radio', { name: 'Cálculo' }));
    const columnas = within(form).getByLabelText('Agregar una columna');
    await waitFor(() => expect(columnas).toBeEnabled(), ESPERA);
    await user.selectOptions(columnas, 'Cantidad');

    await user.click(within(form).getByRole('radio', { name: 'Texto' }));
    await user.click(within(form).getByRole('radio', { name: 'Cálculo' }));

    expect(within(form).getByText('Todavía no armaste el cálculo.')).toBeVisible();
  });
});

describe('ENS-166: ver el valor calculado', () => {
  it('no se pide al cargar una fila y aparece calculado en la tabla', async () => {
    const { proyecto, cantidad } = await setupVentasConTotal();
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await verifyHeadingPresentAsync('Mi almacén');
    const region = await screen.findByRole('region', { name: 'Ventas' }, ESPERA);

    expect(within(region).queryByLabelText('Total')).not.toBeInTheDocument();
    await user.type(within(region).getByLabelText('Cantidad'), '3');
    await user.type(within(region).getByLabelText('Precio'), '12.5');
    await user.click(within(region).getByRole('button', { name: 'Guardar fila' }));
    expect(
      await screen.findByText('Fila guardada. Ya podés cargar otra.', {}, ESPERA),
    ).toBeVisible();

    await user.click(screen.getByRole('tab', { name: 'Ver filas' }));
    expect(await screen.findByRole('cell', { name: '37,5' }, ESPERA)).toBeVisible();
    const guardado = [...estado.registros.values()][0];
    expect(guardado?.valores).toHaveProperty(cantidad.id, 3);
    expect(Object.keys(guardado?.valores ?? {})).toHaveLength(2);
  });
});
