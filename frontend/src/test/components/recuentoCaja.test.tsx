import { describe, it } from 'vitest';
import {
  givenCierreConEfectivo,
  whenSeCuentaEfectivo,
  whenSeExplicaDiferencia,
  thenNoSePuedeGuardarCierre,
  thenSePuedeGuardarCierre,
  thenSeGuardaRecuentoCero,
} from '@/test/caja/recuentoCajaHelpers';

describe('recuento obligatorio al cerrar caja', () => {
  it('exige escribir el efectivo contado antes de guardar', () => {
    givenCierreConEfectivo();
    thenNoSePuedeGuardarCierre();
  });
  it('exige explicar la diferencia aunque el recuento sea cero', async () => {
    const { user, confirmar } = givenCierreConEfectivo();
    await whenSeCuentaEfectivo(user, '0');
    thenNoSePuedeGuardarCierre();
    await whenSeExplicaDiferencia(user);
    thenSeGuardaRecuentoCero(confirmar);
  });
  it('acepta cero sin motivo cuando no se esperaba efectivo', async () => {
    const { user } = givenCierreConEfectivo(0);
    await whenSeCuentaEfectivo(user, '0');
    thenSePuedeGuardarCierre();
  });
  it('acepta el efectivo coincidente sin exigir una nota', async () => {
    const { user } = givenCierreConEfectivo();
    await whenSeCuentaEfectivo(user, '1000');
    thenSePuedeGuardarCierre();
  });
});
