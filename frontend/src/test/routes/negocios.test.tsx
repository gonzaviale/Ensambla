import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { estado, sistemasDe, type UsuarioMock } from '@/mocks/db/estado';
import { leerNegocioGuardado } from '@/lib/negocios/lista';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { TIPOS_OBLIGATORIOS } from '@/lib/sistema/estructuraFija';
import { hoy } from '@/lib/sistema/formato';
import { seleccionarSistemasDelNegocio, useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  negocioDePrueba,
  setupNegocios,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';

const ALMACEN = {
  ...crearAlmacenCargado(hoy()),
  id: '10000000-0000-4000-8000-000000000001',
  name: 'Almacén San Justo',
};
const KIOSCO = negocioDePrueba('20000000-0000-4000-8000-000000000002', 'Kiosco de la plaza');

const dadoPremium = (email: string) =>
  estado.pruebasPremium.set(email, {
    inicioEn: new Date(Date.now() - 1000).toISOString(),
    finEn: new Date(Date.now() + 86_400_000).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });

function dadoDosNegocios(usuario: UsuarioMock) {
  dadoPremium(usuario.email);
  setupNegocios(usuario.email, ALMACEN, KIOSCO);
}

const selectorDe = (nombre: RegExp) =>
  screen.findByRole('button', { name: new RegExp(`Negocio activo.*${nombre.source}`) }, ESPERA);

async function cuandoAbreAgregarNegocioEnLaBarra(user: ReturnType<typeof userEvent.setup>) {
  await user.click(await screen.findByRole('button', { name: /Negocio activo/ }, ESPERA));
  await user.click(
    within(screen.getByRole('menu')).getByRole('menuitem', { name: /Agregar un negocio/ }),
  );
}

async function cuandoEligeEnLaBarra(nombre: RegExp) {
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /Negocio activo/ }, ESPERA));
  await user.click(within(screen.getByRole('menu')).getByRole('menuitem', { name: nombre }));
}

const nombresEnElServidor = (usuario: UsuarioMock) =>
  sistemasDe(usuario.email).map((sistema) => sistema.name);

function entoncesElNegocioGuardadoTieneLasSeccionesObligatorias(usuario: UsuarioMock, nombre: string) {
  const guardado = sistemasDe(usuario.email).find((sistema) => sistema.name === nombre);
  const tipos = guardado?.sections.flatMap((seccion) => seccion.blocks.map((bloque) => bloque.type));
  expect(tipos).toEqual(expect.arrayContaining([...TIPOS_OBLIGATORIOS]));
}

describe('mis negocios: cada negocio es un cuaderno', () => {
  it('con un solo negocio el selector de la barra deja agregar otro, sin comparar', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    setupNegocios(usuario.email, KIOSCO);
    renderApp('/inicio');
    const user = userEvent.setup();

    await user.click(await selectorDe(/Kiosco de la plaza/));

    const menu = within(screen.getByRole('menu'));
    expect(menu.getByRole('menuitem', { name: /Agregar un negocio/ })).toBeVisible();
    expect(menu.queryByRole('menuitem', { name: /Todos mis negocios/ })).toBeNull();
  });

  it('lista los negocios de la cuenta y arranca con el primero', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    dadoDosNegocios(usuario);
    renderApp('/inicio');
    const user = userEvent.setup();

    await user.click(await selectorDe(/Almacén San Justo/));

    const menu = screen.getByRole('menu');
    expect(within(menu).getByRole('menuitem', { name: /Almacén San Justo/ })).toHaveAttribute(
      'aria-current',
      'true',
    );
    expect(within(menu).getByRole('menuitem', { name: /Kiosco de la plaza/ })).toBeEnabled();
    expect(within(menu).getByRole('menuitem', { name: /Todos mis negocios/ })).toBeEnabled();
  });

  it('cambiar de negocio en la barra lo deja activo y lo recuerda', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    dadoDosNegocios(usuario);
    renderApp('/inicio');

    await cuandoEligeEnLaBarra(/Kiosco de la plaza/);

    expect(await selectorDe(/Kiosco de la plaza/)).toBeVisible();
    expect(leerNegocioGuardado()).toBe(KIOSCO.id);
  });

  it('las pantallas sólo ven el cuaderno del negocio activo y no se pierde nada del otro', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    dadoDosNegocios(usuario);
    renderApp('/inicio');

    await cuandoEligeEnLaBarra(/Kiosco de la plaza/);
    await selectorDe(/Kiosco de la plaza/);

    const visibles = seleccionarSistemasDelNegocio(useSistemaStore.getState());
    expect(visibles.map((sistema) => sistema.id)).toEqual([KIOSCO.id]);
    expect(useSistemaStore.getState().sistemas).toHaveLength(2);
  });

  it('«Todos mis negocios» lleva a comparar los negocios de la cuenta', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    dadoDosNegocios(usuario);
    const router = renderApp('/inicio');

    await cuandoEligeEnLaBarra(/Todos mis negocios/);

    await waitFor(() => expect(router.state.location.pathname).toBe('/negocios/comparar'), ESPERA);
    expect(
      await screen.findByRole('heading', { name: 'Comparar mis negocios', level: 1 }, ESPERA),
    ).toBeVisible();
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('crea un negocio desde el selector de arriba con las secciones obligatorias, lo activa y lo guarda', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    dadoPremium(usuario.email);
    setupNegocios(usuario.email, KIOSCO);
    const router = renderApp('/inicio');
    const user = userEvent.setup();
    await verifyHeadingPresentAsync(/En qué puedo/);

    await cuandoAbreAgregarNegocioEnLaBarra(user);
    await user.type(screen.getByLabelText('Nombre del negocio'), 'Dietética Centro');
    await user.click(screen.getByRole('button', { name: 'Crear y entrar al negocio' }));

    expect(await selectorDe(/Dietética Centro/)).toBeVisible();
    expect(router.state.location.pathname).toBe('/inicio');
    await waitFor(
      () =>
        expect(nombresEnElServidor(usuario)).toEqual(['Kiosco de la plaza', 'Dietética Centro']),
      ESPERA,
    );
    entoncesElNegocioGuardadoTieneLasSeccionesObligatorias(usuario, 'Dietética Centro');
  });

  it('con el cupo del plan agotado explica el límite y lleva a Planes', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    setupNegocios(usuario.email, KIOSCO);
    const router = renderApp('/inicio');
    const user = userEvent.setup();
    await verifyHeadingPresentAsync(/En qué puedo/);

    await cuandoAbreAgregarNegocioEnLaBarra(user);

    expect(
      await screen.findByRole('heading', { name: 'Tu plan ya alcanzó el límite' }, ESPERA),
    ).toBeVisible();
    await user.click(screen.getByRole('link', { name: 'Ver planes' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/planes'), ESPERA);
  });

  it('elimina el negocio activo desde Configuración y conserva el otro', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    dadoDosNegocios(usuario);
    const router = renderApp('/cuenta');
    const user = userEvent.setup();
    await verifyHeadingPresentAsync('Tu cuenta');

    await user.click(await screen.findByRole('button', { name: 'Borrar negocio' }, ESPERA));
    expect(screen.getByText(/tablas y todo lo que cargaste en ellas/)).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Sí, eliminar negocio' }));

    await waitFor(
      () => expect(nombresEnElServidor(usuario)).toEqual(['Kiosco de la plaza']),
      ESPERA,
    );
    await waitFor(() => expect(router.state.location.pathname).toBe('/inicio'), ESPERA);
    expect(await selectorDe(/Kiosco de la plaza/)).toBeVisible();
    expect(leerNegocioGuardado()).toBe(KIOSCO.id);
  });
});
