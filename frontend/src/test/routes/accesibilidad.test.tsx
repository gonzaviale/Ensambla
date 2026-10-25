import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

// Reglas que no aplican a una pantalla suelta en jsdom (no hay layout real ni colores calculados).
// `heading-order`: el mockup titula las tarjetas con h3 bajo el h1 y se respeta su marcado.
const OPCIONES = {
  rules: {
    'color-contrast': { enabled: false },
    region: { enabled: false },
    'heading-order': { enabled: false },
  },
};

const sinViolaciones = async () => {
  const resultado = await axe(document.body, OPCIONES);
  expect(resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html}`)).toEqual([]);
};

describe('accesibilidad de las pantallas', () => {
  it('inicio', async () => {
    setupUsuarioConSesion();
    renderApp('/inicio');
    await screen.findByRole('heading', { name: /En qué puedo/ }, ESPERA);
    await sinViolaciones();
  });

  it('plantillas', async () => {
    setupUsuarioConSesion();
    renderApp('/plantillas');
    await screen.findByRole('heading', { name: /Elegí una base/ }, ESPERA);
    await sinViolaciones();
  });

  it('taller de Sami', async () => {
    setupUsuarioConSesion();
    renderApp('/taller');
    await screen.findByRole('heading', { name: 'Lo armamos juntos.' }, ESPERA);
    await sinViolaciones();
  });

  it('Mi zona', async () => {
    setupUsuarioConSesion();
    renderApp('/zona');
    await screen.findByRole('heading', { level: 1, name: /Mirá cuánto se cobra/ }, ESPERA);
    await sinViolaciones();
  });

  it.each(['construir', 'usar'])('editor en modo %s', async (modo) => {
    const usuario = setupUsuarioConSesion();
    const sistema = crearAlmacenDeEjemplo(hoy());
    estado.sistemas.set(usuario.email, [sistema]);
    renderApp(`/sistemas/${sistema.id}?modo=${modo}`);
    await screen.findByRole('heading', { level: 1, name: /Almacén de ejemplo/ }, ESPERA);
    await sinViolaciones();
  });

  it('el panel de avisos y un modal', async () => {
    const usuario = setupUsuarioConSesion();
    const sistema = crearAlmacenDeEjemplo(hoy());
    estado.sistemas.set(usuario.email, [sistema]);
    renderApp(`/sistemas/${sistema.id}?modo=usar`);
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /Avisos de Sami/ }, ESPERA));
    await screen.findByRole('region', { name: /Lo que encontré/ }, ESPERA);
    await sinViolaciones();
  });

  it('la bienvenida', async () => {
    setupUsuarioConSesion({}, { bienvenidaVista: false });
    renderApp('/inicio');
    await screen.findByRole('dialog', { name: /Bienvenido a Ensambla/ }, ESPERA);
    await sinViolaciones();
  });
});
