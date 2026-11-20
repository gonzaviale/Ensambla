import { afterEach, describe, it, vi } from 'vitest';
import {
  dadoDialogoConSelector,
  cuandoAvanzaHastaSelector,
  cuandoAvanzaUnControl,
  cuandoRetrocedeUnControl,
  cuandoSolicitaCerrarDialogo,
  entoncesSelectorEnfocado,
  entoncesCierreEnfocado,
  entoncesDialogoAbierto,
  entoncesDialogoCerradoYFocoRestaurado,
} from '@/test/caja/dialogoCajaHelpers';

afterEach(() => vi.restoreAllMocks());

describe('teclado del diálogo de Caja', () => {
  it('permite llegar al selector sin enfocar controles deshabilitados', async () => {
    const user = await dadoDialogoConSelector();
    await cuandoAvanzaHastaSelector(user);
    entoncesSelectorEnfocado();
  });
  it('vuelve al primer control después del selector final', async () => {
    const user = await dadoDialogoConSelector();
    await cuandoAvanzaHastaSelector(user);
    await cuandoAvanzaUnControl(user);
    entoncesCierreEnfocado();
  });
  it('vuelve al selector final al retroceder desde el primer control', async () => {
    const user = await dadoDialogoConSelector();
    await cuandoRetrocedeUnControl(user);
    entoncesSelectorEnfocado();
  });
  it('restaura el foco al cerrar con Escape', async () => {
    const user = await dadoDialogoConSelector();
    await cuandoSolicitaCerrarDialogo(user);
    entoncesDialogoCerradoYFocoRestaurado();
  });
  it('conserva abierto el cobro mientras se está guardando', async () => {
    const user = await dadoDialogoConSelector(true);
    await cuandoSolicitaCerrarDialogo(user);
    entoncesDialogoAbierto();
  });
});
