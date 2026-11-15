import { expect } from 'vitest';
import { aplicarAcciones, sumarPendientes, type AccionDeSami } from '@/lib/sistema/acciones';
import { allBlocks, upsertRow } from '@/lib/sistema/core';
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

export const dadoUnCuadernoConUnaVelaDeEjemploYUnaClientaReal = (sistema: Sistema): Fila => {
  aplicarAcciones(sistema, [
    accion('agregar_pieza', {
      tipo: 'inventory',
      nombre: 'Velas',
      ejemplos: [{ Producto: 'Vela lavanda', Cantidad: 5 }],
    }),
    accion('agregar_pieza', { tipo: 'clients', nombre: 'Clientas', ejemplos: [] }),
  ]);
  piezaLlamada(sistema, 'Clientas').rows = [];
  return upsertRow(piezaLlamada(sistema, 'Clientas'), { name: 'Rosa' }, null, sistema).row as Fila;
};

export const cuandoSeCarganTresVentasPorNombre = (sistema: Sistema): Resultado =>
  aplicarAcciones(sistema, [
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

export const cuandoSeSumanLosPendientes = (sistema: Sistema, resultado: Resultado) =>
  sumarPendientes(sistema, resultado.pendientes);

export const entoncesSoloRosaQuedaVinculada = (sistema: Sistema, rosa: Fila) => {
  expect(vinculosDeLasVentas(sistema)).toEqual([
    ['', rosa.id],
    ['', ''],
    ['', ''],
  ]);
};

export const entoncesNoSeCreoNingunRegistroNuevo = (sistema: Sistema) => {
  expect(registrosReales(sistema, 'Velas')).toEqual([]);
  expect(registrosReales(sistema, 'Clientas').map((fila) => fila.name)).toEqual(['Rosa']);
};

export const entoncesLosFaltantesQuedanPendientesUnaSolaVez = (resultado: Resultado) => {
  const pendientes = resultado.pendientes.map((item) => [item.nombre, item.filas.length]);
  expect(pendientes).toEqual([
    ['Vela lavanda', 1],
    ['Vela vainilla', 2],
    ['Lucía', 2],
  ]);
};

export const entoncesLasVentasQuedanVinculadasALosRegistrosNuevos = (
  sistema: Sistema,
  rosa: Fila,
) => {
  const velas = registrosReales(sistema, 'Velas');
  const lucia = piezaLlamada(sistema, 'Clientas').rows.find((fila) => fila.name === 'Lucía');
  expect(velas.map((fila) => fila.name)).toEqual(['Vela lavanda', 'Vela vainilla']);
  expect(vinculosDeLasVentas(sistema)).toEqual([
    [velas[0]?.id, rosa.id],
    [velas[1]?.id, lucia?.id],
    [velas[1]?.id, lucia?.id],
  ]);
};
