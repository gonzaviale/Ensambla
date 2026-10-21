import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { PreviewBoard } from '@/components/sistema/PreviewBoard';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';

describe('las filas de ejemplo en la vista previa', () => {
  it('se ven distintas: etiqueta «Ejemplo», estilo propio y un aviso de que no son reales', () => {
    const sistema = makeTemplate('shop', {}, true);
    const inventario = allBlocks(sistema).find((bloque) => bloque.type === 'inventory');
    const { container } = render(<PreviewBoard sistema={sistema} />);

    const bloque = screen.getByText(inventario?.name ?? '').closest('section');
    if (!bloque) throw new Error('Falta el bloque');
    expect(container.querySelectorAll('tr.example-row').length).toBeGreaterThan(0);
    expect(within(bloque).getAllByText('Ejemplo').length).toBeGreaterThan(0);
    expect(within(bloque).getByText('Datos de ejemplo, no son reales')).toBeVisible();
  });

  it('las filas reales no llevan marca ni aviso', () => {
    const sistema = makeTemplate('shop', {}, true);
    for (const bloque of allBlocks(sistema)) {
      for (const fila of bloque.rows) delete fila.__example;
    }
    const { container } = render(<PreviewBoard sistema={sistema} />);

    expect(container.querySelector('tr.example-row')).toBeNull();
    expect(screen.queryByText('Ejemplo')).not.toBeInTheDocument();
    expect(screen.queryByText('Datos de ejemplo, no son reales')).not.toBeInTheDocument();
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
