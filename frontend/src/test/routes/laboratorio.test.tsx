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

  it('conecta un aviso económico con su fundamento contable', async () => {
    setupUsuarioConSesion();
    renderApp('/laboratorio');
    const user = userEvent.setup();

    await screen.findByRole('heading', { name: 'Probá una decisión antes de tomarla' }, ESPERA);
    await user.click(screen.getByRole('button', { name: /Ver el recorrido completo/ }));
    expect(screen.getByRole('heading', { name: 'Vendiste más, pero tu margen bajó' })).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Entender por qué' }));

    expect(screen.getByRole('heading', { name: 'Así se forma el resultado del negocio' })).toBeVisible();
    expect(screen.getByText('= Resultado operativo estimado')).toBeVisible();
  });

  it('cruza el escenario con Mi zona y arma un plan medible', async () => {
    setupUsuarioConSesion();
    renderApp('/laboratorio');
    const user = userEvent.setup();

    await screen.findByRole('heading', { name: 'Probá una decisión antes de tomarla' }, ESPERA);
    await user.click(screen.getByRole('button', { name: /Quiero probar una decisión/ }));
    expect(screen.getByRole('heading', { name: '¿Qué pasa si aplicás este camino?' })).toBeVisible();

    await user.click(screen.getByRole('button', { name: '7 Mi zona' }));
    expect(screen.getByRole('heading', { name: 'Compará la decisión con tu contexto' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Usar en mi plan' }));
    expect(screen.getByRole('heading', { name: 'Un plan pequeño, medible y revisable' })).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Ver cómo se mediría' }));
    expect(screen.getByRole('heading', { name: 'Ensambla también controla qué ocurrió' })).toBeVisible();
  });
});
