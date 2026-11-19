import { describe, expect, it } from 'vitest';
import { iniciarSesion, obtenerUsuarioActual, registrarUsuario } from '@/api/auth';
import { ApiError } from '@/api/apiError';
import { setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

const DATOS_DE_LUCIA = {
  nombre: 'Lucía Gómez',
  nombreNegocio: 'Velas Lumbre',
  email: 'lucia@example.com',
  contrasenia: 'una-contrasenia-segura',
};

type Usuario = Awaited<ReturnType<typeof obtenerUsuarioActual>>;

let usuario: Usuario;
let error: unknown;
let consulta: Promise<Usuario>;

describe('auth', () => {
  it('devuelve null cuando no hay sesión (401 es un estado, no un error)', async () => {
    await cuandoSePideElUsuarioActual();

    entoncesNoHayUsuario();
  });

  it('devuelve el usuario de la sesión abierta', async () => {
    dadoUnaSesionAbiertaDe('Lucía Gómez');

    await cuandoSePideElUsuarioActual();

    entoncesElUsuarioEs('Lucía Gómez');
  });

  it('propaga los errores que no son de sesión', async () => {
    dadoQueElServidorFalla();

    cuandoSeIntentaPedirElUsuarioActual();

    await entoncesSePropagaUnErrorDeLaApi();
  });

  it('registra una cuenta y después permite iniciar sesión', async () => {
    await cuandoSeRegistraLuciaYSeInicia();

    entoncesElUsuarioTieneElEmail('lucia@example.com');
  });

  it('rechaza un email ya registrado con 409', async () => {
    dadoUnaSesionConElEmail('lucia@example.com');

    await cuandoSeRegistraOtraCuentaConElMismoEmail();

    entoncesElEstadoDelErrorEs(409);
  });

  it('rechaza credenciales incorrectas con 401', async () => {
    dadoUnaSesionConElEmail('lucia@example.com');

    await cuandoSeIniciaSesionConUnaContraseniaIncorrecta();

    entoncesElEstadoDelErrorEs(401);
  });
});

function dadoUnaSesionAbiertaDe(nombre: string) {
  setupUsuarioConSesion({ nombre });
}

function dadoUnaSesionConElEmail(email: string) {
  setupUsuarioConSesion({ email });
}

function dadoQueElServidorFalla() {
  setupServerError('get', '/auth/me', 500, 'Ocurrió un error interno en el servidor');
}

async function cuandoSePideElUsuarioActual() {
  usuario = await obtenerUsuarioActual();
}

function cuandoSeIntentaPedirElUsuarioActual() {
  consulta = obtenerUsuarioActual();
}

async function cuandoSeRegistraLuciaYSeInicia() {
  await registrarUsuario(DATOS_DE_LUCIA);
  usuario = await iniciarSesion({
    email: DATOS_DE_LUCIA.email,
    contrasenia: DATOS_DE_LUCIA.contrasenia,
  });
}

async function cuandoSeRegistraOtraCuentaConElMismoEmail() {
  error = await registrarUsuario({
    nombre: 'Otra',
    nombreNegocio: 'Otro negocio',
    email: 'lucia@example.com',
    contrasenia: 'una-contrasenia-segura',
  }).catch((e: unknown) => e);
}

async function cuandoSeIniciaSesionConUnaContraseniaIncorrecta() {
  error = await iniciarSesion({ email: 'lucia@example.com', contrasenia: 'incorrecta' }).catch(
    (e: unknown) => e,
  );
}

function entoncesNoHayUsuario() {
  expect(usuario).toBeNull();
}

function entoncesElUsuarioEs(nombre: string) {
  expect(usuario).toMatchObject({ nombre });
}

function entoncesElUsuarioTieneElEmail(email: string) {
  expect(usuario?.email).toBe(email);
}

async function entoncesSePropagaUnErrorDeLaApi() {
  await expect(consulta).rejects.toBeInstanceOf(ApiError);
}

function entoncesElEstadoDelErrorEs(estadoHttp: number) {
  expect(error).toMatchObject({ status: estadoHttp });
}
