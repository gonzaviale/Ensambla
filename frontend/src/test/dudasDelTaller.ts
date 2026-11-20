import { screen, within } from '@testing-library/react';
import { http, HttpResponse, type JsonBodyType } from 'msw';
import { expect } from 'vitest';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';
import { dadoQueSeAbreElTaller, laPersonaDelTaller } from '@/test/taller';
import { ESPERA, clickButton } from '@/test/testHelpers';

type Pedido = Record<string, unknown>;

const DUDAS = [
  {
    tema: 'Tildes',
    pregunta: '¿Qué significan los tildes?',
    opciones: ['Ya entregado y cobrado', 'Entregado sin cobrar'],
  },
  {
    tema: 'Número de la derecha',
    pregunta: '¿El número de la derecha es costo o lo que deben?',
    opciones: ['El costo', 'Lo que me deben'],
  },
  {
    tema: 'Total de Giz. Pilates',
    pregunta: '¿Cuál es el total correcto de Giz. Pilates?',
    opciones: ['37.000', '28.800'],
  },
];

let pedidos: Pedido[] = [];

const persona = laPersonaDelTaller;
const botonera = () => screen.findByRole('group', { name: 'Respuestas rápidas' }, ESPERA);
const opcion = async (nombre: string | RegExp) =>
  within(await botonera()).getByRole('button', { name: nombre });

const espiarElTaller = (responder: (pedido: Pedido) => JsonBodyType) => {
  pedidos = [];
  server.use(
    http.post('/api/sami/taller', async ({ request }) => {
      const pedido = (await request.json()) as Pedido;
      pedidos.push(pedido);
      return HttpResponse.json(responder(pedido));
    }),
  );
};

const escribirAlTaller = async (texto: string) => {
  await persona().type(screen.getByRole('textbox', { name: /Contame un poco/ }), texto);
  await clickButton(persona(), /Enviar a Sami/);
};

/* dado */

/** El primer turno deja tres dudas; el siguiente (con las respuestas juntas) contesta sin más preguntas. */
export const dadoQueSamiPlanteoTresDudas = async () => {
  espiarElTaller((pedido) =>
    pedido.mensaje === 'tengo una dietética'
      ? { respuesta: 'Veo tu sección.', acciones: [], preguntas: DUDAS }
      : { respuesta: 'Listo, ya lo ajusté.', acciones: [], preguntas: [] },
  );
  await dadoQueSeAbreElTaller();
  await escribirAlTaller('tengo una dietética');
};

export const dadoQueSamiPlanteoUnaSolaDuda = async () => {
  espiarElTaller((pedido) =>
    pedido.mensaje === 'Sí'
      ? { respuesta: 'Perfecto.', acciones: [], preguntas: [] }
      : {
          respuesta: 'Hola.',
          acciones: [],
          preguntas: [{ pregunta: '¿Sumamos gastos?', opciones: ['Sí', 'No'] }],
        },
  );
  await dadoQueSeAbreElTaller();
  await escribirAlTaller('hola sami');
};

export const dadoQueSeRespondieronLasDosPrimerasDudas = async () => {
  await persona().click(await opcion(/^Ya entregado y cobrado/));
  await persona().click(await opcion(/^Lo que me deben/));
};

/* cuando */

export const cuandoSeRespondenLasDosPrimerasDudas = dadoQueSeRespondieronLasDosPrimerasDudas;

export const cuandoSeRespondeLaDuda = async (respuesta: RegExp) => {
  await persona().click(await opcion(respuesta));
};

export const cuandoSeRespondenLasTresDudas = async () => {
  await dadoQueSeRespondieronLasDosPrimerasDudas();
  await persona().click(await opcion(/^28\.800/));
};

export const cuandoSeRespondenLasTresDudasConLasPrimerasOpciones = async () => {
  for (const nombre of [/^Ya entregado/, /^El costo/, /^37\.000/]) {
    await persona().click(await opcion(nombre));
  }
  await screen.findByText('Listo, ya lo ajusté.', {}, ESPERA);
};

export const cuandoSeRespondeSaltandoUnaDudaYEscribiendoOtra = async () => {
  await persona().click(await opcion('Saltear esta duda'));
  await persona().click(await opcion(/Escribir otra respuesta/));
  await persona().type(
    screen.getByRole('textbox', { name: 'Tu respuesta a esta duda' }),
    'Es lo que ya pagaron',
  );
  await persona().click(screen.getByRole('button', { name: 'Responder' }));
  await persona().click(await opcion(/^28\.800/));
};

export const cuandoSeSaltanLasTresDudas = async () => {
  for (let n = 0; n < 3; n++) {
    await persona().click(await opcion('Saltear esta duda'));
  }
};

/* entonces */

export const entoncesMuestraLaPrimeraDudaConSuProgreso = async () => {
  expect(within(await botonera()).getByText('Duda 1 de 3')).toBeVisible();
  expect(screen.getByText('¿Qué significan los tildes?')).toBeVisible();
  expect(screen.queryByText('¿Cuál es el total correcto de Giz. Pilates?')).not.toBeInTheDocument();
};

export const entoncesMuestraLaTerceraDudaSinMandarNada = async () => {
  expect(within(await botonera()).getByText('Duda 3 de 3')).toBeVisible();
  expect(pedidos).toHaveLength(1);
};

export const entoncesMandaTodasLasRespuestasJuntasEnUnSoloMensaje = async () => {
  expect(await screen.findByText('Listo, ya lo ajusté.', {}, ESPERA)).toBeVisible();
  expect(pedidos).toHaveLength(2);
  expect(pedidos[1]?.mensaje).toBe(
    'Tildes: Ya entregado y cobrado. Número de la derecha: Lo que me deben. Total de Giz. Pilates: 28.800.',
  );
  expect(screen.queryByRole('group', { name: 'Respuestas rápidas' })).not.toBeInTheDocument();
};

export const entoncesLaHistoriaIncluyeLasDudasQueSamiPlanteo = () => {
  const historial = (pedidos[1]?.historial ?? []) as { quien: string; texto: string }[];
  expect(historial.at(-1)).toEqual({
    quien: 'sami',
    texto:
      '¿Qué significan los tildes? ¿El número de la derecha es costo o lo que deben? ¿Cuál es el total correcto de Giz. Pilates?',
  });
};

export const entoncesLoSalteadoNoViaja = async () => {
  await screen.findByText('Listo, ya lo ajusté.', {}, ESPERA);
  expect(pedidos[1]?.mensaje).toBe(
    'Número de la derecha: Es lo que ya pagaron. Total de Giz. Pilates: 28.800.',
  );
};

export const entoncesNoSeMandaNadaYLaBotoneraDesaparece = () => {
  expect(screen.queryByRole('group', { name: 'Respuestas rápidas' })).not.toBeInTheDocument();
  expect(pedidos).toHaveLength(1);
  expect(useSistemaStore.getState().taller?.dudas).toEqual([]);
};

export const entoncesLaBotoneraEsLaDeSiempreSinProgresoNiSaltear = async () => {
  const grupo = await botonera();
  expect(within(grupo).queryByText(/Duda 1 de/)).not.toBeInTheDocument();
  expect(within(grupo).queryByRole('button', { name: 'Saltear esta duda' })).not.toBeInTheDocument();
};

export const entoncesLaRespuestaVaTalCual = async () => {
  expect(await screen.findByText('Perfecto.', {}, ESPERA)).toBeVisible();
  expect(pedidos.map((pedido) => pedido.mensaje)).toEqual(['hola sami', 'Sí']);
};
