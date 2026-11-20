import type { NombreDeIcono } from '@/lib/sistema/iconos';
import { claseDeSeleccion, tonoDe, type ConceptoDeColor } from '@/lib/sistema/tonos';
import type { MedioCaja } from '@/types/caja';

type MedioDePago = { nombre: string; icono: NombreDeIcono; concepto: ConceptoDeColor };

const MEDIOS_DE_PAGO: Record<MedioCaja, MedioDePago> = {
  EFECTIVO: { nombre: 'Efectivo', icono: 'wallet', concepto: 'efectivo' },
  TRANSFERENCIA: { nombre: 'Transferencia', icono: 'arrow', concepto: 'transferencia' },
  TARJETA: { nombre: 'Tarjeta', icono: 'orders', concepto: 'tarjeta' },
};

export const nombreDelMedio = (medio: MedioCaja) => MEDIOS_DE_PAGO[medio].nombre;

export const iconoDelMedio = (medio: MedioCaja) => MEDIOS_DE_PAGO[medio].icono;

export function aspectoDelMedio(medio: MedioCaja, elegido: boolean) {
  return { className: claseDeSeleccion(elegido), 'data-tono': tonoDe(MEDIOS_DE_PAGO[medio].concepto) };
}
