import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

const abrirCuenta = async (ruta = '/cuenta') => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez', email: 'ana@example.com' });
  const router = renderApp(ruta);
  await verifyHeadingPresentAsync('Tu cuenta');
  return { usuario, router, user: userEvent.setup() };
};

const menu = () => screen.getByRole('navigation', { name: 'Secciones de tu cuenta' });

describe('tu cuenta', () => {
  it('abre en el perfil: el nombre, el email de la cuenta y el comercio', async () => {
    await abrirCuenta();

    expect(await screen.findByLabelText('¿Cómo querés que te llame Sami?')).toHaveValue(
      'Ana Pérez',
    );
    expect(screen.getByLabelText('Email de la cuenta')).toHaveValue('ana@example.com');
    expect(screen.getByLabelText('Email de la cuenta')).toHaveAttribute('readonly');
    expect(await screen.findByRole('heading', { name: 'Tu comercio' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Ubicación y rubro de mi comercio' })).toHaveAttribute(
      'href',
      '/ubicacion',
    );
    expect(within(menu()).getByRole('link', { name: 'Perfil' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('guardar el nombre queda en las preferencias y sólo se habilita con un cambio', async () => {
    const { user, usuario } = await abrirCuenta();
    const guardar = await screen.findByRole('button', { name: 'Guardar nombre' });
    expect(guardar).toBeDisabled();

    const campo = screen.getByLabelText('¿Cómo querés que te llame Sami?');
    await user.clear(campo);
    await user.type(campo, 'Anita');
    expect(guardar).toBeEnabled();
    await user.click(guardar);

    await waitFor(
      () => expect(estado.preferencias.get(usuario.email)?.nombre).toBe('Anita'),
      ESPERA,
    );
  });

  it('un nombre vacío no se puede guardar', async () => {
    const { user } = await abrirCuenta();
    const campo = await screen.findByLabelText('¿Cómo querés que te llame Sami?');

    await user.clear(campo);

    expect(screen.getByRole('button', { name: 'Guardar nombre' })).toBeDisabled();
  });

  it('el menú cambia de sección y la dirección lo recuerda', async () => {
    const { user, router } = await abrirCuenta();

    await user.click(within(menu()).getByRole('link', { name: 'Notificaciones' }));
    expect(await screen.findByRole('heading', { name: 'Qué me avisa Sami' })).toBeVisible();
    expect(router.state.location.search).toMatchObject({ seccion: 'notificaciones' });
    expect(within(menu()).getByRole('link', { name: 'Notificaciones' })).toHaveAttribute(
      'aria-current',
      'page',
    );

    await user.click(within(menu()).getByRole('link', { name: 'Suscripción' }));
    expect(await screen.findByRole('heading', { name: 'Tu suscripción' })).toBeVisible();
  });

  it('entrar directo a una sección desde la dirección la muestra', async () => {
    await abrirCuenta('/cuenta?seccion=notificaciones');
    expect(await screen.findByRole('heading', { name: 'Qué me avisa Sami' })).toBeVisible();
  });

  it('una sección que no existe cae en el perfil', async () => {
    await abrirCuenta('/cuenta?seccion=cualquiera');
    expect(await screen.findByLabelText('¿Cómo querés que te llame Sami?')).toBeVisible();
  });

  it('en notificaciones se apagan y prenden los temas, y se guarda en la cuenta', async () => {
    const { user, usuario } = await abrirCuenta('/cuenta?seccion=notificaciones');
    const stock = await screen.findByRole('checkbox', { name: 'Tu stock' });
    expect(stock).toBeChecked();

    await user.click(stock);

    expect(screen.getByRole('checkbox', { name: 'Tu stock' })).not.toBeChecked();
    await waitFor(
      () => expect(estado.preferencias.get(usuario.email)?.grupos).toMatchObject({ stock: false }),
      ESPERA,
    );

    await user.click(screen.getByRole('checkbox', { name: 'Tu stock' }));
    await waitFor(
      () => expect(estado.preferencias.get(usuario.email)?.grupos).toMatchObject({ stock: true }),
      ESPERA,
    );
  });

  it('en notificaciones se pueden volver a mostrar los avisos ocultos', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    estado.preferencias.set(usuario.email, {
      descartadas: ['aviso-1', 'aviso-2'],
      grupos: {},
      bienvenidaVista: true,
    });
    renderApp('/cuenta?seccion=notificaciones');
    const user = userEvent.setup();

    await user.click(
      await screen.findByRole('button', { name: 'Volver a mostrar 2 avisos ocultos' }),
    );

    await waitFor(
      () => expect(estado.preferencias.get(usuario.email)?.descartadas).toEqual([]),
      ESPERA,
    );
    expect(screen.queryByRole('button', { name: /avisos? ocultos?/ })).toBeNull();
  });

  it('en suscripción muestra el plan, lo que se usó y lleva a los planes', async () => {
    const { user, router } = await abrirCuenta('/cuenta?seccion=suscripcion');

    expect(await screen.findByText('Gratuito', {}, ESPERA)).toBeVisible();
    expect(screen.getByText('Sin costo')).toBeVisible();
    expect(screen.getByText('Sistemas')).toBeVisible();
    expect(screen.getByText('Registros')).toBeVisible();

    await user.click(screen.getByRole('link', { name: 'Ver planes y beneficios' }));

    await verifyPathAsync(router, '/planes');
  });

  it('en sesión se cierra la sesión y vuelve al ingreso', async () => {
    const { user, router } = await abrirCuenta('/cuenta?seccion=sesion');

    await clickButton(user, 'Cerrar sesión');

    await verifyPathAsync(router, '/login');
  });

  it('sin sesión lleva al ingreso', async () => {
    const router = renderApp('/cuenta');

    await verifyPathAsync(router, '/login');
  });
});
