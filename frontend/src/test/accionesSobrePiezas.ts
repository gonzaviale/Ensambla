import { expect } from 'vitest';
import { mismoSistema, resumirSistema, type EstadoCompacto } from '@/lib/sistema/acciones';
import { allBlocks, clone } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import {
  agregarPieza,
  cargarRegistros,
  conectar,
  dadoQueSamiYaCargoYSeSumaronLosPendientes,
  dadoQueSamiYaPidio,
  dadoUnAlmacenDeEjemplo,
  piezaDelTipo,
  taller,
} from './accionesDeSami';

let resumen: EstadoCompacto;
let copia: Sistema;

const piezaResumida = (tipo: string) => resumen.piezas.find((item) => item.tipo === tipo);

const campoDe = (tipo: string, clave: string) =>
  piezaDelTipo(tipo).fields.find((campo) => campo.key === clave || campo.label === clave);

export const dadoUnAlmacenDeEjemploDeUnaSolaColumna = () => {
  dadoUnAlmacenDeEjemplo();
  taller.sistema.layout = 'single';
};

export const dadoUnAlmacenSinPestanas = () => {
  dadoUnAlmacenDeEjemplo();
  taller.sistema.sections = [];
};

export const dadoUnAlmacenConDosVentasCargadas = () => {
  dadoUnAlmacenDeEjemplo();
  dadoQueSamiYaCargoYSeSumaronLosPendientes(
    cargarRegistros('sales', [
      { Producto: 'Yerba', Cantidad: 2, Cliente: 'Anto' },
      { Producto: 'Fideos', Cantidad: 1 },
    ]),
  );
};

export const dadoUnAlmacenConUnaVentaAAntoGuardada = () => {
  dadoUnAlmacenDeEjemplo();
  dadoQueSamiYaCargoYSeSumaronLosPendientes(
    cargarRegistros('sales', [{ Producto: 'Yerba', Cantidad: 2, Cliente: 'Anto' }]),
  );
};

export const dadoUnAlmacenConUnaVentaDeUnProductoQueNoExiste = () => {
  dadoUnAlmacenDeEjemplo();
  dadoQueSamiYaPidio(cargarRegistros('sales', [{ Producto: 'Yerba', Cantidad: 2 }]));
  const [venta] = piezaDelTipo('sales').rows;
  if (venta) venta.product = 'e0000000-nada';
};

export const dadaUnaTablaDeTurnosConectadaALosClientes = () => {
  dadoUnAlmacenDeEjemplo();
  dadoQueSamiYaPidio(
    agregarPieza('form', { nombre: 'Turnos' }),
    conectar('Turnos', 'clients', { etiqueta: 'Clienta' }),
  );
};

export const cuandoSeResume = () => {
  resumen = resumirSistema(taller.sistema);
};

export const cuandoSeResumeConLaPrimeraVentaYaGuardada = () => {
  resumen = resumirSistema(taller.sistema, new Set([piezaDelTipo('sales').rows[0]?.id ?? '']));
};

export const cuandoSeResumeConTodoGuardado = () => {
  const guardadas = allBlocks(taller.sistema).flatMap((bloque) =>
    bloque.rows.map((fila) => fila.id),
  );
  resumen = resumirSistema(taller.sistema, new Set(guardadas));
};

export const cuandoSeCopiaConOtraFecha = () => {
  copia = { ...clone(taller.sistema), updatedAt: 'otra fecha' };
};

export const cuandoSeCopiaConOtroNombre = () => {
  copia = { ...clone(taller.sistema), name: 'Otro nombre' };
};

export const entoncesElResumenTraeNombreDistribucionYPiezas = () => {
  expect(resumen.nombre).toBe(taller.sistema.name);
  expect(resumen.distribucion).toBe('mosaic');
  expect(resumen.piezas.map((item) => item.tipo)).toEqual(['inventory', 'sales', 'clients']);
  expect(resumen.piezas[0]?.id).toBe(piezaDelTipo('inventory').id);
  expect(resumen.piezas[0]?.campos[0]).toEqual({ etiqueta: 'Producto', tipo: 'text' });
};

export const entoncesElResumenNoLlevaLasFilasDeEjemplo = () => {
  expect(JSON.stringify(resumen)).not.toContain('Yerba');
};

export const entoncesElResumenEsDeUnaColumnaConTodoEnLaPrimeraPestana = () => {
  expect(resumen.distribucion).toBe('single');
  expect(resumen.piezas.every((item) => item.seccion === taller.sistema.sections[0]?.name)).toBe(
    true,
  );
};

export const entoncesSoloViajaLaVentaNueva = () => {
  const nueva = piezaDelTipo('sales').rows[1];
  expect(piezaResumida('sales')?.registros).toEqual([
    { id: nueva?.id, datos: { Producto: 'Fideos', Cantidad: 1 } },
  ]);
};

export const entoncesLaPrimeraVentaViajaConSusDatosYSuClienta = () => {
  const primera = piezaDelTipo('sales').rows[0];
  expect(piezaResumida('sales')?.registros[0]).toEqual({
    id: primera?.id,
    datos: { Producto: 'Yerba', Cantidad: 2, Cliente: 'Anto' },
  });
};

export const entoncesLosProductosSumadosAlVincularViajanSinStock = () => {
  expect(piezaResumida('inventory')?.registros.map((registro) => registro.datos)).toEqual([
    { Producto: 'Yerba', Cantidad: 0 },
    { Producto: 'Fideos', Cantidad: 0 },
  ]);
};

export const entoncesLasPiezasVinculadasLlevanLosNombresQueTienen = () => {
  expect(piezaResumida('inventory')?.nombres).toEqual(['Yerba']);
  expect(piezaResumida('clients')?.nombres).toEqual(['Anto']);
};

export const entoncesLoGuardadoNoViajaComoRegistro = () => {
  expect(piezaResumida('clients')?.registros).toEqual([]);
};

export const entoncesUnaPiezaQueNadieVinculaNoLlevaNombres = () => {
  expect(piezaResumida('sales')).not.toHaveProperty('nombres');
};

export const entoncesLaVentaViajaSoloConSuCantidad = () => {
  expect(piezaResumida('sales')?.registros[0]?.datos).toEqual({ Cantidad: 2 });
};

export const entoncesLaPiezaTraeEjemplos = (tipo: string) => {
  expect(piezaDelTipo(tipo).rows.length).toBeGreaterThan(0);
};

export const entoncesLaPiezaNoTieneDatos = (tipo: string) => {
  expect(piezaDelTipo(tipo).fields).toEqual([]);
};

export const entoncesHayPiezas = (cantidad: number) => {
  expect(allBlocks(taller.sistema)).toHaveLength(cantidad);
};

export const entoncesElClienteDeLasVentasVuelveASerTextoSinValores = () => {
  const cliente = campoDe('sales', 'customer');
  expect(cliente).toMatchObject({ type: 'text' });
  expect(cliente?.target).toBeUndefined();
  expect(piezaDelTipo('sales').rows.every((fila) => !('customer' in fila))).toBe(true);
};

export const entoncesLaClientaDeLosTurnosVuelveASerTexto = () => {
  const clienta = campoDe('form', 'Clienta');
  expect(clienta).toMatchObject({ type: 'text' });
  expect(clienta?.target).toBeUndefined();
};

export const entoncesElProductoDeLasVentasSigueSiendoUnVinculoSinDestino = () => {
  const producto = campoDe('sales', 'product');
  expect(producto).toMatchObject({ type: 'relation' });
  expect(producto?.target).toBeUndefined();
};

export const entoncesLaPiezaYElSistemaSeLlaman = (pieza: string, sistema: string) => {
  expect(piezaDelTipo('sales').name).toBe(pieza);
  expect(taller.sistema.name).toBe(sistema);
};

export const entoncesLosNombresMiden = (deLaPieza: number, delSistema: number) => {
  expect(piezaDelTipo('sales').name).toHaveLength(deLaPieza);
  expect(taller.sistema.name).toHaveLength(delSistema);
};

export const entoncesLaPestanaTieneLasPiezas = (pestana: string, tipos: string[]) => {
  const seccion = taller.sistema.sections.find((item) => item.name === pestana);
  expect(seccion?.blocks.map((bloque) => bloque.type)).toEqual(tipos);
};

export const entoncesHayUnaSolaPestanaLlamada = (pestana: string) => {
  const iguales = taller.sistema.sections.filter(
    (item) => item.name.toLowerCase() === pestana.toLowerCase(),
  );
  expect(iguales).toHaveLength(1);
};

export const entoncesTodasLasPiezasOcupanTodoElAncho = () => {
  expect(taller.sistema.layout).toBe('single');
  expect(allBlocks(taller.sistema).every((bloque) => bloque.width === 'full')).toBe(true);
};

export const entoncesLaCopiaEsElMismoSistema = () => {
  expect(mismoSistema(taller.sistema, copia)).toBe(true);
};

export const entoncesLaCopiaEsOtroSistema = () => {
  expect(mismoSistema(taller.sistema, copia)).toBe(false);
};
