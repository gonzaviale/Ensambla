import { screen, waitFor, within } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { crearBloque } from '@/api/bloques';
import { crearCampo } from '@/api/campos';
import { crearProyecto } from '@/api/proyectos';
import { eliminarRegistro } from '@/api/registros';
import { consultarStock, listarMovimientos, registrarMovimiento } from '@/api/stock';
import { estado } from '@/mocks/db/estado';
import { agregarRegistro } from '@/mocks/db/registros';
import type { GuardarCampo } from '@/types/campo';
import type { NuevoMovimiento } from '@/types/stock';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

const columna = (datos: Partial<GuardarCampo> & Pick<GuardarCampo, 'nombre'>): GuardarCampo => ({
  tipo: 'TEXTO',
  obligatorio: false,
  valorInicial: null,
  opciones: [],
  bloqueRelacionadoId: null,
  formula: null,
  ...datos,
});

const hoy = () => new Date().toISOString().slice(0, 10);

/* setup: Productos (Nombre) con Harina 000 y Azúcar */

async function setupProductos() {
  setupUsuarioConSesion();
  const proyecto = await crearProyecto({ nombre: 'Mi almacén' });
  const productos = await crearBloque({ proyectoId: proyecto.id, nombre: 'Productos' });
  const nombre = await crearCampo({ bloqueId: productos.id, datos: columna({ nombre: 'Nombre' }) });
  const harina = agregarRegistro(
    productos.id,
    { [nombre.id]: 'Harina 000' },
    '2026-10-01T10:00:00Z',
  );
  const azucar = agregarRegistro(productos.id, { [nombre.id]: 'Azúcar' }, '2026-10-02T10:00:00Z');
  return { proyecto, productos, harina, azucar };
}

const movimiento = (datos: Partial<NuevoMovimiento> & Pick<NuevoMovimiento, 'registroId'>) => ({
  tipo: 'ENTRADA' as const,
  cantidad: 1,
  fecha: hoy(),
  nota: null,
  ...datos,
});

async function abrirStock(proyectoId: string) {
  renderApp(`/proyectos/${proyectoId}/stock`);
  await verifyHeadingPresentAsync('Mi almacén');
  return screen
    .getByRole('form', { name: 'Registrar un movimiento' })
    .closest('section') as HTMLElement;
}

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

async function elegirProducto(form: HTMLElement, nombre: string, user: UserEvent) {
  await user.click(await within(form).findByRole('radio', { name: nombre }, ESPERA));
}

describe('ENS-159: la API simulada de movimientos y stock', () => {
  it('el stock es la suma de entradas menos salidas de cada producto', async () => {
    const { productos, harina, azucar } = await setupProductos();

    await registrarMovimiento(productos.id, movimiento({ registroId: harina.id, cantidad: 10 }));
    await registrarMovimiento(
      productos.id,
      movimiento({ registroId: harina.id, tipo: 'SALIDA', cantidad: 2.5 }),
    );
    const registrado = await registrarMovimiento(
      productos.id,
      movimiento({ registroId: azucar.id, cantidad: 4, nota: ' Proveedor ' }),
    );

    expect(registrado.stockActual).toBe(4);
    expect(registrado.movimiento).toMatchObject({ producto: 'Azúcar', nota: 'Proveedor' });
    const stock = await consultarStock(productos.id, {});
    expect(stock.content).toEqual([
      { registroId: azucar.id, producto: 'Azúcar', stock: 4 },
      { registroId: harina.id, producto: 'Harina 000', stock: 7.5 },
    ]);
  });

  it('los productos sin movimientos tienen stock cero y se pueden buscar', async () => {
    const { productos, harina } = await setupProductos();

    const stock = await consultarStock(productos.id, { q: 'hari' });

    expect(stock.content).toEqual([{ registroId: harina.id, producto: 'Harina 000', stock: 0 }]);
    expect(stock).toMatchObject({ totalElements: 1, first: true, last: true });
  });

  it('rechaza una salida mayor al stock y avisa cuánto queda', async () => {
    const { productos, harina } = await setupProductos();
    await registrarMovimiento(productos.id, movimiento({ registroId: harina.id, cantidad: 3 }));

    const error = await registrarMovimiento(
      productos.id,
      movimiento({ registroId: harina.id, tipo: 'SALIDA', cantidad: 5 }),
    ).catch((e: unknown) => e);

    expect(error).toMatchObject({
      status: 400,
      fieldErrors: { cantidad: 'No hay stock suficiente: quedan 3' },
    });
    expect((await consultarStock(productos.id, { q: 'hari' })).content[0]?.stock).toBe(3);
  });

  it('valida cantidad, tipo, fecha y producto', async () => {
    const { productos, harina } = await setupProductos();
    const error = (cuerpo: Partial<NuevoMovimiento>) =>
      registrarMovimiento(productos.id, {
        ...movimiento({ registroId: harina.id }),
        ...cuerpo,
      }).catch((e: unknown) => e);

    expect(await error({ cantidad: 0 })).toMatchObject({
      fieldErrors: { cantidad: 'Tiene que ser mayor a cero' },
    });
    expect(await error({ cantidad: 0.0001 })).toMatchObject({
      fieldErrors: { cantidad: 'Máximo 3 decimales' },
    });
    expect(await error({ fecha: '2999-01-01' })).toMatchObject({
      fieldErrors: { fecha: 'No puede ser una fecha futura' },
    });
    expect(await error({ registroId: '' })).toMatchObject({
      fieldErrors: { registroId: 'Elegí el producto' },
    });
    expect(await error({ tipo: 'OTRO' as never })).toMatchObject({
      fieldErrors: { tipo: 'Elegí si es una entrada o una salida' },
    });
    expect(await error({ registroId: crypto.randomUUID() })).toMatchObject({ status: 404 });
  });

  it('lista los últimos movimientos, los más nuevos primero, y borrar el producto borra su historial', async () => {
    const { productos, harina, azucar } = await setupProductos();
    await registrarMovimiento(productos.id, movimiento({ registroId: harina.id, cantidad: 1 }));
    await registrarMovimiento(productos.id, movimiento({ registroId: azucar.id, cantidad: 2 }));

    const todos = await listarMovimientos(productos.id);
    const deHarina = await listarMovimientos(productos.id, harina.id);

    expect(todos.map((m) => m.producto)).toEqual(['Azúcar', 'Harina 000']);
    expect(deHarina.map((m) => m.producto)).toEqual(['Harina 000']);
    await eliminarRegistro(harina.id);
    expect((await listarMovimientos(productos.id)).map((m) => m.producto)).toEqual(['Azúcar']);
    expect(estado.movimientos.size).toBe(1);
  });
});

describe('ENS-159: cargar movimientos y ver el stock', () => {
  it('registra una entrada y el stock mostrado se actualiza', async () => {
    const { proyecto } = await setupProductos();
    const user = userEvent.setup();
    const region = await abrirStock(proyecto.id);
    const form = within(region).getByRole('form', { name: 'Registrar un movimiento' });

    expect(await screen.findAllByRole('cell', { name: 'Sin stock' }, ESPERA)).toHaveLength(2);
    await elegirProducto(form, 'Harina 000', user);
    await user.type(within(form).getByLabelText('Cantidad'), '10,5');
    await user.type(within(form).getByLabelText('Nota (opcional)'), 'Compra semanal');
    await user.click(within(form).getByRole('button', { name: 'Registrar movimiento' }));

    expect(
      await screen.findByText('Entrada registrada: Harina 000. Ahora hay 10,5.', {}, ESPERA),
    ).toBeVisible();
    const tabla = screen.getByRole('table', { name: 'Stock de Productos' });
    expect(await within(tabla).findByText('10,5', {}, ESPERA)).toBeVisible();
    const lista = screen.getByRole('list', { name: 'Últimos movimientos' });
    expect(await within(lista).findByText('Entrada de 10,5', {}, ESPERA)).toBeVisible();
    expect(within(lista).getByText('Compra semanal')).toBeVisible();
    // El formulario queda listo para otro movimiento.
    expect(within(form).getByLabelText('Cantidad')).toHaveValue('');
    expect(within(form).getByLabelText('Buscá en «Productos»')).toHaveValue('');
  });

  it('registra una salida y descuenta del stock', async () => {
    const { proyecto, productos, harina } = await setupProductos();
    await registrarMovimiento(productos.id, movimiento({ registroId: harina.id, cantidad: 10 }));
    const user = userEvent.setup();
    const region = await abrirStock(proyecto.id);
    const form = within(region).getByRole('form', { name: 'Registrar un movimiento' });

    await elegirProducto(form, 'Harina 000', user);
    await user.click(within(form).getByRole('radio', { name: /Salida/ }));
    await user.type(within(form).getByLabelText('Cantidad'), '4');
    await user.click(within(form).getByRole('button', { name: 'Registrar movimiento' }));

    expect(
      await screen.findByText('Salida registrada: Harina 000. Ahora hay 6.', {}, ESPERA),
    ).toBeVisible();
    const lista = screen.getByRole('list', { name: 'Últimos movimientos' });
    expect(await within(lista).findByText('Salida de 4', {}, ESPERA)).toBeVisible();
  });

  it('muestra junto a la cantidad el error del servidor cuando no alcanza el stock', async () => {
    const { proyecto, productos, harina } = await setupProductos();
    await registrarMovimiento(productos.id, movimiento({ registroId: harina.id, cantidad: 2 }));
    const user = userEvent.setup();
    const region = await abrirStock(proyecto.id);
    const form = within(region).getByRole('form', { name: 'Registrar un movimiento' });

    await elegirProducto(form, 'Harina 000', user);
    await user.click(within(form).getByRole('radio', { name: /Salida/ }));
    await user.type(within(form).getByLabelText('Cantidad'), '5');
    await user.click(within(form).getByRole('button', { name: 'Registrar movimiento' }));

    expect(
      await within(form).findByText('No hay stock suficiente: quedan 2', {}, ESPERA),
    ).toBeVisible();
    expect(within(form).getByLabelText('Cantidad')).toHaveValue('5');
  });

  it('pide el producto y la cantidad antes de enviar', async () => {
    const { proyecto } = await setupProductos();
    const user = userEvent.setup();
    const region = await abrirStock(proyecto.id);
    const form = within(region).getByRole('form', { name: 'Registrar un movimiento' });

    await user.click(within(form).getByRole('button', { name: 'Registrar movimiento' }));

    expect(await within(form).findByText('Elegí el producto.')).toBeVisible();
    expect(within(form).getByText('Escribí la cantidad, por ejemplo 5 o 2,5.')).toBeVisible();
    await user.type(within(form).getByLabelText('Cantidad'), '0');
    await user.click(within(form).getByRole('button', { name: 'Registrar movimiento' }));
    expect(await within(form).findByText('Tiene que ser mayor a cero.')).toBeVisible();
    expect(estado.movimientos.size).toBe(0);
  });

  it('busca un producto en el stock y explica cuando no hay resultados', async () => {
    const { proyecto } = await setupProductos();
    const user = userEvent.setup();
    await abrirStock(proyecto.id);
    const tabla = await screen.findByRole('table', { name: 'Stock de Productos' }, ESPERA);
    expect(within(tabla).getByRole('rowheader', { name: 'Azúcar' })).toBeVisible();

    await user.type(screen.getByRole('searchbox', { name: 'Buscar en Productos' }), 'hari');
    await waitFor(
      () =>
        expect(within(tabla).queryByRole('rowheader', { name: 'Azúcar' })).not.toBeInTheDocument(),
      ESPERA,
    );
    expect(within(tabla).getByRole('rowheader', { name: 'Harina 000' })).toBeVisible();

    await user.clear(screen.getByRole('searchbox', { name: 'Buscar en Productos' }));
    await user.type(screen.getByRole('searchbox', { name: 'Buscar en Productos' }), 'zzz');
    expect(
      await screen.findByText('Ningún producto coincide con «zzz».', {}, ESPERA),
    ).toBeVisible();
  });

  it('elige la tabla que tiene los productos cuando el sistema tiene varias', async () => {
    const { proyecto } = await setupProductos();
    const clientes = await crearBloque({ proyectoId: proyecto.id, nombre: 'Clientes' });
    const nombre = await crearCampo({
      bloqueId: clientes.id,
      datos: columna({ nombre: 'Nombre' }),
    });
    agregarRegistro(clientes.id, { [nombre.id]: 'Beto' });
    const user = userEvent.setup();
    await abrirStock(proyecto.id);

    await user.selectOptions(
      await screen.findByLabelText('¿Qué tabla tiene tus productos?'),
      'Clientes',
    );

    const tabla = await screen.findByRole('table', { name: 'Stock de Clientes' }, ESPERA);
    expect(within(tabla).getByRole('rowheader', { name: 'Beto' })).toBeVisible();
  });

  it('explica que no hay tablas y el estado vacío de los movimientos', async () => {
    setupUsuarioConSesion();
    const sin = await crearProyecto({ nombre: 'Vacío' });
    renderApp(`/proyectos/${sin.id}/stock`);

    expect(await screen.findByText('Este sistema no tiene tablas', {}, ESPERA)).toBeVisible();
    expect(screen.getByRole('link', { name: 'Armar mis tablas' })).toBeVisible();
  });

  it('muestra que todavía no hay movimientos y es accesible', async () => {
    const { proyecto } = await setupProductos();
    await abrirStock(proyecto.id);

    expect(await screen.findByText('Todavía no hay movimientos', {}, ESPERA)).toBeVisible();
    await screen.findByRole('table', { name: 'Stock de Productos' }, ESPERA);
    await sinViolaciones();
  });

  it('el selector de modo del sistema lleva a Stock', async () => {
    const { proyecto } = await setupProductos();
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await verifyHeadingPresentAsync('Mi almacén');

    await user.click(
      within(screen.getByRole('navigation', { name: 'Modo del sistema' })).getByRole('link', {
        name: 'Stock',
      }),
    );

    expect(await screen.findByRole('heading', { name: 'Mi almacén' }, ESPERA)).toBeVisible();
    expect(
      await screen.findByRole('form', { name: 'Registrar un movimiento' }, ESPERA),
    ).toBeVisible();
    expect(
      within(screen.getByRole('navigation', { name: 'Ruta de navegación' })).getByText('Stock'),
    ).toBeVisible();
  });
});
