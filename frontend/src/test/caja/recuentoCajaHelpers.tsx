import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, vi } from 'vitest';
import { CierreCajaForm } from '@/components/caja/CierreCajaForm';

export function givenCierreConEfectivo(efectivo = 1000) {
  const confirmar = vi.fn();
  render(<CierreCajaForm resumen={{ ventas: [], total: efectivo, efectivo, transferencia: 0,
    tarjeta: 0, desde: null, revision: 'vigente' }} ocupada={false} incierta={false}
    desactualizada={false} error="" compraPendiente={false} onCerrar={vi.fn()} onConfirmar={confirmar} />);
  return { user: userEvent.setup(), confirmar };
}
export async function whenSeCuentaEfectivo(user: ReturnType<typeof userEvent.setup>, contado: string) {
  await user.type(screen.getByLabelText('Efectivo contado al cerrar ($)'), contado);
}
export async function whenSeExplicaDiferencia(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Motivo de la diferencia (obligatorio)'), 'Retiro pendiente de registrar');
  await user.click(screen.getByRole('button', { name: 'Guardar cierre' }));
}
export function thenNoSePuedeGuardarCierre() {
  expect(screen.getByRole('button', { name: 'Guardar cierre' })).toBeDisabled();
}
export function thenSePuedeGuardarCierre() {
  expect(screen.getByRole('button', { name: 'Guardar cierre' })).toBeEnabled();
}
export function thenSeGuardaRecuentoCero(confirmar: ReturnType<typeof vi.fn>) {
  expect(confirmar).toHaveBeenCalledWith(expect.objectContaining({ contado: 0, nota: 'Retiro pendiente de registrar' }));
}

