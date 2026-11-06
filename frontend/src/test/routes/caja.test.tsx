import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

describe('caja inteligente', () => {
  it('se abre desde la navegación principal', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/inicio');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Caja' }, ESPERA));
    await screen.findByRole('heading', { name: 'Caja del negocio' }, ESPERA);
    expect(router.state.location.pathname).toBe('/caja');
    expect(screen.getByRole('button', { name: 'Caja' })).toHaveAttribute('aria-current', 'page');
    const navegacion = screen.getByRole('navigation', { name: 'Principal' });
    const secciones = within(navegacion).getAllByRole('button').map((boton) => boton.textContent);
    expect(secciones.slice(0, 2)).toEqual(['Inicio', 'Caja']);
  });

  it('explica una diferencia y permite cerrar sin alterar movimientos', async () => {
    setupUsuarioConSesion();
    renderApp('/caja');
    const user = userEvent.setup();
    await screen.findByRole('heading', { name: 'Caja del negocio' }, ESPERA);
    await user.click(screen.getByRole('button', { name: /Empezar cierre de caja/ }));
    expect(screen.getByRole('heading', { name: '¿Cuánto dinero hay realmente?' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Comparar y revisar cierre' }));
    expect(screen.getByRole('heading', { name: 'Hay una diferencia de −$3.000' })).toBeVisible();
    await user.click(screen.getByRole('radio', { name: 'Gasto todavía no registrado' }));
    await user.click(screen.getByRole('button', { name: 'Cerrar con diferencia justificada' }));
    expect(screen.getByRole('heading', { name: 'Caja cerrada y explicada' })).toBeVisible();
    expect(screen.getByText('Lectura contable del cierre')).toBeVisible();
  });
});
