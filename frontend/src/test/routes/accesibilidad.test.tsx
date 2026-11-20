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

let almacenId = '';

describe('accesibilidad de las pantallas', () => {
  it('inicio', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeAbreLaPantalla('/inicio', { name: /En qué puedo/ });

    await entoncesNoHayViolaciones();
  });

  it('plantillas', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeAbreLaPantalla('/plantillas', { name: /Elegí una base/ });

    await entoncesNoHayViolaciones();
  });

  it('taller de Sami', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeAbreLaPantalla('/taller', { name: 'Construyamos juntos' });

    await entoncesNoHayViolaciones();
  });

  it('Mi zona', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeAbreLaPantalla('/zona', { level: 1, name: /Compará los precios de tu zona/ });

    await entoncesNoHayViolaciones();
  });

  it.each(['construir', 'usar'])('editor en modo %s', async (modo) => {
    dadoUnAlmacenDeEjemplo();

    await cuandoSeAbreLaPantalla(`/sistemas/${almacenId}?modo=${modo}`, {
      level: 1,
      name: /Almacén de ejemplo/,
    });

    await entoncesNoHayViolaciones();
  });

  it('el panel de avisos', async () => {
    dadoUnAlmacenDeEjemplo();
    renderApp(`/sistemas/${almacenId}?modo=usar`);

    await cuandoSeAbreElPanelDeAvisos();

    await entoncesNoHayViolaciones();
  });

  it('el modal con todos los avisos', async () => {
    dadoUnAlmacenDeEjemplo();
    renderApp(`/sistemas/${almacenId}?modo=usar`);
    await cuandoSeAbreElPanelDeAvisos();

    await cuandoSeVenTodosLosAvisos();

    await entoncesNoHayViolaciones();
  });

  it('la bienvenida', async () => {
    dadoUnUsuarioQueNoVioLaBienvenida();

    await cuandoSeAbreLaBienvenida();

    await entoncesNoHayViolaciones();
  });
});

function dadoUnUsuarioConSesion() {
  setupUsuarioConSesion();
}

function dadoUnAlmacenDeEjemplo() {
  const usuario = setupUsuarioConSesion();
  const sistema = crearAlmacenDeEjemplo(hoy());
  estado.sistemas.set(usuario.email, [sistema]);
  almacenId = sistema.id;
}

function dadoUnUsuarioQueNoVioLaBienvenida() {
  setupUsuarioConSesion({}, { bienvenidaVista: false });
}

async function cuandoSeAbreLaPantalla(
  ruta: string,
  titulo: { level?: number; name: string | RegExp },
) {
  renderApp(ruta);
  await screen.findByRole('heading', titulo, ESPERA);
}

async function cuandoSeAbreElPanelDeAvisos() {
  await userEvent.click(await screen.findByRole('button', { name: /Avisos de Sami/ }, ESPERA));
}

async function cuandoSeVenTodosLosAvisos() {
  await userEvent.click(screen.getByRole('button', { name: /Ver todos los avisos/ }));
  await screen.findByRole('region', { name: /Lo que encontré/ }, ESPERA);
}

async function cuandoSeAbreLaBienvenida() {
  renderApp('/inicio');
  await screen.findByRole('dialog', { name: /Bienvenido a Ensambla/ }, ESPERA);
}

async function entoncesNoHayViolaciones() {
  const resultado = await axe(document.body, OPCIONES);
  expect(resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html}`)).toEqual([]);
}
