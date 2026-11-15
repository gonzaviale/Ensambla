import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import {
  guardarNegociosDe,
  negociosDe,
  reflejarNegocioActivo,
  type NegocioMock,
} from '@/mocks/db/negocios';
import { estado, type UsuarioMock } from '@/mocks/db/estado';
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
    expect(within(menu).getByRole('menuitem', { name: /Todos mis negocios/ })).toBeDisabled();

    await user.click(within(menu).getByRole('menuitem', { name: /Kiosco de la plaza/ }));

    expect(await screen.findByRole('button', { name: /Negocio activo.*Kiosco de la plaza/ }, ESPERA)).toBeVisible();
    expect(negociosDe(usuario).activoId).toBe(segundo.id);
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
