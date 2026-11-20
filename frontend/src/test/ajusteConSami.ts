import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { expect } from 'vitest';
import { axe } from 'vitest-axe';
import { allBlocks } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { renderApp } from '@/test/renderApp';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';
import {
  cuandoSePideAjustarConSami,
  dadoQueSeAbreUnAlmacenDeAna,
  elEmailDeAna,
  elRouterDelTaller,
  elSistemaQueSeAjusta,
  laPersonaDelTaller,
} from '@/test/taller';
import {
  ESPERA,
  clickButton,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
  verifyToastAsync,
} from '@/test/testHelpers';

type PedidoAlTaller = {
  mensaje: string;
  estado: {
    nombre: string;
    piezas: { id: string; tipo: string; campos: { etiqueta: string }[] }[];
  };
};

let pedidos: PedidoAlTaller[] = [];
let filasAntes: number[] = [];

const persona = laPersonaDelTaller;

const enElStore = (id: string) => {
  const sistema = useSistemaStore.getState().sistemas.find((item) => item.id === id);
  if (!sistema) throw new Error('Falta el sistema en el store');
  return sistema;
};

const deTipo = (sistema: Sistema | undefined, tipo: string) => {
  const bloque = allBlocks(sistema).find((item) => item.type === tipo);
  if (!bloque) throw new Error(`Falta la pieza ${tipo}`);
  return bloque;
};

const etiquetas = (sistema: Sistema | undefined, tipo: string) =>
  deTipo(sistema, tipo).fields.map((campo) => campo.label);

const borradorDelTaller = () => useSistemaStore.getState().taller?.borrador ?? undefined;

const sinViolaciones = async () => {
  const resultado = await axe(document.body, {
    rules: {
      'color-contrast': { enabled: false },
      region: { enabled: false },
      'heading-order': { enabled: false },
    },
  });
  expect(resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html}`)).toEqual([]);
};

const pedirASami = async (texto: string) => {
  await persona().type(screen.getByRole('textbox', { name: /¿Qué le falta/ }), texto);
  await clickButton(persona(), /Ajustar con Sami/);
};

/* dado */

export const dadoUnAlmacenDeAnaConEjemplosAbierto = () =>
  dadoQueSeAbreUnAlmacenDeAna(undefined, { conEjemplos: true });

export const dadoQueSeVieronLasFilasDelSistema = () => {
  filasAntes = allBlocks(elSistemaQueSeAjusta()).map((bloque) => bloque.rows.length);
};

/** Sami responde que Cliente pase a llamarse Clienta y vaya primero en las ventas. */
export const dadoQueSamiRenombraClienteYLoPonePrimero = () => {
  pedidos = [];
  server.use(
    http.post('/api/sami/taller', async ({ request }) => {
      const pedido = (await request.json()) as PedidoAlTaller;
      pedidos.push(pedido);
      const ventas = pedido.estado.piezas.find((pieza) => pieza.tipo === 'sales');
      return HttpResponse.json({
        respuesta: 'Listo, ahora es Clienta y va primero en las ventas.',
        acciones: [
          {
            herramienta: 'editar_campo',
            argumentos: { pieza: ventas?.id, campo: 'Cliente', etiqueta: 'Clienta', posicion: 1 },
          },
        ],
        opciones: null,
      });
    }),
  );
};

export const dadoQueSeAjustaConSamiUnAlmacenDeAnaConEjemplos = async () => {
  await dadoUnAlmacenDeAnaConEjemplosAbierto();
  await cuandoSePideAjustarConSami();
};

export const dadoQueSePidioRenombrarClienteYPonerloPrimero = async () => {
  await pedirASami('cambiale el nombre a Cliente por Clienta y ponelo primero');
  await screen.findByText(/ahora es Clienta/, {}, ESPERA);
};

export const dadoQueSePidioPonerClientePrimero = async () => {
  await pedirASami('poné Cliente primero');
  await screen.findByText(/puse «Cliente» primero en ventas/, {}, ESPERA);
};

/* cuando */

export const cuandoSePideAjustarElAlmacenConSami = cuandoSePideAjustarConSami;

export const cuandoSePideRenombrarClienteYPonerloPrimero = () =>
  pedirASami('cambiale el nombre a Cliente por Clienta y ponelo primero');

export const cuandoSePidePonerClientePrimero = () => pedirASami('poné Cliente primero');

export const cuandoSePideCambiarTelefonoPorCelular = () =>
  pedirASami('cambiale el nombre a Teléfono por Celular');

export const cuandoSeGuardanLosCambios = () => clickButton(persona(), /Guardar cambios/);

export const cuandoSeVuelveSinGuardar = () => clickButton(persona(), /Volver a Almacén de Ana/);

export const cuandoSeVeLaTablaDeClientesAPantallaCompleta = () =>
  clickButton(
    persona(),
    `Ver ${deTipo(borradorDelTaller(), 'clients').name} a pantalla completa`,
  );

export const cuandoSeVuelveALaPropuesta = () => clickButton(persona(), /Volver a la propuesta/);

export const cuandoSeAbreElTallerDeUnSistemaQueNoExiste = async () => {
  setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  renderApp('/taller?sistema=no-existe');
  await verifyHeadingPresentAsync('Construyamos juntos');
};

/* entonces */

export const entoncesElTallerSeAbreConElSistemaParaAjustarlo = async () => {
  const sistema = elSistemaQueSeAjusta();
  expect(elRouterDelTaller().state.location.search).toEqual({ sistema: sistema.id });
  expect(screen.getByText(/Estoy mirando «Almacén de Ana»/)).toBeVisible();
  expect(screen.getByLabelText('Nombre de tu sección')).toHaveValue('Almacén de Ana');
  expect(screen.getByRole('button', { name: /Guardar cambios/ })).toBeVisible();
  expect(screen.queryByRole('button', { name: /Sí, crear mi sección/ })).toBeNull();
  expect(screen.queryByText(/TU PRIMERA SECCIÓN/)).toBeNull();
  await sinViolaciones();
};

export const entoncesSamiAplicaLoQuePidioSinTocarElSistemaGuardado = async () => {
  expect(await screen.findByText(/ahora es Clienta/, {}, ESPERA)).toBeVisible();
  expect(pedidos[0]?.estado.piezas.map((pieza) => pieza.tipo)).toEqual(['inventory', 'sales', 'clients']);
  expect(etiquetas(enElStore(elSistemaQueSeAjusta().id), 'sales')[0]).toBe('Producto');
  expect(screen.queryByText(/sin guardar/i)).toBeNull();
};

export const entoncesSeVuelveAlSistemaConLosCambiosGuardados = async () => {
  const sistema = elSistemaQueSeAjusta();
  await verifyPathAsync(elRouterDelTaller(), `/sistemas/${sistema.id}`);
  await screen.findByRole('heading', { level: 1, name: /Almacén de Ana/ }, ESPERA);
  await verifyToastAsync('Guardé los cambios en «Almacén de Ana».');
  const guardado = enElStore(sistema.id);
  expect(etiquetas(guardado, 'sales')).toEqual([
    'Clienta',
    'Producto',
    'Cantidad',
    'Precio',
    'Fecha',
    'Fiado',
  ]);
  expect(deTipo(guardado, 'sales').fields[0]).toMatchObject({ key: 'customer', type: 'relation' });
  expect(allBlocks(guardado).map((bloque) => bloque.rows.length)).toEqual(filasAntes);
};

export const entoncesLosCambiosViajanAlServidor = async () => {
  await waitFor(() => {
    const enElServidor = estado.sistemas.get(elEmailDeAna())?.[0];
    expect(etiquetas(enElServidor, 'sales')[0]).toBe('Clienta');
  }, ESPERA);
};

export const entoncesSamiPusoClientePrimeroEnLaPropuesta = () => {
  expect(etiquetas(borradorDelTaller(), 'sales')[0]).toBe('Cliente');
};

export const entoncesElSistemaGuardadoNoCambio = async () => {
  const sistema = elSistemaQueSeAjusta();
  await verifyPathAsync(elRouterDelTaller(), `/sistemas/${sistema.id}`);
  expect(etiquetas(enElStore(sistema.id), 'sales')[0]).toBe('Producto');
  expect(useSistemaStore.getState().pendientes).not.toContain(sistema.id);
};

export const entoncesTelefonoPasaALlamarseCelular = async () => {
  expect(await screen.findByText(/«Teléfono» ahora se llama «Celular»/, {}, ESPERA)).toBeVisible();
  expect(etiquetas(borradorDelTaller(), 'clients')).toContain('Celular');
};

export const entoncesSeVeLaTablaDeClientesAPantallaCompleta = async () => {
  const nombre = deTipo(borradorDelTaller(), 'clients').name;
  const tabla = await screen.findByRole('dialog', { name: nombre }, ESPERA);
  expect(tabla).toBeVisible();
  expect(within(tabla).getByLabelText(`Buscar en ${nombre}`)).toBeVisible();
};

export const entoncesSeVuelveALaPropuestaSinTablaCompleta = () => {
  const nombre = deTipo(borradorDelTaller(), 'clients').name;
  expect(screen.queryByRole('dialog', { name: nombre })).toBeNull();
  expect(useSistemaStore.getState().tablaCompletaId).toBeNull();
};

export const entoncesSeArmaUnSistemaNuevo = () => {
  expect(useSistemaStore.getState().taller).toMatchObject({ sistemaId: null, borrador: null });
  expect(screen.queryByRole('button', { name: /Guardar cambios/ })).toBeNull();
};
