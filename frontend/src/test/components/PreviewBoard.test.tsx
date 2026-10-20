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

  it('avisa cuántas filas se ven cuando la tabla tiene más de las que entran en la vista previa', () => {
    const sistema = makeTemplate('shop', {}, true);
    const inventario = allBlocks(sistema).find((bloque) => bloque.type === 'inventory');
    if (!inventario) throw new Error('Falta el inventario');
    const base = inventario.rows[0];
    inventario.rows = Array.from({ length: 16 }, (_, i) => ({ ...base, id: `fila-${i}`, __example: undefined }));
    render(<PreviewBoard sistema={sistema} />);

    expect(screen.getByText('Se ven 3 de 16 filas; el resto está cargado.')).toBeVisible();
  });
});
