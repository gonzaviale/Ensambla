import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { expect } from 'vitest';
import { axe } from 'vitest-axe';
import { allBlocks } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';
import {
  campoDeLaConversacion,
  dadoQueSamiArmoUnaPropuestaDeAlmacen,
  dadoQueSeAbreElTaller,
  dadoQueSeAjustaConSamiUnAlmacenDeAna,
  elRouterDelTaller,
  elSistemaQueSeAjusta,
  laPersonaDelTaller,
  selectorDeFoto,
} from '@/test/taller';
import { ESPERA, clickButton, verifyPathAsync, verifyToastAsync } from '@/test/testHelpers';

type PedidoAlTaller = {
  mensaje: string;
  historial: Record<string, unknown>[];
  imagen?: { tipo: string; datos: string };
};

type PedidoConEstado = { estado: { piezas: { tipo: string; nombres?: string[] }[] } };

const MINIATURA = 'La foto que vas a mandar a Sami';
const FOTO_EN_GRANDE = 'La foto en grande';
const REGISTROS_NUEVOS = 'Registros que no encontré';
const PIXELES_EN_BASE64 = `data:image/jpeg;base64,${btoa('pixeles')}`;

let pedidos: PedidoAlTaller[] = [];
let pedidosConEstado: PedidoConEstado[] = [];
let escuchaDePedidos: ((evento: { request: Request }) => Promise<void>) | null = null;

const persona = laPersonaDelTaller;
const miniatura = () => screen.queryByRole('img', { name: MINIATURA });
const botonParaVerLaFotoEnGrande = () => screen.getByRole('button', { name: 'Ver la foto en grande' });
const dialogoDeLaFoto = () => screen.getByRole('dialog', { name: FOTO_EN_GRANDE });
const tarjetaDeRegistrosNuevos = () => screen.getByRole('group', { name: REGISTROS_NUEVOS });
const borradorDelTaller = () => useSistemaStore.getState().taller?.borrador ?? undefined;

const foto = (contenido: BlobPart = 'pixeles', tipo = 'image/jpeg') =>
  new File([contenido], 'sistema.jpg', { type: tipo });

const deTipo = (sistema: Sistema | undefined, tipo: string) => {
  const bloque = allBlocks(sistema).find((item) => item.type === tipo);
  if (!bloque) throw new Error(`Falta la pieza ${tipo}`);
  return bloque;
};

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

/** Las dudas de la foto van primero, de a una; la tarjeta de los vínculos nuevos aparece cuando no queda ninguna. */
const saltearLasDudas = async () => {
  while (screen.queryByRole('button', { name: 'Saltear esta duda' })) {
    await persona().click(screen.getByRole('button', { name: 'Saltear esta duda' }));
  }
};

const adjuntar = (archivo: File = foto()) => persona().upload(selectorDeFoto(), archivo);

const esperarLaMiniatura = () => screen.findByRole('img', { name: MINIATURA });

const ajustarConLaFoto = async () => {
  await adjuntar();
  await esperarLaMiniatura();
  await clickButton(persona(), /Ajustalo conmigo/);
  await screen.findByText(/Miré tu foto/, {}, ESPERA);
};

const ventasDe = (sistema: Sistema | undefined) => deTipo(sistema, 'sales').rows;

/* dado */

export const dadoQueSeAbreElTallerParaMandarFotos = dadoQueSeAbreElTaller;

export const dadoQueElTallerDeSamiSeEspia = () => {
  pedidos = [];
  server.use(
    http.post('/api/sami/taller', async ({ request }) => {
      pedidos.push((await request.json()) as PedidoAlTaller);
      return HttpResponse.json({ respuesta: 'Ya la miré.', acciones: [], opciones: null });
    }),
  );
};

export const dadoQueSeEspianLosPedidosQueRecibeSami = () => {
  pedidosConEstado = [];
  escuchaDePedidos = async ({ request }) => {
    if (request.url.endsWith('/api/sami/taller')) pedidosConEstado.push(await request.clone().json());
  };
  server.events.on('request:start', escuchaDePedidos);
};

export const dejarDeEspiarLosPedidosQueRecibeSami = () => {
  if (escuchaDePedidos) server.events.removeListener('request:start', escuchaDePedidos);
  escuchaDePedidos = null;
};

export const dadoQueSeAdjuntoLaFoto = async () => {
  await adjuntar();
  await esperarLaMiniatura();
};

export const dadoQueSeAbrioLaFotoEnGrande = async () => {
  await dadoQueSeAdjuntoLaFoto();
  await persona().click(botonParaVerLaFotoEnGrande());
};

export const dadoQueSeMandoLaFoto = async () => {
  await dadoQueSeAdjuntoLaFoto();
  await clickButton(persona(), /Pensalo conmigo/);
  await screen.findByText('Ya la miré.', {}, ESPERA);
};

export const dadoQueSamiLeyoLaFotoDeLaPropuesta = async () => {
  await dadoQueSamiArmoUnaPropuestaDeAlmacen();
  await ajustarConLaFoto();
};

export const dadoQueSamiLeyoLaFotoYSeSalteoronLasDudas = async () => {
  await dadoQueSamiLeyoLaFotoDeLaPropuesta();
  await saltearLasDudas();
};

export const dadoQueSeSumaronLosRegistrosLeidos = async () => {
  await dadoQueSamiLeyoLaFotoYSeSalteoronLasDudas();
  await persona().click(within(tarjetaDeRegistrosNuevos()).getByRole('button', { name: /Sumarlos/ }));
};

export const dadoQueSeAjustaUnAlmacenConUnaClientaYUnProductoYaCargados = () =>
  dadoQueSeAjustaConSamiUnAlmacenDeAna((sistema) => {
    deTipo(sistema, 'clients').rows.push({ id: 'e1111111-cli', name: 'Lucía' });
    deTipo(sistema, 'inventory').rows.push({ id: 'e2222222-pro', name: 'Vela lavanda', quantity: 1 });
  });

export const dadoQueSamiLeyoLaFotoDeUnAlmacenDeAna = async () => {
  await dadoQueSeAjustaConSamiUnAlmacenDeAna();
  await ajustarConLaFoto();
};

export const dadoQueSamiLeyoLaFotoDelAlmacenDeAna = async () => {
  await dadoQueSeAjustaUnAlmacenConUnaClientaYUnProductoYaCargados();
  dadoQueSeEspianLosPedidosQueRecibeSami();
  await ajustarConLaFoto();
  await saltearLasDudas();
};

/* cuando */

export const cuandoSeAdjuntaLaFotoConLaCamara = () => adjuntar();

export const cuandoSeAdjuntaUnaFotoPngConElTextoDeLasVentas = async () => {
  await adjuntar(foto('otra', 'image/png'));
  await esperarLaMiniatura();
  await persona().type(campoDeLaConversacion(), 'Mis ventas de ayer');
  await clickButton(persona(), /Pensalo conmigo/);
};

export const cuandoSeAdjuntaUnaFotoDeMasDe5MB = () => adjuntar(foto(new Uint8Array(6 * 1024 * 1024)));

export const cuandoSeQuitaLaFoto = () => clickButton(persona(), 'Quitar foto');

export const cuandoSeEnviaElMensajeSinTexto = () => clickButton(persona(), /Pensalo conmigo/);

export const cuandoSeEscribeGraciasYSeEnvia = async () => {
  await persona().type(screen.getByRole('textbox', { name: /Contame un poco/ }), 'Gracias');
  await clickButton(persona(), /Pensalo conmigo/);
};

export const cuandoSeAbreLaFotoEnGrande = () => persona().click(botonParaVerLaFotoEnGrande());

export const cuandoSeCierraLaFotoConLaX = () =>
  persona().click(within(dialogoDeLaFoto()).getByRole('button', { name: 'Cerrar' }));

export const cuandoSeCierraLaFotoConEscape = () => persona().keyboard('{Escape}');

export const cuandoSeCierraLaFotoTocandoAfuera = () =>
  persona().click(screen.getByRole('dialog').parentElement as HTMLElement);

export const cuandoSeAjustaConLaFotoYSeSaltanLasDudas = async () => {
  await ajustarConLaFoto();
  await saltearLasDudas();
};

export const cuandoSeSaltanLasDudas = saltearLasDudas;

export const cuandoSePideAjustarElPrecio = async () => {
  await persona().type(screen.getByRole('textbox', { name: /¿Qué le falta/ }), 'El precio era 9000');
  await clickButton(persona(), /Ajustalo conmigo/);
};

export const cuandoSeSumanLosRegistrosLeidos = () =>
  persona().click(within(tarjetaDeRegistrosNuevos()).getByRole('button', { name: /Sumarlos/ }));

export const cuandoSeDejanSinVincular = () => clickButton(persona(), /No, dejalos sin vincular/);

export const cuandoSeCreaLaSeccionConLoLeido = () => clickButton(persona(), /Sí, crear mi sección/);

export const cuandoSeDeshaceLoUltimo = () => clickButton(persona(), /Deshacer lo último/);

export const cuandoSeGuardanLosCambiosDelSistema = async () => {
  await saltearLasDudas();
  await clickButton(persona(), /Sumarlos/);
  await clickButton(persona(), /Guardar cambios/);
};

/* entonces */

export const entoncesLaFotoSeVeEnMiniaturaYSePuedeCambiar = async () => {
  expect(selectorDeFoto()).toHaveAttribute('accept', 'image/*');
  expect(await screen.findByRole('img', { name: MINIATURA })).toBeVisible();
  expect(screen.getByRole('button', { name: 'Cambiar la foto para Sami' })).toBeVisible();
  await sinViolaciones();
};

export const entoncesLaFotoYaNoEstaAdjunta = () => {
  expect(miniatura()).toBeNull();
  expect(useSistemaStore.getState().taller?.foto).toBeNull();
};

export const entoncesLaFotoViajaConElMensajeAutomatico = async () => {
  expect(await screen.findByText('Ya la miré.', {}, ESPERA)).toBeVisible();
  expect(pedidos).toHaveLength(1);
  expect(pedidos[0]?.mensaje).toBe('Te mando una foto.');
  expect(pedidos[0]?.imagen).toEqual({ tipo: 'image/jpeg', datos: btoa('pixeles') });
};

export const entoncesLaCharlaMuestraLaFotoYYaNoQuedaAdjunta = async () => {
  expect(screen.getByText('Te mando una foto.')).toBeVisible();
  expect(screen.getByRole('img', { name: 'La foto que le mandaste a Sami' })).toHaveAttribute(
    'src',
    PIXELES_EN_BASE64,
  );
  expect(miniatura()).toBeNull();
  await sinViolaciones();
};

export const entoncesElHistorialSoloLlevaElTextoDeLaFoto = async () => {
  await waitFor(() => expect(pedidos).toHaveLength(2), ESPERA);
  expect(pedidos[1]?.imagen).toBeUndefined();
  expect(pedidos[1]?.historial).toContainEqual({ quien: 'you', texto: 'Te mando una foto.' });
  expect(JSON.stringify(pedidos[1]?.historial)).not.toContain(btoa('pixeles'));
};

export const entoncesLaFotoSeAbreEnUnDialogoConElFocoEnCerrar = async () => {
  expect(within(dialogoDeLaFoto()).getByRole('img')).toHaveAttribute('src', PIXELES_EN_BASE64);
  expect(within(dialogoDeLaFoto()).getByRole('button', { name: 'Cerrar' })).toHaveFocus();
  await sinViolaciones();
};

export const entoncesSeCierraElDialogoYElFocoVuelveALaMiniatura = () => {
  expect(screen.queryByRole('dialog', { name: FOTO_EN_GRANDE })).toBeNull();
  expect(botonParaVerLaFotoEnGrande()).toHaveFocus();
};

export const entoncesSeCierraElDialogoDeLaFoto = () => {
  expect(screen.queryByRole('dialog', { name: FOTO_EN_GRANDE })).toBeNull();
};

export const entoncesLaFotoSigueAdjunta = () => {
  expect(miniatura()).toBeVisible();
};

export const entoncesElPedidoLlevaLaFotoPngYElTexto = async () => {
  await screen.findByText('Ya la miré.', {}, ESPERA);
  expect(pedidos[0]).toMatchObject({
    mensaje: 'Mis ventas de ayer',
    imagen: { tipo: 'image/png', datos: btoa('otra') },
  });
};

export const entoncesAvisaQueLaFotoPesaDemasiadoYNoLaAdjunta = async () => {
  await verifyToastAsync(/pesa más de 5 MB/);
  expect(miniatura()).toBeNull();
  expect(useSistemaStore.getState().taller?.foto).toBeNull();
  expect(pedidos).toHaveLength(0);
};

export const entoncesSamiSugiereLosRegistrosQueNoEncontro = async () => {
  expect(await screen.findByText(/Miré tu foto: anotaste tres ventas/, {}, ESPERA)).toBeVisible();
  expect(tarjetaDeRegistrosNuevos()).toHaveTextContent('Lucía, Marta');
  expect(tarjetaDeRegistrosNuevos()).toHaveTextContent('Vela lavanda, Vela vainilla, Vela flotante');
  await sinViolaciones();
};

export const entoncesSeAvisaQueElStockNoAlcanzo = async () => {
  expect(screen.queryByRole('group', { name: REGISTROS_NUEVOS })).toBeNull();
  await verifyToastAsync(
    'Ojo: no alcanzó el stock de Vela lavanda, Vela vainilla y Vela flotante; quedaron en 0.',
  );
};

export const entoncesElSistemaCreadoTieneLosRegistrosLeidos = async () => {
  await waitFor(() => expect(useSistemaStore.getState().sistemas).toHaveLength(1), ESPERA);
  const [sistema] = useSistemaStore.getState().sistemas;
  await verifyPathAsync(elRouterDelTaller(), `/sistemas/${sistema?.id}`);
  const filas = allBlocks(sistema).flatMap((bloque) => bloque.rows);
  expect(filas.some((fila) => fila.__example)).toBe(false);
  const productos = deTipo(sistema, 'inventory').rows;
  const clientes = deTipo(sistema, 'clients').rows;
  expect(productos.map((fila) => fila.name)).toEqual(['Vela lavanda', 'Vela vainilla', 'Vela flotante']);
  expect(clientes.map((fila) => fila.name)).toEqual(['Lucía', 'Marta']);
  expect(ventasDe(sistema).map((fila) => [fila.product, fila.customer, fila.quantity])).toEqual([
    [productos[0]?.id, clientes[0]?.id, 2],
    [productos[1]?.id, clientes[1]?.id, 1],
    [productos[2]?.id, clientes[0]?.id, 4],
  ]);
};

export const entoncesSamiCorrigeElPrecioSinVolverACargarlo = async () => {
  expect(await screen.findByText(/corregí el precio a 9000/, {}, ESPERA)).toBeVisible();
  expect(ventasDe(borradorDelTaller()).map((fila) => fila.price)).toEqual([9000, 8200, 6000]);
};

export const entoncesSamiRecibeLosNombresQueYaExisten = () => {
  const piezas = pedidosConEstado[0]?.estado.piezas ?? [];
  expect(piezas.find((pieza) => pieza.tipo === 'clients')?.nombres).toEqual(['Lucía']);
  expect(piezas.find((pieza) => pieza.tipo === 'inventory')?.nombres).toEqual(['Vela lavanda']);
};

export const entoncesSoloPreguntaPorLosRegistrosQueFaltan = () => {
  expect(tarjetaDeRegistrosNuevos()).toHaveTextContent('Marta');
  expect(tarjetaDeRegistrosNuevos()).toHaveTextContent('Vela vainilla, Vela flotante');
  expect(tarjetaDeRegistrosNuevos()).not.toHaveTextContent('Lucía');
  expect(tarjetaDeRegistrosNuevos()).not.toHaveTextContent('Vela lavanda');
};

export const entoncesLasVentasUsanLosRegistrosQueYaEstan = () => {
  expect(ventasDe(borradorDelTaller()).map((fila) => [fila.product, fila.customer])).toEqual([
    ['e2222222-pro', 'e1111111-cli'],
    ['', ''],
    ['', 'e1111111-cli'],
  ]);
};

export const entoncesLaVentaDescuentaElStockYSamiAvisa = () => {
  expect(deTipo(borradorDelTaller(), 'inventory').rows[0]?.quantity).toBe(0);
  expect(
    screen.getByText(/Ojo: no alcanzó el stock de Vela lavanda \(vendiste 2 y había 1\); quedó en 0\./),
  ).toBeVisible();
};

export const entoncesElStockVuelveAComoEstaba = () => {
  expect(deTipo(borradorDelTaller(), 'inventory').rows[0]?.quantity).toBe(1);
};

export const entoncesLasVentasQuedanSinVincularYNoSeCreaNada = () => {
  expect(screen.queryByRole('group', { name: REGISTROS_NUEVOS })).toBeNull();
  const propuesta = borradorDelTaller();
  expect(deTipo(propuesta, 'clients').rows.filter((fila) => !fila.__example)).toEqual([]);
  expect(deTipo(propuesta, 'inventory').rows.filter((fila) => !fila.__example)).toEqual([]);
  expect(ventasDe(propuesta).map((fila) => fila.customer)).toEqual(['', '', '']);
};

export const entoncesElSistemaGuardadoTieneLaFotoLeida = async () => {
  const sistema = elSistemaQueSeAjusta();
  await verifyPathAsync(elRouterDelTaller(), `/sistemas/${sistema.id}`);
  const guardado = useSistemaStore.getState().sistemas.find((item) => item.id === sistema.id);
  expect(ventasDe(guardado).map((fila) => fila.quantity)).toEqual([2, 1, 4]);
  expect(deTipo(guardado, 'inventory').rows.map((fila) => fila.name)).toEqual([
    'Vela lavanda',
    'Vela vainilla',
    'Vela flotante',
  ]);
  expect(
    allBlocks(guardado)
      .flatMap((bloque) => bloque.rows)
      .some((fila) => fila.__example),
  ).toBe(false);
};

export const entoncesSoloSeMuestranLasDudasDeAUna = () => {
  expect(screen.getByRole('group', { name: 'Respuestas rápidas' })).toBeVisible();
  expect(screen.queryByRole('group', { name: REGISTROS_NUEVOS })).toBeNull();
};

export const entoncesRecienAhoraApareceLaTarjetaDeLosVinculosNuevos = () => {
  expect(screen.queryByRole('group', { name: 'Respuestas rápidas' })).toBeNull();
  expect(screen.getByRole('group', { name: REGISTROS_NUEVOS })).toBeVisible();
};
