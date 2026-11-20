import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { expect, vi } from 'vitest';
import { sistemaParaCrear } from '@/lib/sistema/acciones';
import { debeMostrarConsejosDeFoto } from '@/lib/sistema/consejosDeFoto';
import { allBlocks } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { useSistemaStore } from '@/stores/sistemaStore';
import {
  botonParaVolverAlInicio,
  campoDeLaConversacion,
  campoDeLosAjustes,
  cantidadDePiezasDeLaPropuesta,
  dadoQueSamiArmoUnaPropuestaDeAlmacen,
  dadoQueSeAbreElTaller,
  elRouterDelTaller,
  entrarAlTaller,
  entoncesLaConversacionYElLienzoEstanVacios,
  laPersonaDelTaller,
  lienzoDeLaPropuesta,
  piezaDeLaPropuesta,
  piezasDeLaPropuesta,
  selectorDeFoto,
} from '@/test/taller';
import { ESPERA, clickButton, verifyPathAsync, verifyToastAsync } from '@/test/testHelpers';

const DIALOGO_DE_CONSEJOS = 'Para que Sami lea bien tu foto';
const BOTON_DE_LA_CAMARA = 'Mandarle una foto a Sami';

let abrirSelector: ReturnType<typeof vi.spyOn>;
let piezasAntes: (string | undefined)[];
let filasAntes: number;
let sistemaACrear: Sistema;

const persona = laPersonaDelTaller;
const productos = () => piezaDeLaPropuesta(/^Productos/);
const filasDeEjemplo = () => document.querySelectorAll('#draft-canvas .example-row').length;
const borrador = () => useSistemaStore.getState().taller?.borrador ?? null;
const registrosDeProductos = (sistema: Sistema) =>
  allBlocks(sistema).find((pieza) => pieza.type === 'inventory')?.rows ?? [];

/* dado */

export const dadoQueSeAbreElTallerDeSami = dadoQueSeAbreElTaller;

export const dadoQueSamiArmoLaPropuesta = dadoQueSamiArmoUnaPropuestaDeAlmacen;

export const dadoQueElSelectorDeFotoSeEspia = () => {
  abrirSelector = vi.spyOn(selectorDeFoto(), 'click');
};

export const dadoQueNoSeQuierenVerLosConsejosDeLaFoto = () => {
  vi.mocked(debeMostrarConsejosDeFoto).mockReturnValueOnce(false);
};

export const dadoQueSeAbrioElPopUpDeLosConsejos = async () => {
  await persona().click(screen.getByRole('button', { name: BOTON_DE_LA_CAMARA }));
  await screen.findByRole('dialog', { name: DIALOGO_DE_CONSEJOS });
};

export const dadoQueSeMinimizoElChat = () => clickButton(persona(), 'Minimizar el chat');

export const dadoQueSeVieronLasPiezasEnOrden = () => {
  piezasAntes = piezasDeLaPropuesta();
};

export const dadoQueSeVieronLasFilasDeProductos = () => {
  filasAntes = within(productos()).getAllByRole('row').length;
};

export const dadoQueSeEditoElPrimerRegistroDeEjemplo = async () => {
  await cuandoSeEditaElPrimerRegistroDeEjemplo();
  await verifyToastAsync('Registro actualizado.');
};

export const dadoQueSeAgregoUnRegistroPropio = async () => {
  await cuandoSeRegistraUnProductoPropio();
  await verifyToastAsync('Registro guardado.');
};

export const dadoQueSePidioQuitarLosEjemplos = () => clickButton(persona(), /Quitar ejemplos/);

/* cuando */

export const cuandoSeAbreElTallerDeSami = dadoQueSeAbreElTaller;

export const cuandoSamiArmaLaPropuesta = dadoQueSamiArmoUnaPropuestaDeAlmacen;

export const cuandoSeTocaLaCamara = () =>
  persona().click(screen.getByRole('button', { name: BOTON_DE_LA_CAMARA }));

export const cuandoSeEligeTomarFoto = () =>
  persona().click(
    within(screen.getByRole('dialog', { name: DIALOGO_DE_CONSEJOS })).getByRole('button', {
      name: 'Tomar foto',
    }),
  );

export const cuandoSePulsaEscape = () => persona().keyboard('{Escape}');

export const cuandoSeMinimizaElChat = dadoQueSeMinimizoElChat;

export const cuandoSeAbreElChatMinimizado = () =>
  persona().click(screen.getByRole('button', { name: 'Abrir el chat con Sami' }));

export const cuandoSeEscribeUnMensajeMuyCorto = async () => {
  await persona().type(campoDeLaConversacion(), 'a');
  await clickButton(persona(), /Pensalo conmigo/);
};

export const cuandoSePideSumarGastos = async () => {
  await persona().type(campoDeLosAjustes(), 'sumá gastos');
  await clickButton(persona(), /Ajustalo conmigo/);
};

export const cuandoSeRenombraLaPiezaDeClientes = async () => {
  await persona().click(within(lienzoDeLaPropuesta()).getByRole('button', { name: /^Clientes/ }));
  const campo = await screen.findByLabelText('Nombre de la pieza');
  await persona().clear(campo);
  await persona().type(campo, 'Los de la cuadra');
  await clickButton(persona(), 'Guardar nombre');
};

export const cuandoSeAnchaLaPiezaDeClientesAFilaCompleta = () =>
  persona().click(
    within(piezaDeLaPropuesta(/^Clientes/)).getByRole('button', { name: 'Fila completa' }),
  );

export const cuandoSeQuitaLaPiezaDeClientes = () =>
  persona().click(
    within(piezaDeLaPropuesta(/^Clientes/)).getByRole('button', { name: 'Quitar de la propuesta' }),
  );

export const cuandoSeAbreElPrimerRegistroDeEjemplo = async () => {
  const [primerLapiz] = within(productos()).getAllByRole('button', { name: 'Editar registro' });
  if (!primerLapiz) throw new Error('Falta el lápiz del primer registro');
  await persona().click(primerLapiz);
};

export const cuandoSeEditaElPrimerRegistroDeEjemplo = async () => {
  await cuandoSeAbreElPrimerRegistroDeEjemplo();
  await screen.findByRole('heading', { name: 'Editar producto' });
  const nombre = screen.getByLabelText(/^Producto/);
  await persona().clear(nombre);
  await persona().type(nombre, 'Yerba Taragüí 1 kg');
  await clickButton(persona(), 'Guardar');
};

export const cuandoSeRegistraUnProductoPropio = async () => {
  await persona().click(within(productos()).getByRole('button', { name: 'Registrar' }));
  await screen.findByRole('heading', { name: 'Registrar producto' });
  await persona().type(screen.getByLabelText(/^Producto/), 'Mate de calabaza');
  await persona().type(screen.getByLabelText(/^Cantidad/), '8');
  await clickButton(persona(), 'Guardar');
};

export const cuandoSeIntentaRegistrarUnProductoSinDatos = async () => {
  await persona().click(within(productos()).getByRole('button', { name: 'Registrar' }));
  await screen.findByRole('heading', { name: 'Registrar producto' });
  await clickButton(persona(), 'Guardar');
};

export const cuandoSeArmaElSistemaACrear = () => {
  const propuesta = borrador();
  if (!propuesta) throw new Error('Falta la propuesta');
  sistemaACrear = sistemaParaCrear(propuesta);
};

export const cuandoSeSumaLaPiezaDeGastosDesdeLaPaleta = async () => {
  await persona().click(screen.getByRole('button', { name: 'Agregar y organizar' }));
  await persona().click(screen.getByRole('button', { name: 'Bloques' }));
  const dialogo = await screen.findByRole('dialog');
  await persona().click(within(dialogo).getByRole('button', { name: /^Gastos/ }));
};

export const cuandoSeElijeLaDistribucionDeUnaColumna = async () => {
  await persona().click(screen.getByRole('button', { name: 'Agregar y organizar' }));
  await persona().click(screen.getByRole('button', { name: 'Distribución de los bloques' }));
  await clickButton(persona(), /1 columna/);
};

export const cuandoSeArrastraLaSegundaPiezaSobreLaPrimera = () => {
  const tirador = screen.getAllByRole('button', { name: 'Arrastrar esta pieza' })[1];
  const destino = document.querySelectorAll<HTMLElement>('[data-draft-block]')[0];
  if (!tirador || !destino) throw new Error('Faltan piezas');
  const dataTransfer = { setData: () => undefined, effectAllowed: '' };
  fireEvent.dragStart(tirador, { dataTransfer });
  fireEvent.dragOver(destino, { dataTransfer });
  fireEvent.drop(destino, { dataTransfer });
};

export const cuandoSeCreaLaSeccionConElNombre = async (nombre: string) => {
  const campo = screen.getByLabelText('Nombre de tu sección');
  await persona().clear(campo);
  await persona().type(campo, nombre);
  await clickButton(persona(), /Sí, crear mi sección/);
};

export const cuandoSeBorranLosEjemplosConfirmando = async () => {
  await clickButton(persona(), /Quitar ejemplos/);
  const dialogo = await screen.findByRole('dialog');
  await persona().click(within(dialogo).getByRole('button', { name: 'Borrar ejemplos' }));
};

export const cuandoSeVuelveAEntrarAlTaller = async () => {
  await persona().click(botonParaVolverAlInicio());
  await entrarAlTaller();
};

/* entonces */

export const entoncesEmpiezaConElSaludoLosEjemplosYElLienzoVacio = () => {
  expect(screen.getByText('Hola, Ana Pérez.')).toBeVisible();
  expect(screen.getByText('Contame qué hacés y cómo te organizás hoy.')).toBeVisible();
  expect(screen.getByText('O probá con un ejemplo:')).toBeVisible();
  entoncesLaConversacionYElLienzoEstanVacios();
  expect(screen.queryByText('Tu idea es el punto de partida')).toBeNull();
};

export const entoncesSeMuestranLosConsejosSinAbrirElSelector = async () => {
  const dialogo = await screen.findByRole('dialog', { name: DIALOGO_DE_CONSEJOS });
  expect(within(dialogo).getAllByRole('listitem').length).toBeGreaterThanOrEqual(4);
  expect(abrirSelector).not.toHaveBeenCalled();
};

export const entoncesSeAbreElSelectorUnaVezYSeCierraElPopUp = () => {
  expect(abrirSelector).toHaveBeenCalledTimes(1);
  expect(screen.queryByRole('dialog')).toBeNull();
};

export const entoncesSeCierraElPopUpSinAbrirElSelector = () => {
  expect(screen.queryByRole('dialog')).toBeNull();
  expect(abrirSelector).not.toHaveBeenCalled();
};

export const entoncesElChatQuedaMinimizadoYElFocoVaAlBotonParaAbrirlo = async () => {
  expect(screen.getByText('Hola, Ana Pérez.')).not.toBeVisible();
  expect(screen.getByText('TU IDEA VA A APARECER ACÁ')).toBeVisible();
  const abrir = screen.getByRole('button', { name: 'Abrir el chat con Sami' });
  await waitFor(() => expect(abrir).toHaveFocus());
};

export const entoncesSeVuelveALaConversacionSinPerderla = async () => {
  expect(screen.getByText('Hola, Ana Pérez.')).toBeVisible();
  expect(screen.queryByRole('button', { name: 'Abrir el chat con Sami' })).toBeNull();
  await waitFor(() => expect(campoDeLaConversacion()).toHaveFocus());
};

export const entoncesPideUnPocoMasYNoArmaNingunaPropuesta = async () => {
  await verifyToastAsync(/Contame un poquito más/);
  expect(borrador()).toBeNull();
};

export const entoncesSamiArmaUnaBaseQueSeVeALaDerecha = async () => {
  expect(screen.getByLabelText('Nombre de tu sección')).toHaveValue('Mi almacén');
  expect(screen.queryByText(/sin guardar/i)).toBeNull();
  expect(screen.getByText(/^Te armé los productos/)).toBeVisible();
  expect(await screen.findByText(/Ya lo podés ver a la derecha\./, {}, ESPERA)).toBeVisible();
  expect(cantidadDePiezasDeLaPropuesta()).toBe(3);
  expect(campoDeLosAjustes()).toBeVisible();
};

export const entoncesSamiSumaLosGastosALaPropuesta = async () => {
  expect(await screen.findByText('Listo, sumé los gastos.', {}, ESPERA)).toBeVisible();
  expect(cantidadDePiezasDeLaPropuesta()).toBe(4);
};

export const entoncesLaPiezaSeLlamaComoLaPersonaQuiso = async () => {
  expect(await screen.findByRole('button', { name: /^Los de la cuadra/ })).toBeVisible();
};

export const entoncesLaPiezaOcupaLaFilaCompleta = () => {
  expect(piezaDeLaPropuesta(/^Clientes/)).toHaveAttribute('data-width', 'full');
};

export const entoncesLaPiezaYaNoEstaEnLaPropuesta = () => {
  expect(screen.queryByRole('button', { name: /^Clientes/ })).toBeNull();
  expect(cantidadDePiezasDeLaPropuesta()).toBe(2);
};

export const entoncesElFormularioNoOfreceEliminarElRegistroDeEjemplo = async () => {
  expect(await screen.findByRole('heading', { name: 'Editar producto' })).toBeVisible();
  expect(within(productos()).getAllByText('Ejemplo').length).toBeGreaterThan(0);
  expect(screen.queryByRole('button', { name: /Eliminar/ })).toBeNull();
};

export const entoncesElRegistroEditadoDejaDeSerDeEjemplo = async () => {
  await verifyToastAsync('Registro actualizado.');
  const editada = within(productos()).getByText('Yerba Taragüí 1 kg').closest('tr');
  expect(editada).not.toHaveClass('example-row');
};

export const entoncesElRegistroPropioApareceEnLaPieza = async () => {
  await verifyToastAsync('Registro guardado.');
  expect(within(productos()).getByText('Mate de calabaza')).toBeVisible();
};

export const entoncesLosRegistrosEditadosYPropiosPasanAlSistema = () => {
  expect(registrosDeProductos(sistemaACrear).map((fila) => fila.name)).toEqual([
    'Yerba Taragüí 1 kg',
    'Mate de calabaza',
  ]);
};

export const entoncesNoSeGuardaYElFormularioSigueAbierto = async () => {
  expect((await screen.findAllByText('Completá este dato.')).length).toBeGreaterThan(0);
  expect(screen.getByRole('heading', { name: 'Registrar producto' })).toBeVisible();
  expect(within(productos()).getAllByRole('row')).toHaveLength(filasAntes);
};

export const entoncesLaPropuestaTieneUnaPiezaMas = () => {
  expect(cantidadDePiezasDeLaPropuesta()).toBe(4);
};

export const entoncesTodasLasPiezasOcupanLaFilaCompleta = () => {
  const anchos = [...document.querySelectorAll('#draft-canvas .block')].map((nodo) =>
    nodo.getAttribute('data-width'),
  );
  expect(new Set(anchos)).toEqual(new Set(['full']));
};

export const entoncesLaPrimeraPiezaPasaALaSegundaPosicion = async () => {
  const [primera, segunda] = piezasAntes;
  await waitFor(() => expect(piezasDeLaPropuesta().slice(0, 2)).toEqual([segunda, primera]), ESPERA);
};

export const entoncesLaSeccionSeGuardaSinEjemplosYSeAbreLista = async (nombre: string) => {
  await waitFor(() => expect(useSistemaStore.getState().sistemas).toHaveLength(1), ESPERA);
  const [sistema] = useSistemaStore.getState().sistemas;
  expect(sistema?.name).toBe(nombre);
  expect(allBlocks(sistema).every((bloque) => bloque.rows.length === 0)).toBe(true);
  expect(useSistemaStore.getState().listo).toBe(true);
  await verifyPathAsync(elRouterDelTaller(), `/sistemas/${sistema?.id}`);
};

export const entoncesSigueHabiendoEjemplosYSePideConfirmar = async () => {
  expect(filasDeEjemplo()).toBeGreaterThan(0);
  expect(await screen.findByRole('dialog')).toBeVisible();
};

export const entoncesSeSacaronLasFilasDeEjemploYDesapareceElBoton = async () => {
  await waitFor(() => expect(filasDeEjemplo()).toBe(0), ESPERA);
  expect(screen.queryByRole('button', { name: /Quitar ejemplos/ })).not.toBeInTheDocument();
  const propuesta = borrador();
  expect(propuesta && allBlocks(propuesta).every((bloque) => bloque.rows.length === 0)).toBe(true);
};

export const entoncesLaConversacionEmpiezaDeNuevo = () => {
  expect(borrador()).toBeNull();
  entoncesLaConversacionYElLienzoEstanVacios();
};
