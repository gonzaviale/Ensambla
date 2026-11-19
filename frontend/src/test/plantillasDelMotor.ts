import { expect } from 'vitest';
import {
  allBlocks,
  applyLayout,
  clearExamples,
  makeTemplate,
  removeBlock,
  removeRow,
  splitBlocks,
  totals,
  upsertRow,
  validateRow,
} from '@/lib/sistema/core';
import type {
  Ancho,
  Bloque,
  ClavePlantilla,
  ErroresFila,
  Fila,
  OpcionesPlantilla,
  Sistema,
  TipoBloque,
} from '@/lib/sistema/tipos';

type Valores = Record<string, string | number | boolean>;

let sistema: Sistema;
let venta: Fila;
let salida: Fila;
let entrada: Fila;
let productos: Record<string, Fila> = {};
let primerProducto: Fila;
let stockInicial: number;
let errores: ErroresFila | undefined;
let quitado: boolean | undefined;
let separado: boolean;
let instantaneaDeLosPedidos: string;
let quitadosAlLimpiar: string[];

const bloqueDelTipo = (tipo: TipoBloque): Bloque => {
  const encontrado = allBlocks(sistema).find((bloque) => bloque.type === tipo);
  if (!encontrado) throw new Error(`Falta el bloque ${tipo}`);
  return encontrado;
};

const destinoDe = (tipo: TipoBloque, clave: string) =>
  bloqueDelTipo(tipo).fields.find((campo) => campo.key === clave)?.target;

const primeraFila = (tipo: TipoBloque) => bloqueDelTipo(tipo).rows[0] as Fila;

function guardar(tipo: TipoBloque, valores: Valores, filaId: string | null = null): Fila {
  const resultado = upsertRow(bloqueDelTipo(tipo), valores, filaId, sistema);
  if (!resultado.row) throw new Error(`No se pudo guardar: ${JSON.stringify(resultado.errors)}`);
  return resultado.row;
}

const intentarGuardar = (tipo: TipoBloque, valores: Valores) => {
  errores = upsertRow(bloqueDelTipo(tipo), valores, null, sistema).errors;
};

const quitar = (tipo: TipoBloque, fila: Fila) => {
  const resultado = removeRow(bloqueDelTipo(tipo), fila.id, sistema);
  errores = resultado.errors;
  quitado = resultado.removed;
};

const quitarElBloque = (tipo: TipoBloque) => {
  const resultado = removeBlock(sistema, bloqueDelTipo(tipo).id);
  errores = resultado.errors;
  quitado = resultado.removed;
};

const cargarProducto = (nombre: string, valores: Valores = {}) => {
  productos[nombre] = guardar('inventory', { name: nombre, ...valores });
};

const stockDe = (nombre: string) => productos[nombre]?.quantity;

export const cuandoSeArmaLaPlantilla = (
  clave: ClavePlantilla,
  opciones: Partial<OpcionesPlantilla> = {},
  conEjemplos = true,
) => {
  sistema = makeTemplate(clave, opciones, conEjemplos);
  productos = {};
};

export const dadaLaPlantilla = cuandoSeArmaLaPlantilla;

export const entoncesTraeLasPiezas = (tipos: TipoBloque[]) => {
  expect(allBlocks(sistema).map((bloque) => bloque.type)).toEqual(tipos);
};

export const entoncesLosAnchosSon = (anchos: Partial<Record<TipoBloque, Ancho>>) => {
  const reales = Object.fromEntries(
    Object.keys(anchos).map((tipo) => [tipo, bloqueDelTipo(tipo as TipoBloque).width]),
  );
  expect(reales).toEqual(anchos);
};

export const entoncesLosPedidosApuntanALosClientes = () => {
  expect(destinoDe('orders', 'customer')).toBe(bloqueDelTipo('clients').id);
};

export const entoncesLasEntradasYSalidasApuntanAlInventario = () => {
  expect([destinoDe('entries', 'product'), destinoDe('exits', 'product')]).toEqual([
    bloqueDelTipo('inventory').id,
    bloqueDelTipo('inventory').id,
  ]);
};

export const entoncesLasVentasApuntanAlInventarioYALosClientesYDescuentanStock = () => {
  expect(destinoDe('sales', 'product')).toBe(bloqueDelTipo('inventory').id);
  expect(destinoDe('sales', 'customer')).toBe(bloqueDelTipo('clients').id);
  expect(bloqueDelTipo('sales').stockDirection).toBe(-1);
};

export const entoncesLasOpcionesSon = (opciones: OpcionesPlantilla) => {
  expect(sistema.options).toEqual(opciones);
};

export const entoncesElPrimerPedidoEsValido = () => {
  expect(validateRow(bloqueDelTipo('orders'), primeraFila('orders'), sistema)).toEqual({});
};

export const cuandoSeVendenFiadoTresDelPrimerProductoSinPrecio = () => {
  primerProducto = primeraFila('inventory');
  stockInicial = Number(primerProducto.quantity);
  venta = guardar('sales', {
    product: primerProducto.id,
    quantity: 3,
    price: '',
    customer: primeraFila('clients').id,
    credit: true,
  });
};

export const cuandoSeVendeUnoConPrecioManual = (precio: number) => {
  venta = guardar('sales', { product: primeraFila('inventory').id, quantity: 1, price: precio });
};

export const cuandoSeVendenTresConPrecioSugeridoYUnoManual = (precio: number) => {
  cuandoSeVendenFiadoTresDelPrimerProductoSinPrecio();
  cuandoSeVendeUnoConPrecioManual(precio);
};

export const entoncesLaVentaTieneElPrecio = (precio: number) => {
  expect(venta.price).toBe(precio);
};

export const entoncesElPrimerProductoBajo = (cantidad: number) => {
  expect(primerProducto.quantity).toBe(stockInicial - cantidad);
};

export const entoncesElClienteYaNoEsEjemplo = () => {
  expect(primeraFila('clients').__example).toBeUndefined();
};

export const entoncesLosIngresosSon = (ingresos: number) => {
  expect(totals(sistema).income).toBe(ingresos);
};

export const dadoUnAlmacenConAlfajoresYUnaVentaDe = (stock: number, vendidos: number) => {
  dadaLaPlantilla('shop');
  cargarProducto('Alfajor', { quantity: stock, price: 900 });
  venta = guardar('sales', { product: productos.Alfajor?.id ?? '', quantity: vendidos });
};

export const cuandoSeIntentaVender = (cantidad: number) => {
  intentarGuardar('sales', { product: productos.Alfajor?.id ?? '', quantity: cantidad });
};

export const cuandoSeCorrigeLaVentaA = (cantidad: number) => {
  guardar('sales', { product: productos.Alfajor?.id ?? '', quantity: cantidad }, venta.id);
};

export const cuandoSeQuitaLaVenta = () => quitar('sales', venta);

export const entoncesQuedanAlfajores = (cantidad: number) => {
  expect(stockDe('Alfajor')).toBe(cantidad);
};

export const entoncesSeRechazaPorFaltaDeStock = () => {
  expect(errores?.quantity).toBeTruthy();
};

export const entoncesSeQuito = () => {
  expect(quitado).toBe(true);
};

export const dadosUnProductoDeEjemploYOtroRealConUnaVentaCadaUno = () => {
  dadaLaPlantilla('shop', {}, false);
  cargarProducto('Alfajor', { quantity: 10 });
  cargarProducto('Galletitas', { quantity: 10 });
  const ventaDeEjemplo = guardar('sales', { product: productos.Alfajor?.id ?? '', quantity: 2 });
  venta = guardar('sales', { product: productos.Galletitas?.id ?? '', quantity: 2 });
  marcarComoEjemplos(productos.Alfajor, productos.Galletitas, ventaDeEjemplo);
};

const marcarComoEjemplos = (...filas: (Fila | undefined)[]) => {
  for (const fila of filas) if (fila) fila.__example = true;
};

export const cuandoSeQuitanAmbasVentas = () => {
  for (const fila of [...bloqueDelTipo('sales').rows]) quitar('sales', fila);
};

export const entoncesSigueSiendoEjemplo = (nombre: string) => {
  expect(productos[nombre]?.__example).toBe(true);
};

export const entoncesDejoDeSerEjemplo = (nombre: string) => {
  expect(productos[nombre]?.__example).toBeUndefined();
};

export const dadoUnDepositoConAzucarConUnaEntradaYUnaSalida = (
  stock: number,
  entran: number,
  salen: number,
) => {
  dadaLaPlantilla('warehouse', {}, false);
  cargarProducto('Azúcar', { quantity: stock, price: 1150 });
  entrada = guardar('entries', {
    product: productos['Azúcar']?.id ?? '',
    quantity: entran,
    date: '2026-10-16',
  });
  salida = guardar('exits', {
    product: productos['Azúcar']?.id ?? '',
    quantity: salen,
    date: '2026-10-16',
  });
};

export const cuandoSeIntentaSacar = (cantidad: number) => {
  intentarGuardar('exits', { product: productos['Azúcar']?.id ?? '', quantity: cantidad });
};

export const cuandoSeCorrigeLaSalidaA = (cantidad: number) => {
  guardar(
    'exits',
    { product: productos['Azúcar']?.id ?? '', quantity: cantidad, date: '2026-10-16' },
    salida.id,
  );
};

export const cuandoSeQuitanLaSalidaYLaEntrada = () => {
  quitar('exits', salida);
  quitar('entries', entrada);
};

export const entoncesHayAzucar = (cantidad: number) => {
  expect(stockDe('Azúcar')).toBe(cantidad);
};

export const entoncesQuedaUnaSolaSalida = () => {
  expect(bloqueDelTipo('exits').rows).toHaveLength(1);
};

export const dadosDosProductosYUnaEntradaDelPrimero = (
  stockA: number,
  stockB: number,
  entran: number,
) => {
  dadaLaPlantilla('warehouse', {}, false);
  cargarProducto('A', { quantity: stockA });
  cargarProducto('B', { quantity: stockB });
  entrada = guardar('entries', { product: productos.A?.id ?? '', quantity: entran });
};

export const cuandoLaEntradaPasaAlSegundoProductoCon = (cantidad: number) => {
  guardar('entries', { product: productos.B?.id ?? '', quantity: cantidad }, entrada.id);
};

export const dadaUnaEntradaDelSegundoProductoYaConsumida = () => {
  dadosDosProductosYUnaEntradaDelPrimero(5, 3, 7);
  cuandoLaEntradaPasaAlSegundoProductoCon(4);
  guardar('exits', { product: productos.B?.id ?? '', quantity: 6 });
};

export const cuandoSeQuitaLaEntrada = () => quitar('entries', entrada);

export const entoncesLosStocksSon = (stocks: Record<string, number>) => {
  const reales = Object.fromEntries(Object.keys(stocks).map((nombre) => [nombre, stockDe(nombre)]));
  expect(reales).toEqual(stocks);
};

export const dadaHarinaConUnaEntradaDeEjemplo = (stock: number, entran: number) => {
  dadaLaPlantilla('warehouse', {}, false);
  cargarProducto('Harina', { quantity: stock });
  marcarComoEjemplos(guardar('entries', { product: productos.Harina?.id ?? '', quantity: entran }));
};

export const dadaHarinaConEntradasYaConsumidas = (stock: number, entran: number, salen: number) => {
  dadaLaPlantilla('warehouse', {}, false);
  cargarProducto('Harina', { quantity: stock });
  guardar('entries', { product: productos.Harina?.id ?? '', quantity: entran });
  guardar('exits', { product: productos.Harina?.id ?? '', quantity: salen });
};

export const cuandoSeQuitanLosEjemplos = () => {
  quitadosAlLimpiar = clearExamples(sistema).removed;
};

export const cuandoSeQuitaElBloqueDe = (tipo: TipoBloque) => quitarElBloque(tipo);

export const cuandoSeQuitanLasSalidasYDespuesLasEntradas = () => {
  quitarElBloque('exits');
  quitarElBloque('entries');
};

export const entoncesSeQuitoLaEntradaDeEjemplo = () => {
  expect(quitadosAlLimpiar).toHaveLength(1);
  expect(bloqueDelTipo('entries').rows).toHaveLength(0);
};

export const entoncesHayHarina = (cantidad: number) => {
  expect(stockDe('Harina')).toBe(cantidad);
};

export const entoncesLasEntradasSiguenCargadas = () => {
  expect(bloqueDelTipo('entries').rows).toHaveLength(1);
};

export const cuandoSePoneElStockAlLadoDeLosClientes = () => {
  instantaneaDeLosPedidos = JSON.stringify(bloqueDelTipo('orders').rows);
  separado = splitBlocks(
    sistema,
    bloqueDelTipo('inventory').id,
    bloqueDelTipo('clients').id,
    'right',
  );
};

export const cuandoSeSeparaYSeVuelveAlMosaico = () => {
  cuandoSePoneElStockAlLadoDeLosClientes();
  applyLayout(sistema, 'mosaic');
};

export const entoncesQuedanLadoALadoEmpezandoPorLosClientes = () => {
  expect(separado).toBe(true);
  expect([bloqueDelTipo('inventory').width, bloqueDelTipo('clients').width]).toEqual([
    'half',
    'half',
  ]);
  expect(bloqueDelTipo('clients').startRow).toBe(true);
  expect(sistema.sections[0]?.blocks[2]?.id).toBe(bloqueDelTipo('inventory').id);
};

export const entoncesLosPedidosNoCambiaron = () => {
  expect(JSON.stringify(bloqueDelTipo('orders').rows)).toBe(instantaneaDeLosPedidos);
};

export const entoncesElStockVuelveAOcuparTodoElAncho = () => {
  expect(bloqueDelTipo('inventory').width).toBe('full');
  expect(bloqueDelTipo('clients').startRow).toBeUndefined();
};
