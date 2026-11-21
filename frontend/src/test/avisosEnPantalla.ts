import { screen, within } from '@testing-library/react';
import type userEvent from '@testing-library/user-event';
import { expect } from 'vitest';

type Usuario = ReturnType<typeof userEvent.setup>;
type Texto = RegExp | string;

export const FILA_DE_AVISO = '.aviso-fila';

export const entoncesHayFilasDeAviso = (
  panel: HTMLElement,
  cantidad: number,
  selector = FILA_DE_AVISO,
) => {
  expect(panel.querySelectorAll(selector)).toHaveLength(cantidad);
};

export const entoncesElAvisoEstaEnSuSeccion = (seccion: Texto, titulo: Texto) => {
  const delTipo = screen.getByRole('region', { name: seccion });
  expect(within(delTipo).getByRole('heading', { level: 3, name: titulo })).toBeVisible();
};

export const cuandoSeAbreElPorQue = (user: Usuario, fila: HTMLElement) =>
  user.click(within(fila).getByRole('button', { name: /Ver por qué/ }));

export const entoncesElPorQueMuestra = async (titulo: Texto, ...textos: Texto[]) => {
  const detalle = await screen.findByRole('dialog', { name: titulo });
  textos.forEach((texto) => expect(within(detalle).getByText(texto)).toBeVisible());
};

export const entoncesLaFilaNoOfreceElPorQue = (fila: HTMLElement) => {
  expect(within(fila).queryByRole('button', { name: /Ver por qué/ })).toBeNull();
};

export const cuandoSeAplicaDesdeElPorQue = async (user: Usuario, titulo: Texto, accion: string) => {
  const detalle = await screen.findByRole('dialog', { name: titulo });
  await user.click(within(detalle).getByRole('button', { name: accion }));
};

export const entoncesNoHayPorQueAbierto = () => {
  expect(screen.queryByRole('dialog')).toBeNull();
};
