import { productoDeZona, rotaciones } from '@/lib/sistema/alertas';
import { allBlocks } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { sinDatosInsuficientes, type Zona } from '@/lib/sistema/zona';
import type { ProductoParaPaso } from '@/types/objetivos';

type Producto = {
  id: string;
  nombre: string;
  precio: number;
  costo: number;
  stock: number;
  vendidas30Dias: number;
};

const numero = (valor: unknown) => Number(valor ?? 0) || 0;
const ganancia = (producto: Producto) => producto.precio - producto.costo;
const pasoDePrecio = (precio: number) => (precio >= 1500 ? 50 : 10);
const orden = (producto: Producto) => `${producto.nombre.toLocaleLowerCase()}|${producto.id}`;

const precioZonaDe = (producto: Producto, zona: Zona) =>
  productoDeZona(producto.nombre, sinDatosInsuficientes(zona))?.precio ?? null;

const respuesta = (
  tipo: ProductoParaPaso['tipo'],
  producto: Producto,
  zona: Zona,
): ProductoParaPaso => ({
  tipo,
  nombre: producto.nombre,
  precio: producto.precio,
  costo: producto.costo,
  stock: producto.stock,
  vendidas30Dias: producto.vendidas30Dias,
  precioZona: precioZonaDe(producto, zona),
});

/** Mantiene funcional la demo totalmente simulada; producción siempre usa el endpoint Java. */
export function pasosObjetivoSimulados(
  sistemas: Sistema[],
  hoy: string,
  zona: Zona,
): ProductoParaPaso[] {
  const productos = sistemas.flatMap((sistema) => {
    const { porProducto } = rotaciones(sistema, hoy);
    return allBlocks(sistema)
      .filter((bloque) => bloque.type === 'inventory')
      .flatMap((bloque) => bloque.rows)
      .map((fila) => ({
        id: fila.id,
        nombre: String(fila.name ?? '').trim(),
        precio: numero(fila.price),
        costo: numero(fila.cost),
        stock: numero(fila.quantity),
        vendidas30Dias: porProducto.get(fila.id)?.u30 ?? 0,
      }))
      .filter((producto) => producto.nombre && producto.precio > 0 && producto.costo > 0);
  });
  const pasos: ProductoParaPaso[] = [];
  const usados = new Set<string>();
  const pocoMargen = [...productos]
    .filter((producto) => producto.vendidas30Dias > 0 && ganancia(producto) < producto.precio * 0.3)
    .sort((a, b) => b.vendidas30Dias - a.vendidas30Dias || orden(a).localeCompare(orden(b)))[0];
  if (pocoMargen) {
    usados.add(pocoMargen.id);
    pasos.push(respuesta('SUBIR_PRECIO', pocoMargen, zona));
  }
  const parado = [...productos]
    .filter(
      (producto) =>
        producto.vendidas30Dias === 0 &&
        producto.stock > 0 &&
        ganancia(producto) >= pasoDePrecio(producto.precio) * 2,
    )
    .sort((a, b) => b.costo * b.stock - a.costo * a.stock || orden(a).localeCompare(orden(b)))[0];
  if (parado) {
    usados.add(parado.id);
    pasos.push(respuesta('MOVER_PARADO', parado, zona));
  }
  const rinde = [...productos]
    .filter((producto) => producto.vendidas30Dias > 0 && !usados.has(producto.id))
    .sort((a, b) => ganancia(b) - ganancia(a) || orden(a).localeCompare(orden(b)))[0];
  if (rinde) pasos.push(respuesta('PONER_A_LA_VISTA', rinde, zona));
  return pasos;
}
