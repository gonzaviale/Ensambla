import { describe, it } from 'vitest';
import {
  givenContratosCajaSimulados,
  whenSeConsultaCuentaSimulada,
  whenSeCobraImporteLibreSimulado,
  whenSeConsultaCierreSimulado,
  thenSeRecibenSaldosAgrupados,
  thenSeRecibeImporteLibre,
  thenSeRecibeCierreConCero,
  thenSeExplicaRechazoDeCierre,
} from '@/test/caja/contratosCajaHelpers';

describe('contratos de caja con mocks compartidos', () => {
  it('valida las cuentas agrupadas por cliente que devuelve la API', async () => {
    givenContratosCajaSimulados();
    const cuentas = await whenSeConsultaCuentaSimulada();
    thenSeRecibenSaldosAgrupados(cuentas);
  });
  it('acepta una línea sin producto en el comprobante del importe libre', async () => {
    givenContratosCajaSimulados();
    const venta = await whenSeCobraImporteLibreSimulado();
    thenSeRecibeImporteLibre(venta);
  });
  it('distingue un recuento cero de los cierres históricos sin recuento', async () => {
    givenContratosCajaSimulados();
    const datos = await whenSeConsultaCierreSimulado();
    thenSeRecibeCierreConCero(datos);
  });
  it('conserva el error del servidor cuando falta explicar una diferencia', async () => {
    givenContratosCajaSimulados(true);
    await thenSeExplicaRechazoDeCierre();
  });
});
