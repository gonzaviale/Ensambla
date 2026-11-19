import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { AvanceDeLasSemanas } from '@/pages/Objetivos/autogestion/AvanceDeLasSemanas';
import { manualContado, semanaCerrada } from '@/test/semanasDeObjetivo';
import type { SemanaObjetivo } from '@/types/objetivos';

const dadoSemanasCerradas = (...semanas: SemanaObjetivo[]) => semanas;

const cuandoSeMuestraElAvance = (semanas: SemanaObjetivo[]) =>
  render(<AvanceDeLasSemanas semanas={semanas} />);

const columnasDelAvance = () =>
  within(screen.getByRole('list', { name: 'Cómo te fue en cada semana' })).getAllByRole('listitem');

const entoncesExplicaQueElGraficoSeArmaSolo = () => {
  expect(screen.getByText(/Este gráfico se arma solo/)).toBeVisible();
  expect(screen.queryByRole('list', { name: 'Cómo te fue en cada semana' })).toBeNull();
};

const entoncesHayUnaColumnaPorSemana = (cantidad: number) => {
  expect(columnasDelAvance()).toHaveLength(cantidad);
};

const entoncesLaColumnaTieneAcciones = (posicion: number, cantidad: number) => {
  const columna = columnasDelAvance()[posicion] as HTMLElement;
  expect(within(columna).getAllByTitle(/:/)).toHaveLength(cantidad);
};

const entoncesElResumenDiceHechas = (hechas: number, total: number) => {
  expect(screen.getByText(`Hiciste ${hechas} de ${total} acciones`)).toBeVisible();
};

describe('AvanceDeLasSemanas', () => {
  it('sin semanas cerradas explica que el gráfico se arma solo', () => {
    cuandoSeMuestraElAvance(dadoSemanasCerradas());

    entoncesExplicaQueElGraficoSeArmaSolo();
  });

  it('una semana sin respuestas manuales no dibuja columnas', () => {
    const semanas = dadoSemanasCerradas(
      semanaCerrada('2026-11-02', undefined),
      semanaCerrada('2026-11-09', []),
    );

    cuandoSeMuestraElAvance(semanas);

    entoncesExplicaQueElGraficoSeArmaSolo();
  });

  it('dibuja una columna por cada semana cerrada con acciones contadas', () => {
    const semanas = dadoSemanasCerradas(
      semanaCerrada('2026-11-09', [manualContado('Probar el combo', 'SI')]),
      semanaCerrada('2026-11-02', [manualContado('Cartel nuevo', 'NO')]),
    );

    cuandoSeMuestraElAvance(semanas);

    entoncesHayUnaColumnaPorSemana(2);
  });

  it('pone un cuadrado por cada acción de la semana, empezando por la semana más vieja', () => {
    const semanas = dadoSemanasCerradas(
      semanaCerrada('2026-11-09', [
        manualContado('Probar el combo', 'SI'),
        manualContado('Cartel nuevo', 'NO'),
      ]),
      semanaCerrada('2026-11-02', [manualContado('Hablar con clientes', 'EN_PARTE')]),
    );

    cuandoSeMuestraElAvance(semanas);

    entoncesLaColumnaTieneAcciones(0, 1);
    entoncesLaColumnaTieneAcciones(1, 2);
  });

  it('resume cuántas acciones se hicieron en total', () => {
    const semanas = dadoSemanasCerradas(
      semanaCerrada('2026-11-09', [
        manualContado('Probar el combo', 'SI'),
        manualContado('Cartel nuevo', 'NO'),
      ]),
      semanaCerrada('2026-11-02', [manualContado('Hablar con clientes', 'EN_PARTE')]),
    );

    cuandoSeMuestraElAvance(semanas);

    entoncesElResumenDiceHechas(1, 3);
  });
});
