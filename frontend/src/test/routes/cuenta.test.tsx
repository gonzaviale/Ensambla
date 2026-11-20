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

type Router = ReturnType<typeof renderApp>;
type Usuario = ReturnType<typeof userEvent.setup>;

let router: Router;
let user: Usuario;
let email = '';

describe('tu cuenta', () => {
  it('abre en el perfil con el nombre y el email de la cuenta', async () => {
    await dadaLaCuenta();

    await entoncesElPerfilMuestraElNombreYElEmail();
  });

  it('el perfil ofrece el comercio y lleva a su ubicación', async () => {
    await dadaLaCuenta();

    await entoncesElPerfilOfreceElComercio();
  });

  it('el menú marca el perfil como la sección actual', async () => {
    await dadaLaCuenta();

    entoncesLaSeccionActualEs('Perfil');
  });

  it('guardar el nombre sólo se habilita con un cambio', async () => {
    await dadaLaCuenta();

    expect(await screen.findByRole('button', { name: 'Guardar nombre' })).toBeDisabled();
  });

  it('guardar el nombre queda en las preferencias', async () => {
    await dadaLaCuenta();

    await cuandoSeCambiaElNombreA('Anita');

    await entoncesLasPreferenciasTienenElNombre('Anita');
  });

  it('un nombre vacío no se puede guardar', async () => {
    await dadaLaCuenta();

    await cuandoSeBorraElNombre();

    entoncesNoSePuedeGuardarElNombre();
  });

  it('el menú cambia de sección y la dirección lo recuerda', async () => {
    await dadaLaCuenta();

    await cuandoSeElijeLaSeccion('Notificaciones');

    await entoncesSeMuestraLaSeccion('Qué me avisa Sami', 'notificaciones');
  });

  it('el menú marca la sección elegida como la actual', async () => {
    await dadaLaCuenta();

    await cuandoSeElijeLaSeccion('Notificaciones');

    entoncesLaSeccionActualEs('Notificaciones');
  });

  it('el menú lleva a la suscripción', async () => {
    await dadaLaCuenta();

    await cuandoSeElijeLaSeccion('Suscripción');

    expect(await screen.findByRole('heading', { name: 'Tu suscripción' })).toBeVisible();
  });

  it('entrar directo a una sección desde la dirección la muestra', async () => {
    await dadaLaCuenta('/cuenta?seccion=notificaciones');

    expect(await screen.findByRole('heading', { name: 'Qué me avisa Sami' })).toBeVisible();
  });

  it('una sección que no existe cae en el perfil', async () => {
    await dadaLaCuenta('/cuenta?seccion=cualquiera');

    expect(await screen.findByLabelText('¿Cómo querés que te llame Sami?')).toBeVisible();
  });

  it('en notificaciones se apaga un tema y se guarda en la cuenta', async () => {
    await dadaLaCuenta('/cuenta?seccion=notificaciones');

    await cuandoSeApagaElTemaDeStock();

    await entoncesElTemaDeStockQuedaGuardadoComo(false);
  });

  it('en notificaciones se vuelve a prender un tema y se guarda en la cuenta', async () => {
    await dadaLaCuenta('/cuenta?seccion=notificaciones');
    await cuandoSeApagaElTemaDeStock();

    await cuandoSePrendeElTemaDeStock();

    await entoncesElTemaDeStockQuedaGuardadoComo(true);
  });

  it('en notificaciones se pueden volver a mostrar los avisos ocultos', async () => {
    dadaUnaCuentaConAvisosOcultos();

    await cuandoSeVuelvenAMostrarLosAvisosOcultos();

    await entoncesYaNoHayAvisosOcultos();
  });

  it('en suscripción muestra el plan y lo que se usó', async () => {
    await dadaLaCuenta('/cuenta?seccion=suscripcion');

    await entoncesLaSuscripcionMuestraElPlanYLoUsado();
  });

  it('en suscripción lleva a los planes', async () => {
    await dadaLaCuenta('/cuenta?seccion=suscripcion');
    await screen.findByText('Gratuito', {}, ESPERA);

    await cuandoSePideVerLosPlanes();

    await verifyPathAsync(router, '/planes');
  });

  it('en sesión se cierra la sesión y vuelve al ingreso', async () => {
    await dadaLaCuenta('/cuenta?seccion=sesion');

    await clickButton(user, 'Cerrar sesión');

    await verifyPathAsync(router, '/login');
  });

  it('sin sesión lleva al ingreso', async () => {
    router = renderApp('/cuenta');

    await verifyPathAsync(router, '/login');
  });
});

const menu = () => screen.getByRole('navigation', { name: 'Secciones de tu cuenta' });

async function dadaLaCuenta(ruta = '/cuenta') {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez', email: 'ana@example.com' });
  email = usuario.email;
  router = renderApp(ruta);
  await verifyHeadingPresentAsync('Tu cuenta');
  user = userEvent.setup();
}

function dadaUnaCuentaConAvisosOcultos() {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  email = usuario.email;
  estado.preferencias.set(email, {
    descartadas: ['aviso-1', 'aviso-2'],
    grupos: {},
    bienvenidaVista: true,
  });
  renderApp('/cuenta?seccion=notificaciones');
  user = userEvent.setup();
}

const campoDelNombre = () => screen.getByLabelText('¿Cómo querés que te llame Sami?');

async function cuandoSeCambiaElNombreA(nombre: string) {
  await screen.findByRole('button', { name: 'Guardar nombre' });
  await user.clear(campoDelNombre());
  await user.type(campoDelNombre(), nombre);
  await user.click(screen.getByRole('button', { name: 'Guardar nombre' }));
}

async function cuandoSeBorraElNombre() {
  await user.clear(await screen.findByLabelText('¿Cómo querés que te llame Sami?'));
}

async function cuandoSeElijeLaSeccion(nombre: string) {
  await user.click(within(menu()).getByRole('link', { name: nombre }));
}

async function cuandoSeApagaElTemaDeStock() {
  await user.click(await screen.findByRole('checkbox', { name: 'Tu stock' }));
}

async function cuandoSePrendeElTemaDeStock() {
  await user.click(screen.getByRole('checkbox', { name: 'Tu stock' }));
}

async function cuandoSeVuelvenAMostrarLosAvisosOcultos() {
  await user.click(
    await screen.findByRole('button', { name: 'Volver a mostrar 2 avisos ocultos' }),
  );
}

async function cuandoSePideVerLosPlanes() {
  await user.click(screen.getByRole('link', { name: 'Ver planes y beneficios' }));
}

async function entoncesElPerfilMuestraElNombreYElEmail() {
  expect(await screen.findByLabelText('¿Cómo querés que te llame Sami?')).toHaveValue('Ana Pérez');
  const correo = screen.getByLabelText('Email de la cuenta');
  expect(correo).toHaveValue('ana@example.com');
  expect(correo).toHaveAttribute('readonly');
}

async function entoncesElPerfilOfreceElComercio() {
  expect(await screen.findByRole('heading', { name: 'Tu comercio' })).toBeVisible();
  expect(screen.getByRole('link', { name: 'Ubicación y rubro de mi comercio' })).toHaveAttribute(
    'href',
    '/ubicacion',
  );
}

function entoncesLaSeccionActualEs(nombre: string) {
  expect(within(menu()).getByRole('link', { name: nombre })).toHaveAttribute(
    'aria-current',
    'page',
  );
}

async function entoncesLasPreferenciasTienenElNombre(nombre: string) {
  await waitFor(() => expect(estado.preferencias.get(email)?.nombre).toBe(nombre), ESPERA);
}

function entoncesNoSePuedeGuardarElNombre() {
  expect(screen.getByRole('button', { name: 'Guardar nombre' })).toBeDisabled();
}

async function entoncesSeMuestraLaSeccion(titulo: string, seccion: string) {
  expect(await screen.findByRole('heading', { name: titulo })).toBeVisible();
  expect(router.state.location.search).toMatchObject({ seccion });
}

async function entoncesElTemaDeStockQuedaGuardadoComo(prendido: boolean) {
  const casilla = screen.getByRole('checkbox', { name: 'Tu stock' });
  if (prendido) expect(casilla).toBeChecked();
  else expect(casilla).not.toBeChecked();
  await waitFor(
    () => expect(estado.preferencias.get(email)?.grupos).toMatchObject({ stock: prendido }),
    ESPERA,
  );
}

async function entoncesYaNoHayAvisosOcultos() {
  await waitFor(() => expect(estado.preferencias.get(email)?.descartadas).toEqual([]), ESPERA);
  expect(screen.queryByRole('button', { name: /avisos? ocultos?/ })).toBeNull();
}

async function entoncesLaSuscripcionMuestraElPlanYLoUsado() {
  expect(await screen.findByText('Gratuito', {}, ESPERA)).toBeVisible();
  expect(screen.getByText('Sin costo')).toBeVisible();
  expect(screen.getByText('Negocios')).toBeVisible();
  expect(screen.getByText('Registros')).toBeVisible();
}
