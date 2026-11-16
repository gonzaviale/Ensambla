import { describe, it } from 'vitest';
import {
  givenSegundoNegocioRecordado,
  whenAbreLectorDesdeInicio,
  thenLectorDelNegocioElegido,
  whenVuelveAlInventario,
  thenConservaElNegocioAlVolver,
  whenVuelveAlInicio,
  thenInicioDelNegocioElegido,
} from '@/test/escaneo/negociosInventarioHelpers';

describe('lector con la navegación de negocios', () => {
  it('abre desde Inicio y vuelve al inventario del negocio recordado', async () => {
    const negocio = givenSegundoNegocioRecordado();
    const caso = await whenAbreLectorDesdeInicio(negocio.inventario);
    thenLectorDelNegocioElegido(caso, negocio);
    await whenVuelveAlInventario(caso, negocio);
    thenConservaElNegocioAlVolver(caso, negocio);
  });

  it('vuelve a Inicio desde el lector y conserva el negocio elegido', async () => {
    const negocio = givenSegundoNegocioRecordado();
    const caso = await whenAbreLectorDesdeInicio(negocio.inventario);
    await whenVuelveAlInicio(caso);
    thenInicioDelNegocioElegido(caso, negocio);
  });
});
