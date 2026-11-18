import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, vi } from 'vitest';
import { DialogoDePrueba } from './DialogoDePrueba';

export async function givenDialogoConSelector(bloqueado = false) {
  vi.spyOn(HTMLElement.prototype, 'getClientRects').mockReturnValue({ length: 1 } as DOMRectList);
  render(<DialogoDePrueba bloqueado={bloqueado} />);
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: 'Abrir cobro' }));
  return user;
}

type Usuario = Awaited<ReturnType<typeof givenDialogoConSelector>>;
export const whenAvanzaHastaSelector = (user: Usuario) => user.keyboard('{Tab}{Tab}');
export const whenAvanzaUnControl = (user: Usuario) => user.keyboard('{Tab}');
export const whenRetrocedeUnControl = (user: Usuario) => user.keyboard('{Shift>}{Tab}{/Shift}');
export const whenSolicitaCerrarDialogo = (user: Usuario) => user.keyboard('{Escape}');
export const thenSelectorEnfocado = () =>
  expect(screen.getByRole('combobox', { name: 'Medio' })).toHaveFocus();
export const thenCierreEnfocado = () =>
  expect(screen.getByRole('button', { name: 'Cerrar' })).toHaveFocus();
export const thenDialogoAbierto = () =>
  expect(screen.getByRole('dialog', { name: 'Cobro' })).toBeInTheDocument();
export function thenDialogoCerradoYFocoRestaurado() {
  expect(screen.queryByRole('dialog')).toBeNull();
  expect(screen.getByRole('button', { name: 'Abrir cobro' })).toHaveFocus();
}
