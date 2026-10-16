import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { verifyHeadingPresentAsync } from '@/test/testHelpers';

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

async function abrirProbar() {
  renderApp('/probar');
  await verifyHeadingPresentAsync('Contá tu negocio y mirá cómo se arma');
  return userEvent.setup();
}

describe('Probá ahora: demo pública sin cuenta', () => {
  it('se ve sin sesión, con un solo h1 y las dos salidas', async () => {
    await abrirProbar();

    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    expect(screen.getByRole('link', { name: 'Guardar y empezar' })).toHaveAttribute(
      'href',
      '/registro',
    );
    expect(screen.getByRole('link', { name: 'Ya tengo cuenta' })).toHaveAttribute('href', '/login');
    expect(screen.queryByRole('list', { name: 'Bloques armados' })).not.toBeInTheDocument();
  });

  it('escribir una frase arma los bloques', async () => {
    const user = await abrirProbar();

    await user.type(
      screen.getByLabelText('Contá qué hacés, en una frase'),
      'Vendo viandas por encargo y cobro una seña',
    );

    const bloques = await screen.findByRole('list', { name: 'Bloques armados' });
    expect(within(bloques).getByRole('heading', { name: 'Platos' })).toBeInTheDocument();
    expect(within(bloques).getByRole('heading', { name: 'Pedidos' })).toBeInTheDocument();
    expect(screen.getByText('Datos de ejemplo')).toBeInTheDocument();
  });

  it('sumar un bloque desbloquea una respuesta', async () => {
    const user = await abrirProbar();
    await user.click(screen.getByRole('button', { name: 'Comercio de barrio' }));

    const bloqueado = screen.getByRole('heading', { name: '¿Quién te debe?' }).closest('section');
    expect(bloqueado).toHaveTextContent('Te falta el bloque Fiados');
    expect(within(bloqueado as HTMLElement).queryByText('$ 24.600')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Sumar Fiados' }));

    const desbloqueado = screen
      .getByRole('heading', { name: '¿Quién te debe?' })
      .closest('section');
    expect(within(desbloqueado as HTMLElement).getByText('$ 24.600')).toBeInTheDocument();
    expect(
      within(screen.getByRole('list', { name: 'Bloques armados' })).getByRole('heading', {
        name: 'Fiados',
      }),
    ).toBeInTheDocument();
  });

  it('no tiene violaciones de accesibilidad', async () => {
    const user = await abrirProbar();
    await sinViolaciones();

    await user.click(screen.getByRole('button', { name: 'Gastronomía y viandas' }));
    await screen.findByRole('list', { name: 'Bloques armados' });
    await sinViolaciones();
  });
});
