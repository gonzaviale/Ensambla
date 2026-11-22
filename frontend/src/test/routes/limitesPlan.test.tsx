import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { avisoDelServidor } from '@/test/avisosData';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

describe('vista gratuita y acceso a Premium', () => {
  it('Mi zona ofrece seis productos', async () => {
    dadoUnUsuarioGratuito();

    await cuandoSeAbreMiZona();

    entoncesMiZonaMuestraSeisProductos();
  });

  it('Mi zona ofrece una vista previa sin el contenido restringido', async () => {
    dadoUnUsuarioGratuito();

    await cuandoSeAbreMiZona();

    entoncesMiZonaOfreceConocerPremiumSinElContenidoRestringido();
  });

  it('la campana cuenta el aviso bloqueado', async () => {
    dadoUnUsuarioConSieteAvisos();

    await cuandoSeAbreLaCampana();

    await entoncesLaCampanaAvisaQueHayUnAvisoMasConPremium();
  });

  it('el panel de avisos sólo entrega seis detalles', async () => {
    dadoUnUsuarioConSieteAvisos();
    await cuandoSeAbreLaCampana();

    await cuandoSeVenTodosLosAvisos();

    entoncesElPanelEntregaSeisDetallesYOfreceConocerPremium();
  });
});

function dadoUnUsuarioGratuito() {
  setupUsuarioConSesion();
}

function dadoUnUsuarioConSieteAvisos() {
  const usuario = setupUsuarioConSesion();
  estado.avisosDelServidor.set(
    usuario.email,
    Array.from({ length: 7 }, (_, indice) =>
      avisoDelServidor({ id: `aviso-${indice}`, titulo: `Detalle reservado ${indice}` }),
    ),
  );
  renderApp('/inicio');
}

async function cuandoSeAbreMiZona() {
  renderApp('/zona');
  await screen.findByRole('heading', { level: 1, name: /Compará los precios de tu zona/ }, ESPERA);
  await screen.findByRole('region', { name: 'Tu acceso a recomendaciones' }, ESPERA);
}

async function cuandoSeAbreLaCampana() {
  const campana = await screen.findByRole('button', { name: /Avisos de Sami: 7/ }, ESPERA);
  await userEvent.click(campana);
}

async function cuandoSeVenTodosLosAvisos() {
  await userEvent.click(screen.getByRole('button', { name: /Ver todos los avisos/ }));
  await screen.findByRole('region', { name: /Lo que encontré en tus datos/ }, ESPERA);
  await waitFor(() => expect(screen.getByText(/6 de 6/)).toBeVisible(), ESPERA);
}

function entoncesMiZonaMuestraSeisProductos() {
  expect(document.querySelectorAll('.zona-tarjeta')).toHaveLength(6);
  expect(screen.getByText(/6 de 6/)).toBeVisible();
}

function entoncesMiZonaOfreceConocerPremiumSinElContenidoRestringido() {
  expect(screen.getByRole('link', { name: 'Conocer Premium' })).toHaveAttribute('href', '/planes');
  expect(document.querySelector('.contenido-premium-muestra')?.textContent).toBe('');
}

async function entoncesLaCampanaAvisaQueHayUnAvisoMasConPremium() {
  expect(await screen.findByText('Hay 1 aviso más con Premium.', {}, ESPERA)).toBeVisible();
}

function entoncesElPanelEntregaSeisDetallesYOfreceConocerPremium() {
  expect(screen.getByRole('heading', { name: 'Detalle reservado 0' })).toBeVisible();
  expect(screen.queryByRole('heading', { name: 'Detalle reservado 6' })).toBeNull();
  expect(screen.getByText(/Hay 1 aviso más/)).toBeVisible();
  expect(screen.getByRole('link', { name: 'Conocer Premium' })).toBeVisible();
}
