import { describe, expect, it } from 'vitest';
import { actualizarNegocio, obtenerConfiguracionNegocio } from '@/api/negocio';
import { resolverLocalidad } from '@/api/ubicacion';
import { LOCALIDAD_FUERA_DE_LA_ZONA } from '@/mocks/db/ubicacion';
import { handlersPara } from '@/mocks/handlers';
import { setupUsuarioConSesion } from '@/test/testHelpers';

type Configuracion = Awaited<ReturnType<typeof obtenerConfiguracionNegocio>>;
type Modo = Parameters<typeof handlersPara>[0];

const PUNTO_EN_SAN_JUSTO = { latitud: -34.6701, longitud: -58.5625 };
const PUNTO_EN_CABA = { latitud: -34.6037, longitud: -58.3816 };
const DIRECCION = { direccion: 'Calle 123', ...PUNTO_EN_SAN_JUSTO };

let respuesta: Configuracion;
let configuracion: Configuracion;
let rechazo: Promise<unknown>;
let localidadResuelta: Awaited<ReturnType<typeof resolverLocalidad>>;
let rutasDelNegocio = 0;

describe('Configuración del negocio', () => {
  it('guarda con sesión y CSRF y conserva los campos de un PATCH parcial', async () => {
    dadoUnUsuarioConSesion();
    await dadoUnNegocioGuardadoComoKiosco();

    await cuandoSeEditaSoloElNombre();

    entoncesLaRespuestaConservaElRubroYLaPrivacidad();
    await entoncesLaConsultaDevuelveLoMismo();
  });

  it('asigna la zona que cubre la ubicación', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeGuardaUnaUbicacionEnSanJusto();

    entoncesLaZonaEs('San Justo - UNLaM');
  });

  it('quita la zona si la ubicación queda fuera de toda zona', async () => {
    dadoUnUsuarioConSesion();
    await cuandoSeGuardaUnaUbicacionEnSanJusto();

    await cuandoSeGuardaUnaUbicacionFueraDeToda();

    entoncesNoHayZona();
    await entoncesLaConsultaNoTieneZona();
  });

  it('rechaza una localidad que no coincide con la del mapa', async () => {
    dadoUnUsuarioConSesion();
    await dadoUnaUbicacionGuardadaEnSanJusto();

    cuandoSeIntentaGuardarLaLocalidad('Ramos Mejía');

    await entoncesSeRechazaConEstado(400);
  });

  it('conserva lo guardado cuando rechaza una localidad que no coincide', async () => {
    dadoUnUsuarioConSesion();
    await dadoUnaUbicacionGuardadaEnSanJusto();
    cuandoSeIntentaGuardarLaLocalidad('Ramos Mejía');
    await entoncesSeRechazaConEstado(400);

    await cuandoSeConsultaLaConfiguracion();

    entoncesLaLocalidadGuardadaEs('san JUSTO');
  });

  it('resuelve la localidad que el mapa reconoce en un punto', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeResuelveLaLocalidadDe(PUNTO_EN_SAN_JUSTO);

    entoncesLaLocalidadResueltaEs('San Justo', ['San Justo']);
  });

  it('resuelve como fuera de la zona un punto que el mapa no reconoce', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeResuelveLaLocalidadDe(PUNTO_EN_CABA);

    entoncesLaLocalidadResueltaEs(LOCALIDAD_FUERA_DE_LA_ZONA);
  });

  it('no mezcla la configuración entre dos cuentas', async () => {
    await dadoUnNegocioDeLaPrimeraCuentaYUnaSesionDeLaSegunda();

    await cuandoSeConsultaLaConfiguracion();

    entoncesElNombreNoEs('Primera cuenta');
  });

  it('rechaza un rubro inexistente', async () => {
    dadoUnUsuarioConSesion();

    cuandoSeIntentaGuardarElRubro('INEXISTENTE');

    await entoncesSeRechazaConEstado(400);
  });

  it('requiere una sesión para consultar el negocio', async () => {
    cuandoSeIntentaConsultarLaConfiguracion();

    await entoncesSeRechazaConEstado(401);
  });

  it('simula la configuración en modo completo', () => {
    cuandoSeContabilizanLasRutasDelNegocioEnModo('completo');

    entoncesHayRutasDelNegocio(2);
  });

  it('no simula la configuración en modo parcial', () => {
    cuandoSeContabilizanLasRutasDelNegocioEnModo('parcial');

    entoncesHayRutasDelNegocio(0);
  });

  it('no simula nada en modo ninguno', () => {
    cuandoSeContabilizanLasRutasDelNegocioEnModo('ninguno');

    entoncesHayRutasDelNegocio(0);
  });
});

function dadoUnUsuarioConSesion() {
  setupUsuarioConSesion();
}

async function dadoUnNegocioGuardadoComoKiosco() {
  await actualizarNegocio({ nombre: 'Almacén nuevo', rubro: 'KIOSCO', comparteDatosZona: false });
}

async function dadoUnaUbicacionGuardadaEnSanJusto() {
  await actualizarNegocio({ ubicacion: { ...DIRECCION, localidad: ' san JUSTO ' } });
}

async function dadoUnNegocioDeLaPrimeraCuentaYUnaSesionDeLaSegunda() {
  setupUsuarioConSesion({ email: 'primera@example.com' });
  await actualizarNegocio({ nombre: 'Primera cuenta' });
  setupUsuarioConSesion({ email: 'segunda@example.com' });
}

async function cuandoSeEditaSoloElNombre() {
  respuesta = await actualizarNegocio({ nombre: 'Almacén editado' });
}

async function cuandoSeGuardaUnaUbicacionEnSanJusto() {
  respuesta = await actualizarNegocio({ ubicacion: { ...DIRECCION, localidad: 'San Justo' } });
}

async function cuandoSeGuardaUnaUbicacionFueraDeToda() {
  respuesta = await actualizarNegocio({
    ubicacion: {
      direccion: 'Calle 123',
      localidad: LOCALIDAD_FUERA_DE_LA_ZONA,
      ...PUNTO_EN_CABA,
    },
  });
}

function cuandoSeIntentaGuardarLaLocalidad(localidad: string) {
  rechazo = actualizarNegocio({ ubicacion: { ...DIRECCION, localidad } });
}

function cuandoSeIntentaGuardarElRubro(rubro: string) {
  rechazo = actualizarNegocio({ rubro });
}

async function cuandoSeConsultaLaConfiguracion() {
  configuracion = await obtenerConfiguracionNegocio();
}

function cuandoSeIntentaConsultarLaConfiguracion() {
  rechazo = obtenerConfiguracionNegocio();
}

async function cuandoSeResuelveLaLocalidadDe(punto: { latitud: number; longitud: number }) {
  localidadResuelta = await resolverLocalidad(punto);
}

function cuandoSeContabilizanLasRutasDelNegocioEnModo(modo: Modo) {
  const esNegocio = (path: unknown) => String(path).endsWith('/business');
  rutasDelNegocio = handlersPara(modo).filter((handler) => esNegocio(handler.info.path)).length;
}

function entoncesLaRespuestaConservaElRubroYLaPrivacidad() {
  expect(respuesta).toEqual({
    nombre: 'Almacén editado',
    rubro: 'KIOSCO',
    ubicacion: null,
    comparteDatosZona: false,
    zona: null,
  });
}

async function entoncesLaConsultaDevuelveLoMismo() {
  expect(await obtenerConfiguracionNegocio()).toEqual(respuesta);
}

function entoncesLaZonaEs(zona: string) {
  expect(respuesta.zona).toBe(zona);
}

function entoncesNoHayZona() {
  expect(respuesta.zona).toBeNull();
}

async function entoncesLaConsultaNoTieneZona() {
  expect((await obtenerConfiguracionNegocio()).zona).toBeNull();
}

async function entoncesSeRechazaConEstado(estado: number) {
  await expect(rechazo).rejects.toMatchObject({ status: estado });
}

function entoncesLaLocalidadGuardadaEs(localidad: string) {
  expect(configuracion.ubicacion?.localidad).toBe(localidad);
}

function entoncesLaLocalidadResueltaEs(localidad: string, localidades?: string[]) {
  expect(localidadResuelta.localidad).toBe(localidad);
  if (localidades) expect(localidadResuelta.localidades).toEqual(localidades);
}

function entoncesElNombreNoEs(nombre: string) {
  expect(configuracion.nombre).not.toBe(nombre);
}

function entoncesHayRutasDelNegocio(cantidad: number) {
  expect(rutasDelNegocio).toBe(cantidad);
}
