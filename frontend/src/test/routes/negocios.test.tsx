import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import {
  guardarNegociosDe,
  negociosDe,
  reflejarNegocioActivo,
  type NegocioMock,
} from '@/mocks/db/negocios';
import { estado, guardarSistemasDe, type UsuarioMock } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

const premium = (email: string) =>
  estado.pruebasPremium.set(email, {
    inicioEn: new Date(Date.now() - 1000).toISOString(),
    finEn: new Date(Date.now() + 86_400_000).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });

const segundo: NegocioMock = {
  id: '20000000-0000-4000-8000-000000000002',
  nombre: 'Kiosco de la plaza',
  rubro: 'KIOSCO',
  creadoEn: '2026-11-13T12:00:00Z',
  configuracion: {
    nombre: 'Kiosco de la plaza',
    rubro: 'KIOSCO',
    ubicacion: null,
    comparteDatosZona: true,
    zona: null,
  },
};

function conSegundoNegocio(usuario: UsuarioMock, activoSegundo = false) {
  premium(usuario.email);
  const cuenta = negociosDe(usuario);
  guardarNegociosDe(usuario.email, {
    activoId: activoSegundo ? segundo.id : cuenta.activoId,
    negocios: [...cuenta.negocios, structuredClone(segundo)],
  });
  reflejarNegocioActivo(usuario.email);
}

describe('mis negocios · Ola 2', () => {
  it('con un solo negocio no muestra selector en la barra', async () => {
    setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    renderApp('/inicio');

    await verifyHeadingPresentAsync(/En qué puedo/);

    expect(screen.queryByRole('button', { name: /Negocio activo/ })).toBeNull();
  });

  it('lista los negocios y cambia el activo desde la barra', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    conSegundoNegocio(usuario);
    renderApp('/inicio');
    const user = userEvent.setup();

    const selector = await screen.findByRole('button', { name: /Negocio activo.*Negocio de Ana Pérez/ }, ESPERA);
    await user.click(selector);
    const menu = screen.getByRole('menu');
    expect(within(menu).getByText('2 de 6')).toBeVisible();
    expect(within(menu).getByRole('menuitem', { name: /Todos mis negocios/ })).toBeEnabled();

    await user.click(within(menu).getByRole('menuitem', { name: /Kiosco de la plaza/ }));

    expect(await screen.findByRole('button', { name: /Negocio activo.*Kiosco de la plaza/ }, ESPERA)).toBeVisible();
    expect(negociosDe(usuario).activoId).toBe(segundo.id);
  });

  it('«Todos mis negocios» lleva a comparar los negocios de la cuenta', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    conSegundoNegocio(usuario);
    const router = renderApp('/inicio');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /Negocio activo/ }, ESPERA));
    await user.click(screen.getByRole('menuitem', { name: /Todos mis negocios/ }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/negocios/comparar'), ESPERA);
    expect(await screen.findByRole('heading', { name: 'Comparar mis negocios', level: 1 }, ESPERA)).toBeVisible();
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('al cambiar de negocio guarda antes lo pendiente y no deja el borrador del anterior', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    conSegundoNegocio(usuario);
    const almacen = crearAlmacenCargado(hoy());
    guardarSistemasDe(usuario.email, [almacen], negociosDe(usuario).negocios[0]?.id);
    const guardados: string[] = [];
    server.events.on('request:start', ({ request }) => {
      if (request.method === 'PUT' && request.url.includes('/api/sistemas/')) guardados.push(request.url);
    });
    renderApp('/inicio');
    const user = userEvent.setup();
    const selector = await screen.findByRole('button', { name: /Negocio activo/ }, ESPERA);
    await waitFor(() => expect(useSistemaStore.getState().sistemas).toHaveLength(1), ESPERA);
    useSistemaStore.setState({ pendientes: [almacen.id] });
    sessionStorage.setItem(
      'ensambla:borrador-de-sesion',
      JSON.stringify({ usuarioId: usuario.id, fecha: Date.now(), sistemas: [almacen], pendientes: [almacen.id], eliminados: [] }),
    );

    await user.click(selector);
    await user.click(screen.getByRole('menuitem', { name: /Kiosco de la plaza/ }));

    expect(await screen.findByRole('button', { name: /Negocio activo.*Kiosco de la plaza/ }, ESPERA)).toBeVisible();
    expect(guardados.some((url) => url.endsWith(encodeURIComponent(almacen.id)))).toBe(true);
    expect(sessionStorage.getItem('ensambla:borrador-de-sesion')).toBeNull();
    expect(useSistemaStore.getState().pendientes).toEqual([]);
    expect(useSistemaStore.getState().sistemas.map((sistema) => sistema.id)).not.toContain(almacen.id);
    server.events.removeAllListeners();
  });

  it('crea un negocio desde Inicio, lo activa y vuelve a entrar a Inicio', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    premium(usuario.email);
    const router = renderApp('/inicio');
    const user = userEvent.setup();
    await verifyHeadingPresentAsync(/En qué puedo/);

    await user.click(screen.getByRole('button', { name: 'Agregar un negocio' }));
    await user.type(screen.getByLabelText('Nombre del negocio'), 'Dietética Centro');
    await user.click(screen.getByRole('button', { name: 'Crear y entrar al negocio' }));

    expect(await screen.findByRole('button', { name: /Negocio activo.*Dietética Centro/ }, ESPERA)).toBeVisible();
    await waitFor(() => expect(router.state.location.pathname).toBe('/inicio'), ESPERA);
    expect(negociosDe(usuario).negocios).toHaveLength(2);
    expect(negociosDe(usuario).negocios.find((negocio) => negocio.id === negociosDe(usuario).activoId)?.nombre)
      .toBe('Dietética Centro');
  });

  it('con el cupo gratuito agotado explica el límite y lleva a Planes', async () => {
    setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    const router = renderApp('/inicio');
    const user = userEvent.setup();
    await verifyHeadingPresentAsync(/En qué puedo/);

    await user.click(screen.getByRole('button', { name: 'Agregar un negocio' }));

    expect(screen.getByRole('heading', { name: 'Tu plan ya alcanzó el límite' })).toBeVisible();
    await user.click(screen.getByRole('link', { name: 'Ver planes' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/planes'), ESPERA);
  });

  it('elimina desde Configuración, confirma el alcance y conserva el otro negocio', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    conSegundoNegocio(usuario);
    const router = renderApp('/cuenta');
    const user = userEvent.setup();
    await verifyHeadingPresentAsync('Tu cuenta');

    await user.click(await screen.findByRole('button', { name: 'Eliminar negocio' }, ESPERA));
    expect(screen.getByText(/secciones, movimientos, avisos, objetivos, zona/)).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Sí, eliminar negocio' }));

    await waitFor(() => expect(negociosDe(usuario).negocios).toHaveLength(1), ESPERA);
    expect(negociosDe(usuario).negocios[0]?.nombre).toBe('Kiosco de la plaza');
    await waitFor(() => expect(router.state.location.pathname).toBe('/inicio'), ESPERA);
    expect(screen.queryByRole('button', { name: /Negocio activo/ })).toBeNull();
  });
});
