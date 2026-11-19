/// <reference types="node" />
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { expect } from 'vitest';
import { calcularMetricaDeEjemplo, fuentesDe } from '@/mocks/db/metricas';
import { interpretarPedido, type PedidoEntendido } from '@/mocks/db/pedidosDeMetricas';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import type { Sistema } from '@/lib/sistema/tipos';
import type { DefinicionDeMetrica, FuenteDeMetrica } from '@/types/metricas';

// Helpers para mirar el intérprete y el cálculo de los mocks como los mira la persona: un pedido en palabras y lo
// que el almacén de demo responde.

const DICCIONARIO_DEL_SERVIDOR = resolve(
  process.cwd(),
  '../backend/src/main/resources/metricas/diccionario-pedidos.json',
);
const DICCIONARIO_DE_LOS_MOCKS = resolve(process.cwd(), 'src/mocks/db/diccionario-pedidos.json');

const leerJson = (archivo: string): unknown => JSON.parse(readFileSync(archivo, 'utf8'));

export const entoncesLosDiccionariosSonIguales = () =>
  expect(leerJson(DICCIONARIO_DE_LOS_MOCKS)).toEqual(leerJson(DICCIONARIO_DEL_SERVIDOR));

let respuesta: PedidoEntendido | { motivo: string };

export const cuandoSePideAlMock = (pedido: string) => {
  respuesta = interpretarPedido(pedido);
};

const entendido = () => {
  if ('motivo' in respuesta) throw new Error(`No lo entendió: ${respuesta.motivo}`);
  return respuesta;
};

/** La receta sin el nombre: fuente, cálculo, reparto, período y fiado, en ese orden. */
export const entoncesArmaLaReceta = (receta: Omit<DefinicionDeMetrica, 'nombre'>) =>
  expect(entendido().definicion).toEqual({ ...receta, nombre: entendido().definicion.nombre });

export const entoncesNoEntendioLasPalabras = (palabras: string[]) =>
  expect(entendido().lectura.noEntendidas).toEqual(palabras);

export const entoncesNoSeEntiendePorque = (motivo: string) =>
  expect(respuesta).toEqual({ motivo: expect.stringContaining(motivo) as string });

let almacen: Sistema;

export const dadoElAlmacenDeDemo = () => {
  almacen = crearAlmacenCargado(hoy());
};

export const entoncesElAlmacenCalculaLoQueSePidio = () =>
  expect(calcularMetricaDeEjemplo(entendido().definicion, almacen, hoy()).valor).toEqual(
    expect.any(Number),
  );

export const entoncesSeArmaDe = (fuentes: FuenteDeMetrica[]) =>
  expect(fuentesDe(almacen)).toEqual(fuentes);
