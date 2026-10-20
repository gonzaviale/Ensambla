import { render, screen, within } from '@testing-library/react';
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
});
