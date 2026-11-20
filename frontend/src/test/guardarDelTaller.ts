import { expect } from 'vitest';
import { ajusteParaGuardar, mismoSistema, sistemaParaCrear } from '@/lib/sistema/acciones';
import { allBlocks, clone, makeTemplate } from '@/lib/sistema/core';
import type { Fila, Sistema } from '@/lib/sistema/tipos';
import {
  agregarPieza,
  cargarRegistros,
  dadoQueSamiYaCargoYSeSumaronLosPendientes,
  dadoQueSamiYaPidio,
  editarCampo,
  piezaDelTipo,
  taller,
} from './accionesDeSami';

let original: Sistema;
let resultado: Sistema;
let ejemploDeCliente: Fila;

const filasDe = (sistema: Sistema) => allBlocks(sistema).flatMap((bloque) => bloque.rows);

const idsDeLosClientes = (sistema: Sistema) =>
  piezaDelTipo('clients', sistema).rows.map((fila) => fila.id);

export const dadoUnBorradorConGastosUnClienteDeEjemploNuevoYElTelefonoRenombrado = () => {
  original = makeTemplate('shop', {}, true);
  piezaDelTipo('clients', original).rows.push({ id: 'real', name: 'Rosa', phone: '11 4444-0000' });
  piezaDelTipo('sales', original).rows.push({ id: 'venta', __example: true, quantity: 1 });
  taller.sistema = { ...clone(original), name: '  ', id: 'otro' };
  dadoQueSamiYaPidio(
    agregarPieza('expenses'),
    editarCampo('clients', 'Teléfono', { etiqueta: 'Celular' }),
  );
  piezaDelTipo('clients').rows.push({ id: 'nueva', __example: true, name: 'Nueva' });
  (piezaDelTipo('sales').rows[0] as Fila).customer = 'nueva';
};

export const dadoUnAlmacenSinCambios = () => {
  original = makeTemplate('shop', {}, true);
  taller.sistema = clone(original);
};

export const dadoUnBorradorConUnaVentaLeidaDeUnaFoto = () => {
  original = makeTemplate('shop', {}, false);
  taller.sistema = clone(original);
  dadoQueSamiYaCargoYSeSumaronLosPendientes(
    cargarRegistros('sales', [
      { Producto: 'Vela lavanda', Cliente: 'Lucía', Cantidad: 2, Precio: 13000 },
    ]),
  );
};

export const dadoUnBorradorConDosVentasRealesUnaApuntandoAUnEjemplo = () => {
  taller.sistema = { ...makeTemplate('shop', {}, true), name: '  ' };
  ejemploDeCliente = piezaDelTipo('clients').rows[0] as Fila;
  dadoQueSamiYaCargoYSeSumaronLosPendientes(
    cargarRegistros('sales', [
      { Producto: 'Vela lavanda', Cliente: 'Lucía', Cantidad: 2 },
      { Producto: 'Vela vainilla', Cantidad: 1 },
    ]),
  );
  const [, segunda] = piezaDelTipo('sales').rows.filter((fila) => !fila.__example);
  if (segunda) segunda.customer = ejemploDeCliente.id;
};

export const dadoUnAlmacenDeEjemploComoBorrador = () => {
  taller.sistema = makeTemplate('shop', {}, true);
};

export const cuandoSeAjustaParaGuardar = () => {
  resultado = ajusteParaGuardar(original, taller.sistema);
};

export const cuandoSeCreaDesdeElBorrador = () => {
  resultado = sistemaParaCrear(taller.sistema);
};

export const entoncesConservaLaIdentidadDelOriginal = () => {
  expect(resultado).toMatchObject({
    id: original.id,
    createdAt: original.createdAt,
    name: original.name,
  });
};

export const entoncesLaPiezaSumadaQuedaSinFilas = (tipo: string) => {
  expect(piezaDelTipo(tipo, resultado).rows).toEqual([]);
};

export const entoncesLosClientesSonLosDelOriginal = () => {
  expect(idsDeLosClientes(resultado)).toEqual(idsDeLosClientes(original));
};

export const entoncesElTelefonoSeLlamaCelular = () => {
  expect(piezaDelTipo('clients', resultado).fields[1]?.label).toBe('Celular');
};

export const entoncesLaVentaQuedaSinCliente = () => {
  expect(piezaDelTipo('sales', resultado).rows[0]?.customer).toBe('');
};

export const entoncesElBorradorConservaSusGastos = () => {
  expect(piezaDelTipo('expenses').rows.length).toBeGreaterThan(0);
};

export const entoncesQuedaIgualAlOriginal = () => {
  expect(mismoSistema(resultado, original)).toBe(true);
};

export const entoncesLaVentaGuardadaApuntaASuVelaYSuClienta = () => {
  const [venta] = piezaDelTipo('sales', resultado).rows;
  const [vela] = piezaDelTipo('inventory', resultado).rows;
  const [lucia] = piezaDelTipo('clients', resultado).rows;
  expect([vela?.name, lucia?.name]).toEqual(['Vela lavanda', 'Lucía']);
  expect(venta).toMatchObject({
    product: vela?.id,
    customer: lucia?.id,
    quantity: 2,
    price: 13000,
  });
};

export const entoncesNadaEsEjemplo = () => {
  expect(filasDe(resultado).some((fila) => fila.__example)).toBe(false);
};

export const entoncesSeLlamaMiSeccionConElMismoId = () => {
  expect([resultado.name, resultado.id]).toEqual(['Mi sección', taller.sistema.id]);
};

export const entoncesConservaLosRegistrosReales = () => {
  expect(piezaDelTipo('inventory', resultado).rows.map((fila) => fila.name)).toEqual([
    'Vela lavanda',
    'Vela vainilla',
  ]);
  expect(piezaDelTipo('clients', resultado).rows.map((fila) => fila.name)).toEqual(['Lucía']);
};

export const entoncesElVinculoAUnEjemploQuedaVacio = () => {
  const lucia = piezaDelTipo('clients', resultado).rows[0]?.id;
  expect(piezaDelTipo('sales', resultado).rows.map((fila) => fila.customer)).toEqual([lucia, '']);
};

export const entoncesLaPropuestaQuedaComoEstaba = () => {
  expect(piezaDelTipo('clients').rows[0]).toBe(ejemploDeCliente);
};

export const entoncesNingunaPiezaTieneFilas = () => {
  expect(filasDe(resultado)).toEqual([]);
};
