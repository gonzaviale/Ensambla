import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { CompararNegocios } from '@/components/negocios/CompararNegocios';
import type { ResumenConjunto } from '@/types/negocios';

const RESUMEN: ResumenConjunto = {
  dias: 30,
  total: {
    negocios: 3,
    vendiste: 1530000,
    gastos: 462000,
    queda: 768000,
    teDeben: 60000,
  },
  negocios: [
    {
      id: 'kiosco',
      nombre: 'Kiosco de la estación',
      activo: false,
      hayDatos: true,
      vendiste: 280000,
      gastos: 52000,
      queda: -72000,
      teDeben: 0,
      avisosPendientes: null,
    },
    {
      id: 'feria',
      nombre: 'Puesto de la feria',
      activo: false,
      hayDatos: false,
      vendiste: null,
      gastos: null,
      queda: null,
      teDeben: null,
      avisosPendientes: null,
    },
    {
      id: 'almacen',
      nombre: 'Almacén San Justo',
      activo: true,
      hayDatos: true,
      vendiste: 1250000,
      gastos: 410000,
      queda: 840000,
      teDeben: 60000,
      avisosPendientes: null,
    },
  ],
};

const montar = () => {
  render(<CompararNegocios resumen={RESUMEN} />);
  return userEvent.setup();
};

const tabla = () => screen.getByRole('table', { name: 'Negocio por negocio' });
const nombresDeLasFilas = () =>
  within(tabla())
    .getAllByRole('rowheader')
    .map((celda) => celda.querySelector('.comparar-nombre')?.textContent);
const fila = (nombre: string) => {
  const encontrada = within(tabla())
    .getAllByRole('row')
    .find((candidata) => candidata.textContent?.includes(nombre));
  if (!encontrada) throw new Error(`No hay una fila de ${nombre}`);
  return encontrada;
};

describe('CompararNegocios', () => {
  it('muestra los totales de toda la cuenta', () => {
    montar();

    const totales = screen.getByRole('region', { name: 'Todos tus negocios juntos' });
    expect(totales).toHaveTextContent('3 negocios');
    expect(within(totales).getByText('Vendiste').nextElementSibling).toHaveTextContent(
      '$1.530.000',
    );
    expect(within(totales).getByText('Te queda').nextElementSibling).toHaveTextContent('$768.000');
    expect(within(totales).queryByText('Avisos pendientes')).toBeNull();
  });

  it('arranca ordenada por lo vendido, con el negocio sin ventas al final', () => {
    montar();

    expect(nombresDeLasFilas()).toEqual([
      'Almacén San Justo',
      'Kiosco de la estación',
      'Puesto de la feria',
    ]);
    expect(screen.getByRole('columnheader', { name: /Vendiste/ })).toHaveAttribute(
      'aria-sort',
      'descending',
    );
  });

  it('tocar una columna ordena por ella y tocarla de nuevo da vuelta el orden', async () => {
    const user = montar();

    await user.click(screen.getByRole('button', { name: /Te queda/ }));
    expect(nombresDeLasFilas()).toEqual([
      'Almacén San Justo',
      'Kiosco de la estación',
      'Puesto de la feria',
    ]);
    await user.click(screen.getByRole('button', { name: /Te queda/ }));

    expect(nombresDeLasFilas()).toEqual([
      'Kiosco de la estación',
      'Almacén San Justo',
      'Puesto de la feria',
    ]);
    expect(screen.getByRole('columnheader', { name: /Te queda/ })).toHaveAttribute(
      'aria-sort',
      'ascending',
    );
    expect(screen.getByRole('columnheader', { name: /Vendiste/ })).not.toHaveAttribute('aria-sort');
  });

  it('cada fila dice sus números, y marca lo negativo', () => {
    montar();

    const kiosco = fila('Kiosco de la estación');
    expect(kiosco).toHaveTextContent('$280.000');
    expect(within(kiosco).getByText('-$72.000')).toHaveAttribute('data-negativo', 'true');
    expect(fila('Almacén San Justo')).not.toHaveTextContent('El que estás usando');
  });

  it('un negocio sin ventas lo dice y no inventa ceros', () => {
    montar();

    const feria = fila('Puesto de la feria');
    expect(feria).toHaveTextContent('Sin ventas en estos 30 días');
    expect(within(feria).getAllByText('—')).toHaveLength(4);
    expect(feria).not.toHaveTextContent('$0');
  });
});
