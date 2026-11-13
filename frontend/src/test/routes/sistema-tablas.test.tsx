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

/** Un almacén con muchos productos, abierto en su pantalla de siempre. */
const abrirSistemaConMuchosRegistros = async () => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const sistema = makeTemplate('shop', {}, false);
  sistema.name = 'Almacén de Ana';
  const inventario = allBlocks(sistema).find((bloque) => bloque.type === 'inventory') as Bloque;
  const clave = inventario.fields[0]?.key ?? 'name';
  inventario.rows = Array.from({ length: FILAS }, (_, indice) => ({
    id: `fila-${indice}`,
    [clave]: `Producto ${indice}`,
  }));
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp(`/sistemas/${sistema.id}?modo=usar`);
  await screen.findByRole('heading', { level: 1, name: /Almacén de Ana/ }, ESPERA);
  const bloque = document.querySelector<HTMLElement>(`[data-block="${inventario.id}"]`);
  if (!bloque) throw new Error('Falta el bloque del inventario');
  return { user: userEvent.setup(), bloque, inventario };
};

const filasDe = (bloque: HTMLElement) => bloque.querySelectorAll('tbody tr[data-record]');

describe('las tablas de la sección se cargan de a poco', () => {
  it('muestran 10 registros con el scroll del bloque y suman de a 10 hasta el final', async () => {
    const { user, bloque } = await abrirSistemaConMuchosRegistros();

    expect(filasDe(bloque)).toHaveLength(10);
    expect(bloque.querySelector('.table-scroll')).not.toBeNull();
    expect(within(bloque).getByText(`${FILAS} registros`)).toBeVisible();

    await user.click(
      within(bloque).getByRole('button', { name: `Mostrar 10 más · quedan ${FILAS - 10}` }),
    );
    expect(filasDe(bloque)).toHaveLength(20);

    await user.click(within(bloque).getByRole('button', { name: 'Mostrar 5 más · quedan 5' }));
    expect(filasDe(bloque)).toHaveLength(FILAS);
    expect(within(bloque).queryByRole('button', { name: /^Mostrar \d+ más/ })).toBeNull();
  });

  it('al llegar al final del scroll del bloque se suma la tanda siguiente', async () => {
    const { bloque } = await abrirSistemaConMuchosRegistros();
    const scroll = bloque.querySelector<HTMLElement>('.table-scroll');
    if (!scroll) throw new Error('Falta el scroll del bloque');
    Object.defineProperties(scroll, {
      scrollTop: { value: 400, configurable: true },
      clientHeight: { value: 375, configurable: true },
      scrollHeight: { value: 800, configurable: true },
    });

    fireEvent.scroll(scroll);

    await waitFor(() => expect(filasDe(bloque)).toHaveLength(20));
  });

  it('la búsqueda filtra sobre todos los registros, también los que todavía no se dibujaron', async () => {
    const { user, bloque } = await abrirSistemaConMuchosRegistros();

    await user.type(within(bloque).getByRole('textbox'), 'Producto 24');

    expect(filasDe(bloque)).toHaveLength(1);
    expect(filasDe(bloque)[0]).toHaveTextContent('Producto 24');
    expect(within(bloque).queryByRole('button', { name: /^Mostrar \d+ más/ })).toBeNull();

    await user.clear(within(bloque).getByRole('textbox'));
    expect(filasDe(bloque)).toHaveLength(10);
  });

  it('un registro que todavía no se dibujó aparece cuando se pide llevar la vista a él', async () => {
    const { bloque, inventario } = await abrirSistemaConMuchosRegistros();
    expect(bloque.querySelector('[data-record="fila-17"]')).toBeNull();

    act(() => pedirMostrarFila(inventario.id, 'fila-17'));

    await waitFor(() => expect(bloque.querySelector('[data-record="fila-17"]')).not.toBeNull());
    expect(filasDe(bloque)).toHaveLength(18);
  });
});
