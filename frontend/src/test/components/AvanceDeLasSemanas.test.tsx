import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { AvanceDeLasSemanas } from '@/pages/Objetivos/autogestion/AvanceDeLasSemanas';
import { manualContado, semanaCerrada } from '@/test/semanasDeObjetivo';
import type { SemanaObjetivo } from '@/types/objetivos';

const givenSemanasCerradas = (...semanas: SemanaObjetivo[]) => semanas;

const whenSeMuestraElAvance = (semanas: SemanaObjetivo[]) =>
  render(<AvanceDeLasSemanas semanas={semanas} />);

const columnasDelAvance = () =>
  within(screen.getByRole('list', { name: 'Cómo te fue en cada semana' })).getAllByRole('listitem');

const thenExplicaQueElGraficoSeArmaSolo = () => {
  expect(screen.getByText(/Este gráfico se arma solo/)).toBeVisible();
  expect(screen.queryByRole('list', { name: 'Cómo te fue en cada semana' })).toBeNull();
};

const thenHayUnaColumnaPorSemana = (cantidad: number) => {
  expect(columnasDelAvance()).toHaveLength(cantidad);
};

const thenLaColumnaTieneAcciones = (posicion: number, cantidad: number) => {
  const columna = columnasDelAvance()[posicion] as HTMLElement;
  expect(within(columna).getAllByTitle(/:/)).toHaveLength(cantidad);
};

const thenElResumenDiceHechas = (hechas: number, total: number) => {
  expect(screen.getByText(`Hiciste ${hechas} de ${total} acciones`)).toBeVisible();
};

describe('AvanceDeLasSemanas', () => {
  it('sin semanas cerradas explica que el gráfico se arma solo', () => {
    whenSeMuestraElAvance(givenSemanasCerradas());

    thenExplicaQueElGraficoSeArmaSolo();
  });

  it('una semana sin respuestas manuales no dibuja columnas', () => {
    const semanas = givenSemanasCerradas(
      semanaCerrada('2026-11-02', undefined),
      semanaCerrada('2026-11-09', []),
    );

    whenSeMuestraElAvance(semanas);

    thenExplicaQueElGraficoSeArmaSolo();
  });

  it('dibuja una columna por cada semana cerrada con acciones contadas', () => {
    const semanas = givenSemanasCerradas(
      semanaCerrada('2026-11-09', [manualContado('Probar el combo', 'SI')]),
      semanaCerrada('2026-11-02', [manualContado('Cartel nuevo', 'NO')]),
    );

    whenSeMuestraElAvance(semanas);

    thenHayUnaColumnaPorSemana(2);
  });

  it('pone un cuadrado por cada acción de la semana, empezando por la semana más vieja', () => {
    const semanas = givenSemanasCerradas(
      semanaCerrada('2026-11-09', [
        manualContado('Probar el combo', 'SI'),
        manualContado('Cartel nuevo', 'NO'),
      ]),
      semanaCerrada('2026-11-02', [manualContado('Hablar con clientes', 'EN_PARTE')]),
    );

    whenSeMuestraElAvance(semanas);

    thenLaColumnaTieneAcciones(0, 1);
    thenLaColumnaTieneAcciones(1, 2);
  });

  it('resume cuántas acciones se hicieron en total', () => {
    const semanas = givenSemanasCerradas(
      semanaCerrada('2026-11-09', [
        manualContado('Probar el combo', 'SI'),
        manualContado('Cartel nuevo', 'NO'),
      ]),
      semanaCerrada('2026-11-02', [manualContado('Hablar con clientes', 'EN_PARTE')]),
    );

    whenSeMuestraElAvance(semanas);

    thenElResumenDiceHechas(1, 3);
  });
});
