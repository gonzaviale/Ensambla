import { screen, waitFor, within } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { crearBloque } from '@/api/bloques';
import { crearCampo } from '@/api/campos';
import { crearProyecto } from '@/api/proyectos';
import { eliminarRegistro, editarRegistro } from '@/api/registros';
import { guardarReceta, obtenerReceta } from '@/api/receta';
import { estado } from '@/mocks/db/estado';
import { agregarRegistro } from '@/mocks/db/registros';
import type { GuardarCampo } from '@/types/campo';
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

/* setup: Productos (Pan) e Insumos (Harina $1.200, Azúcar $850,50 y Sal sin precio) */

async function setupPanaderia() {
  setupUsuarioConSesion();
  const proyecto = await crearProyecto({ nombre: 'Mi panadería' });
  const productos = await crearBloque({ proyectoId: proyecto.id, nombre: 'Productos' });
  const insumos = await crearBloque({ proyectoId: proyecto.id, nombre: 'Insumos' });
  const nombreProducto = await crearCampo({
    bloqueId: productos.id,
    datos: columna({ nombre: 'Nombre' }),
  });
  const nombre = await crearCampo({ bloqueId: insumos.id, datos: columna({ nombre: 'Nombre' }) });
  const precio = await crearCampo({
    bloqueId: insumos.id,
    datos: columna({ nombre: 'Precio', tipo: 'NUMERO' }),
  });
  const pan = agregarRegistro(productos.id, { [nombreProducto.id]: 'Pan' }, '2026-10-01T10:00:00Z');
  const harina = agregarRegistro(
    insumos.id,
    { [nombre.id]: 'Harina', [precio.id]: 1200 },
    '2026-10-01T10:00:00Z',
  );
  const azucar = agregarRegistro(
    insumos.id,
    { [nombre.id]: 'Azúcar', [precio.id]: 850.5 },
    '2026-10-02T10:00:00Z',
  );
  const sal = agregarRegistro(insumos.id, { [nombre.id]: 'Sal' }, '2026-10-03T10:00:00Z');
  return { proyecto, productos, insumos, precio, pan, harina, azucar, sal };
}

async function sinViolaciones(elemento: HTMLElement) {
  const resultado = await axe(elemento, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

async function abrirReceta(proyectoId: string, user: UserEvent) {
  renderApp(`/proyectos/${proyectoId}/usar`);
  await verifyHeadingPresentAsync('Mi panadería');
  await user.click(await screen.findByRole('tab', { name: 'Ver filas' }, ESPERA));
  await user.click(await screen.findByRole('button', { name: 'Ver ficha de Pan' }, ESPERA));
  const ficha = screen.getByRole('complementary', { name: 'Ficha de la fila' });
  await user.click(within(ficha).getByRole('button', { name: 'Receta y costo' }));
  return within(await screen.findByRole('form', { name: 'Receta de Pan' }, ESPERA));
}

describe('ENS-160: la API simulada de recetas', () => {
  it('el costo es la suma de cantidad por precio de cada insumo, igual que el cálculo manual', async () => {
    const { pan, harina, azucar, precio, insumos } = await setupPanaderia();

    const receta = await guardarReceta(pan.id, {
      campoPrecioId: precio.id,
      lineas: [
        { insumoId: harina.id, cantidad: 0.5, unidad: 'kg' },
        { insumoId: azucar.id, cantidad: 2, unidad: null },
      ],
    });

    // 0,5 × 1.200 + 2 × 850,50 = 600 + 1.701
    expect(receta).toMatchObject({
      bloqueInsumosId: insumos.id,
      campoPrecioId: precio.id,
      costo: 2301,
      incompleta: false,
    });
    expect(receta.lineas.map((l) => [l.insumo, l.precioUnitario, l.subtotal])).toEqual([
      ['Harina', 1200, 600],
      ['Azúcar', 850.5, 1701],
    ]);
    expect(await obtenerReceta(pan.id)).toEqual(receta);
  });

  it('usa el precio actual de los insumos y avisa cuando falta uno', async () => {
    const { pan, harina, sal, precio } = await setupPanaderia();
    await guardarReceta(pan.id, {
      campoPrecioId: precio.id,
      lineas: [
        { insumoId: harina.id, cantidad: 1, unidad: null },
        { insumoId: sal.id, cantidad: 1, unidad: null },
      ],
    });

    const incompleta = await obtenerReceta(pan.id);
    expect(incompleta).toMatchObject({ costo: 1200, incompleta: true });
    expect(incompleta.lineas[1]).toMatchObject({
      insumo: 'Sal',
      precioUnitario: null,
      subtotal: null,
    });

    await editarRegistro(harina.id, { [precio.id]: 1500 });
    expect((await obtenerReceta(pan.id)).costo).toBe(1500);
  });

  it('un insumo borrado queda como eliminado y la receta como incompleta', async () => {
    const { pan, harina, azucar, precio } = await setupPanaderia();
    await guardarReceta(pan.id, {
      campoPrecioId: precio.id,
      lineas: [
        { insumoId: harina.id, cantidad: 1, unidad: null },
        { insumoId: azucar.id, cantidad: 1, unidad: null },
      ],
    });

    await eliminarRegistro(azucar.id);

    const receta = await obtenerReceta(pan.id);
    expect(receta.incompleta).toBe(true);
    expect(receta.lineas[1]).toMatchObject({ insumo: 'Insumo eliminado', subtotal: null });
  });

  it('una receta sin líneas se quita; un producto sin receta cuesta cero', async () => {
    const { pan, harina, precio } = await setupPanaderia();
    expect(await obtenerReceta(pan.id)).toMatchObject({ lineas: [], costo: 0, incompleta: false });
    await guardarReceta(pan.id, {
      campoPrecioId: precio.id,
      lineas: [{ insumoId: harina.id, cantidad: 1, unidad: null }],
    });

    const quitada = await guardarReceta(pan.id, { campoPrecioId: null, lineas: [] });

    expect(quitada).toMatchObject({ lineas: [], costo: 0, bloqueInsumosId: null });
    expect(estado.recetas.size).toBe(0);
  });

  it('valida las líneas y la columna del precio como el backend', async () => {
    const { pan, harina, azucar, precio, productos } = await setupPanaderia();
    const nombreProducto = Object.keys(pan.valores)[0] as string;
    const error = (receta: Parameters<typeof guardarReceta>[1]) =>
      guardarReceta(pan.id, receta).catch((e: unknown) => e);
    const linea = (insumoId: string, cantidad: number, unidad: string | null = null) => ({
      insumoId,
      cantidad,
      unidad,
    });

    expect(await error({ campoPrecioId: null, lineas: [linea(harina.id, 1)] })).toMatchObject({
      fieldErrors: { campoPrecioId: 'Elegí la columna con el precio de los insumos' },
    });
    expect(await error({ campoPrecioId: precio.id, lineas: [linea(harina.id, 0)] })).toMatchObject({
      fieldErrors: { 'lineas[0]': 'La cantidad tiene que ser mayor a cero' },
    });
    expect(
      await error({ campoPrecioId: precio.id, lineas: [linea(harina.id, 0.0001)] }),
    ).toMatchObject({ fieldErrors: { 'lineas[0]': 'La cantidad admite hasta 3 decimales' } });
    expect(
      await error({ campoPrecioId: precio.id, lineas: [linea(harina.id, 1), linea(harina.id, 2)] }),
    ).toMatchObject({ fieldErrors: { 'lineas[1]': 'Ese insumo está repetido' } });
    expect(await error({ campoPrecioId: precio.id, lineas: [linea(pan.id, 1)] })).toMatchObject({
      fieldErrors: { 'lineas[0]': 'Un producto no puede ser insumo de sí mismo' },
    });
    expect(
      await error({ campoPrecioId: precio.id, lineas: [linea(azucar.id, 1, 'x'.repeat(31))] }),
    ).toMatchObject({ fieldErrors: { 'lineas[0]': 'La unidad puede tener hasta 30 caracteres' } });
    expect(
      await error({ campoPrecioId: precio.id, lineas: [linea(crypto.randomUUID(), 1)] }),
    ).toMatchObject({
      fieldErrors: { 'lineas[0]': 'Ese insumo ya no existe' },
    });
    expect(
      await error({ campoPrecioId: nombreProducto, lineas: [linea(harina.id, 1)] }),
    ).toMatchObject({
      fieldErrors: { campoPrecioId: 'Elegí una columna numérica con el precio de los insumos' },
    });
    expect(productos.id).toBeTruthy();
  });
});

describe('ENS-160: armar la receta y ver el costo', () => {
  it('arma la receta eligiendo insumos y el costo mostrado coincide con el de la API', async () => {
    const { proyecto, pan, precio } = await setupPanaderia();
    const user = userEvent.setup();
    const receta = await abrirReceta(proyecto.id, user);

    expect(receta.getByText('Todavía no hay una receta guardada.')).toBeVisible();
    await waitFor(
      () => expect(receta.getByLabelText('¿Qué columna tiene el precio?')).toHaveValue(precio.id),
      ESPERA,
    );

    await user.click(receta.getByRole('button', { name: 'Agregar insumo' }));
    await user.click(await receta.findByRole('radio', { name: 'Harina' }, ESPERA));
    await user.type(receta.getByLabelText('Cantidad del insumo 1'), '0,5');
    await user.type(receta.getByLabelText('Unidad del insumo 1 (opcional)'), 'kg');
    await user.click(receta.getByRole('button', { name: 'Agregar insumo' }));
    await user.click(
      await receta
        .findAllByRole('radio', { name: 'Azúcar' }, ESPERA)
        .then((r) => r[0] as HTMLElement),
    );
    await user.type(receta.getByLabelText('Cantidad del insumo 2'), '2');
    await sinViolaciones(screen.getByRole('form', { name: 'Receta de Pan' }));
    await user.click(receta.getByRole('button', { name: 'Guardar receta' }));

    expect(await receta.findByText('Receta guardada.', {}, ESPERA)).toBeVisible();
    const api = await obtenerReceta(pan.id);
    expect(api.costo).toBe(2301);
    expect(api.lineas).toHaveLength(2);
    expect(api.campoPrecioId).toBe(precio.id);
    expect(await receta.findByText(/2\.301,00/, {}, ESPERA)).toBeVisible();
    expect(receta.getByText(/0,5 kg × .*1\.200,00 =/)).toBeVisible();
  });

  it('abre la receta guardada con sus insumos y avisa si falta el precio de alguno', async () => {
    const { proyecto, pan, harina, sal, precio } = await setupPanaderia();
    await guardarReceta(pan.id, {
      campoPrecioId: precio.id,
      lineas: [
        { insumoId: harina.id, cantidad: 3, unidad: 'kg' },
        { insumoId: sal.id, cantidad: 1, unidad: null },
      ],
    });
    const user = userEvent.setup();
    const receta = await abrirReceta(proyecto.id, user);

    expect(await receta.findByLabelText('Cantidad del insumo 1', {}, ESPERA)).toHaveValue('3');
    expect(receta.getByLabelText('Unidad del insumo 1 (opcional)')).toHaveValue('kg');
    expect(receta.getByLabelText('Cantidad del insumo 2')).toHaveValue('1');
    expect(receta.getByText(/3 kg × .*1\.200,00 =/)).toBeVisible();
    expect(receta.getByText('A «Sal» le falta el precio.')).toBeVisible();
    expect(
      receta.getByText(
        'Falta el precio de algún insumo (o ya no existe): este costo no está completo.',
      ),
    ).toBeVisible();
    expect(receta.getAllByText(/3\.600,00/).length).toBeGreaterThan(0);
  });

  it('pide el insumo, la cantidad y la columna del precio antes de enviar', async () => {
    const { proyecto } = await setupPanaderia();
    const user = userEvent.setup();
    const receta = await abrirReceta(proyecto.id, user);

    await user.click(receta.getByRole('button', { name: 'Agregar insumo' }));
    await user.click(receta.getByRole('button', { name: 'Guardar receta' }));

    expect(await receta.findByText('Elegí el insumo.')).toBeVisible();
    expect(estado.recetas.size).toBe(0);
    await user.click(await receta.findByRole('radio', { name: 'Harina' }, ESPERA));
    await user.click(receta.getByRole('button', { name: 'Guardar receta' }));
    expect(await receta.findByText('Escribí la cantidad, por ejemplo 2 o 0,5.')).toBeVisible();
    await user.type(receta.getByLabelText('Cantidad del insumo 1'), '0');
    await user.click(receta.getByRole('button', { name: 'Guardar receta' }));
    expect(await receta.findByText('La cantidad tiene que ser mayor a cero.')).toBeVisible();
  });

  it('muestra junto al insumo el error del servidor cuando ya no existe', async () => {
    const { proyecto, harina } = await setupPanaderia();
    const user = userEvent.setup();
    const receta = await abrirReceta(proyecto.id, user);
    await user.click(receta.getByRole('button', { name: 'Agregar insumo' }));
    await user.click(await receta.findByRole('radio', { name: 'Harina' }, ESPERA));
    await user.type(receta.getByLabelText('Cantidad del insumo 1'), '1');
    await eliminarRegistro(harina.id);

    await user.click(receta.getByRole('button', { name: 'Guardar receta' }));

    expect(await receta.findByText('Ese insumo ya no existe', {}, ESPERA)).toBeVisible();
  });

  it('quita un insumo y, sin ninguno, guardar deja al producto sin receta', async () => {
    const { proyecto, pan, harina, precio } = await setupPanaderia();
    await guardarReceta(pan.id, {
      campoPrecioId: precio.id,
      lineas: [{ insumoId: harina.id, cantidad: 1, unidad: null }],
    });
    const user = userEvent.setup();
    const receta = await abrirReceta(proyecto.id, user);

    await user.click(await receta.findByRole('button', { name: 'Quitar el insumo 1' }, ESPERA));
    expect(
      receta.getByText('Hay cambios sin guardar: el costo es el de la receta guardada.'),
    ).toBeVisible();
    await user.click(receta.getByRole('button', { name: 'Guardar receta' }));

    expect(
      await receta.findByText('Todavía no hay una receta guardada.', {}, ESPERA),
    ).toBeVisible();
    expect(estado.recetas.size).toBe(0);
  });

  it('cambia de tabla de insumos y vacía los insumos elegidos', async () => {
    const { proyecto } = await setupPanaderia();
    const user = userEvent.setup();
    const receta = await abrirReceta(proyecto.id, user);
    await user.click(receta.getByRole('button', { name: 'Agregar insumo' }));
    await user.click(await receta.findByRole('radio', { name: 'Harina' }, ESPERA));

    await user.selectOptions(
      receta.getByLabelText('¿En qué tabla están los insumos?'),
      'Productos',
    );

    expect(await receta.findByLabelText('Buscá en «Productos»', {}, ESPERA)).toHaveValue('');
    expect(
      await receta.findByText(
        'Esa tabla no tiene columnas numéricas. Agregá una con el precio en Armar.',
        {},
        ESPERA,
      ),
    ).toBeVisible();
  });
});
