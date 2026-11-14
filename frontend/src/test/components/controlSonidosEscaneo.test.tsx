import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { ControlSonidosEscaneo } from '@/components/escaneo/ControlSonidosEscaneo';

describe('controles de sonido del lector', () => {
  it('permite silenciar, recuerda la preferencia al volver y permite reactivar', async () => {
    const user = userEvent.setup();
    const vista = render(<ControlSonidosEscaneo />);
    await user.click(screen.getByRole('button', { name: 'Silenciar sonidos del lector' }));
    expect(screen.getByRole('button', { name: 'Probar sonidos' })).toBeDisabled();
    vista.unmount();
    render(<ControlSonidosEscaneo />);
    const activar = screen.getByRole('button', { name: 'Activar sonidos del lector' });
    expect(activar).toHaveAttribute('aria-pressed', 'false');
    await user.click(activar);
    expect(screen.getByRole('button', { name: 'Probar sonidos' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Silenciar sonidos del lector' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });
});
