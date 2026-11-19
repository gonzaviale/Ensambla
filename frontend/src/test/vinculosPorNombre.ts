import { expect } from 'vitest';
import { aplicarAcciones, sumarPendientes, type AccionDeSami } from '@/lib/sistema/acciones';
import { allBlocks, makeSystem, upsertRow } from '@/lib/sistema/core';
import type { Fila, Sistema } from '@/lib/sistema/tipos';

type Resultado = ReturnType<typeof aplicarAcciones>;

const accion = (herramienta: string, argumentos: Record<string, unknown>): AccionDeSami => ({
  herramienta,
  argumentos,
});

export const piezaLlamada = (sistema: Sistema, nombre: string) => {
  const encontrada = allBlocks(sistema).find((bloque) => bloque.name === nombre);
  if (!encontrada) throw new Error(`Falta la pieza ${nombre}`);
  return encontrada;
};

const registrosReales = (sistema: Sistema, nombre: string) =>
  piezaLlamada(sistema, nombre).rows.filter((fila) => !fila.__example);

const vinculosDeLasVentas = (sistema: Sistema) =>
  piezaLlamada(sistema, 'Ventas').rows.map((fila) => [fila.product, fila.customer]);

let cuaderno: Sistema;
let rosa: Fila;
let resultado: Resultado;

export const dadoUnCuadernoConVelasDeEjemploYUnaClientaReal = () => {
  cuaderno = makeSystem('Mi sistema', [], false);
  aplicarAcciones(cuaderno, [
    accion('agregar_pieza', {
      tipo: 'inventory',
      nombre: 'Velas',
      ejemplos: [{ Producto: 'Vela lavanda', Cantidad: 5 }],
    }),
    accion('agregar_pieza', { tipo: 'clients', nombre: 'Clientas', ejemplos: [] }),
  ]);
  piezaLlamada(cuaderno, 'Clientas').rows = [];
  rosa = upsertRow(piezaLlamada(cuaderno, 'Clientas'), { name: 'Rosa' }, null, cuaderno)
    .row as Fila;
};

export const cuandoSeCarganTresVentasPorNombre = () => {
  resultado = aplicarAcciones(cuaderno, [
    accion('agregar_pieza', { tipo: 'sales', ejemplos: [] }),
    accion('cargar_registros', {
      pieza: 'Ventas',
      filas: [
        { Producto: 'Vela lavanda', Cliente: 'rosa', Cantidad: 2 },
        { Producto: 'Vela vainilla', Cliente: 'Lucía', Cantidad: 1 },
        { Producto: 'vela vainilla', Cliente: 'Lucía', Cantidad: 3 },
      ],
    }),
  ]);
};

export const cuandoSeSumanLosPendientes = () => {
  sumarPendientes(cuaderno, resultado.pendientes);
};

export const entoncesSoloRosaQuedaVinculada = () => {
  expect(vinculosDeLasVentas(cuaderno)).toEqual([
    ['', rosa.id],
    ['', ''],
    ['', ''],
  ]);
};

export const entoncesNoSeCreoNingunRegistroNuevo = () => {
  expect(registrosReales(cuaderno, 'Velas')).toEqual([]);
  expect(registrosReales(cuaderno, 'Clientas').map((fila) => fila.name)).toEqual(['Rosa']);
};

export const entoncesLosFaltantesQuedanPendientesUnaSolaVez = () => {
  const pendientes = resultado.pendientes.map((item) => [item.nombre, item.filas.length]);
  expect(pendientes).toEqual([
    ['Vela lavanda', 1],
    ['Vela vainilla', 2],
    ['Lucía', 2],
  ]);
};

export const entoncesLasVentasQuedanVinculadasALosRegistrosNuevos = () => {
  const velas = registrosReales(cuaderno, 'Velas');
  const lucia = piezaLlamada(cuaderno, 'Clientas').rows.find((fila) => fila.name === 'Lucía');
  expect(velas.map((fila) => fila.name)).toEqual(['Vela lavanda', 'Vela vainilla']);
  expect(vinculosDeLasVentas(cuaderno)).toEqual([
    [velas[0]?.id, rosa.id],
    [velas[1]?.id, lucia?.id],
    [velas[1]?.id, lucia?.id],
  ]);
};
