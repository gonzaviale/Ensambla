import { expect } from 'vitest';
import { allBlocks, makeSystem } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import {
  destinoParaSumar,
  frasePorComercios,
  masVendidosPrimero,
  nombresDeInventario,
  porcentajeDeComercios,
  precioParaSumar,
  productosBuscados,
  productosPropios,
  yaLoTiene,
  type ProductoDeZona,
  type Zona,
} from '@/lib/sistema/zona';

export const dadoUnProductoDeZona = (
  nombre: string,
  vendenComercios = 5,
  unidadesPorSemana = 10,
  promedio = 1000,
): ProductoDeZona => ({
  nombre,
  comercios: 5,
  precio: { minimo: promedio - 100, promedio, maximo: promedio + 100 },
  ventas: { comercios: vendenComercios, unidadesPorSemana },
});

export const dadoUnaZonaCon = (productos: ProductoDeZona[], comercios = 10): Zona => ({
  nombre: 'San Justo',
  comercios,
  productos,
});

export const dadoUnSistemaConInventarioDe = (nombres: string[]): Sistema => {
  const sistema = makeSystem('Mi negocio', ['inventory'], false);
  const inventario = allBlocks(sistema).find((bloque) => bloque.type === 'inventory');
  if (inventario) {
    inventario.rows = nombres.map((nombre, indice) => ({
      id: `fila-${indice}`,
      name: nombre,
      price: 100,
    }));
  }
  return sistema;
};

export const dadoUnSistemaSinInventario = (): Sistema => makeSystem('Vacío', ['clients'], false);

export const cuandoSeBuscaEnLaZona = (zona: Zona, sistema: Sistema, consulta: string) =>
  productosBuscados(zona, productosPropios([sistema]), consulta).map((producto) => producto.nombre);

export const cuandoSeOrdenanPorLoMasVendido = (productos: ProductoDeZona[]) =>
  masVendidosPrimero(productos).map((producto) => producto.nombre);

export const cuandoSeCalculaElPorcentaje = porcentajeDeComercios;
export const cuandoSeCalculaElPrecioParaSumar = precioParaSumar;
export const cuandoSeBuscaDondeSumar = destinoParaSumar;
export const cuandoSeListanLosNombresDeInventario = nombresDeInventario;
export const cuandoSePreguntaSiYaLoTiene = yaLoTiene;
export const cuandoSeEscribeLaFraseDeComercios = frasePorComercios;

export const entoncesSale = <T>(obtenido: T, esperado: T) => expect(obtenido).toEqual(esperado);

export const entoncesSeSumaAlPrimerInventarioDe = (
  destino: ReturnType<typeof destinoParaSumar>,
  sistema: Sistema,
) => expect(destino?.sistema.id).toBe(sistema.id);

export const entoncesNoHayDondeSumar = (destino: ReturnType<typeof destinoParaSumar>) =>
  expect(destino).toBeUndefined();
