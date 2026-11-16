import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { clickButton, ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

/* given */

export async function givenLaPantallaDeCarteles() {
  setupUsuarioConSesion();
  const router = renderApp('/carteles');
  await screen.findByRole('heading', { level: 1, name: 'Carteles y promociones' }, ESPERA);
  return { user: userEvent.setup(), router };
}

export async function givenUnCartelArmado() {
  const pantalla = await givenLaPantallaDeCarteles();
  await whenSeArmaElCartelDelAceite(pantalla.user);
  return pantalla;
}

/* when */

type Usuario = ReturnType<typeof userEvent.setup>;

export async function whenSeArmaElCartelDelAceite(user: Usuario) {
  await user.click(
    await screen.findByRole('button', { name: /Armar cartel de Aceite de girasol 900 ml/ }, ESPERA),
  );
  await screen.findByRole('radiogroup', { name: 'Opciones de cartel' }, ESPERA);
}

export async function whenSeCambiaElTitular(user: Usuario, titular: string) {
  const campo = screen.getByLabelText('Titular');
  await user.clear(campo);
  await user.type(campo, titular);
  await clickButton(user, 'Actualizar vista previa');
}

export async function whenSeElijeLaOpcion(user: Usuario, nombre: RegExp) {
  await user.click(screen.getByRole('radio', { name: nombre }));
}

/* then */

export const thenSeSugiereElAceite = async () => {
  expect(await screen.findByText('Aceite de girasol 900 ml', undefined, ESPERA)).toBeInTheDocument();
  expect(screen.getByText(/ahorrás \$500 \(20 %\)/)).toBeInTheDocument();
};

export const thenSeVenLasTresOpciones = () => {
  expect(screen.getAllByRole('radio')).toHaveLength(3);
  expect(screen.getByAltText(/Opción 1 · Bloque/)).toBeInTheDocument();
  expect(screen.getByAltText(/Opción 3 · Marco/)).toBeInTheDocument();
};

export const thenLaVistaPreviaDiceElTitular = (titular: string) =>
  expect(
    decodeURIComponent(screen.getAllByRole('img')[0]?.getAttribute('src') ?? ''),
  ).toContain(titular);

export const thenSeGuardoLaOpcion = (nombre: RegExp) =>
  expect(screen.getByRole('radio', { name: nombre })).toBeChecked();
