import { allBlocks } from '@/lib/sistema/core';
import type { Fila, Sistema } from '@/lib/sistema/tipos';
import type { ProductoPromocionado, Promocion } from '@/types/promociones';

const MAXIMO_DE_LA_LISTA = 12;

const tieneInventario = (sistema: Sistema) => allBlocks(sistema).some((bloque) => bloque.type === 'inventory');

/** El cuaderno con el que se promociona: el que se está usando si tiene inventario y, si no, el primero que lo tenga. */
export function sistemaParaPromocionar(
  sistemas: Sistema[],
  activoId: string | null,
): Sistema | undefined {
  const activo = sistemas.find((sistema) => sistema.id === activoId);
  return activo && tieneInventario(activo) ? activo : sistemas.find(tieneInventario);
}

const productoDe = (fila: Fila): ProductoPromocionado[] => {
  const nombre = typeof fila.name === 'string' ? fila.name.trim() : '';
  const precio = typeof fila.price === 'number' ? fila.price : 0;
  return !fila.__example && nombre !== '' && precio > 0 ? [{ nombre, precio }] : [];
};

/** La lista de precios del cuaderno, con los productos propios que tienen precio; `null` si no hay ninguno. */
export function promocionListaDePrecios(sistema: Sistema): Promocion | null {
  const productos = allBlocks(sistema)
    .filter((bloque) => bloque.type === 'inventory')
    .flatMap((bloque) => bloque.rows.flatMap(productoDe))
    .slice(0, MAXIMO_DE_LA_LISTA);
  return productos.length === 0
    ? null
    : { clave: 'lista', titulo: 'Lista de precios', gancho: 'Precios al día', productos };
}
