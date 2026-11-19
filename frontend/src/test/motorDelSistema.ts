import { expect } from 'vitest';
import {
  allBlocks,
  clone,
  id,
  importRows,
  makeSystem,
  moveBlock,
  totals,
  upsertRow,
  validateRow,
  type ResultadoFila,
} from '@/lib/sistema/core';
import type {
  Bloque,
  ErroresFila,
  Fila,
  Seccion,
  Sistema,
  TipoBloque,
  Valor,
} from '@/lib/sistema/tipos';

let sistema: Sistema;
let otroSistema: Sistema;
let otraPestana: Seccion;
let instantaneaDeLosPedidos: string;
let errores: ErroresFila;
let resultado: ResultadoFila;
let importacion: ReturnType<typeof importRows>;
let movido: boolean;
let resumen: ReturnType<typeof totals>;
let copia: Sistema;

export const bloqueDelTipo = (tipo: TipoBloque, en: Sistema = sistema): Bloque => {
  const encontrado = allBlocks(en).find((bloque) => bloque.type === tipo);
  if (!encontrado) throw new Error(`Falta el bloque ${tipo}`);
  return encontrado;
};

const primera = <T>(items: readonly T[], indice = 0): T => {
  const item = items[indice];
  if (item === undefined) throw new Error(`Falta el elemento ${indice}`);
  return item;
};

const destinoDelCliente = (tipo: TipoBloque) =>
  bloqueDelTipo(tipo).fields.find((campo) => campo.key === 'customer')?.target;

const pestanaPrincipal = () => primera(sistema.sections);

const pedidos = () => bloqueDelTipo('orders');

export const dadoUnSistemaCon = (tipos: TipoBloque[], conEjemplos = false) => {
  sistema = makeSystem('Mi negocio', tipos, conEjemplos);
};

export const cuandoSeArmaUnSistemaConEjemplosDe = (tipos: TipoBloque[]) =>
  dadoUnSistemaCon(tipos, true);

export const cuandoSeArmanDosSistemasConClientes = () => {
  dadoUnSistemaCon(['clients']);
  otroSistema = makeSystem('Otro', ['clients']);
};

export const entoncesLosPedidosYLasVentasApuntanALosClientes = () => {
  expect(destinoDelCliente('orders')).toBe(bloqueDelTipo('clients').id);
  expect(destinoDelCliente('sales')).toBe(bloqueDelTipo('clients').id);
};

export const entoncesElPrimerPedidoEsDelPrimerClienteYEsValido = () => {
  const pedido = primera(pedidos().rows);
  expect(pedido.customer).toBe(primera(bloqueDelTipo('clients').rows).id);
  expect(validateRow(pedidos(), pedido, sistema)).toEqual({});
};

export const entoncesCadaSistemaTieneSusPropiosClientes = () => {
  expect(bloqueDelTipo('clients').id).not.toBe(bloqueDelTipo('clients', otroSistema).id);
};

export const cuandoSeValidaEn = (tipo: TipoBloque, valores: Record<string, Valor>) => {
  errores = validateRow(bloqueDelTipo(tipo), valores, sistema);
};

export const cuandoSeVendeFiadoSinCliente = () => {
  const producto = primera(bloqueDelTipo('inventory').rows);
  cuandoSeValidaEn('sales', { product: producto.id, quantity: 1, credit: true });
};

export const entoncesHayUnErrorEn = (clave: string) => {
  expect(errores[clave]).toBeTruthy();
};

export const entoncesElErrorDe = (clave: string, mensaje: string) => {
  expect(errores[clave]).toBe(mensaje);
};

export const entoncesNoHayErrores = () => {
  expect(errores).toEqual({});
};

export const cuandoSeRenombraElPrimerProducto = () => {
  const original = primera(bloqueDelTipo('inventory').rows);
  resultado = upsertRow(
    bloqueDelTipo('inventory'),
    { ...original, name: 'Mi producto' },
    original.id,
    sistema,
  );
};

export const cuandoSeCargaUnProductoSinPrecio = () => {
  resultado = upsertRow(
    bloqueDelTipo('inventory'),
    { name: 'Mi segundo producto', quantity: 0 },
    null,
    sistema,
  );
};

export const entoncesElProductoEditadoYaNoEsEjemplo = () => {
  expect(resultado.row?.__example).toBeUndefined();
  expect(bloqueDelTipo('inventory').rows.filter((fila) => !fila.__example)).toHaveLength(1);
};

export const entoncesElPrecioQuedaVacio = () => {
  expect(resultado.row?.price).toBe('');
};

export const dadosClientesYPedidosDeEjemploYOtraPestana = () => {
  dadoUnSistemaCon(['clients', 'orders'], true);
  otraPestana = { id: id(), name: 'Ventas', blocks: [] };
  sistema.sections.push(otraPestana);
  instantaneaDeLosPedidos = JSON.stringify(pedidos().rows);
};

export const cuandoSeMuevenLosPedidosALaOtraPestana = () => {
  movido = moveBlock(sistema, pedidos().id, otraPestana.id);
};

export const cuandoSeVuelvenAPonerLosPedidosAntesDeLosClientes = () => {
  cuandoSeMuevenLosPedidosALaOtraPestana();
  movido = moveBlock(sistema, pedidos().id, pestanaPrincipal().id, bloqueDelTipo('clients').id);
};

export const cuandoSeIntentaPonerLosPedidosAntesDeSiMismos = () => {
  movido = moveBlock(sistema, pedidos().id, pestanaPrincipal().id, pedidos().id);
};

export const entoncesLosPedidosEstanEnLaOtraPestanaConSusDatos = () => {
  const movidos = primera(otraPestana.blocks);
  expect(movido).toBe(true);
  expect(pestanaPrincipal().blocks).toHaveLength(1);
  expect(JSON.stringify(movidos.rows)).toBe(instantaneaDeLosPedidos);
  expect(movidos.fields.find((campo) => campo.key === 'customer')?.target).toBe(
    bloqueDelTipo('clients').id,
  );
};

export const entoncesLosPedidosQuedanPrimeros = () => {
  expect(movido).toBe(true);
  expect(primera(pestanaPrincipal().blocks).id).toBe(pedidos().id);
};

export const entoncesNoSeMovio = () => {
  expect(movido).toBe(false);
};

export const dadoUnInventarioConYerba = (cantidad: number) => {
  dadoUnSistemaCon(['inventory']);
  upsertRow(
    bloqueDelTipo('inventory'),
    { name: 'Yerba Playadito', quantity: cantidad },
    null,
    sistema,
  );
};

export const cuandoSeImportanYerbaRepetidaYAceite = () => {
  const leidas = [
    { name: ' yerba playadito ', quantity: 999 },
    { name: 'Aceite Natura', quantity: 7 },
  ];
  importacion = importRows(bloqueDelTipo('inventory'), leidas, sistema, true);
};

const filaDelInventario = (indice: number): Fila =>
  primera(bloqueDelTipo('inventory').rows, indice);

export const entoncesSeAgregoUnaYSeOmitioLaRepetidaSinPisarla = (cantidadOriginal: number) => {
  expect(importacion).toEqual({ added: 1, skipped: 1, errors: [] });
  expect(filaDelInventario(0).quantity).toBe(cantidadOriginal);
};

export const entoncesLaFilaImportadaVieneDeLaLectura = () => {
  expect(filaDelInventario(1).__source).toBe('Lectura simulada');
};

export const cuandoSeCalculanLosTotales = () => {
  resumen = totals(sistema);
};

export const entoncesLosTotalesSon = (esperados: Partial<ReturnType<typeof totals>>) => {
  expect(resumen).toMatchObject(esperados);
};

export const cuandoSeCopiaElSistema = () => {
  copia = clone(sistema);
};

export const entoncesLaCopiaEsIgualAlOriginal = () => {
  expect(copia).toEqual(sistema);
};
