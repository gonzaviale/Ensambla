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

type Respuesta = { hizo: string; quePaso: string; comentario?: string };
type Vista = BoundFunctions<typeof queries>;

export const seccionDeSami = async () =>
  within(await screen.findByRole('region', { name: /Sami quiere saber cómo te fue/ }, ESPERA));

export const formularioDe = (titulo: string): Vista =>
  within(screen.getByRole('form', { name: `Contale a Sami cómo te fue con «${titulo}»` }));

const tocar = async (vista: Vista, nombre: string) => {
  await userEvent.click(vista.getByRole('button', { name: nombre }));
};

export const whenSePasaALaSiguientePregunta = async () => {
  await userEvent.click(await screen.findByRole('button', { name: 'Pregunta siguiente' }, ESPERA));
};

export const whenSeVuelveALaPreguntaAnterior = async () => {
  await userEvent.click(screen.getByRole('button', { name: 'Pregunta anterior' }));
};

export const whenSeDejaParaMasTarde = async (titulo: string) => {
  await userEvent.click(screen.getByRole('button', { name: `Más tarde: ${titulo}` }));
};

export const whenSeElige = (titulo: string, opcion: string) => tocar(formularioDe(titulo), opcion);

export const whenSeVuelveUnPaso = (titulo: string) => tocar(formularioDe(titulo), 'Volver');

export const whenSeEnvia = (titulo: string) => tocar(formularioDe(titulo), 'Contarle a Sami');

export const whenSeEscribeElComentario = async (titulo: string, comentario: string) => {
  const campo = formularioDe(titulo).getByLabelText('Contale un poco más (opcional)');
  await userEvent.type(campo, comentario);
};

export const whenSeResponde = async (titulo: string, respuesta: Respuesta) => {
  await whenSeElige(titulo, respuesta.hizo);
  await whenSeElige(titulo, respuesta.quePaso);
  if (respuesta.comentario) await whenSeEscribeElComentario(titulo, respuesta.comentario);
  await whenSeEnvia(titulo);
};

export const thenNoHayPreguntasDeSami = async () => {
  await waitFor(() => {
    expect(screen.queryByRole('region', { name: /Sami quiere saber cómo te fue/ })).not.toBeInTheDocument();
  }, ESPERA);
};

export const thenSePreguntaPor = (seccion: Vista, titulos: string[]) => {
  const preguntas = seccion.getAllByRole('heading', { level: 3 }).map((titulo) => titulo.textContent);
  expect(preguntas).toEqual(titulos);
};

export const thenElPaginadorDice = (seccion: Vista, posicion: string) => {
  expect(seccion.getByRole('navigation', { name: 'Preguntas de Sami' })).toHaveTextContent(posicion);
};

export const thenNoHayPaginador = (seccion: Vista) => {
  expect(seccion.queryByRole('navigation', { name: 'Preguntas de Sami' })).not.toBeInTheDocument();
};

export const thenSePideElPaso = (titulo: string, pregunta: string) => {
  expect(formularioDe(titulo).getByText(pregunta)).toBeVisible();
};

export const thenNoSePideTodavia = (titulo: string, pregunta: string) => {
  expect(formularioDe(titulo).queryByText(pregunta)).not.toBeInTheDocument();
};

export const thenLaOpcionEsta = (titulo: string, opcion: string, estado: 'elegida' | 'sinElegir') => {
  const boton = formularioDe(titulo).getByRole('button', { name: opcion });
  expect(boton).toHaveAttribute('aria-pressed', estado === 'elegida' ? 'true' : 'false');
};

export const thenLaOpcionNoSePuedeElegir = (titulo: string, opcion: string) => {
  expect(formularioDe(titulo).getByRole('button', { name: opcion })).toBeDisabled();
};

export const thenNoHayBotonDeEnviar = (titulo: string) => {
  expect(formularioDe(titulo).queryByRole('button', { name: 'Contarle a Sami' })).not.toBeInTheDocument();
};
