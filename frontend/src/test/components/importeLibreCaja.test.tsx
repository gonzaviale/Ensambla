import { describe, it } from 'vitest';
import {
  dadoFormularioImporteLibre,
  cuandoSeAgregaServicio,
  entoncesSeAgregaImporteSinProducto,
  entoncesSeRechazaImporteCero,
  dadoDosImportesLibres,
  entoncesNoSeAgrupanImportesDistintos,
  dadoDeudaDesde,
  entoncesEsDeudaAntigua,
} from '@/test/caja/importeLibreHelpers';

describe('importe libre de caja', () => {
  it('agrega una descripción y un importe a la compra sin crear un producto', async () => {
    const { user, agregado, cerrado } = dadoFormularioImporteLibre();
    await cuandoSeAgregaServicio(user);
    entoncesSeAgregaImporteSinProducto(agregado, cerrado);
  });
  it('mantiene abierto el formulario cuando el importe no es positivo', async () => {
    const { user, cerrado } = dadoFormularioImporteLibre();
    await cuandoSeAgregaServicio(user, '0');
    entoncesSeRechazaImporteCero(cerrado);
  });
  it('conserva separados los importes libres y envía sus descripciones al cobrar', () => {
    const lineas = dadoDosImportesLibres();
    entoncesNoSeAgrupanImportesDistintos(lineas);
  });
});
describe('antigüedad de deuda desde la venta original', () => {
  it('espera los treinta días completos según la fecha del negocio', () => {
    const venta = dadoDeudaDesde('2026-10-16T12:00:00-03:00');
    entoncesEsDeudaAntigua(venta, '2026-11-14T23:59:59-03:00', false);
    entoncesEsDeudaAntigua(venta, '2026-11-15T00:00:00-03:00', true);
  });
  it('usa el día argentino aunque la fecha UTC ya haya cambiado', () => {
    const venta = dadoDeudaDesde('2026-10-17T01:30:00Z');
    entoncesEsDeudaAntigua(venta, '2026-11-15T02:59:59Z', false);
    entoncesEsDeudaAntigua(venta, '2026-11-15T03:00:00Z', true);
  });
  it('deja de destacar una deuda saldada aunque la venta sea antigua', () => {
    const venta = dadoDeudaDesde('2026-09-01T12:00:00Z', 0);
    entoncesEsDeudaAntigua(venta, '2026-11-15T12:00:00Z', false);
  });
});
