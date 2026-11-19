import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, vi } from 'vitest';
import { ImporteLibreCaja } from '@/components/caja/ImporteLibreCaja';
import { agregarLinea } from '@/lib/caja';
import { crearImporteLibre, lineaParaCobrar } from '@/lib/caja/importeLibre';
import { esDeudaAntigua } from '@/lib/caja/antiguedadDeuda';
import type { LineaCaja, VentaCaja } from '@/types/caja';

export function dadoFormularioImporteLibre() {
  const cerrado = vi.fn();
  const agregado = vi.fn(crearImporteLibre);
  render(<ImporteLibreCaja onAgregar={agregado} onCerrar={cerrado} />);
  return { user: userEvent.setup(), agregado, cerrado };
}
export async function cuandoSeAgregaServicio(
  user: ReturnType<typeof userEvent.setup>,
  importe = '2500',
) {
  await user.type(screen.getByLabelText('Descripción'), 'Servicio');
  await user.type(screen.getByLabelText('Importe ($)'), importe);
  await user.click(screen.getByRole('button', { name: 'Agregar a la compra' }));
}
export function entoncesSeAgregaImporteSinProducto(
  agregado: ReturnType<typeof vi.fn>,
  cerrado: ReturnType<typeof vi.fn>,
) {
  expect(agregado).toHaveBeenCalledWith('Servicio', '2500');
  expect(cerrado).toHaveBeenCalledOnce();
}
export function entoncesSeRechazaImporteCero(cerrado: ReturnType<typeof vi.fn>) {
  expect(screen.getByRole('alert')).toHaveTextContent('El precio debe ser mayor a cero');
  expect(cerrado).not.toHaveBeenCalled();
}
export function dadoDosImportesLibres(): [LineaCaja, LineaCaja] {
  return [crearImporteLibre('Servicio A', '10'), crearImporteLibre('Servicio B', '20')];
}
export function entoncesNoSeAgrupanImportesDistintos(
  lineas: ReturnType<typeof dadoDosImportesLibres>,
) {
  const compra = agregarLinea([lineas[0]], lineas[1]);
  expect(compra).toHaveLength(2);
  expect(compra.map(lineaParaCobrar)).toEqual([
    { productoId: null, descripcion: 'Servicio A', cantidad: 1, precio: 10, modo: 'UNIDAD' },
    { productoId: null, descripcion: 'Servicio B', cantidad: 1, precio: 20, modo: 'UNIDAD' },
  ]);
}
export function dadoDeudaDesde(creadoEn: string, saldo = 100): VentaCaja {
  return {
    id: 'venta',
    lineas: [],
    medio: 'EFECTIVO',
    total: 100,
    recibido: 0,
    vuelto: 0,
    creadoEn,
    pagos: [],
    saldo,
  };
}
export function entoncesEsDeudaAntigua(venta: VentaCaja, hoy: string, esperado: boolean) {
  expect(esDeudaAntigua(venta, new Date(hoy))).toBe(esperado);
}
