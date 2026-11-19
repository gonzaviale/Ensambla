import { describe, expect, it } from 'vitest';
import { obtenerPasosObjetivo } from '@/api/objetivos';
import { setupServerResponse, setupUsuarioConSesion } from '@/test/testHelpers';

const PASO = {
  tipo: 'SUBIR_PRECIO',
  nombre: 'Yerba',
  precio: 3600,
  costo: 2700,
  stock: 4,
  vendidas30Dias: 60,
  precioZona: { minimo: 3500, promedio: 3900, maximo: 4300 },
};

type Pasos = Awaited<ReturnType<typeof obtenerPasosObjetivo>>;

let pasos: Pasos;
let rechazo: Promise<Pasos>;

describe('API de objetivos', () => {
  it('obtiene los productos elegidos por el servidor con sus datos reales', async () => {
    dadoQueElServidorElige([PASO]);

    await cuandoSePidenLosPasos();

    entoncesSeRecibenLosPasos([PASO]);
  });

  it('admite un producto sin precio de zona', async () => {
    dadoQueElServidorElige([{ ...PASO, precioZona: null }]);

    await cuandoSePidenLosPasos();

    entoncesElPrimerPasoNoTienePrecioDeZona();
  });

  it('admite una lista vacía', async () => {
    dadoQueElServidorElige([]);

    await cuandoSePidenLosPasos();

    entoncesSeRecibenLosPasos([]);
  });

  it('rechaza una respuesta que no respeta el contrato', async () => {
    dadoQueElServidorElige([{ ...PASO, vendidas30Dias: -1 }]);

    cuandoSeIntentanPedirLosPasos();

    await entoncesSeRechazaElContrato();
  });
});

function dadoQueElServidorElige(respuesta: unknown[]) {
  setupUsuarioConSesion();
  setupServerResponse('get', '/objetivos/pasos', 200, respuesta);
}

async function cuandoSePidenLosPasos() {
  pasos = await obtenerPasosObjetivo();
}

function cuandoSeIntentanPedirLosPasos() {
  rechazo = obtenerPasosObjetivo();
}

function entoncesSeRecibenLosPasos(esperados: unknown[]) {
  expect(pasos).toEqual(esperados);
}

function entoncesElPrimerPasoNoTienePrecioDeZona() {
  expect(pasos[0]?.precioZona).toBeNull();
}

async function entoncesSeRechazaElContrato() {
  await expect(rechazo).rejects.toMatchObject({ name: 'ZodError' });
}
