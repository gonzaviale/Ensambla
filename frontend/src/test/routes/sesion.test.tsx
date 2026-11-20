import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import {
  clickButton,
  setupServerError,
  setupUsuarioConSesion,
  setupUsuarioSinSesion,
  typeLabel,
  verifyAlertPresentAsync,
  verifyHeadingPresentAsync,
  verifyPathAsync,
  verifyTextPresentAsync,
} from '@/test/testHelpers';
import { estado } from '@/mocks/db/estado';
import { usuarioSchema } from '@/types/auth';

const EMAIL = 'brian-sesion@example.com';

type Router = ReturnType<typeof renderApp>;

let router: Router;
let contrato: unknown;

describe('registro y sesión del cliente', () => {
  it('registra usuario y negocio, inicia sesión y muestra el espacio privado', async () => {
    dadaLaPantallaDeRegistro();

    await cuandoSeCompletaElRegistro();

    await entoncesSeMuestraElEspacioPrivado();
    entoncesQuedoRegistradoBrianSinGuardarNadaEnElNavegador();
  });

  it('rechaza un email repetido con el mensaje del servidor', async () => {
    dadoUnUsuarioConEseEmail();
    dadaLaPantallaDeRegistro();

    await cuandoSeCompletaElRegistro();

    await verifyAlertPresentAsync('Ya existe una cuenta con ese email');
    entoncesSigueEnElRegistro();
  });

  it('valida campos obligatorios antes de enviar el registro', async () => {
    dadaLaPantallaDeRegistro();
    await verifyHeadingPresentAsync('Creá tu cuenta');

    await cuandoSeEnviaElRegistroVacio();

    await entoncesSePidenLosCamposObligatorios();
    entoncesNoSeRegistroNadie();
  });

  it('muestra los errores del servidor junto al campo del negocio', async () => {
    dadoQueElServidorRechazaElNombreDelNegocio();
    dadaLaPantallaDeRegistro();

    await cuandoSeCompletaElRegistro();

    await entoncesElCampoDelNegocioMuestraElErrorDelServidor();
  });

  // El cierre de sesión vive en el perfil del encabezado: sus pruebas se suman cuando esa pantalla esté.

  it('verifica la sesión en el servidor aunque el cliente hubiera cacheado un usuario', async () => {
    await dadoUnUsuarioConSesionYElInicioAbierto();
    dadoQueElServidorYaNoReconoceLaSesion();

    await cuandoSeVuelveAlInicio();

    await entoncesSeLlevaAIngresar();
  });

  it('el contrato de usuario solo contiene datos públicos', () => {
    cuandoSeLeeUnUsuarioConHashDeContrasenia();

    entoncesSoloQuedanLosDatosPublicos();
  });
});

function dadaLaPantallaDeRegistro() {
  router = renderApp('/registro');
}

function dadoUnUsuarioConEseEmail() {
  setupUsuarioSinSesion({ email: EMAIL });
}

function dadoQueElServidorRechazaElNombreDelNegocio() {
  setupServerError('post', '/auth/register', 400, 'Revisá los datos enviados', {
    nombreNegocio: 'El nombre del negocio no es válido',
  });
}

async function dadoUnUsuarioConSesionYElInicioAbierto() {
  setupUsuarioConSesion();
  router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
}

function dadoQueElServidorYaNoReconoceLaSesion() {
  estado.sesionUsuarioId = null;
}

async function cuandoSeCompletaElRegistro() {
  const user = userEvent.setup();
  await verifyHeadingPresentAsync('Creá tu cuenta');
  await typeLabel(user, 'Tu nombre', 'Brian');
  await typeLabel(user, 'Nombre del negocio', 'Mi negocio');
  await typeLabel(user, 'Email', EMAIL);
  await typeLabel(user, 'Contraseña', 'ClaveSegura123!');
  await clickButton(user, 'Crear cuenta');
}

async function cuandoSeEnviaElRegistroVacio() {
  await clickButton(userEvent.setup(), 'Crear cuenta');
}

async function cuandoSeVuelveAlInicio() {
  await router.navigate({ to: '/inicio' });
}

function cuandoSeLeeUnUsuarioConHashDeContrasenia() {
  contrato = usuarioSchema.parse({
    id: 'id',
    nombre: 'Brian',
    email: EMAIL,
    hashContrasenia: 'no-se-expone',
  });
}

async function entoncesSeMuestraElEspacioPrivado() {
  await verifyPathAsync(router, '/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
}

function entoncesQuedoRegistradoBrianSinGuardarNadaEnElNavegador() {
  expect(estado.usuarios.get(EMAIL)?.nombre).toBe('Brian');
  expect(localStorage.length).toBe(0);
}

function entoncesSigueEnElRegistro() {
  expect(router.state.location.pathname).toBe('/registro');
}

async function entoncesSePidenLosCamposObligatorios() {
  await verifyTextPresentAsync('Escribí tu nombre');
  await verifyTextPresentAsync('Escribí el nombre de tu negocio');
  await verifyTextPresentAsync('Escribí tu email');
  await verifyTextPresentAsync('Usá al menos 8 caracteres');
}

function entoncesNoSeRegistroNadie() {
  expect(estado.usuarios.size).toBe(0);
}

async function entoncesElCampoDelNegocioMuestraElErrorDelServidor() {
  await verifyTextPresentAsync('El nombre del negocio no es válido');
  await verifyAlertPresentAsync('Revisá los datos enviados');
  expect(screen.getByLabelText('Nombre del negocio')).toHaveAttribute('aria-invalid', 'true');
}

async function entoncesSeLlevaAIngresar() {
  await verifyPathAsync(router, '/login');
  await verifyHeadingPresentAsync('Entrá a tu cuenta');
}

function entoncesSoloQuedanLosDatosPublicos() {
  expect(contrato).toEqual({ id: 'id', nombre: 'Brian', email: EMAIL });
}
