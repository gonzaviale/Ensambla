import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { ApiError } from '@/api/apiError';
import { pedirLectura } from '@/api/importaciones';
import { makeBlock } from '@/lib/sistema/core';
import { server } from '@/mocks/node';
import { setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';
import type { PedidoDeLectura } from '@/types/importacion';

type Lectura = Awaited<ReturnType<typeof pedirLectura>>;
type PedidoEnviado = { nombre: string; destinoExistente: boolean; campos: { key: string }[] };

let pedido: PedidoDeLectura;
let lectura: Lectura;
let error: unknown;
let nombreDeLaImagen = '';
let pedidoEnviado: PedidoEnviado | null = null;

const pedidoDe = (destinoExistente: boolean): PedidoDeLectura => {
  const bloque = makeBlock('sales');
  return {
    nombre: 'Mis ventas',
    contexto: 'registros',
    tipo: bloque.type,
    campos: bloque.fields,
    vinculos: {},
    destinoExistente,
  };
};

const foto = () => new File(['pixeles'], 'sistema.jpg', { type: 'image/jpeg' });

const textoDe = async (parte: FormDataEntryValue | null) =>
  typeof parte === 'string' ? parte : ((await parte?.text()) ?? 'null');

describe('API de importaciones', () => {
  it('manda la foto en un multipart', async () => {
    dadoUnServidorQueRecuerdaLoQueRecibe();

    await cuandoSePideLaLecturaDeUnaFoto();

    entoncesElServidorRecibioLaImagen('sistema.jpg');
  });

  it('manda el bloque de destino con sus campos', async () => {
    dadoUnServidorQueRecuerdaLoQueRecibe();

    await cuandoSePideLaLecturaDeUnaFoto();

    entoncesElServidorRecibioElBloqueDeDestino();
  });

  it('devuelve las filas leídas sin columnas nuevas ni dudas', async () => {
    dadoUnServidorQueRecuerdaLoQueRecibe();

    await cuandoSePideLaLecturaDeUnaFoto();

    entoncesLaLecturaTraeLaFilaDe1500SinNovedades();
  });

  it('una imagen sin nombre (la que sale de achicar una foto) se manda como foto.jpg', async () => {
    dadoUnServidorQueRecuerdaLoQueRecibe();

    await cuandoSePideLaLecturaDeUnaImagenSinNombre();

    entoncesElServidorRecibioLaImagen('foto.jpg');
    entoncesElPedidoIndicaQueNoHayDestinoExistente();
  });

  it('en la simulación devuelve registros, columnas nuevas y dudas de ejemplo', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSePideLaLecturaDeUnaFoto();

    entoncesLaLecturaTraeRegistrosColumnasNuevasYDudas();
  });

  it('propaga el mensaje del servidor cuando la foto no se puede leer', async () => {
    dadoQueElServidorNoPuedeLeerLaFoto();

    await cuandoSeIntentaLeerLaFoto();

    entoncesSePropagaElMensaje('La estructura leída no es compatible con el bloque de destino');
  });
});

function dadoUnUsuarioConSesion() {
  setupUsuarioConSesion();
}

function dadoUnServidorQueRecuerdaLoQueRecibe() {
  setupUsuarioConSesion();
  nombreDeLaImagen = '';
  pedidoEnviado = null;
  server.use(
    http.post('/api/importaciones/lectura', async ({ request }) => {
      const cuerpo = await request.formData();
      nombreDeLaImagen = (cuerpo.get('imagen') as File).name;
      pedidoEnviado = JSON.parse(await textoDe(cuerpo.get('pedido')));
      return HttpResponse.json({ filas: [{ monto: 1500 }], descripcion: '' });
    }),
  );
}

function dadoQueElServidorNoPuedeLeerLaFoto() {
  setupUsuarioConSesion();
  setupServerError(
    'post',
    '/importaciones/lectura',
    400,
    'La estructura leída no es compatible con el bloque de destino',
  );
}

async function cuandoSePideLaLecturaDeUnaFoto() {
  pedido = pedidoDe(true);
  lectura = await pedirLectura(pedido, foto());
}

async function cuandoSePideLaLecturaDeUnaImagenSinNombre() {
  lectura = await pedirLectura(
    { ...pedidoDe(false), destinoExistente: undefined },
    new Blob(['x']),
  );
}

async function cuandoSeIntentaLeerLaFoto() {
  error = await pedirLectura(pedidoDe(true), foto()).catch((e: unknown) => e);
}

function entoncesElServidorRecibioLaImagen(nombre: string) {
  expect(nombreDeLaImagen).toBe(nombre);
}

function entoncesElServidorRecibioElBloqueDeDestino() {
  expect(pedidoEnviado).toMatchObject({ nombre: 'Mis ventas', destinoExistente: true });
  expect(pedidoEnviado?.campos.map((campo) => campo.key)).toEqual(
    pedido.campos.map((campo) => campo.key),
  );
}

function entoncesElPedidoIndicaQueNoHayDestinoExistente() {
  expect(pedidoEnviado?.destinoExistente).toBe(false);
}

function entoncesLaLecturaTraeLaFilaDe1500SinNovedades() {
  expect(lectura.filas).toEqual([{ monto: 1500 }]);
  expect(lectura.camposNuevos).toEqual([]);
  expect(lectura.dudas).toEqual([]);
}

function entoncesLaLecturaTraeRegistrosColumnasNuevasYDudas() {
  expect(lectura.filas.length).toBeGreaterThan(0);
  expect(lectura.camposNuevos.length).toBeGreaterThan(0);
  expect(lectura.dudas.length).toBeGreaterThan(0);
}

function entoncesSePropagaElMensaje(mensaje: string) {
  expect(error).toBeInstanceOf(ApiError);
  expect((error as ApiError).message).toBe(mensaje);
}
