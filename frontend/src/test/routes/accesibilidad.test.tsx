import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { estado, guardarOrdenesDe } from '@/mocks/db/estado';
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
    await screen.findByRole('heading', { name: 'Construyamos juntos' }, ESPERA);
    await sinViolaciones();
  });

  it('Mi zona', async () => {
    setupUsuarioConSesion();
    renderApp('/zona');
    await screen.findByRole('heading', { level: 1, name: /Mirá cuánto se cobra/ }, ESPERA);
    await sinViolaciones();
  });

  it('Mi zona, en la vista «Otra zona» con el mapa y un resultado', async () => {
    setupUsuarioConSesion();
    renderApp('/zona');
    await screen.findByRole('heading', { level: 1, name: /Mirá cuánto se cobra/ }, ESPERA);
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: /Otra zona/ }));
    await user.type(
      screen.getByLabelText('Barrio o dirección'),
      'Florencio Varela 1903, San Justo',
    );
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    await user.click(await screen.findByRole('radio', { name: /Florencio Varela 1903/ }, ESPERA));
    await user.click(screen.getByRole('button', { name: 'Analizar la zona' }));
    await screen.findByRole('heading', { level: 2, name: 'Precios en San Justo - UNLaM' }, ESPERA);
    expect(screen.getByRole('region', { name: 'Mapa de la zona a analizar' })).toBeVisible();
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
    await sinViolaciones();
    await user.click(screen.getByRole('button', { name: /Ver todos los avisos/ }));
    await screen.findByRole('region', { name: /Lo que encontré/ }, ESPERA);
    await sinViolaciones();
  });

  it('pedidos, con órdenes y con el formulario de rechazo abierto', async () => {
    const { email } = setupUsuarioConSesion();
    guardarOrdenesDe(email, [
      {
        id: crypto.randomUUID(),
        clienteTelefono: '5491155550001',
        estado: 'PENDIENTE',
        items: [
          { nombre: 'Yerba Playadito 1 kg', cantidad: 2, precioUnitario: 3500, subtotal: 7000 },
        ],
        total: 7000,
        motivoRechazo: null,
        creadoEn: '2026-11-11T12:00:00Z',
        actualizadoEn: '2026-11-11T12:00:00Z',
      },
    ]);
    renderApp('/pedidos');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Rechazar' }, ESPERA));
    await screen.findByLabelText('Motivo del rechazo (opcional)', undefined, ESPERA);
    await sinViolaciones();
  });

  it('pedidos, con el QR para que los clientes pidan por WhatsApp', async () => {
    const { email } = setupUsuarioConSesion();
    estado.whatsapp.set(email, {
      vinculo: {
        telefono: '5491122334455',
        vinculadoEn: '2026-10-01T12:00:00Z',
        numeroBot: '5491155550000',
      },
      verificacion: null,
    });
    renderApp('/pedidos');
    await screen.findByRole('img', { name: /Código QR/ }, ESPERA);
    await sinViolaciones();
  });

  it('la bienvenida', async () => {
    setupUsuarioConSesion({}, { bienvenidaVista: false });
    renderApp('/inicio');
    await screen.findByRole('dialog', { name: /Bienvenido a Ensambla/ }, ESPERA);
    await sinViolaciones();
  });
});
