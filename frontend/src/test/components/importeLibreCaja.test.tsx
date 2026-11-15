import { describe, it } from 'vitest';
import {
  givenFormularioImporteLibre,
  whenSeAgregaServicio,
  thenSeAgregaImporteSinProducto,
  thenSeRechazaImporteCero,
  givenDosImportesLibres,
  thenNoSeAgrupanImportesDistintos,
  givenDeudaDesde,
  thenEsDeudaAntigua,
} from '@/test/caja/importeLibreHelpers';

describe('importe libre de caja', () => {
  it('agrega una descripción y un importe a la compra sin crear un producto', async () => {
    const { user, agregado, cerrado } = givenFormularioImporteLibre();
    await whenSeAgregaServicio(user);
    thenSeAgregaImporteSinProducto(agregado, cerrado);
  });
  it('mantiene abierto el formulario cuando el importe no es positivo', async () => {
    const { user, cerrado } = givenFormularioImporteLibre();
    await whenSeAgregaServicio(user, '0');
    thenSeRechazaImporteCero(cerrado);
  });
  it('conserva separados los importes libres y envía sus descripciones al cobrar', () => {
    const lineas = givenDosImportesLibres();
    thenNoSeAgrupanImportesDistintos(lineas);
  });
});
describe('antigüedad de deuda desde la venta original', () => {
  it('espera los treinta días completos según la fecha del negocio', () => {
    const venta = givenDeudaDesde('2026-10-16T12:00:00-03:00');
    thenEsDeudaAntigua(venta, '2026-11-14T23:59:59-03:00', false);
    thenEsDeudaAntigua(venta, '2026-11-15T00:00:00-03:00', true);
  });
  it('usa el día argentino aunque la fecha UTC ya haya cambiado', () => {
    const venta = givenDeudaDesde('2026-10-17T01:30:00Z');
    thenEsDeudaAntigua(venta, '2026-11-15T02:59:59Z', false);
    thenEsDeudaAntigua(venta, '2026-11-15T03:00:00Z', true);
  });
  it('deja de destacar una deuda saldada aunque la venta sea antigua', () => {
    const venta = givenDeudaDesde('2026-09-01T12:00:00Z', 0);
    thenEsDeudaAntigua(venta, '2026-11-15T12:00:00Z', false);
  });
});
