import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { CompararNegocios } from '@/components/negocios/CompararNegocios';
import type { ResumenConjunto } from '@/types/negocios';

const RESUMEN: ResumenConjunto = {
  desde: '2026-10-15',
  hasta: '2026-11-13',
  dias: 30,
  total: {
    negocios: 3,
    vendiste: 1530000,
    gastos: 462000,
    queda: 768000,
    teDeben: 60000,
    avisosPendientes: 6,
  },
  negocios: [
    {
      id: 'kiosco',
      nombre: 'Kiosco de la estación',
      rubro: 'KIOSCO',
      activo: false,
      hayDatos: true,
      vendiste: 280000,
      gastos: 52000,
      queda: -72000,
      teDeben: 0,
      avisosPendientes: 2,
    },
    {
      id: 'feria',
      nombre: 'Puesto de la feria',
      rubro: 'OTRO',
      activo: false,
      hayDatos: false,
      vendiste: null,
      gastos: null,
      queda: null,
      teDeben: null,
      avisosPendientes: 1,
    },
    {
      id: 'almacen',
      nombre: 'Almacén San Justo',
      rubro: 'ALMACEN',
      activo: true,
      hayDatos: true,
      vendiste: 1250000,
      gastos: 410000,
      queda: 840000,
      teDeben: 60000,
      avisosPendientes: 3,
    },
  ],
};

const montar = (nombreDelRubro?: (codigo: string) => string | undefined) => {
  render(<CompararNegocios resumen={RESUMEN} nombreDelRubro={nombreDelRubro} />);
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

    const totales = screen.getByRole('region', { name: 'Toda la cuenta' });
    expect(totales).toHaveTextContent('3 negocios');
    expect(within(totales).getByText('Vendiste').nextElementSibling).toHaveTextContent(
      '$1.530.000',
    );
    expect(within(totales).getByText('Te queda').nextElementSibling).toHaveTextContent('$768.000');
    expect(within(totales).getByText('Avisos pendientes').nextElementSibling).toHaveTextContent(
      '6',
    );
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
    expect(feria).toHaveTextContent('Sin ventas en el período');
    expect(within(feria).getAllByText('—')).toHaveLength(4);
    expect(feria).not.toHaveTextContent('$0');
  });

  it('usa el nombre del rubro que da el servidor y, si no lo tiene, el código legible', () => {
    montar((codigo) => (codigo === 'ALMACEN' ? 'Almacén de barrio' : undefined));

    expect(fila('Almacén San Justo')).toHaveTextContent('Almacén de barrio');
    expect(fila('Kiosco de la estación')).toHaveTextContent('Kiosco');
  });
});
