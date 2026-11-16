import { afterEach, describe, it, vi } from 'vitest';
import {
  givenDialogoConSelector,
  whenAvanzaHastaSelector,
  whenAvanzaUnControl,
  whenRetrocedeUnControl,
  whenSolicitaCerrarDialogo,
  thenSelectorEnfocado,
  thenCierreEnfocado,
  thenDialogoAbierto,
  thenDialogoCerradoYFocoRestaurado,
} from '@/test/caja/dialogoCajaHelpers';

afterEach(() => vi.restoreAllMocks());

describe('teclado del diálogo de Caja', () => {
  it('permite llegar al selector sin enfocar controles deshabilitados', async () => {
    const user = await givenDialogoConSelector();
    await whenAvanzaHastaSelector(user);
    thenSelectorEnfocado();
  });
  it('vuelve al primer control después del selector final', async () => {
    const user = await givenDialogoConSelector();
    await whenAvanzaHastaSelector(user);
    await whenAvanzaUnControl(user);
    thenCierreEnfocado();
  });
  it('vuelve al selector final al retroceder desde el primer control', async () => {
    const user = await givenDialogoConSelector();
    await whenRetrocedeUnControl(user);
    thenSelectorEnfocado();
  });
  it('restaura el foco al cerrar con Escape', async () => {
    const user = await givenDialogoConSelector();
    await whenSolicitaCerrarDialogo(user);
    thenDialogoCerradoYFocoRestaurado();
  });
  it('conserva abierto el cobro mientras se está guardando', async () => {
    const user = await givenDialogoConSelector(true);
    await whenSolicitaCerrarDialogo(user);
    thenDialogoAbierto();
  });
});
