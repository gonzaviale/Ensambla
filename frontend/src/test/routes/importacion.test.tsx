import { act, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { findBlock, makeSystem } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyToastAsync } from '@/test/testHelpers';

// El almacén de la prueba: un inventario con un producto que ya estaba cargado antes de leer la foto.
const sistemaConProducto = () => {
  const sistema = makeSystem('Mi almacén', ['inventory']);
  const inventario = sistema.sections[0]?.blocks[0];
  inventario?.rows.push({ id: 'previa', name: 'Arroz', quantity: 5, price: 1000 });
  return { sistema, inventarioId: inventario?.id ?? '' };
};

const abrirImportador = async () => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const { sistema, inventarioId } = sistemaConProducto();
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp(`/sistemas/${sistema.id}?modo=usar`);
  await screen.findByRole('heading', { level: 1, name: /Mi almacén/ }, ESPERA);
  const user = userEvent.setup();
  act(() =>
    useSistemaStore.getState().abrirModal({
      tipo: 'importar',
      sistemaId: sistema.id,
      bloqueId: inventarioId,
      contexto: 'registros',
    }),
  );
  await user.click(await screen.findByRole('button', { name: /Probar con un ejemplo/ }));
  await screen.findByRole('heading', { name: 'Confirmar registros' }, ESPERA);
  return { user, inventarioId };
};

const inventario = (inventarioId: string) => {
  const bloque = findBlock(seleccionarSistemaActivo(useSistemaStore.getState()), inventarioId);
  if (!bloque) throw new Error('Falta el inventario');
  return bloque;
};

const tarjeta = (numero: number) => screen.getByRole('article', { name: `Registro ${numero}` });
const columnasNuevas = () => screen.getByRole('region', { name: 'Campos nuevos detectados' });
const incorporar = (user: ReturnType<typeof userEvent.setup>) =>
  user.click(screen.getByRole('button', { name: /Incorporar registros/ }));

describe('la revisión de lo que leyó una foto, en tarjetas', () => {
  it('muestra un registro por tarjeta, con la foto de referencia al lado y los datos para corregir', async () => {
    await abrirImportador();

    expect(screen.getAllByRole('article')).toHaveLength(3);
    expect(screen.getByLabelText('Ejemplo de una anotación')).toBeVisible();
    expect(within(screen.getByRole('dialog')).queryByRole('table')).toBeNull();
    expect(within(tarjeta(1)).getByLabelText('Producto, registro 1')).toHaveValue(
      'Yerba Playadito 1 kg',
    );
    expect(screen.getByText('Quedan 2 datos por revisar.')).toBeVisible();
  });

  it('un dato dudoso se marca con texto y se resuelve corrigiéndolo o con «Está bien»', async () => {
    const { user } = await abrirImportador();

    const aviso = within(tarjeta(2)).getByText(
      /Revisá este dato: La letra no se lee con claridad./,
    );
    expect(aviso).toBeVisible();
    expect(aviso.closest('.field')).toHaveClass('con-duda');
    expect(within(tarjeta(1)).queryByText(/Revisá este dato/)).toBeNull();

    await user.clear(within(tarjeta(2)).getByLabelText('Producto, registro 2'));
    await user.type(within(tarjeta(2)).getByLabelText('Producto, registro 2'), 'Coca-Cola 1 L');
    expect(within(tarjeta(2)).queryByText(/Revisá este dato/)).toBeNull();
    expect(screen.getByText('Queda 1 dato por revisar.')).toBeVisible();

    await user.click(within(tarjeta(3)).getByRole('button', { name: /Está bien/ }));
    expect(screen.getByText('No quedan datos por revisar.')).toBeVisible();
  });

  it('quita un registro y se puede deshacer', async () => {
    const { user } = await abrirImportador();

    await user.click(screen.getByRole('button', { name: 'Quitar el registro 2' }));

    expect(screen.getAllByRole('article')).toHaveLength(2);
    expect(screen.getByText('Quitaste el registro 2.')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Deshacer' }));
    expect(screen.getAllByRole('article')).toHaveLength(3);
    expect(within(tarjeta(2)).getByLabelText('Producto, registro 2')).toBeVisible();
  });
});

describe('«Campos nuevos detectados»', () => {
  it('lista la columna que el bloque no tiene, con su tipo sugerido y ejemplos, y la muestra en las tarjetas', async () => {
    await abrirImportador();

    const tarjetaNueva = within(columnasNuevas());
    expect(tarjetaNueva.getByLabelText('Nombre del campo nuevo')).toHaveValue('Proveedor');
    expect(tarjetaNueva.getByLabelText('Tipo de dato sugerido')).toHaveValue('text');
    expect(tarjetaNueva.getByText(/Distribuidora Sur · Mayorista Norte/)).toBeVisible();
    expect(within(tarjeta(1)).getByLabelText('Proveedor, registro 1')).toHaveValue(
      'Distribuidora Sur',
    );
    expect(within(tarjeta(1)).getByText('Campo nuevo')).toBeVisible();
  });

  it('hasta decidir qué hacer con el campo no se puede incorporar nada', async () => {
    const { user, inventarioId } = await abrirImportador();

    await incorporar(user);

    expect(await screen.findByText('Aceptá o descartá los campos nuevos.')).toBeVisible();
    expect(inventario(inventarioId).rows).toHaveLength(1);
    expect(inventario(inventarioId).fields).toHaveLength(5);
  });

  it('un campo aceptado, renombrado y con otro tipo se crea con los registros, sin tocar los que había', async () => {
    const { user, inventarioId } = await abrirImportador();
    const nuevas = within(columnasNuevas());

    await user.clear(nuevas.getByLabelText('Nombre del campo nuevo'));
    await user.type(nuevas.getByLabelText('Nombre del campo nuevo'), 'Quién lo vende');
    await user.selectOptions(nuevas.getByLabelText('Tipo de dato sugerido'), 'textarea');
    await user.click(nuevas.getByRole('button', { name: /Aceptar/ }));
    expect(nuevas.getByText('Aceptado')).toBeVisible();
    expect(within(tarjeta(1)).queryByText('Campo nuevo')).toBeNull();
    expect(within(tarjeta(1)).getByLabelText('Quién lo vende, registro 1')).toBeVisible();
    await incorporar(user);

    await verifyToastAsync('3 registros incorporados · 1 campo nuevo.');
    const bloque = inventario(inventarioId);
    const campo = bloque.fields.at(-1);
    expect(bloque.fields).toHaveLength(6);
    expect(campo).toMatchObject({ label: 'Quién lo vende', type: 'textarea', required: false });
    expect(bloque.rows).toHaveLength(4);
    const nuevos = bloque.rows.filter((fila) => fila.id !== 'previa');
    expect(nuevos.map((fila) => fila[campo?.key ?? ''])).toEqual([
      'Distribuidora Sur',
      'Mayorista Norte',
      'Distribuidora Sur',
    ]);
    const previa = bloque.rows.find((fila) => fila.id === 'previa');
    expect(previa).toMatchObject({ name: 'Arroz', quantity: 5, price: 1000 });
    expect(previa).not.toHaveProperty(campo?.key ?? 'x');
  });

  it('un campo descartado no se crea y sus datos se ignoran', async () => {
    const { user, inventarioId } = await abrirImportador();

    await user.click(within(columnasNuevas()).getByRole('button', { name: /Descartar/ }));
    expect(screen.queryByLabelText('Proveedor, registro 1')).toBeNull();
    expect(within(columnasNuevas()).getByText('Descartado')).toBeVisible();
    await incorporar(user);

    await verifyToastAsync('3 registros incorporados.');
    const bloque = inventario(inventarioId);
    expect(bloque.fields).toHaveLength(5);
    expect(bloque.rows).toHaveLength(4);
    for (const fila of bloque.rows) expect(JSON.stringify(fila)).not.toContain('Distribuidora');
  });

  it('un descarte se puede reabrir y entonces vuelve a pedir una decisión', async () => {
    const { user } = await abrirImportador();

    await user.click(within(columnasNuevas()).getByRole('button', { name: /Descartar/ }));
    await user.click(within(columnasNuevas()).getByRole('button', { name: /Volver a sumarlo/ }));

    expect(within(columnasNuevas()).getByRole('button', { name: /Aceptar/ })).toBeVisible();
    expect(screen.getByLabelText('Proveedor, registro 1')).toBeVisible();
  });

  it('no deja aceptar un nombre repetido ni vacío', async () => {
    const { user } = await abrirImportador();
    const nuevas = within(columnasNuevas());
    const nombre = nuevas.getByLabelText('Nombre del campo nuevo');

    await user.clear(nombre);
    await user.type(nombre, 'Precio');

    expect(await nuevas.findByText('Ya hay un campo con ese nombre.')).toBeVisible();
    expect(nuevas.getByRole('button', { name: /Aceptar/ })).toBeDisabled();
    await user.clear(nombre);
    expect(await nuevas.findByText('Elegí un nombre para el campo.')).toBeVisible();
  });

  it('si algún dato no cumple el tipo elegido no se guarda nada: ni el campo ni los registros', async () => {
    const { user, inventarioId } = await abrirImportador();
    const nuevas = within(columnasNuevas());

    await user.selectOptions(nuevas.getByLabelText('Tipo de dato sugerido'), 'number');
    await user.click(nuevas.getByRole('button', { name: /Aceptar/ }));
    await incorporar(user);

    expect(
      await screen.findByText(/Revisá el registro 1: Ingresá un número válido./),
    ).toBeVisible();
    expect(within(tarjeta(1)).getByText('Ingresá un número válido.')).toBeVisible();
    const bloque = inventario(inventarioId);
    expect(bloque.fields).toHaveLength(5);
    expect(bloque.rows).toHaveLength(1);
    expect(screen.getByRole('dialog')).toBeVisible();
  });
});
