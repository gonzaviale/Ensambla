import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, vi } from 'vitest';
import { DialogoDePrueba } from './DialogoDePrueba';

export async function dadoDialogoConSelector(bloqueado = false) {
  vi.spyOn(HTMLElement.prototype, 'getClientRects').mockReturnValue({ length: 1 } as DOMRectList);
  render(<DialogoDePrueba bloqueado={bloqueado} />);
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: 'Abrir cobro' }));
  return user;
}

type Usuario = Awaited<ReturnType<typeof dadoDialogoConSelector>>;
export const cuandoAvanzaHastaSelector = (user: Usuario) => user.keyboard('{Tab}{Tab}');
export const cuandoAvanzaUnControl = (user: Usuario) => user.keyboard('{Tab}');
export const cuandoRetrocedeUnControl = (user: Usuario) => user.keyboard('{Shift>}{Tab}{/Shift}');
export const cuandoSolicitaCerrarDialogo = (user: Usuario) => user.keyboard('{Escape}');
export const entoncesSelectorEnfocado = () =>
  expect(screen.getByRole('combobox', { name: 'Medio' })).toHaveFocus();
export const entoncesCierreEnfocado = () =>
  expect(screen.getByRole('button', { name: 'Cerrar' })).toHaveFocus();
export const entoncesDialogoAbierto = () =>
  expect(screen.getByRole('dialog', { name: 'Cobro' })).toBeInTheDocument();
export function entoncesDialogoCerradoYFocoRestaurado() {
  expect(screen.queryByRole('dialog')).toBeNull();
  expect(screen.getByRole('button', { name: 'Abrir cobro' })).toHaveFocus();
}
