import { expect } from 'vitest';
import {
  ajusteParaGuardar,
  aplicarAcciones,
  sistemaParaCrear,
  sumarPendientes,
  type SinStock,
  type VinculoPendiente,
} from '@/lib/sistema/acciones';
import { allBlocks, clone } from '@/lib/sistema/core';
import type { Fila, Sistema } from '@/lib/sistema/tipos';
import {
  agregarPieza,
  borrarRegistros,
  cargarRegistros,
  cuandoSamiPide,
  dadoQueSamiYaPidio,
  dadoUnAlmacenSinEjemplos,
  editarRegistros,
  piezaDelTipo,
  taller,
} from './accionesDeSami';

const YERBA = 'e1111111-yerba';
const MATE = 'e2222222-mate';

let faltantes: SinStock[] = [];
let pendientes: VinculoPendiente[] = [];
let original: Sistema;
let preparados: Sistema[] = [];

const stockDe = (nombre: string) =>
  piezaDelTipo('inventory').rows.find((fila) => fila.name === nombre)?.quantity;

const sumarFilas = (tipo: string, ...filas: Fila[]) => {
  piezaDelTipo(tipo).rows.push(...filas);
};

export const dadoUnAlmacenConYerbaYMate = () => {
  dadoUnAlmacenSinEjemplos();
  sumarFilas(
    'inventory',
    { id: YERBA, name: 'Yerba', quantity: 5 },
    { id: MATE, name: 'Mate', quantity: 1 },
  );
};

export const dadasEntradasSinFilas = () => {
  dadoUnAlmacenConYerbaYMate();
  dadoQueSamiYaPidio(agregarPieza('entries', { ejemplos: [] }));
};

export const dadaUnaVentaDeSami = (producto: string, cantidad: number) => {
  dadoUnAlmacenConYerbaYMate();
  dadoQueSamiYaPidio(cargarRegistros('sales', [{ Producto: producto, Cantidad: cantidad }]));
};

export const dadaUnaVentaGuardadaDeDosYerbas = () => {
  dadoUnAlmacenConYerbaYMate();
  sumarFilas('sales', { id: 'e3333333-vta', product: YERBA, quantity: 2 });
};

export const dadasEntradasGuardadasDeYerbaYMate = () => {
  dadasEntradasSinFilas();
  sumarFilas(
    'entries',
    { id: 'e4444444-ent', product: YERBA, quantity: 2 },
    { id: 'e5555555-ent', product: MATE, quantity: 4 },
  );
};

export const dadasUnaVentaDeEjemploYOtraSinProducto = () => {
  dadoUnAlmacenConYerbaYMate();
  sumarFilas(
    'sales',
    { id: 'e6666666-ej', product: YERBA, quantity: 2, __example: true },
    { id: 'e7777777-sin', product: '', quantity: 2 },
  );
};

export const dadoUnClienteGuardado = () => {
  dadoUnAlmacenConYerbaYMate();
  sumarFilas('clients', { id: 'e8888888-cli', name: 'Ana' });
};

export const dadaUnaVentaDeUnProductoQueNoEsta = () => {
  dadoUnAlmacenConYerbaYMate();
  pendientes = dadoQueSamiYaPidio(
    cargarRegistros('sales', [{ Producto: 'Bombilla', Cantidad: 2 }]),
  ).pendientes;
};

export const dadaUnaVentaDeSamiSobreUnAlmacenGuardado = () => {
  dadoUnAlmacenConYerbaYMate();
  original = clone(taller.sistema);
  dadoQueSamiYaPidio(cargarRegistros('sales', [{ Producto: 'Yerba', Cantidad: 2 }]));
};

export const cuandoSeVende = (...filas: Record<string, unknown>[]) => {
  cuandoSamiPide(cargarRegistros('sales', filas));
  faltantes = taller.resultado.sinStock;
};

export const cuandoSeCorrigeLaVentaAProductoYCantidad = (producto: string, cantidad: number) => {
  const [venta] = piezaDelTipo('sales').rows;
  cuandoSamiPide(
    editarRegistros('sales', [
      { id: venta?.id, datos: { Producto: producto, Cantidad: cantidad } },
    ]),
  );
};

export const cuandoSeBorraLaVenta = () => {
  cuandoSamiPide(borrarRegistros('sales', { ids: [piezaDelTipo('sales').rows[0]?.id] }));
};

export const cuandoSeBorranLaSinProductoYLaDeEjemplo = () => {
  aplicarAcciones(taller.sistema, [borrarRegistros('sales', { ids: ['e7777777-sin'] })]);
  cuandoSamiPide(borrarRegistros('sales', { donde: { Cantidad: 2 } }));
};

export const cuandoSeSumaElProductoPendiente = () => {
  faltantes = sumarPendientes(taller.sistema, pendientes);
};

export const cuandoSePreparaParaGuardarYParaCrear = () => {
  preparados = [ajusteParaGuardar(original, taller.sistema), sistemaParaCrear(taller.sistema)];
};

export const entoncesLosStocksSon = (stocks: Record<string, number>) => {
  const reales = Object.fromEntries(Object.keys(stocks).map((nombre) => [nombre, stockDe(nombre)]));
  expect(reales).toEqual(stocks);
};

export const entoncesNoFaltoStock = () => {
  expect(faltantes).toEqual([]);
};

export const entoncesFaltoStockDe = (producto: string, pedido: number, habia: number) => {
  expect(faltantes).toEqual([{ producto, pedido, habia }]);
};

export const entoncesNoQuedanVentas = () => {
  expect(piezaDelTipo('sales').rows).toEqual([]);
};

export const entoncesElInventarioSigueConSusProductos = (cantidad: number) => {
  expect(piezaDelTipo('inventory').rows).toHaveLength(cantidad);
};

export const entoncesNingunaFilaLlevaMarcasDeStock = () => {
  const filas = preparados
    .flatMap((sistema) => allBlocks(sistema))
    .flatMap((bloque) => bloque.rows);
  expect(filas.some((fila) => '__stockDe' in fila || '__stockDelta' in fila)).toBe(false);
};

export const entoncesLoGuardadoConservaElStockDescontado = () => {
  const [guardado] = preparados;
  const inventario = allBlocks(guardado).find((bloque) => bloque.type === 'inventory');
  expect(inventario?.rows.find((fila) => fila.name === 'Yerba')?.quantity).toBe(3);
};
