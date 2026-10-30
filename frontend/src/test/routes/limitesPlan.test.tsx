import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { avisoDelServidor } from '@/test/avisosData';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

describe('vista gratuita y acceso a Premium', () => {
  it('Mi zona ofrece tres productos y una vista previa sin el contenido restringido', async () => {
    setupUsuarioConSesion();
    renderApp('/zona');

    await screen.findByRole('heading', { level: 1, name: /Mirá cuánto se cobra/ }, ESPERA);

    expect(
      await screen.findByRole('region', { name: 'Tu acceso a recomendaciones' }, ESPERA),
    ).toBeVisible();
    expect(document.querySelectorAll('.zona-tarjeta')).toHaveLength(3);
    expect(screen.getByText(/3 de 3/)).toBeVisible();
    expect(screen.getByRole('link', { name: 'Ver opciones del plan' })).toHaveAttribute(
      'href',
      '/planes',
    );
    expect(document.querySelector('.contenido-premium-muestra')).toBeNull();
  });

  it('la campana cuenta el aviso bloqueado pero el panel sólo entrega tres detalles', async () => {
    const usuario = setupUsuarioConSesion();
    estado.avisosDelServidor.set(
      usuario.email,
      Array.from({ length: 4 }, (_, indice) =>
        avisoDelServidor({ id: `aviso-${indice}`, titulo: `Detalle reservado ${indice}` }),
      ),
    );
    renderApp('/inicio');
    const user = userEvent.setup();

    const campana = await screen.findByRole('button', { name: /Avisos de Sami: 4/ }, ESPERA);
    await user.click(campana);
    expect(screen.getByText('Hay 1 aviso más con Premium.')).toBeVisible();
    await user.click(screen.getByRole('button', { name: /Ver todos los avisos/ }));

    await screen.findByRole('region', { name: /Lo que encontré en tus datos/ }, ESPERA);
    await waitFor(() => expect(screen.getByText(/3 de 3/)).toBeVisible(), ESPERA);
    expect(screen.getByRole('heading', { name: 'Detalle reservado 0' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Detalle reservado 3' })).toBeNull();
    expect(screen.getByText(/Hay 1 aviso más/)).toBeVisible();
    expect(screen.getByRole('link', { name: 'Ver opciones del plan' })).toBeVisible();
  });
});
