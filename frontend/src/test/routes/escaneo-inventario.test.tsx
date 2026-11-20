import { act, fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { allBlocks, clone, makeTemplate } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

type Router = ReturnType<typeof renderApp>;
type Usuario = ReturnType<typeof userEvent.setup>;
type UsuarioDePrueba = ReturnType<typeof setupUsuarioConSesion>;
type Sistema = ReturnType<typeof makeTemplate>;
type Bloque = ReturnType<typeof allBlocks>[number];

const CODIGO_PROPIO = '77903754';
const CODIGO_DE_CATALOGO = '7790957000101';
const CODIGO_SIN_CATALOGO = '3017620422003';
const NOMBRE_DEL_CATALOGO = 'Alfajor de chocolate con mousse Jorgito 45g';

const originales = {
  close: Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, 'close'),
  showModal: Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, 'showModal'),
};

let usuario: UsuarioDePrueba;
let sistema: Sistema;
let inventario: Bloque;
let destino: Bloque;
let router: Router;
let user: Usuario;

beforeAll(() => {
  Object.defineProperty(HTMLDialogElement.prototype, 'close', {
    configurable: true,
    value: function (this: HTMLDialogElement) {
      this.removeAttribute('open');
    },
  });
  Object.defineProperty(HTMLDialogElement.prototype, 'showModal', {
    configurable: true,
    value: function (this: HTMLDialogElement) {
      this.setAttribute('open', '');
    },
  });
});

afterAll(() => {
  for (const nombre of ['close', 'showModal'] as const) {
    const original = originales[nombre];
    if (original) Object.defineProperty(HTMLDialogElement.prototype, nombre, original);
    else Reflect.deleteProperty(HTMLDialogElement.prototype, nombre);
  }
});

describe('carga exclusiva de productos en inventario', () => {
  it('entra desde Inventario apuntando a la tabla elegida', async () => {
    await dadoUnEscanerAbierto();

    entoncesLaUrlApuntaALaTablaElegida();
  });

  it('entra desde Inventario sin caja ni ticket', async () => {
    await dadoUnEscanerAbierto();

    entoncesNoHayCajaNiTicket();
  });

  it('un código del catálogo propone la ficha con el nombre y la foto', async () => {
    dadoUnCatalogoConElAlfajorJorgito();
    await dadoUnEscanerAbierto();

    await cuandoSeBuscaElCodigo(CODIGO_DE_CATALOGO);

    entoncesSePropuneLaFichaDelAlfajorJorgito();
  });

  it('la ficha del catálogo permite ver el origen de los datos', async () => {
    dadoUnCatalogoConElAlfajorJorgito();
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_DE_CATALOGO);

    await cuandoSeMiraElOrigenDeLosDatos();

    entoncesElOrigenEsSepa();
  });

  it('guarda la ficha revisada con los campos propios de la tabla', async () => {
    dadoUnCatalogoConElAlfajorJorgito();
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_DE_CATALOGO);

    await cuandoSeCompletaYGuardaLaFichaDelAlfajor();

    entoncesLaFilaNuevaTieneLosDatosRevisados();
  });

  it('guardar una ficha no toca las ventas ni las entradas', async () => {
    dadoUnCatalogoConElAlfajorJorgito();
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_DE_CATALOGO);

    await cuandoSeCompletaYGuardaLaFichaDelAlfajor();

    entoncesVentasYEntradasSiguenVacias();
  });

  it('guardar una ficha persiste el sistema en el servidor', async () => {
    dadoUnCatalogoConElAlfajorJorgito();
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_DE_CATALOGO);

    await cuandoSeCompletaYGuardaLaFichaDelAlfajor();

    await entoncesElServidorTieneElSistemaActual();
  });

  it('reconoce un código propio y propone sumar unidades', async () => {
    dadoUnCatalogoSinResultadosPara(CODIGO_PROPIO);
    await dadoUnEscanerAbierto();

    await cuandoSeBuscaElCodigo(CODIGO_PROPIO);

    entoncesSeReconoceElProductoPropioYSeProponeSumar();
  });

  it('descartar la carga conserva las unidades del producto', async () => {
    dadoUnCatalogoSinResultadosPara(CODIGO_PROPIO);
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_PROPIO);

    await cuandoSeDescartaUnaCargaDe99Unidades();

    entoncesElProductoSigueConUnidades(10);
  });

  it('descartar la carga devuelve el foco al código de barras', async () => {
    dadoUnCatalogoSinResultadosPara(CODIGO_PROPIO);
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_PROPIO);

    await cuandoSeDescartaUnaCargaDe99Unidades();

    entoncesElFocoVuelveAlCodigoDeBarras();
  });

  it('agrega unidades al producto registrado sin duplicarlo', async () => {
    dadoUnCatalogoSinResultadosPara(CODIGO_PROPIO);
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_PROPIO);

    await cuandoSeSumanUnidades('3');

    entoncesElProductoQuedaConUnaFilaYTrece();
  });

  it('al sumar unidades se confirma lo guardado y el lector queda listo', async () => {
    dadoUnCatalogoSinResultadosPara(CODIGO_PROPIO);
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_PROPIO);

    await cuandoSeSumanUnidades('3');

    await entoncesSeConfirmaElStockActualizadoYElLectorQuedaListo();
  });

  it('sumar unidades persiste el total en el servidor', async () => {
    dadoUnCatalogoSinResultadosPara(CODIGO_PROPIO);
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_PROPIO);

    await cuandoSeSumanUnidades('3');

    await entoncesElServidorTieneElSistemaActual();
  });

  it.each(['', '0', '-3'])(
    'no cambia el stock con la cantidad «%s» y enfoca la corrección',
    async (cantidad) => {
      dadoUnCatalogoSinResultadosPara(CODIGO_PROPIO);
      await dadoUnEscanerAbierto();
      await cuandoSeBuscaElCodigo(CODIGO_PROPIO);

      await cuandoSeIntentaSumarUnaCantidad(cantidad);

      entoncesSePideUnaCantidadMayorQueCeroSinCambiarElStock();
    },
  );

  it('otro escaneo desde la revisión pide confirmar y permite seguir editando', async () => {
    dadoUnCatalogoSinResultadosPara(CODIGO_PROPIO);
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_PROPIO);

    await cuandoSePideOtroEscaneoConDosUnidadesSinGuardar();
    await cuandoSeElige('Seguir editando');

    entoncesSeConservanLasDosUnidadesSinMostrarElLector();
  });

  it('otro escaneo desde la revisión descarta lo no confirmado y muestra el lector', async () => {
    dadoUnCatalogoSinResultadosPara(CODIGO_PROPIO);
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_PROPIO);
    await cuandoSePideOtroEscaneoConDosUnidadesSinGuardar();

    await cuandoSeElige('Descartar y continuar');

    entoncesElLectorQuedaListoSinRevision();
  });

  it('conserva la descripción propia aunque el catálogo no responda', async () => {
    dadoUnCatalogoCaido();
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_PROPIO);

    await cuandoSeSumanUnidades('2');

    entoncesElProductoConservaSuNombreYTieneUnidades(12);
  });

  it('vincula un código nuevo con un producto existente sin otro código', async () => {
    dadoUnCatalogoSinResultadosPara(CODIGO_SIN_CATALOGO);
    await dadoUnEscanerAbierto();
    dadoUnProductoSinCodigo();
    await cuandoSeBuscaElCodigo(CODIGO_SIN_CATALOGO);

    entoncesSoloSeOfrecenProductosSinOtroCodigo();
  });

  it('al vincular el código queda listo para la siguiente lectura con teclado', async () => {
    dadoUnCatalogoSinResultadosPara(CODIGO_SIN_CATALOGO);
    await dadoUnEscanerAbierto();
    dadoUnProductoSinCodigo();
    await cuandoSeBuscaElCodigo(CODIGO_SIN_CATALOGO);

    await cuandoSeVinculaConElProductoSinCodigo();

    await entoncesLaSiguienteLecturaReconoceElProductoVinculado();
  });

  it('permite carga manual: sin nombre se marca el error y se enfoca', async () => {
    dadoUnCatalogoSinResultadosPara('77905741');
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo('77905741');

    await cuandoSeElige('Agregar producto al stock');

    entoncesElNombreSeMarcaInvalidoYTieneElFoco();
  });

  it('permite carga manual con conteo cero y precio opcional', async () => {
    dadoUnCatalogoSinResultadosPara('77905741');
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo('77905741');

    await cuandoSeCargaUnProductoManualConConteoCero();

    entoncesLaFilaManualTieneConteoCeroYPrecioVacio();
  });

  it('suma al stock vigente y conserva los otros datos actualizados mientras se revisaba', async () => {
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_PROPIO);
    cuandoSeEscribenUnidades('3');

    await cuandoSeActualizaElStockMientrasSeRevisa();

    entoncesSeSumaAlStockVigenteYSeConservanLosDatosNuevos();
  });

  it('volver al inventario protege el borrador y permite seguir editando', async () => {
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_PROPIO);

    await cuandoSeIntentaVolverAlStock();
    await cuandoSeElige('Seguir editando');

    entoncesSigueLaRevisionDelBorrador();
  });

  it('volver al inventario abre su tabla cuando se confirma salir', async () => {
    await dadoUnEscanerAbierto();
    await cuandoSeBuscaElCodigo(CODIGO_PROPIO);
    await cuandoSeIntentaVolverAlStock();

    await cuandoSeElige('Descartar y continuar');

    await entoncesSeAbreLaTablaDelInventario();
  });

  it('usa la tabla elegida y no da de alta en el primer inventario del cuaderno', async () => {
    dadoUnCatalogoSinResultadosPara(CODIGO_SIN_CATALOGO);
    await dadoUnEscanerAbierto(true);
    await cuandoSeBuscaElCodigo(CODIGO_SIN_CATALOGO);

    await cuandoSeCargaUnProductoParaElDeposito();

    entoncesElProductoVaAlDepositoYNoAlPrimerInventario();
  });
});

function requerido<T>(dato: T | undefined): T {
  if (dato === undefined) throw new Error('Falta un dato del escenario');
  return dato;
}

function actual() {
  const sistemas = useSistemaStore.getState().sistemas;
  return requerido(sistemas.find((item) => item.id === sistema.id));
}

function filasDe(bloque: Bloque) {
  return requerido(allBlocks(actual()).find((candidato) => candidato.id === bloque.id)).rows;
}

function elInventarioDelAlfajor(): Bloque {
  const bloque = allBlocks(sistema).find((candidato) => candidato.type === 'inventory');
  if (!bloque) throw new Error('Falta la sección de stock');
  return bloque;
}

function armarElKioscoDeNacho() {
  sistema = makeTemplate('shop', { movements: true }, false);
  sistema.name = 'Kiosco de Nacho';
  inventario = elInventarioDelAlfajor();
  inventario.rows = [
    {
      id: 'alfajor',
      name: 'Guaymallén',
      barcode: CODIGO_PROPIO,
      quantity: 10,
      price: 850.25,
      cost: 500,
      minimum: 2,
      categoria: 'Kiosco',
    },
  ];
  inventario.fields.push({
    key: 'categoria',
    label: 'Categoría',
    type: 'select',
    required: true,
    options: ['Kiosco', 'Almacén'],
  });
}

function agregarUnDepositoVacio() {
  destino = { ...clone(inventario), id: 'deposito', name: 'Depósito', rows: [] };
  requerido(sistema.sections[0]).blocks.push(destino);
}

async function dadoUnEscanerAbierto(conDeposito = false) {
  usuario = setupUsuarioConSesion();
  armarElKioscoDeNacho();
  destino = inventario;
  if (conDeposito) agregarUnDepositoVacio();
  estado.sistemas.set(usuario.email, [sistema]);
  router = renderApp(`/sistemas/${sistema.id}`);
  await screen.findByRole('heading', { name: sistema.name, level: 1 }, ESPERA);
  user = userEvent.setup();
  await abrirElEscanerDe(destino);
}

async function abrirElEscanerDe(bloque: Bloque) {
  const region = within(screen.getByRole('region', { name: bloque.name }));
  await user.click(region.getByRole('button', { name: 'Escanear productos' }));
  await screen.findByRole('heading', { name: 'Escaneá, revisá y agregá.' });
}

function dadoUnCatalogoConElAlfajorJorgito() {
  server.use(
    http.get(`/api/catalogos/sepa/${CODIGO_DE_CATALOGO}`, () =>
      HttpResponse.json({
        producto: {
          codigo: CODIGO_DE_CATALOGO,
          nombre: NOMBRE_DEL_CATALOGO,
          marca: 'Jorgito',
          presentacion: '45 g',
        },
      }),
    ),
    http.get(`/api/catalogos/openfoodfacts/${CODIGO_DE_CATALOGO}`, () =>
      HttpResponse.json({
        producto: {
          codigo: CODIGO_DE_CATALOGO,
          nombre: 'Alfajor',
          marca: '',
          presentacion: '',
          imagen: 'https://images.openfoodfacts.org/foto.jpg',
        },
      }),
    ),
  );
}

function dadoUnCatalogoSinResultadosPara(codigo: string) {
  server.use(
    http.get(`/api/catalogos/sepa/${codigo}`, () => HttpResponse.json({ producto: null })),
    http.get(`/api/catalogos/openfoodfacts/${codigo}`, () => HttpResponse.json({ producto: null })),
  );
}

function dadoUnCatalogoCaido() {
  server.use(
    http.get(`/api/catalogos/:fuente/${CODIGO_PROPIO}`, () =>
      HttpResponse.json({}, { status: 503 }),
    ),
  );
}

function dadoUnProductoSinCodigo() {
  act(() =>
    useSistemaStore.getState().actualizarSistema(sistema.id, (copia) => {
      const bloque = requerido(
        allBlocks(copia).find((candidato) => candidato.id === inventario.id),
      );
      bloque.rows.push({
        id: 'sin-codigo',
        name: 'Producto sin código',
        quantity: 7,
        cost: 300,
        price: 500,
        minimum: 1,
        categoria: 'Kiosco',
      });
    }),
  );
}

function cambiar(nombre: string, valor: string) {
  fireEvent.change(screen.getByRole('spinbutton', { name: nombre }), { target: { value: valor } });
}

async function cuandoSeBuscaElCodigo(codigo: string) {
  if (!screen.queryByRole('textbox', { name: 'Código de barras' }))
    await user.click(screen.getByRole('button', { name: 'Escribir código · lector USB' }));
  await user.type(screen.getByRole('textbox', { name: 'Código de barras' }), `${codigo}{Enter}`);
  await screen.findByRole('region', { name: 'Producto detectado' });
  await esperarQueSePuedaGuardar();
}

async function esperarQueSePuedaGuardar() {
  const guardar = /Agregar producto al stock|Guardar cambios del producto|Agregar al stock/;
  await waitFor(() => expect(screen.getByRole('button', { name: guardar })).toBeEnabled());
}

async function cuandoSeElige(nombre: string) {
  await user.click(await screen.findByRole('button', { name: nombre }));
}

async function cuandoSeMiraElOrigenDeLosDatos() {
  await user.click(screen.getByRole('button', { name: 'Ver origen de los datos' }));
}

async function cuandoSeCompletaYGuardaLaFichaDelAlfajor() {
  cambiar('Cantidad', '12');
  cambiar('Precio', '850.25');
  cambiar('Costo', '500.10');
  cambiar('Stock mínimo', '3');
  await user.selectOptions(screen.getByRole('combobox', { name: 'Categoría' }), 'Kiosco');
  await user.click(screen.getByRole('button', { name: 'Agregar producto al stock' }));
  await screen.findByRole('region', { name: 'Último producto guardado' });
}

async function cuandoSeDescartaUnaCargaDe99Unidades() {
  cambiar('Unidades a agregar', '99');
  await user.click(screen.getByRole('button', { name: 'Descartar carga' }));
  await user.click(await screen.findByRole('button', { name: 'Descartar y continuar' }));
}

async function cuandoSeSumanUnidades(unidades: string) {
  cambiar('Unidades a agregar', unidades);
  await user.click(screen.getByRole('button', { name: 'Agregar al stock' }));
  await screen.findByRole('region', { name: 'Último producto guardado' });
}

async function cuandoSeIntentaSumarUnaCantidad(cantidad: string) {
  cambiar('Unidades a agregar', cantidad);
  await user.click(screen.getByRole('button', { name: 'Agregar al stock' }));
}

async function cuandoSePideOtroEscaneoConDosUnidadesSinGuardar() {
  cambiar('Unidades a agregar', '2');
  await user.click(screen.getByRole('button', { name: 'Escanear otro producto' }));
}

async function cuandoSeVinculaConElProductoSinCodigo() {
  const vincular = screen.getByRole('combobox', { name: 'Vincular con un producto existente' });
  await user.selectOptions(vincular, 'sin-codigo');
  await user.click(screen.getByRole('button', { name: 'Guardar cambios del producto' }));
  await screen.findByRole('region', { name: 'Último producto guardado' });
}

async function cuandoSeCargaUnProductoManualConConteoCero() {
  await user.type(screen.getByRole('textbox', { name: 'Producto' }), 'Producto manual');
  cambiar('Cantidad', '0');
  await user.selectOptions(screen.getByRole('combobox', { name: 'Categoría' }), 'Almacén');
  await user.click(screen.getByRole('button', { name: 'Agregar producto al stock' }));
  await screen.findByRole('region', { name: 'Último producto guardado' });
}

function cuandoSeEscribenUnidades(unidades: string) {
  cambiar('Unidades a agregar', unidades);
}

async function cuandoSeActualizaElStockMientrasSeRevisa() {
  act(() =>
    useSistemaStore.getState().actualizarSistema(sistema.id, (copia) => {
      const bloque = requerido(
        allBlocks(copia).find((candidato) => candidato.id === inventario.id),
      );
      const producto = requerido(bloque.rows[0]);
      producto.quantity = 6;
      producto.cost = 600;
    }),
  );
  await user.click(screen.getByRole('button', { name: 'Agregar al stock' }));
  await screen.findByRole('region', { name: 'Último producto guardado' });
}

async function cuandoSeIntentaVolverAlStock() {
  await user.click(screen.getByRole('button', { name: 'Volver al stock' }));
}

async function cuandoSeCargaUnProductoParaElDeposito() {
  await user.type(screen.getByRole('textbox', { name: 'Producto' }), 'Para depósito');
  cambiar('Cantidad', '4');
  await user.selectOptions(screen.getByRole('combobox', { name: 'Categoría' }), 'Kiosco');
  await user.click(screen.getByRole('button', { name: 'Agregar producto al stock' }));
  await screen.findByRole('region', { name: 'Último producto guardado' });
}

function entoncesLaUrlApuntaALaTablaElegida() {
  expect(router.state.location.search).toMatchObject({ escanear: destino.id });
}

function entoncesNoHayCajaNiTicket() {
  expect(screen.queryByRole('button', { name: 'Abrir caja' })).toBeNull();
  expect(screen.queryByRole('region', { name: 'Ticket en curso' })).toBeNull();
}

function entoncesSePropuneLaFichaDelAlfajorJorgito() {
  const detectado = within(screen.getByRole('region', { name: 'Producto detectado' }));
  expect(screen.getByRole('textbox', { name: 'Producto' })).toHaveValue(NOMBRE_DEL_CATALOGO);
  expect(screen.getByRole('img', { name: 'Foto de referencia del producto' })).toBeVisible();
  expect(detectado.getByRole('heading', { name: NOMBRE_DEL_CATALOGO })).toBeVisible();
}

function entoncesElOrigenEsSepa() {
  expect(screen.getByRole('dialog', { name: 'Origen de los datos' })).toHaveTextContent('SEPA');
}

function entoncesLaFilaNuevaTieneLosDatosRevisados() {
  expect(filasDe(destino)).toHaveLength(2);
  expect(filasDe(destino)[1]).toMatchObject({
    barcode: CODIGO_DE_CATALOGO,
    quantity: 12,
    cost: 500.1,
    price: 850.25,
    categoria: 'Kiosco',
  });
}

function entoncesVentasYEntradasSiguenVacias() {
  const movimientos = allBlocks(actual()).filter((bloque) =>
    ['sales', 'entries'].includes(bloque.type),
  );
  expect(movimientos.every((bloque) => bloque.rows.length === 0)).toBe(true);
}

async function entoncesElServidorTieneElSistemaActual() {
  await waitFor(() => expect(seccionesEnElServidor()).toEqual(actual().sections), ESPERA);
}

function seccionesEnElServidor() {
  return estado.sistemas.get(usuario.email)?.find((item) => item.id === sistema.id)?.sections;
}

function entoncesSeReconoceElProductoPropioYSeProponeSumar() {
  const detectado = within(screen.getByRole('region', { name: 'Producto detectado' }));
  expect(detectado.getByRole('heading', { name: 'Guaymallén' })).toBeVisible();
  expect(screen.getByText('Este producto ya está en tu stock')).toBeVisible();
  expect(screen.getByRole('heading', { name: '¿Querés sumar al stock?' })).toBeVisible();
  expect(screen.queryByRole('spinbutton', { name: 'Precio' })).toBeNull();
  expect(screen.queryByRole('region', { name: 'Lector de códigos' })).toBeNull();
}

function entoncesElProductoSigueConUnidades(unidades: number) {
  expect(filasDe(inventario)[0]?.quantity).toBe(unidades);
  expect(screen.queryByRole('region', { name: 'Sumar al stock' })).toBeNull();
}

function entoncesElFocoVuelveAlCodigoDeBarras() {
  expect(screen.getByRole('textbox', { name: 'Código de barras' })).toHaveFocus();
}

function entoncesElProductoQuedaConUnaFilaYTrece() {
  expect(filasDe(inventario)).toHaveLength(1);
  expect(filasDe(inventario)[0]).toMatchObject({
    id: 'alfajor',
    quantity: 13,
    cost: 500,
    price: 850.25,
  });
}

async function entoncesSeConfirmaElStockActualizadoYElLectorQuedaListo() {
  const confirmacion = screen.getByRole('region', { name: 'Último producto guardado' });
  await waitFor(() => expect(confirmacion).toHaveTextContent('Stock actualizado'), ESPERA);
  expect(confirmacion).toHaveTextContent('Agregaste 3 unidades');
  expect(screen.getByRole('region', { name: 'Lector de códigos' })).toBeVisible();
}

function entoncesSePideUnaCantidadMayorQueCeroSinCambiarElStock() {
  expect(screen.getByRole('alert')).toHaveTextContent('mayor que cero');
  expect(screen.getByRole('spinbutton', { name: 'Unidades a agregar' })).toHaveFocus();
  expect(filasDe(inventario)[0]?.quantity).toBe(10);
}

function entoncesSeConservanLasDosUnidadesSinMostrarElLector() {
  expect(screen.getByRole('spinbutton', { name: 'Unidades a agregar' })).toHaveValue(2);
  expect(screen.queryByRole('region', { name: 'Lector de códigos' })).toBeNull();
}

function entoncesElLectorQuedaListoSinRevision() {
  expect(screen.getByRole('region', { name: 'Lector de códigos' })).toBeVisible();
  expect(screen.queryByRole('region', { name: 'Sumar al stock' })).toBeNull();
  expect(screen.getByRole('textbox', { name: 'Código de barras' })).toHaveValue('');
}

function entoncesElProductoConservaSuNombreYTieneUnidades(unidades: number) {
  expect(filasDe(inventario)[0]).toMatchObject({ name: 'Guaymallén', quantity: unidades });
}

function entoncesSoloSeOfrecenProductosSinOtroCodigo() {
  const vincular = screen.getByRole('combobox', { name: 'Vincular con un producto existente' });
  expect(within(vincular).queryByRole('option', { name: 'Guaymallén' })).toBeNull();
  expect(within(vincular).getByRole('option', { name: 'Producto sin código' })).toBeVisible();
}

async function entoncesLaSiguienteLecturaReconoceElProductoVinculado() {
  expect(screen.getByRole('textbox', { name: 'Código de barras' })).toHaveFocus();
  await user.keyboard(`${CODIGO_SIN_CATALOGO}{Enter}`);
  await screen.findByRole('region', { name: 'Sumar al stock' });
  expect(screen.getByRole('spinbutton', { name: 'Unidades a agregar' })).toHaveValue(null);
  expect(screen.queryByRole('region', { name: 'Último producto guardado' })).toBeNull();
  expect(screen.getByRole('heading', { name: 'Producto sin código' })).toBeVisible();
  expect(filasDe(inventario)).toHaveLength(2);
  expect(filasDe(inventario).find((fila) => fila.id === 'sin-codigo')).toMatchObject({
    barcode: CODIGO_SIN_CATALOGO,
    quantity: 7,
  });
}

function entoncesElNombreSeMarcaInvalidoYTieneElFoco() {
  const producto = screen.getByRole('textbox', { name: 'Producto' });
  expect(producto).toHaveAttribute('aria-invalid', 'true');
  expect(producto).toHaveFocus();
}

function entoncesLaFilaManualTieneConteoCeroYPrecioVacio() {
  expect(filasDe(destino)[1]).toMatchObject({
    name: 'Producto manual',
    barcode: '77905741',
    quantity: 0,
    price: '',
  });
}

function entoncesSeSumaAlStockVigenteYSeConservanLosDatosNuevos() {
  expect(filasDe(inventario)[0]).toMatchObject({ quantity: 9, cost: 600, price: 850.25 });
}

function entoncesSigueLaRevisionDelBorrador() {
  expect(router.state.location.search).toMatchObject({ escanear: destino.id });
  expect(screen.getByRole('region', { name: 'Sumar al stock' })).toBeVisible();
}

async function entoncesSeAbreLaTablaDelInventario() {
  await screen.findByRole('dialog', { name: destino.name });
  expect(router.state.location.search.escanear).toBeUndefined();
}

function entoncesElProductoVaAlDepositoYNoAlPrimerInventario() {
  expect(filasDe(inventario)).toHaveLength(1);
  expect(filasDe(destino)[0]).toMatchObject({ name: 'Para depósito', quantity: 4 });
}
