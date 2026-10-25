import { screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { axe } from 'vitest-axe';
import { makeBlock, makeSystem } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { CATALOGO } from '@/mocks/db/planes';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';
import { claveMetaInicio } from '@/hooks/inicio/useMetaInicio';

const conVentas = () => {
  const sistema = makeSystem('Mi almacén');
  const ventas = makeBlock('sales');
  ventas.rows = [{ id: 'v', quantity: 2, price: 500 }];
  sistema.sections = [{ id: 'seccion', name: 'Datos', blocks: [ventas] }];
  return sistema;
};
const abrirConDatos = () => {
  const usuario = setupUsuarioConSesion();
  const sistema = conVentas();
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp('/inicio');
  return { usuario, sistema };
};
describe('ENS-244: planes públicos', () => {
  it('muestra el catálogo sin iniciar sesión ni consultar sistemas, zona, consumo o pagos', async () => {
    const privadas: string[] = [];
    server.use(
      http.get('/api/*', ({ request }) => {
        const path = new URL(request.url).pathname;
        if (!['/api/auth/me', '/api/planes/catalogo'].includes(path)) privadas.push(path);
      }),
    );
    const router = renderApp('/planes');
    await screen.findByRole('article', { name: 'Plan Premium' }, ESPERA);
    expect(router.state.location.pathname).toBe('/planes');
    expect(screen.getByRole('link', { name: 'Empezar gratis' })).toHaveAttribute(
      'href',
      '/registro',
    );
    expect(screen.getByText(/14\.900/)).toBeVisible();
    expect(screen.queryByText('Tu renovación')).toBeNull();
    expect(screen.queryByText('Sin límite comercial')).toBeNull();
    expect(screen.queryByText('Todos los disponibles')).toBeNull();
    expect(screen.queryByRole('button', { name: /Anual/ })).toBeNull();
    expect(privadas).toEqual([]);
    const gratuito = within(screen.getByRole('article', { name: 'Plan Gratuito' }));
    expect(gratuito.getByText('Reportes filtrados').parentElement).toHaveClass(
      'plan-feature-excluded',
    );
    expect(screen.getAllByText('Sugerencias')).toHaveLength(2);
  });
  it('un error público permite reintentar y no inventa el catálogo', async () => {
    setupServerError('get', '/planes/catalogo', 503, 'Sin catálogo');
    renderApp('/planes');
    await screen.findByRole('alert', {}, ESPERA);
    expect(screen.queryByRole('article')).toBeNull();
    server.use(http.get('/api/planes/catalogo', () => HttpResponse.json(CATALOGO)));
    await userEvent.setup().click(screen.getByRole('button', { name: 'Reintentar' }));
    await screen.findByRole('article', { name: 'Plan Gratuito' }, ESPERA);
  });
  it('la comparación pública es accesible', async () => {
    renderApp('/planes');
    await screen.findByRole('article', { name: 'Plan Premium' }, ESPERA);
    const resultado = await axe(document.body, {
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });
    expect(resultado.violations.map((v) => v.id)).toEqual([]);
  });
});
describe('ENS-244: home y metas', () => {
  it('prioriza los números de las tablas y conserva las herramientas', async () => {
    abrirConDatos();
    await screen.findByRole('heading', { name: 'Cómo viene tu negocio' }, ESPERA);
    const ventas = screen.getByRole('article', { name: 'Ventas registradas' });
    expect(within(ventas).getByText(/1\.000/)).toBeVisible();
    expect(within(ventas).getByText(/No representa ganancia/)).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Más herramientas para tu negocio' })).toBeVisible();
  });
  it('guarda una meta, muestra progreso y permite quitarla', async () => {
    const { usuario, sistema } = abrirConDatos();
    const ventas = within(
      await screen.findByRole('article', { name: 'Ventas registradas' }, ESPERA),
    );
    const user = userEvent.setup();
    await user.click(ventas.getByRole('button', { name: 'Definir meta' }));
    await user.type(ventas.getByRole('spinbutton'), '2000');
    await user.click(ventas.getByRole('button', { name: 'Guardar meta' }));
    expect(ventas.getByRole('progressbar')).toHaveValue(1000);
    expect(localStorage.getItem(claveMetaInicio(usuario.id, sistema.id, 'ventas'))).toBe('2000');
    await user.click(ventas.getByRole('button', { name: 'Quitar meta' }));
    expect(localStorage.getItem(claveMetaInicio(usuario.id, sistema.id, 'ventas'))).toBeNull();
  });
  it('no comparte metas al cambiar de sistema y recupera la guardada al volver', async () => {
    const usuario = setupUsuarioConSesion();
    const primero = conVentas();
    const segundo = conVentas();
    segundo.name = 'Segundo';
    estado.sistemas.set(usuario.email, [primero, segundo]);
    localStorage.setItem(claveMetaInicio(usuario.id, primero.id, 'ventas'), '500');
    renderApp('/inicio');
    const user = userEvent.setup();
    await screen.findByText(/Meta alcanzada/, {}, ESPERA);
    await user.selectOptions(screen.getByLabelText('Sistema'), segundo.id);
    expect(screen.queryByText(/Meta alcanzada/)).toBeNull();
    await user.selectOptions(screen.getByLabelText('Sistema'), primero.id);
    expect(screen.getByText(/Meta alcanzada/)).toBeVisible();
  });
  it('no recupera metas de otra cuenta para el mismo identificador de sistema', async () => {
    const usuario = setupUsuarioConSesion();
    const sistema = conVentas();
    estado.sistemas.set(usuario.email, [sistema]);
    localStorage.setItem(claveMetaInicio('otra-cuenta', sistema.id, 'ventas'), '500');
    renderApp('/inicio');
    await screen.findByRole('article', { name: 'Ventas registradas' }, ESPERA);
    expect(screen.queryByText(/Meta alcanzada/)).toBeNull();
  });
  it('señala meta cero alcanzada y excluye los ejemplos de la métrica', async () => {
    const usuario = setupUsuarioConSesion();
    const sistema = conVentas();
    for (const seccion of sistema.sections)
      for (const bloque of seccion.blocks) for (const fila of bloque.rows) fila.__example = true;
    estado.sistemas.set(usuario.email, [sistema]);
    localStorage.setItem(claveMetaInicio(usuario.id, sistema.id, 'ventas'), '0');
    renderApp('/inicio');
    await screen.findByText(/todavía tienen solo ejemplos/, {}, ESPERA);
    expect(screen.getByText(/Meta alcanzada/)).toBeVisible();
  });
  it('valida la meta antes de guardarla y permite cancelar la edición', async () => {
    abrirConDatos();
    const ventas = within(
      await screen.findByRole('article', { name: 'Ventas registradas' }, ESPERA),
    );
    const user = userEvent.setup();
    await user.click(ventas.getByRole('button', { name: 'Definir meta' }));
    await user.type(ventas.getByRole('spinbutton'), '-1');
    await user.click(ventas.getByRole('button', { name: 'Guardar meta' }));
    expect(ventas.getByRole('alert')).toHaveTextContent('igual o mayor a cero');
    await user.click(ventas.getByRole('button', { name: 'Cancelar' }));
    expect(ventas.queryByRole('spinbutton')).toBeNull();
  });
  it('informa si el navegador no permite persistir la meta', async () => {
    abrirConDatos();
    const ventas = within(
      await screen.findByRole('article', { name: 'Ventas registradas' }, ESPERA),
    );
    const user = userEvent.setup();
    await user.click(ventas.getByRole('button', { name: 'Definir meta' }));
    await user.type(ventas.getByRole('spinbutton'), '500');
    const fallo = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    try {
      await user.click(ventas.getByRole('button', { name: 'Guardar meta' }));
      await waitFor(() =>
        expect(ventas.getByRole('status')).toHaveTextContent('no permite guardarla'),
      );
    } finally {
      fallo.mockRestore();
    }
  });
});
