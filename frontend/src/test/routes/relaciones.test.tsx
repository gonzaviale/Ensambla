import { screen, waitFor, within } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { crearBloque } from '@/api/bloques';
import { crearCampo } from '@/api/campos';
import { crearProyecto } from '@/api/proyectos';
import { listarReferencias } from '@/api/registros';
import { estado } from '@/mocks/db/estado';
import { agregarRegistro } from '@/mocks/db/registros';
import type { GuardarCampo } from '@/types/campo';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupServerError,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';

const columna = (datos: Partial<GuardarCampo> & Pick<GuardarCampo, 'nombre'>): GuardarCampo => ({
  tipo: 'TEXTO',
  obligatorio: false,
  valorInicial: null,
  opciones: [],
  bloqueRelacionadoId: null,
  formula: null,
  ...datos,
});

/* setup: Productos (Nombre) y Ventas (Producto → Productos, Cantidad) */

async function setupVentas({ conProductos = true } = {}) {
  setupUsuarioConSesion();
  const proyecto = await crearProyecto({ nombre: 'Mi almacén' });
  const productos = await crearBloque({ proyectoId: proyecto.id, nombre: 'Productos' });
  const ventas = await crearBloque({ proyectoId: proyecto.id, nombre: 'Ventas' });
  const nombre = await crearCampo({ bloqueId: productos.id, datos: columna({ nombre: 'Nombre' }) });
  const producto = await crearCampo({
    bloqueId: ventas.id,
    datos: columna({
      nombre: 'Producto',
      tipo: 'RELACION',
      obligatorio: true,
      bloqueRelacionadoId: productos.id,
      formula: null,
    }),
  });
  const cantidad = await crearCampo({
    bloqueId: ventas.id,
    datos: columna({ nombre: 'Cantidad', tipo: 'NUMERO' }),
  });
  const harina = conProductos
    ? agregarRegistro(productos.id, { [nombre.id]: 'Harina 000' }, '2026-10-01T10:00:00Z')
    : undefined;
  const azucar = conProductos
    ? agregarRegistro(productos.id, { [nombre.id]: 'Azúcar' }, '2026-10-02T10:00:00Z')
    : undefined;
  return { proyecto, productos, ventas, producto, cantidad, harina, azucar };
}

async function abrirCargaDeVentas(proyectoId: string, user: UserEvent) {
  renderApp(`/proyectos/${proyectoId}/usar`);
  await verifyHeadingPresentAsync('Mi almacén');
  await user.click(await screen.findByRole('radio', { name: 'Ventas' }, ESPERA));
  return screen.getByRole('region', { name: 'Ventas' });
}

const selector = (region: HTMLElement) => within(region).getByRole('group', { name: /^Producto/ });

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

describe('ENS-158: referencias de una tabla (API simulada)', () => {
  it('busca por texto, trae las más nuevas primero y filtra por ids', async () => {
    const { productos, harina, azucar } = await setupVentas();

    expect(await listarReferencias(productos.id, {})).toEqual([
      { id: azucar?.id, etiqueta: 'Azúcar' },
      { id: harina?.id, etiqueta: 'Harina 000' },
    ]);
    expect(await listarReferencias(productos.id, { q: 'hari' })).toEqual([
      { id: harina?.id, etiqueta: 'Harina 000' },
    ]);
    expect(
      await listarReferencias(productos.id, { ids: [String(harina?.id), crypto.randomUUID()] }),
    ).toEqual([{ id: harina?.id, etiqueta: 'Harina 000' }]);
  });
});

describe('ENS-158: elegir una fila de otra tabla', () => {
  it('busca, elige y guarda la fila; después se ve su nombre en la tabla y en la ficha', async () => {
    const { proyecto, ventas, producto, cantidad, harina } = await setupVentas();
    const user = userEvent.setup();
    const region = await abrirCargaDeVentas(proyecto.id, user);

    const busqueda = within(region).getByLabelText('Buscá en «Productos»');
    expect(await within(region).findByRole('radio', { name: 'Azúcar' }, ESPERA)).toBeVisible();
    await user.type(busqueda, 'hari');
    await waitFor(
      () => expect(within(region).queryByRole('radio', { name: 'Azúcar' })).not.toBeInTheDocument(),
      ESPERA,
    );
    await user.click(within(region).getByRole('radio', { name: 'Harina 000' }));

    const elegido = selector(region);
    expect(within(elegido).getByText('Harina 000')).toBeVisible();
    expect(within(elegido).queryByLabelText('Buscá en «Productos»')).not.toBeInTheDocument();
    await waitFor(() =>
      expect(
        within(elegido).getByRole('button', { name: 'Cambiar la fila elegida en Producto' }),
      ).toHaveFocus(),
    );

    await user.type(within(region).getByLabelText('Cantidad'), '3');
    await user.click(within(region).getByRole('button', { name: 'Guardar fila' }));
    expect(
      await screen.findByText('Fila guardada. Ya podés cargar otra.', {}, ESPERA),
    ).toBeVisible();
    const guardado = [...estado.registros.values()].find((r) => r.bloqueId === ventas.id);
    expect(guardado?.valores).toEqual({ [producto.id]: harina?.id, [cantidad.id]: 3 });
    // El formulario queda listo para otra fila: sin fila elegida.
    expect(within(region).getByLabelText('Buscá en «Productos»')).toHaveValue('');

    await user.click(screen.getByRole('tab', { name: 'Ver filas' }));
    const celda = await screen.findByRole('cell', { name: 'Harina 000' }, ESPERA);
    expect(celda).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Ver ficha de Harina 000' }));
    const ficha = screen.getByRole('complementary', { name: 'Ficha de la fila' });
    expect(await within(ficha).findByText('Harina 000', {}, ESPERA)).toBeVisible();
  });

  it('cambia y quita la fila elegida', async () => {
    const { proyecto } = await setupVentas();
    const user = userEvent.setup();
    const region = await abrirCargaDeVentas(proyecto.id, user);

    await user.click(await within(region).findByRole('radio', { name: 'Azúcar' }, ESPERA));
    await user.click(within(region).getByRole('button', { name: /Cambiar la fila elegida/ }));
    const busqueda = within(region).getByLabelText('Buscá en «Productos»');
    await waitFor(() => expect(busqueda).toHaveFocus());
    expect(within(region).getByRole('radio', { name: 'Azúcar' })).toBeChecked();
    await user.click(within(region).getByRole('radio', { name: 'Harina 000' }));
    expect(within(selector(region)).getByText('Harina 000')).toBeVisible();

    await user.click(within(region).getByRole('button', { name: /Quitar la fila elegida/ }));
    expect(within(region).queryByText('Fila elegida de «Productos»')).not.toBeInTheDocument();
    expect(within(region).getByRole('radio', { name: 'Harina 000' })).not.toBeChecked();
    await waitFor(() =>
      expect(within(region).getByLabelText('Buscá en «Productos»')).toHaveFocus(),
    );
  });

  it('pide elegir una fila si la columna es obligatoria', async () => {
    const { proyecto } = await setupVentas();
    const user = userEvent.setup();
    const region = await abrirCargaDeVentas(proyecto.id, user);
    await within(region).findByRole('radio', { name: 'Azúcar' }, ESPERA);

    await user.click(within(region).getByRole('button', { name: 'Guardar fila' }));
    expect(selector(region)).toHaveAccessibleDescription('Este dato es obligatorio');
    await waitFor(() =>
      expect(within(region).getByLabelText('Buscá en «Productos»')).toHaveFocus(),
    );
  });

  it('avisa si la fila elegida ya no existe al guardar', async () => {
    const { proyecto, harina } = await setupVentas();
    const user = userEvent.setup();
    const region = await abrirCargaDeVentas(proyecto.id, user);

    await user.click(await within(region).findByRole('radio', { name: 'Harina 000' }, ESPERA));
    estado.registros.delete(String(harina?.id));
    await user.click(within(region).getByRole('button', { name: 'Guardar fila' }));
    await waitFor(
      () => expect(selector(region)).toHaveAccessibleDescription('Esa fila ya no existe'),
      ESPERA,
    );
  });

  it('explica cuando la tabla conectada todavía no tiene filas', async () => {
    const { proyecto } = await setupVentas({ conProductos: false });
    const user = userEvent.setup();
    const region = await abrirCargaDeVentas(proyecto.id, user);

    expect(
      await within(region).findByText(/Todavía no hay filas en «Productos»/, {}, ESPERA),
    ).toBeVisible();
  });

  it('muestra el error y permite reintentar si no se pueden traer las filas', async () => {
    const { proyecto, productos } = await setupVentas();
    setupServerError('get', `/bloques/${productos.id}/referencias`, 500, 'Se cayó el servidor');
    const user = userEvent.setup();
    const region = await abrirCargaDeVentas(proyecto.id, user);

    const alerta = await within(region).findByRole('alert', {}, ESPERA);
    expect(alerta).toHaveTextContent('No pudimos traer las filas de «Productos».');
    expect(within(alerta).getByRole('button', { name: 'Reintentar' })).toBeVisible();
  });
});

describe('ENS-158: filas que apuntan a otra tabla', () => {
  it('muestra «Fila eliminada» si la fila conectada ya no existe', async () => {
    const { proyecto, ventas, producto, cantidad, harina } = await setupVentas();
    agregarRegistro(ventas.id, { [producto.id]: String(harina?.id), [cantidad.id]: 2 });
    agregarRegistro(ventas.id, { [producto.id]: crypto.randomUUID(), [cantidad.id]: 5 });
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await verifyHeadingPresentAsync('Mi almacén');
    await user.click(await screen.findByRole('tab', { name: 'Ver filas' }));
    await user.click(await screen.findByRole('radio', { name: 'Ventas' }, ESPERA));

    expect(await screen.findByRole('cell', { name: 'Harina 000' }, ESPERA)).toBeVisible();
    expect(screen.getByRole('cell', { name: 'Fila eliminada' })).toBeVisible();
  });

  it('al editar la ficha muestra la fila elegida y deja cambiarla', async () => {
    const { proyecto, ventas, producto, cantidad, harina, azucar } = await setupVentas();
    const venta = agregarRegistro(ventas.id, {
      [producto.id]: String(harina?.id),
      [cantidad.id]: 2,
    });
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await verifyHeadingPresentAsync('Mi almacén');
    await user.click(await screen.findByRole('tab', { name: 'Ver filas' }));
    await user.click(await screen.findByRole('radio', { name: 'Ventas' }, ESPERA));
    await screen.findByRole('cell', { name: 'Harina 000' }, ESPERA);
    await user.click(screen.getByRole('button', { name: 'Ver ficha de Harina 000' }));
    const ficha = screen.getByRole('complementary', { name: 'Ficha de la fila' });

    await user.click(within(ficha).getByRole('button', { name: 'Editar fila' }));
    const elegido = within(ficha).getByRole('group', { name: /^Producto/ });
    expect(await within(elegido).findByText('Harina 000', {}, ESPERA)).toBeVisible();
    await user.click(within(elegido).getByRole('button', { name: /Cambiar la fila elegida/ }));
    await user.click(await within(ficha).findByRole('radio', { name: 'Azúcar' }, ESPERA));
    await user.click(within(ficha).getByRole('button', { name: 'Guardar cambios' }));

    expect(await within(ficha).findByText('Cambios guardados.', {}, ESPERA)).toBeVisible();
    expect(await within(ficha).findByText('Azúcar', {}, ESPERA)).toBeVisible();
    expect(estado.registros.get(venta.id)?.valores[producto.id]).toBe(azucar?.id);
  });
});

describe('ENS-158: accesibilidad del selector (axe)', () => {
  it('sin violaciones al buscar y con una fila elegida', async () => {
    const { proyecto } = await setupVentas();
    const user = userEvent.setup();
    const region = await abrirCargaDeVentas(proyecto.id, user);
    await within(region).findByRole('radio', { name: 'Azúcar' }, ESPERA);
    await sinViolaciones();

    await user.click(within(region).getByRole('radio', { name: 'Azúcar' }));
    await within(region).findByRole('button', { name: /Cambiar la fila elegida/ });
    await sinViolaciones();
  });
});
