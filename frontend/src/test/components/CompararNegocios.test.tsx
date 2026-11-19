import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { CompararNegocios } from '@/components/negocios/CompararNegocios';
import type { ResumenConjunto } from '@/types/negocios';

type Usuario = ReturnType<typeof userEvent.setup>;

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

const ORDEN_POR_VENTAS = ['Almacén San Justo', 'Kiosco de la estación', 'Puesto de la feria'];
const ORDEN_POR_QUEDA_ASCENDENTE = [
  'Kiosco de la estación',
  'Almacén San Justo',
  'Puesto de la feria',
];

let user: Usuario;

describe('CompararNegocios', () => {
  it('muestra los totales de toda la cuenta', () => {
    dadoElResumenMostrado();

    entoncesLosTotalesSonDeToda();
  });

  it('no muestra los avisos pendientes en los totales', () => {
    dadoElResumenMostrado();

    entoncesLosTotalesNoMuestranAvisos();
  });

  it('arranca ordenada por lo vendido, con el negocio sin ventas al final', () => {
    dadoElResumenMostrado();

    entoncesLasFilasEstanEnOrden(ORDEN_POR_VENTAS);
  });

  it('arranca marcando la columna de lo vendido como descendente', () => {
    dadoElResumenMostrado();

    entoncesLaColumnaEstaOrdenada(/Vendiste/, 'descending');
  });

  it('tocar una columna ordena por ella', async () => {
    dadoElResumenMostrado();

    await cuandoSeTocaLaColumna(/Te queda/);

    entoncesLasFilasEstanEnOrden(ORDEN_POR_VENTAS);
  });

  it('tocar una columna dos veces da vuelta el orden', async () => {
    dadoElResumenMostrado();

    await cuandoSeTocaLaColumna(/Te queda/);
    await cuandoSeTocaLaColumna(/Te queda/);

    entoncesLasFilasEstanEnOrden(ORDEN_POR_QUEDA_ASCENDENTE);
  });

  it('la columna ordenada se marca y las otras no', async () => {
    dadoElResumenMostrado();

    await cuandoSeTocaLaColumna(/Te queda/);
    await cuandoSeTocaLaColumna(/Te queda/);

    entoncesLaColumnaEstaOrdenada(/Te queda/, 'ascending');
    entoncesLaColumnaNoEstaOrdenada(/Vendiste/);
  });

  it('cada fila dice sus números', () => {
    dadoElResumenMostrado();

    entoncesLaFilaDice('Kiosco de la estación', '$280.000');
  });

  it('marca lo negativo', () => {
    dadoElResumenMostrado();

    entoncesLoNegativoSeMarcaEnElKiosco();
  });

  it('no marca como en uso a un negocio que no se eligió', () => {
    dadoElResumenMostrado();

    entoncesLaFilaNoDice('Almacén San Justo', 'El que estás usando');
  });

  it('un negocio sin ventas lo dice', () => {
    dadoElResumenMostrado();

    entoncesLaFilaDice('Puesto de la feria', 'Sin ventas en estos 30 días');
  });

  it('un negocio sin ventas no inventa ceros', () => {
    dadoElResumenMostrado();

    entoncesLaFeriaNoInventaCeros();
  });
});

const tabla = () => screen.getByRole('table', { name: 'Negocio por negocio' });

const fila = (nombre: string) => {
  const encontrada = within(tabla())
    .getAllByRole('row')
    .find((candidata) => candidata.textContent?.includes(nombre));
  if (!encontrada) throw new Error(`No hay una fila de ${nombre}`);
  return encontrada;
};

function dadoElResumenMostrado() {
  render(<CompararNegocios resumen={RESUMEN} />);
  user = userEvent.setup();
}

async function cuandoSeTocaLaColumna(nombre: RegExp) {
  await user.click(screen.getByRole('button', { name: nombre }));
}

function entoncesLosTotalesSonDeToda() {
  const totales = screen.getByRole('region', { name: 'Todos tus negocios juntos' });
  expect(totales).toHaveTextContent('3 negocios');
  expect(within(totales).getByText('Vendiste').nextElementSibling).toHaveTextContent('$1.530.000');
  expect(within(totales).getByText('Te queda').nextElementSibling).toHaveTextContent('$768.000');
}

function entoncesLosTotalesNoMuestranAvisos() {
  const totales = screen.getByRole('region', { name: 'Todos tus negocios juntos' });
  expect(within(totales).queryByText('Avisos pendientes')).toBeNull();
}

function entoncesLasFilasEstanEnOrden(nombres: string[]) {
  const mostrados = within(tabla())
    .getAllByRole('rowheader')
    .map((celda) => celda.querySelector('.comparar-nombre')?.textContent);
  expect(mostrados).toEqual(nombres);
}

function entoncesLaColumnaEstaOrdenada(nombre: RegExp, sentido: string) {
  expect(screen.getByRole('columnheader', { name: nombre })).toHaveAttribute('aria-sort', sentido);
}

function entoncesLaColumnaNoEstaOrdenada(nombre: RegExp) {
  expect(screen.getByRole('columnheader', { name: nombre })).not.toHaveAttribute('aria-sort');
}

function entoncesLaFilaDice(nombre: string, texto: string) {
  expect(fila(nombre)).toHaveTextContent(texto);
}

function entoncesLaFilaNoDice(nombre: string, texto: string) {
  expect(fila(nombre)).not.toHaveTextContent(texto);
}

function entoncesLoNegativoSeMarcaEnElKiosco() {
  expect(within(fila('Kiosco de la estación')).getByText('-$72.000')).toHaveAttribute(
    'data-negativo',
    'true',
  );
}

function entoncesLaFeriaNoInventaCeros() {
  const feria = fila('Puesto de la feria');
  expect(within(feria).getAllByText('—')).toHaveLength(4);
  expect(feria).not.toHaveTextContent('$0');
}
