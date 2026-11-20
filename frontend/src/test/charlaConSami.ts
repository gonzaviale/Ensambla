import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse, type JsonBodyType } from 'msw';
import { expect } from 'vitest';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';
import {
  cantidadDePiezasDeLaPropuesta,
  dadoQueSamiArmoUnaPropuestaDeAlmacen,
  dadoQueSeAbreElTaller,
  laPersonaDelTaller,
} from '@/test/taller';
import { ESPERA, clickButton, setupServerError, verifyToastAsync } from '@/test/testHelpers';

type Pedido = Record<string, unknown>;

type PedidoAlTaller = {
  mensaje: string;
  estado: { nombre: string; piezas: { tipo: string; nombre: string; campos: unknown[] }[] };
  historial: { quien: string; texto: string }[];
};

const MENSAJE_VAGO = 'no sé por dónde empezar';
const ERROR_DE_CUOTA = 'Hoy ya usaste todas las consultas con IA.';

let pedidos: Pedido[] = [];

const persona = laPersonaDelTaller;
const campo = () => screen.getByRole('textbox', { name: /Contame un poco|¿Qué le falta/ });
const botonera = () => screen.findByRole('group', { name: 'Respuestas rápidas' }, ESPERA);
const borrador = () => useSistemaStore.getState().taller?.borrador ?? null;
const piezasDelBorrador = () => (borrador()?.sections ?? []).flatMap((seccion) => seccion.blocks);
const pieza = (nombre: string) => {
  const encontrada = piezasDelBorrador().find((bloque) => bloque.name === nombre);
  if (!encontrada) throw new Error(`Falta la pieza ${nombre}`);
  return encontrada;
};
const etiquetasDelInventario = () =>
  piezasDelBorrador()
    .filter((bloque) => bloque.type === 'inventory')
    .flatMap((bloque) => bloque.fields.map((dato) => dato.label));
const pedidoNumero = (indice: number) => pedidos[indice] as PedidoAlTaller;

/** Atiende POST /api/sami/taller con `responder` y guarda los pedidos que llegaron. */
const samiResponde = (responder: (pedido: Pedido) => JsonBodyType) => {
  pedidos = [];
  server.use(
    http.post('/api/sami/taller', async ({ request }) => {
      const pedido = (await request.json()) as Pedido;
      pedidos.push(pedido);
      return HttpResponse.json(responder(pedido));
    }),
  );
};

const escribir = async (texto: string) => {
  await persona().type(campo(), texto);
  await clickButton(persona(), /Enviar a Sami|Ajustar con Sami/);
};

const samiPregunta = (pregunta: string, opciones: string[], respuesta: string, final: string) =>
  samiResponde((pedido) =>
    pedido.mensaje === opciones[0] || pedido.mensaje === opciones[1]
      ? { respuesta: final, acciones: [], opciones: null }
      : { respuesta, acciones: [], preguntas: [{ pregunta, opciones }] },
  );

/* dado */

export const dadoQueSeAbreElTallerParaConversar = dadoQueSeAbreElTaller;

export const dadoQueSamiArmoLaPropuestaDeUnAlmacen = dadoQueSamiArmoUnaPropuestaDeAlmacen;

export const dadoQueSamiArmaClientasYEncargosConectados = () =>
  samiResponde(() => ({
    respuesta: 'Te armé las ventas y los clientes.',
    acciones: [
      { herramienta: 'agregar_pieza', argumentos: { tipo: 'clients', nombre: 'Clientas' } },
      {
        herramienta: 'agregar_pieza',
        argumentos: {
          tipo: 'form',
          nombre: 'Encargos',
          campos: [{ etiqueta: 'Clienta', tipo: 'text' }],
        },
      },
      {
        herramienta: 'conectar',
        argumentos: { desde: 'Encargos', hacia: 'Clientas', etiqueta: 'Clienta' },
      },
    ],
    opciones: null,
  }));

export const dadoQueSamiArmaVentasYClientesYPreguntaSiLlevaStock = () =>
  samiResponde(() => ({
    respuesta: 'Empecemos por tus ventas y tus clientes.',
    acciones: [
      { herramienta: 'agregar_pieza', argumentos: { tipo: 'sales' } },
      { herramienta: 'agregar_pieza', argumentos: { tipo: 'clients' } },
    ],
    preguntas: [{ pregunta: '¿Llevás stock?', opciones: ['Sí', 'No'] }],
  }));

export const dadoQueSamiSumaTurnosSinDecirNada = () =>
  samiResponde(() => ({
    respuesta: '',
    acciones: [{ herramienta: 'agregar_pieza', argumentos: { tipo: 'form', nombre: 'Turnos' } }],
    opciones: null,
  }));

export const dadoQueSamiNoPuedeArmarNadaNiDecirNada = () =>
  samiResponde(() => ({
    respuesta: '',
    acciones: [{ herramienta: 'agregar_pieza', argumentos: { tipo: 'turnos' } }],
    opciones: null,
  }));

export const dadoQueSamiPreguntaSiSumaGastos = () =>
  samiPregunta('¿Sumamos gastos?', ['Sí', 'No'], '¿Sumamos gastos?', 'Perfecto.');

export const dadoQueSamiPreguntaSiVendeProductosOSoloServicios = () =>
  samiPregunta(
    '¿Vendés productos o sólo servicios?',
    ['Productos', 'Sólo servicios'],
    'Te armé turnos y servicios.',
    'Listo.',
  );

export const dadoQueSamiSoloPiensa = () =>
  samiResponde(() => ({ respuesta: 'Lo pienso.', acciones: [], opciones: null }));

export const dadoQueSamiSumaLosGastos = () =>
  samiResponde(() => ({
    respuesta: 'Listo, sumé los gastos.',
    acciones: [{ herramienta: 'agregar_pieza', argumentos: { tipo: 'expenses' } }],
    opciones: null,
  }));

export const dadoQueSamiLlamaAlSistemaVelasDeAna = () =>
  samiResponde(() => ({
    respuesta: 'Listo, lo llamé Velas de Ana.',
    acciones: [{ herramienta: 'renombrar_sistema', argumentos: { nombre: 'Velas de Ana' } }],
    opciones: null,
  }));

export const dadoQueElServidorRechazaElPedido = () =>
  setupServerError('post', '/sami/taller', 400, ERROR_DE_CUOTA);

export const dadoQueSeEscribioUnMensajeVago = () => escribir(MENSAJE_VAGO);

export const dadoQueSeEscribioQueTieneUnaPeluqueria = () => escribir('tengo una peluquería');

export const dadoQueSeAplicoUnPedidoDeSami = async () => {
  await escribir('agregá un proveedor al inventario');
  await screen.findByText('Listo, sumé el proveedor al inventario.', {}, ESPERA);
  await waitFor(() => expect(etiquetasDelInventario()).toContain('Proveedor'), ESPERA);
};

/* cuando */

export const cuandoSeAbreElTallerParaConversar = dadoQueSeAbreElTaller;

export const cuandoSeEscribe = escribir;

export const cuandoSeEligeLaRespuestaRapida = async (nombre: RegExp) => {
  await persona().click(within(await botonera()).getByRole('button', { name: nombre }));
};

export const cuandoSeEligeElEjemplo = (nombre: RegExp) => clickButton(persona(), nombre);

export const cuandoSeEscribeUnMensajeVago = dadoQueSeEscribioUnMensajeVago;

export const cuandoSeDeshaceLoUltimo = () => clickButton(persona(), /Deshacer lo último/);

/* entonces */

export const entoncesElPrimerMensajeVaConUnSistemaVacioYLoQueEnsamblaSeVe = async () => {
  expect(await screen.findByText('Te armé las ventas y los clientes.', {}, ESPERA)).toBeVisible();
  expect(screen.getByLabelText('Nombre de tu sección')).toHaveValue('Mi sección');
  expect(cantidadDePiezasDeLaPropuesta()).toBe(2);
  expect(pedidoNumero(0).mensaje).toBe('Tengo un almacén y vendo productos');
  expect(pedidoNumero(0).estado).toMatchObject({ nombre: 'Mi sección', piezas: [] });
  expect(pedidoNumero(0).historial.map((mensaje) => mensaje.quien)).toEqual(['sami']);
  expect(pieza('Encargos').fields.find((dato) => dato.label === 'Clienta')).toMatchObject({
    type: 'relation',
    target: pieza('Clientas').id,
  });
  expect(screen.queryByRole('button', { name: /Deshacer lo último/ })).toBeNull();
};

export const entoncesSamiPreguntaElRubroConUnaBotoneraQueDejaEscribirOtraCosa = async () => {
  const opciones = within(await botonera());
  expect(opciones.getByText('¿De qué es tu negocio?')).toBeVisible();
  expect(opciones.getByRole('button', { name: /Hago velas artesanales/ })).toBeVisible();
  expect(opciones.getByRole('button', { name: /Escribir otra cosa/ })).toBeVisible();
  expect(screen.queryByLabelText('Nombre de tu sección')).toBeNull();
  expect(borrador()).toBeNull();
};

export const entoncesSamiEnsamblaLasPiezasDeLasVelas = async () => {
  expect(await screen.findByLabelText('Nombre de tu sección', {}, ESPERA)).toHaveValue('Mis velas');
  expect(screen.getByText(/Armé tus velas/)).toBeVisible();
  expect(piezasDelBorrador().map((bloque) => bloque.name)).toEqual(['Velas', 'Clientas', 'Pedidos']);
  expect(within(await botonera()).getByText(/gastás en cera, mechas y esencias/)).toBeVisible();
};

export const entoncesLosPedidosQuedanConectadosConLasVelasYSusClientas = async () => {
  await screen.findByLabelText('Nombre de tu sección', {}, ESPERA);
  const velas = pieza('Velas');
  expect(velas.fields.map((dato) => dato.label)).toContain('Aroma');
  expect(velas.rows.map((fila) => fila.name)).toContain('Vela de soja lavanda');
  const pedidosDeVelas = pieza('Pedidos');
  expect(pedidosDeVelas.fields.find((dato) => dato.label === 'Vela')).toMatchObject({
    type: 'relation',
    target: velas.id,
  });
  const lavanda = velas.rows.find((fila) => fila.name === 'Vela de soja lavanda');
  const carla = pieza('Clientas').rows.find((fila) => fila.name === 'Carla Gómez');
  expect(pedidosDeVelas.rows[0]).toMatchObject({ vela: lavanda?.id, customer: carla?.id });
};

export const entoncesSamiSumaUnaTablaDeTurnosAMedida = async () => {
  await screen.findByLabelText('Nombre de tu sección', {}, ESPERA);
  const turnos = pieza('Turnos');
  expect(turnos.type).toBe('form');
  expect(turnos.fields.map((dato) => dato.label)).toEqual([
    'Nombre',
    'Detalle',
    'Fecha',
    'Cliente',
    'Servicio',
    'Hora',
  ]);
  const sofia = pieza('Clientes').rows.find((fila) => fila.name === 'Sofía Ruiz');
  expect(turnos.rows[0]).toMatchObject({ cliente: sofia?.id, hora: '10:30' });
  expect(cantidadDePiezasDeLaPropuesta()).toBe(3);
};

export const entoncesSamiArmaElSistemaDesdeCeroYDejaUnaPregunta = async () => {
  expect(await screen.findByText('Empecemos por tus ventas y tus clientes.', {}, ESPERA)).toBeVisible();
  expect(await screen.findByLabelText('Nombre de tu sección', {}, ESPERA)).toBeVisible();
  expect(cantidadDePiezasDeLaPropuesta()).toBe(2);
  expect(within(await botonera()).getByText('¿Llevás stock?')).toBeVisible();
};

export const entoncesLaCharlaCuentaLoQueSamiHizo = async () => {
  expect(await screen.findByText('Sumé turnos.', {}, ESPERA)).toBeVisible();
  expect(screen.getByText('Ya podés verlo a la derecha.')).toBeVisible();
  expect(screen.getByText('¿Qué cambiarías?')).toBeVisible();
};

export const entoncesSamiCuentaQueNoPudoYSigueSinPropuesta = async () => {
  expect(await screen.findByText(/No pude hacer ese cambio/, {}, ESPERA)).toBeVisible();
  expect(borrador()).toBeNull();
};

export const entoncesLaBotoneraDesapareceYSeEnfocaElCampo = () => {
  expect(screen.queryByRole('group', { name: 'Respuestas rápidas' })).toBeNull();
  expect(campo()).toHaveFocus();
};

export const entoncesLaRespuestaCortaSeMandaTalCual = async () => {
  expect(await screen.findByText('Perfecto.', {}, ESPERA)).toBeVisible();
  expect(pedidos.map((pedido) => pedido.mensaje)).toEqual([MENSAJE_VAGO, 'Sí']);
};

export const entoncesLaPreguntaDeLaBotoneraViajaEnLaCharla = async () => {
  expect(await screen.findByText('Listo.', {}, ESPERA)).toBeVisible();
  expect(pedidoNumero(1).historial.slice(-2)).toEqual([
    { quien: 'sami', texto: 'Te armé turnos y servicios.' },
    { quien: 'sami', texto: '¿Vendés productos o sólo servicios?' },
  ]);
};

export const entoncesElAjusteVaConElSistemaResumidoYLaCharlaReciente = async () => {
  expect(await screen.findByText('Lo pienso.', {}, ESPERA)).toBeVisible();
  const pedido = pedidoNumero(0);
  expect(pedido.mensaje).toBe('¿por qué me pusiste esas piezas?');
  expect(pedido.estado.nombre).toBe('Mi almacén');
  expect(pedido.estado.piezas.map((item) => item.tipo)).toEqual(['inventory', 'sales', 'clients']);
  expect(JSON.stringify(pedido.estado)).not.toContain('Yerba');
  expect(pedido.historial.length).toBeLessThanOrEqual(6);
  expect(pedido.historial.at(-1)?.quien).toBe('sami');
};

export const entoncesSamiSumaLosGastosATravesDeLaOrden = async () => {
  expect(await screen.findByText('Listo, sumé los gastos.', {}, ESPERA)).toBeVisible();
  expect(pedidos.map((pedido) => pedido.mensaje)).toEqual(['sumá gastos']);
  expect(cantidadDePiezasDeLaPropuesta()).toBe(4);
};

export const entoncesSamiSacaLosClientes = async () => {
  expect(await screen.findByText('Listo, saqué clientes.', {}, ESPERA)).toBeVisible();
  expect(cantidadDePiezasDeLaPropuesta()).toBe(2);
};

export const entoncesSamiRenombraElSistema = async () => {
  expect(await screen.findByText('Listo, ahora se llama Almacén Don Luis.', {}, ESPERA)).toBeVisible();
  expect(screen.getByLabelText('Nombre de tu sección')).toHaveValue('Almacén Don Luis');
};

export const entoncesSamiSumaElProveedorAlInventario = async () => {
  expect(await screen.findByText('Listo, sumé el proveedor al inventario.', {}, ESPERA)).toBeVisible();
  await waitFor(() => expect(etiquetasDelInventario()).toContain('Proveedor'), ESPERA);
};

export const entoncesElInventarioNoTieneProveedor = () => {
  expect(etiquetasDelInventario()).not.toContain('Proveedor');
};

export const entoncesSamiConfirmaQueVolvioAtrasYYaNoOfreceDeshacer = () => {
  expect(screen.getByText(/Listo, volví atrás/)).toBeVisible();
  expect(screen.queryByRole('button', { name: /Deshacer lo último/ })).toBeNull();
};

export const entoncesLaPropuestaNoCambiaNiOfreceDeshacer = async () => {
  expect(await screen.findByText(/Cuando quieras seguimos/, {}, ESPERA)).toBeVisible();
  expect(cantidadDePiezasDeLaPropuesta()).toBe(3);
  expect(screen.queryByRole('button', { name: /Deshacer lo último/ })).toBeNull();
};

export const entoncesSeAvisaConElMensajeDelServidorYSeSigueEscribiendo = async () => {
  await verifyToastAsync(/ya usaste todas las consultas con IA/);
  expect(campo()).toBeEnabled();
  expect(useSistemaStore.getState().taller?.pensando).toBe(false);
};

export const entoncesSeAvisaYSigueSinPropuesta = async () => {
  await verifyToastAsync(/ya usaste todas las consultas con IA/);
  expect(borrador()).toBeNull();
  expect(screen.getByText('O probá con un ejemplo:')).toBeVisible();
};

export const entoncesElNombreQueSamiPusoQuedaEnElCampo = async () => {
  expect(await screen.findByText('Listo, lo llamé Velas de Ana.', {}, ESPERA)).toBeVisible();
  expect(screen.getByLabelText('Nombre de tu sección')).toHaveValue('Velas de Ana');
};

export const entoncesSigueTePidiendoMasDetalle = () => verifyToastAsync(/Necesito más detalles/);
