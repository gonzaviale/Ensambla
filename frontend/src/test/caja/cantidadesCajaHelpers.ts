import { screen } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';
import { expect } from 'vitest';

const gaseosa = () => screen.getByRole('button', { name: /Coca-Cola.*2.700/ });

export async function cuandoAgregaDosGaseosas(user: UserEvent) {
  await user.click(gaseosa());
  await user.click(gaseosa());
}
export const cuandoRestaUnaGaseosa = (user: UserEvent) =>
  user.click(screen.getByRole('button', { name: 'Restar una unidad de Coca-Cola' }));
export const cuandoQuitaLaGaseosa = (user: UserEvent) =>
  user.click(screen.getByRole('button', { name: 'Quitar Coca-Cola' }));
export const cuandoDeshaceElCambio = (user: UserEvent) =>
  user.click(screen.getByRole('button', { name: 'Deshacer último cambio' }));
export const entoncesCantidadDeGaseosa = (cantidad: string) =>
  expect(screen.getByLabelText('Cantidad de Coca-Cola')).toHaveValue(cantidad);
export const entoncesGaseosaRetirada = () =>
  expect(screen.queryByLabelText('Cantidad de Coca-Cola')).not.toBeInTheDocument();
