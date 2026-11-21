import { expect } from 'vitest';
import { sistemaParaCrear } from '@/lib/sistema/acciones';
import { allBlocks } from '@/lib/sistema/core';
import type { Fila, Sistema } from '@/lib/sistema/tipos';
import {
  agregarPieza,
  borrarRegistros,
  cargarRegistros,
  cuandoSamiPide,
  dadoQueSamiYaCargoYSeSumaronLosPendientes,
  dadoQueSamiYaPidio,
  dadoUnSistemaVacio,
  editarRegistros,
  piezaLlamada,
  taller,
} from './accionesDeSami';

let ejemploDeCliente: Fila | undefined;
let ejemploDeGasto: Fila | undefined;
let filasDeVentasAntes: string;
let creado: Sistema;

const ventas = () => piezaLlamada('Ventas');
const clientes = () => piezaLlamada('Clientes');
const clienteLlamado = (nombre: string) => clientes().rows.find((fila) => fila.name === nombre);

export const dadasVentasYClientesDeEjemplo = () => {
  dadoUnSistemaVacio();
  dadoQueSamiYaPidio(
    agregarPieza('sales', { ejemplos: [{ Producto: 'Yerba', Cantidad: 2 }] }),
    agregarPieza('clients', { ejemplos: [{ Nombre: 'Lucía' }] }),
  );
};

export const dadasTresVentasCargadasDeAntoYVero = () => {
  dadasVentasYClientesDeEjemplo();
  dadoQueSamiYaCargoYSeSumaronLosPendientes(
    cargarRegistros('sales', [
      { Producto: 'Almohaditas', Cantidad: 0.5, Precio: 3200, Cliente: 'Anto Sdani' },
      { Producto: 'Bolitas', Cantidad: 1, Precio: 2900, Cliente: 'Anto Sdani' },
      { Producto: 'Almohaditas', Cantidad: 1, Precio: 3200, Cliente: 'Vero Susy' },
    ]),
  );
};

export const cuandoSeBorraPorIdLaPrimeraVentaYLaSegundaJuntoConLasDeVero = () => {
  const [primera, segunda] = ventas().rows;
  cuandoSamiPide(
    borrarRegistros('sales', { ids: [primera?.id, 'e-nada', 3] }),
    borrarRegistros('sales', { ids: [segunda?.id], donde: { Cliente: 'Vero Susy' } }),
  );
};

export const cuandoSeBorraPorIdUnClienteDeEjemplo = () => {
  ejemploDeCliente = clientes().rows.find((fila) => fila.__example);
  cuandoSamiPide(borrarRegistros('clients', { ids: [ejemploDeCliente?.id] }));
};

export const entoncesQuedanVentas = (cantidad: number) => {
  expect(ventas().rows).toHaveLength(cantidad);
};

export const entoncesQuedaUnaSolaVentaConSuProducto = () => {
  expect(ventas().rows).toHaveLength(1);
  expect(ventas().rows[0]?.product).toBeDefined();
};

export const entoncesQuedanVentasRealesSinEjemplos = (cantidad: number) => {
  expect(ventas().rows).toHaveLength(cantidad);
  expect(ventas().rows.filter((fila) => fila.__example)).toHaveLength(0);
};

export const entoncesAntoYaNoEstaNiEnLasVentas = () => {
  expect(clienteLlamado('Anto Sdani')).toBeUndefined();
  expect(ventas().rows.map((fila) => fila.customer)).not.toContain(undefined);
  expect(ventas().rows.filter((fila) => fila.customer === '')).toHaveLength(2);
};

export const entoncesElClienteDeEjemploSigue = () => {
  expect(clientes().rows).toContain(ejemploDeCliente);
};

export const dadasDosVentasCargadasAAntoYAVero = () => {
  dadoUnSistemaVacio();
  dadoQueSamiYaCargoYSeSumaronLosPendientes(
    agregarPieza('sales'),
    agregarPieza('clients'),
    cargarRegistros('sales', [
      { Producto: 'Almohaditas', Cantidad: 1, Precio: 3200, Cliente: 'Anto' },
      { Producto: 'Bolitas', Cantidad: 1, Precio: 2900, Cliente: 'Vero' },
    ]),
  );
};

export const cuandoSeCorrigenElPrecioDeUnaYLaCantidadDeOtra = () => {
  const [primera, segunda] = ventas().rows;
  cuandoSamiPide(
    editarRegistros('sales', [
      { id: primera?.id, datos: { precio: '3500', Color: 'Rojo' } },
      { id: segunda?.id, datos: { Cantidad: '0,5' } },
    ]),
  );
};

export const cuandoSeCorrigeElClienteDeLaPrimeraVentaA = (nombre: string) => {
  cuandoSamiPide(
    editarRegistros('sales', [{ id: ventas().rows[0]?.id, datos: { Cliente: nombre } }]),
  );
};

export const cuandoSeIntentanCorregirRegistrosQueNoSePuedenCorregir = () => {
  filasDeVentasAntes = JSON.stringify(ventas().rows);
  dadoQueSamiYaPidio(agregarPieza('expenses', { ejemplos: [{ Concepto: 'Luz' }] }));
  ejemploDeGasto = piezaLlamada('Gastos').rows[0];
  const primera = ventas().rows[0]?.id;
  cuandoSamiPide(
    editarRegistros('sales', [
      { id: 'e-nada', datos: { Precio: 1 } },
      { id: primera, datos: { Color: 'Rojo' } },
      { id: primera, datos: 'Precio 1' },
      'nada',
    ]),
    editarRegistros('expenses', [{ id: ejemploDeGasto?.id, datos: { Concepto: 'Gas' } }]),
    editarRegistros('nada', []),
  );
};

export const entoncesLasVentasQuedanCorregidasSinSumarFilas = () => {
  const [primera, segunda] = ventas().rows;
  expect(ventas().rows).toHaveLength(2);
  expect([primera?.price, primera?.quantity, segunda?.quantity]).toEqual([3500, 1, 0.5]);
};

export const entoncesLaPrimeraVentaEsDe = (nombre: string) => {
  expect(ventas().rows[0]?.customer).toBe(clienteLlamado(nombre)?.id);
};

export const entoncesLaPrimeraVentaQuedaSinCliente = () => {
  expect(ventas().rows[0]?.customer).toBe('');
};

export const entoncesQuedaPendienteSinCrearse = (nombre: string) => {
  expect(taller.resultado.pendientes).toEqual([
    {
      destinoId: clientes().id,
      nombre,
      filas: [{ bloqueId: ventas().id, filaId: ventas().rows[0]?.id, campoKey: 'customer' }],
    },
  ]);
  expect(clienteLlamado(nombre)).toBeUndefined();
};

export const entoncesLasVentasYElEjemploQuedanComoEstaban = () => {
  expect(JSON.stringify(ventas().rows)).toBe(filasDeVentasAntes);
  expect(ejemploDeGasto?.name).toBe('Luz');
};

export const cuandoSeCargaUnaVentaReal = () => {
  cuandoSamiPide(cargarRegistros('sales', [{ Producto: 'Yerba', Cantidad: 1 }]));
};

export const entoncesLasVentasSoloTienenElRegistroReal = () => {
  expect(ventas().rows.some((fila) => fila.__example)).toBe(false);
  expect(ventas().rows).toHaveLength(1);
};

export const entoncesLosClientesSiguenSiendoEjemplos = () => {
  expect(clientes().rows.every((fila) => fila.__example)).toBe(true);
};

export const cuandoSeCreaElSistema = () => {
  creado = sistemaParaCrear(taller.sistema);
};

export const entoncesLoCreadoNoTieneEjemplosYConservaLaVenta = () => {
  const filas = allBlocks(creado).flatMap((bloque) => bloque.rows);
  expect(filas.some((fila) => fila.__example)).toBe(false);
  expect(allBlocks(creado).find((bloque) => bloque.type === 'sales')?.rows).toHaveLength(1);
};
