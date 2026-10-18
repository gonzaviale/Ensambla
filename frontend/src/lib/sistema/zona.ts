import { parecido } from './alertas';
import { allBlocks } from './core';
import type { Sistema } from './tipos';

// Los datos de la zona son AGREGADOS y ANÓNIMOS: nunca se sabe qué comercio cobra qué ni dónde queda. Un
// dato sólo existe si lo aportan al menos tres comercios; con menos, no se muestra ni se usa.
export const MINIMO_DE_COMERCIOS = 3;

export type ProductoDeZona = {
  nombre: string;
  /** Cuántos comercios aportan el precio de este producto. */
  comercios: number;
  precio: { minimo: number; promedio: number; maximo: number };
  ventas: {
    /** Cuántos comercios lo venden. */
    comercios: number;
    /** Unidades que vende por semana un comercio promedio. */
    unidadesPorSemana: number;
  };
};

export type Zona = {
  /** El nombre de la zona tal como se muestra. */
  nombre: string;
  /** Cuántos comercios de la zona entran en el análisis. */
  comercios: number;
  productos: ProductoDeZona[];
};

/** Sin datos: lo que se usa mientras la zona carga o cuando no hay suficientes comercios. */
export const ZONA_VACIA: Zona = { nombre: '', comercios: 0, productos: [] };

/** Deja sólo lo que cumple el mínimo de comercios. Si la zona entera no lo cumple, no queda nada. */
export function sinDatosInsuficientes(zona: Zona): Zona {
  if (zona.comercios < MINIMO_DE_COMERCIOS) return { ...zona, productos: [] };
  return {
    ...zona,
    productos: zona.productos.filter(
      (producto) =>
        producto.comercios >= MINIMO_DE_COMERCIOS &&
        producto.ventas.comercios >= MINIMO_DE_COMERCIOS,
    ),
  };
}

export type PosicionDelPrecio = 'bajo' | 'justo' | 'alto';

/** Tu precio frente al promedio de la zona: más de 8 % abajo o 12 % arriba ya se considera distinto. */
export const posicionDelPrecio = (precio: number, promedio: number): PosicionDelPrecio =>
  precio < promedio * 0.92 ? 'bajo' : precio > promedio * 1.12 ? 'alto' : 'justo';

export type ProductoPropio = {
  nombre: string;
  precio: number;
  sistemaId: string;
  sistema: string;
  bloqueId: string;
  filaId: string;
};

/** Los productos del inventario de la persona que tienen precio, de todos sus sistemas. */
export function productosPropios(sistemas: Sistema[]): ProductoPropio[] {
  return sistemas.flatMap((sistema) =>
    allBlocks(sistema)
      .filter((bloque) => bloque.type === 'inventory')
      .flatMap((bloque) =>
        bloque.rows
          .filter((fila) => String(fila.name ?? '').trim() && Number(fila.price) > 0)
          .map((fila) => ({
            nombre: String(fila.name),
            precio: Number(fila.price),
            sistemaId: sistema.id,
            sistema: sistema.name,
            bloqueId: bloque.id,
            filaId: fila.id,
          })),
      ),
  );
}

/** El producto propio que corresponde a uno de la zona (mismo nombre, salvo tamaño o marca extra). */
export const propioDe = (producto: ProductoDeZona, propios: ProductoPropio[]) =>
  propios.find((propio) => parecido(propio.nombre, producto.nombre) === 1);
