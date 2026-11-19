import { describe, it } from 'vitest';
import {
  dadoCierreConEfectivo,
  cuandoSeCuentaEfectivo,
  cuandoSeExplicaDiferencia,
  entoncesNoSePuedeGuardarCierre,
  entoncesSePuedeGuardarCierre,
  entoncesSeGuardaRecuentoCero,
} from '@/test/caja/recuentoCajaHelpers';

describe('recuento obligatorio al cerrar caja', () => {
  it('exige escribir el efectivo contado antes de guardar', () => {
    dadoCierreConEfectivo();
    entoncesNoSePuedeGuardarCierre();
  });
  it('exige explicar la diferencia aunque el recuento sea cero', async () => {
    const { user, confirmar } = dadoCierreConEfectivo();
    await cuandoSeCuentaEfectivo(user, '0');
    entoncesNoSePuedeGuardarCierre();
    await cuandoSeExplicaDiferencia(user);
    entoncesSeGuardaRecuentoCero(confirmar);
  });
  it('acepta cero sin motivo cuando no se esperaba efectivo', async () => {
    const { user } = dadoCierreConEfectivo(0);
    await cuandoSeCuentaEfectivo(user, '0');
    entoncesSePuedeGuardarCierre();
  });
  it('acepta el efectivo coincidente sin exigir una nota', async () => {
    const { user } = dadoCierreConEfectivo();
    await cuandoSeCuentaEfectivo(user, '1000');
    entoncesSePuedeGuardarCierre();
  });
});
