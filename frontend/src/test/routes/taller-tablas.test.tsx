import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
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

type Usuario = ReturnType<typeof userEvent.setup>;

const FILAS_GUARDADAS = 25;
const FILAS_NUEVAS = 3;

const inventarioDe = (sistema: Sistema | undefined): Bloque => {
  const bloque = allBlocks(sistema).find((item) => item.type === 'inventory');
  if (!bloque) throw new Error('Falta el inventario');
  return bloque;
};

/** Un almacén ya creado con muchos productos guardados, abierto en el taller de Sami. */
const abrirTallerConMuchosRegistros = async () => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const sistema = makeTemplate('shop', {}, false);
  sistema.name = 'Almacén de Ana';
  const inventario = inventarioDe(sistema);
  const clave = inventario.fields[0]?.key ?? 'name';
  inventario.rows = Array.from({ length: FILAS_GUARDADAS }, (_, indice) => ({
    id: `guardada-${indice}`,
    [clave]: `Producto guardado ${indice}`,
  }));
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp(`/taller?sistema=${sistema.id}`);
  await verifyHeadingPresentAsync('Construyamos juntos');
  await screen.findByLabelText('Nombre de tu cuaderno', {}, ESPERA);
  return { user: userEvent.setup(), clave };
};

/** Lo que Sami cargaría de una foto: registros reales que no estaban en el cuaderno guardado. */
const cargarComoSami = (clave: string) =>
  useSistemaStore.getState().editarBorrador((borrador) => {
    const bloque = inventarioDe(borrador);
    const nuevas: Fila[] = Array.from({ length: FILAS_NUEVAS }, (_, indice) => ({
      id: `nueva-${indice}`,
      [clave]: `Producto de la foto ${indice}`,
    }));
    bloque.rows = [...bloque.rows, ...nuevas];
  });

const bloqueDeInventario = () => {
  const nombre = inventarioDe(useSistemaStore.getState().taller?.borrador ?? undefined).name;
  const bloque = screen
    .getByRole('button', { name: new RegExp(`^${nombre}`) })
    .closest<HTMLElement>('.block');
  if (!bloque) throw new Error(`Falta el bloque ${nombre}`);
  return bloque;
};

const filasDe = (bloque: HTMLElement) => bloque.querySelectorAll('tbody tr[data-record]');

const sumarMas = (user: Usuario, bloque: HTMLElement) =>
  user.click(within(bloque).getByRole('button', { name: /^Mostrar \d+ más/ }));

describe('las tablas de la propuesta de Sami', () => {
  it('muestran primero y resaltados los registros nuevos, y avisan cuántos son', async () => {
    const { clave } = await abrirTallerConMuchosRegistros();

    cargarComoSami(clave);

    await waitFor(() => expect(bloqueDeInventario().querySelector('tr.fila-nueva')).not.toBeNull());
    const bloque = bloqueDeInventario();
    const primeras = [...filasDe(bloque)].slice(0, FILAS_NUEVAS);
    expect(primeras.map((fila) => fila.getAttribute('data-record'))).toEqual([
      'nueva-0',
      'nueva-1',
      'nueva-2',
    ]);
    expect(within(bloque).getAllByText('Nuevo')).toHaveLength(FILAS_NUEVAS);
    expect(within(bloque).getByText(`${FILAS_NUEVAS} nuevos`)).toBeVisible();
    // Los que ya estaban guardados no se marcan.
    expect(bloque.querySelectorAll('tr.fila-nueva')).toHaveLength(FILAS_NUEVAS);
  });

  it('se cargan de a 10 con el scroll dentro del bloque, sin mostrar todos los registros', async () => {
    const { user, clave } = await abrirTallerConMuchosRegistros();
    cargarComoSami(clave);
    await waitFor(() => expect(bloqueDeInventario().querySelector('tr.fila-nueva')).not.toBeNull());
    const bloque = bloqueDeInventario();
    const total = FILAS_GUARDADAS + FILAS_NUEVAS;

    expect(filasDe(bloque)).toHaveLength(10);
    expect(bloque.querySelector('.table-scroll')).not.toBeNull();
    expect(
      within(bloque).getByRole('button', { name: `Mostrar 10 más · quedan ${total - 10}` }),
    ).toBeVisible();

    await sumarMas(user, bloque);
    expect(filasDe(bloque)).toHaveLength(20);

    await sumarMas(user, bloque);
    expect(filasDe(bloque)).toHaveLength(total);
    expect(within(bloque).queryByRole('button', { name: /^Mostrar \d+ más/ })).toBeNull();
  });

  it('el buscador del bloque filtra sobre todos los registros y vuelve a la primera tanda', async () => {
    const { user, clave } = await abrirTallerConMuchosRegistros();
    cargarComoSami(clave);
    await waitFor(() => expect(bloqueDeInventario().querySelector('tr.fila-nueva')).not.toBeNull());
    const bloque = bloqueDeInventario();
    await sumarMas(user, bloque);

    await user.type(within(bloque).getByRole('textbox'), 'guardado 24');

    expect(filasDe(bloque)).toHaveLength(1);
    expect(within(bloque).queryByRole('button', { name: /^Mostrar \d+ más/ })).toBeNull();
  });

  it('con una sola sección no se muestran las pestañas; con más de una, sí', async () => {
    const { user } = await abrirTallerConMuchosRegistros();
    expect(screen.queryByRole('navigation', { name: 'Secciones del cuaderno' })).toBeNull();
    expect(screen.queryByText('Vista general')).toBeNull();

    await user.click(screen.getByRole('button', { name: 'Agregar y organizar' }));
    await user.click(screen.getByRole('button', { name: 'Agregar una sección' }));
    const dialogo = await screen.findByRole('dialog');
    await user.type(within(dialogo).getByLabelText('Nombre de la sección'), 'Ventas');
    await user.click(within(dialogo).getByRole('button', { name: 'Agregar sección' }));

    const pestanas = await screen.findByRole('navigation', { name: 'Secciones del cuaderno' });
    expect(within(pestanas).getAllByRole('button')).toHaveLength(2);
    expect(useSistemaStore.getState().taller?.borrador?.sections).toHaveLength(2);
    // Es la propuesta la que cambia: el cuaderno guardado sigue con su sección.
    expect(useSistemaStore.getState().sistemas[0]?.sections).toHaveLength(1);
  });

  it('las secciones se quitan desde el desplegable, sobre la propuesta', async () => {
    const { user } = await abrirTallerConMuchosRegistros();
    useSistemaStore.getState().editarBorrador((borrador) => {
      borrador.sections.push({ id: 'otra', name: 'Ventas', blocks: [] });
    });
    await screen.findByRole('navigation', { name: 'Secciones del cuaderno' });

    await user.click(screen.getByRole('button', { name: 'Agregar y organizar' }));
    await user.click(screen.getByRole('button', { name: 'Secciones' }));
    const dialogo = await screen.findByRole('dialog');
    const quitar = within(dialogo).getAllByRole('button', {
      name: 'Quitar sección y conservar bloques',
    });
    await user.click(quitar[1] as HTMLElement);

    await waitFor(() =>
      expect(useSistemaStore.getState().taller?.borrador?.sections).toHaveLength(1),
    );
    await clickButton(user, 'Guardar nombres');
    expect(screen.queryByRole('navigation', { name: 'Secciones del cuaderno' })).toBeNull();
    expect(useSistemaStore.getState().sistemas[0]?.sections).toHaveLength(1);
  });

  it('ya no muestra «Distribuí tus piezas», «Sumar una pieza» ni el cartel de sin guardar', async () => {
    await abrirTallerConMuchosRegistros();

    expect(screen.queryByText('Distribuí tus piezas')).toBeNull();
    expect(screen.queryByText(/Sumar una pieza/)).toBeNull();
    expect(screen.queryByText(/sin guardar/i)).toBeNull();
    expect(screen.getByRole('button', { name: 'Distribución de los bloques' })).toBeVisible();
  });
});
