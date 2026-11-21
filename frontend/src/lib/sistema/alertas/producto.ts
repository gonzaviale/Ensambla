import type { Bloque, Fila, Sistema } from '../tipos';
import type { ProductoDeZona, Zona } from '../zona';
import { mediana, numero, pesos, productoDeZona } from './medidas';
import type { Rotacion, Rotaciones } from './rotaciones';
import type { AccionDeAviso } from './tipos';

/** Lo que el motor sabe de un sistema mientras lo analiza. */
export type ContextoDelSistema = Rotaciones & {
  sistema: Sistema;
  zona: Zona;
  entradas?: Bloque;
};

/** Un producto del inventario con todo lo que necesitan las reglas para decidir si avisar. */
export type ProductoAnalizado = {
  sistema: Sistema;
  inventario: Bloque;
  fila: Fila;
  nombre: string;
  cantidad: number;
  minimo: number;
  venta: number;
  costo: number;
  rotacion: Rotacion;
  /** Qué tanto se vende «normalmente» entre los demás productos: se compara contra ellos, no consigo mismo. */
  tipica: number;
  hayVentas: boolean;
  entradas?: Bloque;
  delaZona?: ProductoDeZona;
};

export type ProductoConZona = ProductoAnalizado & { delaZona: ProductoDeZona };

export const nombreDelProducto = (fila: Fila) => String(fila.name ?? '').trim();

export function analizarElProducto(
  contexto: ContextoDelSistema,
  inventario: Bloque,
  fila: Fila,
): ProductoAnalizado {
  const nombre = nombreDelProducto(fila);
  return {
    sistema: contexto.sistema,
    inventario,
    fila,
    nombre,
    cantidad: numero(fila.quantity),
    minimo: numero(fila.minimum),
    venta: numero(fila.price),
    costo: numero(fila.cost),
    rotacion: contexto.porProducto.get(fila.id) ?? { u30: 0 },
    tipica: ventaTipicaDeLosDemas(contexto.porProducto, fila.id),
    hayVentas: contexto.hayVentas,
    entradas: contexto.entradas,
    delaZona: productoDeZona(nombre, contexto.zona),
  };
}

const ventaTipicaDeLosDemas = (porProducto: Map<string, Rotacion>, productoId: string) =>
  mediana(
    [...porProducto.entries()]
      .filter(([otroId]) => otroId !== productoId)
      .map(([, otra]) => otra.u30)
      .filter((unidades) => unidades > 0),
  );

export const tienePrecioYZona = (producto: ProductoAnalizado): producto is ProductoConZona =>
  Boolean(producto.delaZona) && producto.venta > 0;

export const datosDelSistema = ({ sistema }: ProductoAnalizado) => ({
  sistemaId: sistema.id,
  sistema: sistema.name,
});

export const referenciaDeZona = ({ delaZona }: ProductoConZona) =>
  `En tu zona, ${delaZona.comercios} comercios lo venden entre ${pesos(delaZona.precio.minimo)} y ${pesos(delaZona.precio.maximo)}. Es un dato agregado: no se sabe quién cobra qué.`;

export const verStock = ({ sistema, inventario }: ProductoAnalizado): AccionDeAviso => ({
  tipo: 'abrir',
  texto: 'Ver stock',
  sistemaId: sistema.id,
  bloqueId: inventario.id,
});

export const aplicarPrecio = (
  { sistema, inventario, fila, nombre }: ProductoAnalizado,
  texto: string,
  precio: number,
): AccionDeAviso => ({
  tipo: 'aplicar',
  texto,
  sistemaId: sistema.id,
  bloqueId: inventario.id,
  filaId: fila.id,
  valores: { price: precio },
  confirmacion: `${nombre} ahora está a ${pesos(precio)}.`,
});
