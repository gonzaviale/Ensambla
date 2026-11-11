import { act, fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import { allBlocks, clone, makeTemplate } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

const originales = {
  close: Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, 'close'),
  showModal: Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, 'showModal'),
};
beforeAll(() => {
  Object.defineProperty(HTMLDialogElement.prototype, 'close', {
    configurable: true,
    value: function (this: HTMLDialogElement) {
      this.removeAttribute('open');
    },
  });
  Object.defineProperty(HTMLDialogElement.prototype, 'showModal', {
    configurable: true,
    value: function (this: HTMLDialogElement) {
      this.setAttribute('open', '');
    },
  });
});
afterAll(() => {
  for (const nombre of ['close', 'showModal'] as const) {
    const original = originales[nombre];
    if (original) Object.defineProperty(HTMLDialogElement.prototype, nombre, original);
    else Reflect.deleteProperty(HTMLDialogElement.prototype, nombre);
  }
});

function requerido<T>(dato: T | undefined): T {
  if (dato === undefined) throw new Error('Falta un dato del escenario');
  return dato;
}

async function abrir(segundaTabla = false) {
  const usuario = setupUsuarioConSesion();
  const sistema = makeTemplate('shop', { movements: true }, false);
  sistema.name = 'Kiosco de Nacho';
  const inventario = allBlocks(sistema).find((bloque) => bloque.type === 'inventory');
  if (!inventario) throw new Error('Falta el inventario');
  inventario.rows = [
    {
      id: 'alfajor',
      name: 'Guaymallén',
      barcode: '77903754',
      quantity: 10,
      price: 850.25,
      cost: 500,
      minimum: 2,
      categoria: 'Kiosco',
    },
  ];
  inventario.fields.push({
    key: 'categoria',
    label: 'Categoría',
    type: 'select',
    required: true,
    options: ['Kiosco', 'Almacén'],
  });
  let destino = inventario;
  if (segundaTabla) {
    destino = { ...clone(inventario), id: 'deposito', name: 'Depósito', rows: [] };
    requerido(sistema.sections[0]).blocks.push(destino);
  }
  estado.sistemas.set(usuario.email, [sistema]);
  const router = renderApp(`/sistemas/${sistema.id}`);
  await screen.findByRole('heading', { name: sistema.name, level: 1 }, ESPERA);
  const user = userEvent.setup();
  await user.click(
    within(screen.getByRole('region', { name: destino.name })).getByRole('button', {
      name: 'Escanear productos',
    }),
  );
  await screen.findByRole('heading', { name: 'Escaneá, revisá y agregá.' });
  const actual = () =>
    requerido(useSistemaStore.getState().sistemas.find((item) => item.id === sistema.id));
  return { usuario, sistema, inventario, destino, router, user, actual };
}
async function buscar(user: ReturnType<typeof userEvent.setup>, codigo: string) {
  await user.type(screen.getByRole('textbox', { name: 'Código de barras' }), `${codigo}{Enter}`);
  await screen.findByRole('region', { name: 'Revisión del producto' });
  await waitFor(() =>
    expect(
      screen.getByRole('button', {
        name: /Agregar producto al inventario|Guardar cambios del producto/,
      }),
    ).toBeEnabled(),
  );
}
function sinCoincidencias(codigo: string) {
  server.use(
    http.get(`/api/catalogos/sepa/${codigo}`, () => HttpResponse.json({ producto: null })),
    http.get(`/api/catalogos/openfoodfacts/${codigo}`, () => HttpResponse.json({ producto: null })),
  );
}
const cambiar = (nombre: string, valor: string) =>
  fireEvent.change(screen.getByRole('spinbutton', { name: nombre }), {
    target: { value: valor },
  });

describe('carga exclusiva de productos en inventario', () => {
  it('entra desde Inventario sin caja y guarda la ficha revisada, con campos propios y persistencia', async () => {
    server.use(
      http.get('/api/catalogos/sepa/7790957000101', () =>
        HttpResponse.json({
          producto: {
            codigo: '7790957000101',
            nombre: 'Alfajor de chocolate con mousse Jorgito 45g',
            marca: 'Jorgito',
            presentacion: '45 g',
          },
        }),
      ),
      http.get('/api/catalogos/openfoodfacts/7790957000101', () =>
        HttpResponse.json({
          producto: {
            codigo: '7790957000101',
            nombre: 'Alfajor',
            marca: '',
            presentacion: '',
            imagen: 'https://images.openfoodfacts.org/foto.jpg',
          },
        }),
      ),
    );
    const { user, destino, router, actual, usuario, sistema } = await abrir();
    expect(router.state.location.search).toMatchObject({ escanear: destino.id });
    expect(screen.queryByRole('button', { name: 'Abrir caja' })).toBeNull();
    expect(screen.queryByRole('region', { name: 'Ticket en curso' })).toBeNull();
    await buscar(user, '7790957000101');
    expect(screen.getByRole('textbox', { name: 'Producto' })).toHaveValue(
      'Alfajor de chocolate con mousse Jorgito 45g',
    );
    expect(screen.getByRole('img', { name: 'Foto de referencia del producto' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Ver origen de los datos' }));
    expect(screen.getByRole('dialog', { name: 'Origen de los datos' })).toHaveTextContent('SEPA');
    await user.click(screen.getByRole('button', { name: 'Cerrar detalle' }));
    cambiar('Cantidad', '12');
    cambiar('Precio', '850.25');
    cambiar('Costo', '500.10');
    cambiar('Stock mínimo', '3');
    await user.selectOptions(screen.getByRole('combobox', { name: 'Categoría' }), 'Kiosco');
    expect(allBlocks(actual()).find((bloque) => bloque.id === destino.id)?.rows).toHaveLength(1);
    await user.click(screen.getByRole('button', { name: 'Agregar producto al inventario' }));
    await screen.findByRole('region', { name: 'Último producto guardado' });
    expect(allBlocks(actual()).find((bloque) => bloque.id === destino.id)?.rows[1]).toMatchObject({
      barcode: '7790957000101',
      quantity: 12,
      cost: 500.1,
      price: 850.25,
      categoria: 'Kiosco',
    });
    expect(
      allBlocks(actual())
        .filter((bloque) => ['sales', 'entries'].includes(bloque.type))
        .every((bloque) => bloque.rows.length === 0),
    ).toBe(true);
    await waitFor(
      () =>
        expect(
          estado.sistemas.get(usuario.email)?.find((item) => item.id === sistema.id)?.sections,
        ).toEqual(actual().sections),
      ESPERA,
    );
  });
  it('reconoce un código propio, conserva las unidades y permite descartar sin guardar', async () => {
    const consulta = vi.fn(() => HttpResponse.json({ producto: null }));
    server.use(http.get('/api/catalogos/:fuente/77903754', consulta));
    const { user, actual, inventario } = await abrir();
    await buscar(user, '77903754');
    expect(screen.getByRole('heading', { name: 'Revisá el producto' })).toBeVisible();
    expect(screen.getByRole('spinbutton', { name: 'Cantidad' })).toHaveValue(10);
    expect(consulta).not.toHaveBeenCalled();
    cambiar('Cantidad', '99');
    await user.click(screen.getByRole('button', { name: 'Descartar carga' }));
    expect(
      allBlocks(actual()).find((bloque) => bloque.id === inventario.id)?.rows[0]?.quantity,
    ).toBe(10);
    expect(screen.queryByRole('region', { name: 'Revisión del producto' })).toBeNull();
    expect(screen.getByRole('textbox', { name: 'Código de barras' })).toHaveFocus();
  });
  it('vincula sólo productos sin otro código y queda listo para la siguiente lectura con teclado', async () => {
    sinCoincidencias('3017620422003');
    const { user, sistema, inventario, actual } = await abrir();
    act(() =>
      useSistemaStore.getState().actualizarSistema(sistema.id, (copia) => {
        requerido(allBlocks(copia).find((bloque) => bloque.id === inventario.id)).rows.push({
          id: 'sin-codigo',
          name: 'Producto sin código',
          quantity: 7,
          cost: 300,
          price: 500,
          minimum: 1,
          categoria: 'Kiosco',
        });
      }),
    );
    await buscar(user, '3017620422003');
    const vincular = screen.getByRole('combobox', { name: 'Vincular con un producto existente' });
    expect(within(vincular).queryByRole('option', { name: 'Guaymallén' })).toBeNull();
    await user.selectOptions(vincular, 'sin-codigo');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios del producto' }));
    await screen.findByRole('region', { name: 'Último producto guardado' });
    expect(screen.getByRole('textbox', { name: 'Código de barras' })).toHaveFocus();
    await user.keyboard('3017620422003{Enter}');
    await screen.findByRole('region', { name: 'Revisión del producto' });
    expect(screen.getByRole('spinbutton', { name: 'Cantidad' })).toHaveValue(7);
    expect(screen.getByRole('textbox', { name: 'Producto' })).toHaveValue('Producto sin código');
    const filas = requerido(allBlocks(actual()).find((bloque) => bloque.id === inventario.id)).rows;
    expect(filas).toHaveLength(2);
    expect(filas.find((fila) => fila.id === 'sin-codigo')).toMatchObject({
      barcode: '3017620422003',
      quantity: 7,
    });
  });
  it('permite carga manual con conteo cero, precio opcional y validación de los campos de la tabla', async () => {
    sinCoincidencias('77905741');
    const { user, actual, destino } = await abrir();
    await buscar(user, '77905741');
    await user.click(screen.getByRole('button', { name: 'Agregar producto al inventario' }));
    expect(screen.getByRole('textbox', { name: 'Producto' })).toHaveAttribute(
      'aria-invalid',
      'true',
    );
    expect(screen.getByRole('textbox', { name: 'Producto' })).toHaveFocus();
    await user.type(screen.getByRole('textbox', { name: 'Producto' }), 'Producto manual');
    cambiar('Cantidad', '0');
    await user.selectOptions(screen.getByRole('combobox', { name: 'Categoría' }), 'Almacén');
    await user.click(screen.getByRole('button', { name: 'Agregar producto al inventario' }));
    await screen.findByRole('region', { name: 'Último producto guardado' });
    expect(allBlocks(actual()).find((bloque) => bloque.id === destino.id)?.rows[1]).toMatchObject({
      name: 'Producto manual',
      barcode: '77905741',
      quantity: 0,
      price: '',
    });
  });
  it('un costo editado no pisa el stock que otro flujo actualizó mientras se revisaba', async () => {
    const { user, sistema, inventario, actual } = await abrir();
    await buscar(user, '77903754');
    cambiar('Costo', '600');
    act(() =>
      useSistemaStore.getState().actualizarSistema(sistema.id, (copia) => {
        requerido(
          requerido(allBlocks(copia).find((bloque) => bloque.id === inventario.id)).rows[0],
        ).quantity = 6;
      }),
    );
    await user.click(screen.getByRole('button', { name: 'Guardar cambios del producto' }));
    await screen.findByRole('region', { name: 'Último producto guardado' });
    expect(
      allBlocks(actual()).find((bloque) => bloque.id === inventario.id)?.rows[0],
    ).toMatchObject({ quantity: 6, cost: 600 });
  });
  it('volver al inventario protege el borrador y abre su tabla cuando se confirma salir', async () => {
    const { user, router, destino } = await abrir();
    await buscar(user, '77903754');
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);
    await user.click(screen.getByRole('button', { name: 'Volver al inventario' }));
    expect(confirmar).toHaveBeenCalledOnce();
    expect(router.state.location.search).toMatchObject({ escanear: destino.id });
    expect(screen.getByRole('region', { name: 'Revisión del producto' })).toBeVisible();
    confirmar.mockReturnValue(true);
    await user.click(screen.getByRole('button', { name: 'Volver al inventario' }));
    await screen.findByRole('dialog', { name: destino.name });
    expect(router.state.location.search.escanear).toBeUndefined();
    confirmar.mockRestore();
  });
  it('usa la tabla elegida y no da de alta en el primer inventario del cuaderno', async () => {
    sinCoincidencias('3017620422003');
    const { user, actual, destino, inventario } = await abrir(true);
    await buscar(user, '3017620422003');
    await user.type(screen.getByRole('textbox', { name: 'Producto' }), 'Para depósito');
    cambiar('Cantidad', '4');
    await user.selectOptions(screen.getByRole('combobox', { name: 'Categoría' }), 'Kiosco');
    await user.click(screen.getByRole('button', { name: 'Agregar producto al inventario' }));
    await screen.findByRole('region', { name: 'Último producto guardado' });
    expect(allBlocks(actual()).find((bloque) => bloque.id === inventario.id)?.rows).toHaveLength(1);
    expect(allBlocks(actual()).find((bloque) => bloque.id === destino.id)?.rows[0]).toMatchObject({
      name: 'Para depósito',
      quantity: 4,
    });
  });
});
