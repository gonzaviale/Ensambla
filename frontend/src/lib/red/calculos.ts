import type {
  CambioPropuesto,
  ItemLista,
  ListaProveedor,
  PresentacionLista,
  ProductoRed,
  ReglaCategoria,
  ReglasPrecio,
  Umbrales,
} from '@/types/red';

const MS_DIA = 24 * 60 * 60 * 1000;

/* ---------- fechas ---------- */

export const sumarDias = (desde: Date, dias: number) =>
  new Date(desde.getTime() + dias * MS_DIA).toISOString();

export const diasEntre = (desde: string | Date, hasta: string | Date) =>
  (new Date(hasta).getTime() - new Date(desde).getTime()) / MS_DIA;

/* ---------- precios y márgenes ---------- */

/** Margen sobre el costo, en %: «cuánto le agrego al costo». */
export function margenSobreCosto(costo: number, precio: number): number {
  return costo > 0 ? ((precio - costo) / costo) * 100 : 0;
}

/** Variación porcentual entre dos importes. */
export function variacionPct(anterior: number, nuevo: number): number {
  return anterior > 0 ? ((nuevo - anterior) / anterior) * 100 : 0;
}

/** Redondea hacia arriba al múltiplo indicado (precio de góndola: de 2.859,5 a 2.860). */
export function redondearHaciaArriba(valor: number, multiplo: number): number {
  if (multiplo <= 0) return Math.ceil(valor);
  return Math.ceil(valor / multiplo - 1e-9) * multiplo;
}

export function reglaDe(reglas: ReglasPrecio, categoria: string): ReglaCategoria {
  return reglas.porCategoria[categoria] ?? reglas.general;
}

/** Precio que resulta de aplicar la regla del dueño a un costo. */
export function precioSegunRegla(costo: number, regla: ReglaCategoria): number {
  return redondearHaciaArriba(costo * (1 + regla.margen / 100), regla.redondeo);
}

/** Costo por unidad vendible de una línea de la lista, según se la interprete por unidad o por bulto. */
export function costoUnitario(
  precioLista: number,
  presentacion: PresentacionLista,
  unidadesPorBulto: number | null,
): number {
  if (presentacion === 'BULTO' && unidadesPorBulto && unidadesPorBulto > 0) {
    return Math.round((precioLista / unidadesPorBulto) * 100) / 100;
  }
  return precioLista;
}

export const costoDeItem = (item: ItemLista) =>
  costoUnitario(item.precioLista, item.presentacion, item.unidadesPorBulto);

/* ---------- stock ---------- */

/** Para cuántos días alcanza el stock al ritmo de ventas de la última semana. */
export function diasDeStock(producto: Pick<ProductoRed, 'stock' | 'ventasSemana'>): number {
  if (producto.ventasSemana <= 0) return Number.POSITIVE_INFINITY;
  return producto.stock / (producto.ventasSemana / 7);
}

/** Cuánto pedir para cubrir `dias` días de ventas, en bultos cerrados si el proveedor los vende así. */
export function cantidadSugeridaDePedido(producto: ProductoRed, dias = 14): number {
  const necesidad = Math.max(1, Math.ceil((producto.ventasSemana / 7) * dias - producto.stock));
  const bulto = producto.unidadesPorBulto ?? 1;
  return Math.ceil(necesidad / bulto) * bulto;
}

/* ---------- lista de proveedor → cambios de precio ---------- */

/** Un ítem entra al cálculo si está emparejado y es seguro o ya lo confirmó el comercio. */
export function itemListoParaCalcular(item: ItemLista): boolean {
  if (item.ignorado || !item.productoId) return false;
  return item.estado === 'seguro' || item.confirmado;
}

export function proponerCambios(
  lista: ListaProveedor,
  productos: ProductoRed[],
  reglas: ReglasPrecio,
  umbrales: Umbrales,
): CambioPropuesto[] {
  const cambios: CambioPropuesto[] = [];
  for (const item of lista.items) {
    if (!itemListoParaCalcular(item)) continue;
    const producto = productos.find((p) => p.id === item.productoId);
    if (!producto) continue;
    const costoNuevo = costoDeItem(item);
    const precioSugerido = precioSegunRegla(costoNuevo, reglaDe(reglas, producto.categoria));
    const variacion = variacionPct(producto.costo, costoNuevo);
    const margenActual = margenSobreCosto(costoNuevo, producto.precio);
    cambios.push({
      itemId: item.id,
      productoId: producto.id,
      nombre: producto.nombre,
      categoria: producto.categoria,
      costoAnterior: producto.costo,
      costoNuevo,
      variacion,
      precioActual: producto.precio,
      precioSugerido,
      margenActual,
      margenConSugerido: margenSobreCosto(costoNuevo, precioSugerido),
      enRiesgo: margenActual < reglas.margenMinimo,
      raro: Math.abs(variacion) >= umbrales.aumentoRaro,
    });
  }
  return cambios;
}

/** Ítems que todavía necesitan una decisión del comercio (dudosos sin confirmar o sin coincidencia). */
export function itemsPendientes(lista: ListaProveedor): ItemLista[] {
  return lista.items.filter(
    (item) => !item.ignorado && !item.confirmado && item.estado !== 'seguro',
  );
}

export function resumenDeLista(
  lista: ListaProveedor,
  productos: ProductoRed[],
  reglas: ReglasPrecio,
  umbrales: Umbrales,
) {
  const cambios = proponerCambios(lista, productos, reglas, umbrales);
  const suben = cambios.filter((c) => c.variacion > 0.05);
  const promedio =
    suben.length > 0 ? suben.reduce((total, c) => total + c.variacion, 0) / suben.length : 0;
  return {
    cambios,
    suben: suben.length,
    aumentoPromedio: promedio,
    enRiesgo: cambios.filter((c) => c.enRiesgo).length,
    raros: cambios.filter((c) => c.raro).length,
    pendientes: itemsPendientes(lista).length,
  };
}
