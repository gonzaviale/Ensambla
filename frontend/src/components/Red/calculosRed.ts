import { cantidadSugeridaDePedido, variacionPct } from '@/lib/red/calculos';
import type { AlternativaProveedor, OfertaProveedor, ProductoRed } from '@/types/red';

// Cuentas de «Mi red»: puras, para que las pantallas sólo muestren.

/** Semanas de ventas que se cuentan para estimar lo que ahorra una oferta. */
export const SEMANAS_DE_UNA_OFERTA = 2;
/** Semanas que se cuentan para estimar el ahorro por mes. */
export const SEMANAS_POR_MES = 4;

export const ofertaVigente = (oferta: OfertaProveedor, ahora: Date) =>
  new Date(oferta.vence).getTime() >= ahora.getTime();

/** Cuánto menos cuesta la oferta contra el costo habitual, en %. Positivo = más barato. */
export const descuentoDeOferta = (oferta: OfertaProveedor) =>
  -variacionPct(oferta.costoHabitual, oferta.costoOferta);

/** Σ (habitual − oferta) × ventas por semana × 2 semanas. */
export const ahorroDeOferta = (oferta: OfertaProveedor, producto: ProductoRed) =>
  Math.max(0, oferta.costoHabitual - oferta.costoOferta) *
  producto.ventasSemana *
  SEMANAS_DE_UNA_OFERTA;

/** Unidades a pedir: el mínimo de bultos de la oferta o, si no hay, lo que sugiere el ritmo de ventas. */
export function cantidadDeOferta(oferta: OfertaProveedor, producto: ProductoRed): number {
  if (!oferta.minimoBultos) return cantidadSugeridaDePedido(producto);
  return oferta.minimoBultos * (producto.unidadesPorBulto ?? 1);
}

/** La alternativa de menor costo entre los otros proveedores de la red (o null si no hay). */
export function mejorAlternativa(producto: ProductoRed): AlternativaProveedor | null {
  return [...producto.alternativas].sort((a, b) => a.costo - b.costo)[0] ?? null;
}

/** Ahorro por mes si se compra a un costo menor: diferencia por unidad × ventas de 4 semanas. */
export const ahorroPorMes = (producto: ProductoRed, costoNuevo: number) =>
  Math.max(0, producto.costo - costoNuevo) * producto.ventasSemana * SEMANAS_POR_MES;
