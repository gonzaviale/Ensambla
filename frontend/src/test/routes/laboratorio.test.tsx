import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

describe('laboratorio de decisiones', () => {
  it('se puede abrir desde la navegación principal', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/inicio');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Laboratorio' }, ESPERA));
    await screen.findByRole('heading', { name: 'Probá una decisión antes de tomarla' }, ESPERA);

    expect(router.state.location.pathname).toBe('/laboratorio');
    expect(screen.getByRole('button', { name: 'Laboratorio' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('recorre la meta y los caminos sin modificar datos', async () => {
    setupUsuarioConSesion();
    renderApp('/laboratorio');
    const user = userEvent.setup();

    await screen.findByRole('heading', { name: 'Probá una decisión antes de tomarla' }, ESPERA);
    await user.click(screen.getByRole('button', { name: /Quiero alcanzar una meta/ }));
    expect(screen.getByRole('heading', { name: '¿Qué querés lograr?' })).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Analizar mi meta' }));

    expect(screen.getByRole('heading', { name: 'Tres caminos para llegar a $500.000' })).toBeVisible();
    expect(screen.getByText('Confianza media-alta')).toBeVisible();
  });
});
