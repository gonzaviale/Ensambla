import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { PreviewBoard } from '@/components/sistema/PreviewBoard';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';

describe('las filas de ejemplo en la vista previa', () => {
  it('se ven distintas: etiqueta «Ejemplo» y estilo propio, sin avisos ni píldoras de columnas', () => {
    const sistema = makeTemplate('shop', {}, true);
    const inventario = allBlocks(sistema).find((bloque) => bloque.type === 'inventory');
    const { container } = render(<PreviewBoard sistema={sistema} />);

    const bloque = screen.getByText(inventario?.name ?? '').closest('section');
    if (!bloque) throw new Error('Falta el bloque');
    expect(container.querySelectorAll('tr.example-row').length).toBeGreaterThan(0);
    expect(within(bloque).getAllByText('Ejemplo').length).toBeGreaterThan(0);
    expect(screen.queryByText('Datos de ejemplo, no son reales')).not.toBeInTheDocument();
    expect(container.querySelector('.preview-field-tags')).toBeNull();
  });

  it('las filas reales no llevan marca', () => {
    const sistema = makeTemplate('shop', {}, true);
    for (const bloque of allBlocks(sistema)) {
      for (const fila of bloque.rows) delete fila.__example;
    }
    const { container } = render(<PreviewBoard sistema={sistema} />);

    expect(container.querySelector('tr.example-row')).toBeNull();
    expect(screen.queryByText('Ejemplo')).not.toBeInTheDocument();
  });

  it('con «onRegistro» cada fila tiene su lápiz y cada bloque «Agregar registro»', async () => {
    const sistema = makeTemplate('shop', {}, true);
    const inventario = allBlocks(sistema).find((bloque) => bloque.type === 'inventory');
    if (!inventario) throw new Error('Falta el inventario');
    const llamadas: [string, string | null][] = [];
    render(
      <PreviewBoard
        sistema={sistema}
        editable
        onRegistro={(bloqueId, filaId) => llamadas.push([bloqueId, filaId])}
      />,
    );

    const tabla = screen.getByText(inventario.name).closest('section');
    if (!tabla) throw new Error('Falta el bloque');
    const lapices = within(tabla).getAllByRole('button', { name: 'Editar registro' });
    const [primerLapiz] = lapices;
    const primeraFila = inventario.rows[0];
    if (!primerLapiz || !primeraFila) throw new Error('Faltan registros de ejemplo');
    expect(lapices).toHaveLength(Math.min(inventario.rows.length, 3));

    await userEvent.click(primerLapiz);
    await userEvent.click(within(tabla).getByRole('button', { name: '+ Agregar registro' }));
    expect(llamadas).toEqual([
      [inventario.id, primeraFila.id],
      [inventario.id, null],
    ]);
  });

  it('sin «editable» la vista previa no deja editar ni agregar', () => {
    const sistema = makeTemplate('shop', {}, true);
    render(<PreviewBoard sistema={sistema} onRegistro={() => undefined} />);

    expect(screen.queryByRole('button', { name: 'Editar registro' })).toBeNull();
    expect(screen.queryByRole('button', { name: '+ Agregar registro' })).toBeNull();
  });

  it('con más filas de las que entran ofrece «Ver todas» y después «Ver menos»', async () => {
    const sistema = makeTemplate('shop', {}, true);
    const inventario = allBlocks(sistema).find((bloque) => bloque.type === 'inventory');
    if (!inventario) throw new Error('Falta el inventario');
    const base = inventario.rows[0];
    inventario.rows = Array.from({ length: 16 }, (_, i) => ({ ...base, id: `fila-${i}`, __example: undefined }));
    render(<PreviewBoard sistema={sistema} />);

    const tabla = screen.getByText(inventario.name).closest('section');
    if (!tabla) throw new Error('Falta el bloque');
    expect(within(tabla).getAllByRole('row')).toHaveLength(1 + 3);

    await userEvent.click(within(tabla).getByRole('button', { name: 'Ver todas (16)' }));
    expect(within(tabla).getAllByRole('row')).toHaveLength(1 + 16);

    await userEvent.click(within(tabla).getByRole('button', { name: 'Ver menos' }));
    expect(within(tabla).getAllByRole('row')).toHaveLength(1 + 3);
  });
});
