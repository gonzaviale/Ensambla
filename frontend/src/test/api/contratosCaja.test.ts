import { describe, it } from 'vitest';
import {
  dadoContratosCajaSimulados,
  cuandoSeConsultaCuentaSimulada,
  cuandoSeCobraImporteLibreSimulado,
  cuandoSeConsultaCierreSimulado,
  entoncesSeRecibenSaldosAgrupados,
  entoncesSeRecibeImporteLibre,
  entoncesSeRecibeCierreConCero,
  entoncesSeExplicaRechazoDeCierre,
} from '@/test/caja/contratosCajaHelpers';

describe('contratos de caja con mocks compartidos', () => {
  it('valida las cuentas agrupadas por cliente que devuelve la API', async () => {
    dadoContratosCajaSimulados();
    const cuentas = await cuandoSeConsultaCuentaSimulada();
    entoncesSeRecibenSaldosAgrupados(cuentas);
  });
  it('acepta una línea sin producto en el comprobante del importe libre', async () => {
    dadoContratosCajaSimulados();
    const venta = await cuandoSeCobraImporteLibreSimulado();
    entoncesSeRecibeImporteLibre(venta);
  });
  it('distingue un recuento cero de los cierres históricos sin recuento', async () => {
    dadoContratosCajaSimulados();
    const datos = await cuandoSeConsultaCierreSimulado();
    entoncesSeRecibeCierreConCero(datos);
  });
  it('conserva el error del servidor cuando falta explicar una diferencia', async () => {
    dadoContratosCajaSimulados(true);
    await entoncesSeExplicaRechazoDeCierre();
  });
});
