import { screen, waitFor, within, type BoundFunctions, type queries } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, vi } from 'vitest';
import { ESPERA } from '@/test/testHelpers';

const MIERCOLES = '2026-11-18';
const LUNES = '2026-11-16';

const fingirQueHoyEs = (dia: string) => {
  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(new Date(`${dia}T15:00:00`));
};

export const givenQueHoyEsMiercoles = () => fingirQueHoyEs(MIERCOLES);
export const givenQueHoyEsLunes = () => fingirQueHoyEs(LUNES);
export const volverAlTiempoReal = () => vi.useRealTimers();

export const seccionDeSami = async () =>
  within(await screen.findByRole('region', { name: /Sami quiere saber cómo te fue/ }, ESPERA));

export const whenSeVenLasOtrasPreguntas = async () => {
  await userEvent.click(await screen.findByRole('button', { name: /Ver las otras/ }, ESPERA));
};

export const whenSeDejaParaMasTarde = async (titulo: string) => {
  await userEvent.click(screen.getByRole('button', { name: `Más tarde: ${titulo}` }));
};

export const thenNoHayPreguntasDeSami = async () => {
  await waitFor(() => {
    expect(screen.queryByRole('region', { name: /Sami quiere saber cómo te fue/ })).not.toBeInTheDocument();
  }, ESPERA);
};

export const thenSePreguntaPor = (seccion: BoundFunctions<typeof queries>, titulos: string[]) => {
  const preguntas = seccion.getAllByRole('heading', { level: 3 }).map((titulo) => titulo.textContent);
  expect(preguntas).toEqual(titulos);
};
