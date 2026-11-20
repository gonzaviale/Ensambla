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

export const dadoQueHoyEsMiercoles = () => fingirQueHoyEs(MIERCOLES);
export const dadoQueHoyEsLunes = () => fingirQueHoyEs(LUNES);
export const volverAlTiempoReal = () => vi.useRealTimers();

type Respuesta = { hizo: string; quePaso: string; comentario?: string };
type Vista = BoundFunctions<typeof queries>;

export const seccionDeSami = async () =>
  within(await screen.findByRole('region', { name: /Sami quiere saber cómo te fue/ }, ESPERA));

export const formularioDe = (titulo: string): Vista =>
  within(screen.getByRole('form', { name: `Contale a Sami cómo te fue con «${titulo}»` }));

const tocar = async (vista: Vista, nombre: string) => {
  await userEvent.click(vista.getByRole('button', { name: nombre }));
};

export const cuandoSePasaALaSiguientePregunta = async () => {
  await userEvent.click(await screen.findByRole('button', { name: 'Pregunta siguiente' }, ESPERA));
};

export const cuandoSeVuelveALaPreguntaAnterior = async () => {
  await userEvent.click(screen.getByRole('button', { name: 'Pregunta anterior' }));
};

export const cuandoSeDejaParaMasTarde = async (titulo: string) => {
  await userEvent.click(screen.getByRole('button', { name: `Más tarde: ${titulo}` }));
};

export const cuandoSeElige = (titulo: string, opcion: string) =>
  tocar(formularioDe(titulo), opcion);

export const cuandoSeVuelveUnPaso = (titulo: string) => tocar(formularioDe(titulo), 'Volver');

export const cuandoSeEnvia = (titulo: string) => tocar(formularioDe(titulo), 'Contarle a Sami');

export const cuandoSeEscribeElComentario = async (titulo: string, comentario: string) => {
  const campo = formularioDe(titulo).getByLabelText('Contale un poco más (opcional)');
  await userEvent.type(campo, comentario);
};

export const cuandoSeResponde = async (titulo: string, respuesta: Respuesta) => {
  await cuandoSeElige(titulo, respuesta.hizo);
  await cuandoSeElige(titulo, respuesta.quePaso);
  if (respuesta.comentario) await cuandoSeEscribeElComentario(titulo, respuesta.comentario);
  await cuandoSeEnvia(titulo);
};

export const entoncesNoHayPreguntasDeSami = async () => {
  await waitFor(() => {
    expect(
      screen.queryByRole('region', { name: /Sami quiere saber cómo te fue/ }),
    ).not.toBeInTheDocument();
  }, ESPERA);
};

export const entoncesSePreguntaPor = (seccion: Vista, titulos: string[]) => {
  const preguntas = seccion
    .getAllByRole('heading', { level: 3 })
    .map((titulo) => titulo.textContent);
  expect(preguntas).toEqual(titulos);
};

export const entoncesElPaginadorDice = (seccion: Vista, posicion: string) => {
  expect(seccion.getByRole('navigation', { name: 'Preguntas de Sami' })).toHaveTextContent(
    posicion,
  );
};

export const entoncesNoHayPaginador = (seccion: Vista) => {
  expect(seccion.queryByRole('navigation', { name: 'Preguntas de Sami' })).not.toBeInTheDocument();
};

export const entoncesSePideElPaso = (titulo: string, pregunta: string) => {
  expect(formularioDe(titulo).getByText(pregunta)).toBeVisible();
};

export const entoncesNoSePideTodavia = (titulo: string, pregunta: string) => {
  expect(formularioDe(titulo).queryByText(pregunta)).not.toBeInTheDocument();
};

export const entoncesLaOpcionEsta = (
  titulo: string,
  opcion: string,
  estado: 'elegida' | 'sinElegir',
) => {
  const boton = formularioDe(titulo).getByRole('button', { name: opcion });
  expect(boton).toHaveAttribute('aria-pressed', estado === 'elegida' ? 'true' : 'false');
};

export const entoncesLaOpcionNoSePuedeElegir = (titulo: string, opcion: string) => {
  expect(formularioDe(titulo).getByRole('button', { name: opcion })).toBeDisabled();
};

export const entoncesNoHayBotonDeEnviar = (titulo: string) => {
  expect(
    formularioDe(titulo).queryByRole('button', { name: 'Contarle a Sami' }),
  ).not.toBeInTheDocument();
};
