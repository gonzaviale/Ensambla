import { describe, it } from 'vitest';
import {
  dadoSegundoNegocioRecordado,
  cuandoAbreLectorDesdeInicio,
  entoncesLectorDelNegocioElegido,
  cuandoVuelveAlInventario,
  entoncesConservaElNegocioAlVolver,
  cuandoVuelveAlInicio,
  entoncesInicioDelNegocioElegido,
} from '@/test/escaneo/negociosInventarioHelpers';

describe('lector con la navegación de negocios', () => {
  it('abre desde Inicio y vuelve al inventario del negocio recordado', async () => {
    const negocio = dadoSegundoNegocioRecordado();
    const caso = await cuandoAbreLectorDesdeInicio(negocio.inventario);
    entoncesLectorDelNegocioElegido(caso, negocio);
    await cuandoVuelveAlInventario(caso, negocio);
    entoncesConservaElNegocioAlVolver(caso, negocio);
  });

  it('vuelve a Inicio desde el lector y conserva el negocio elegido', async () => {
    const negocio = dadoSegundoNegocioRecordado();
    const caso = await cuandoAbreLectorDesdeInicio(negocio.inventario);
    await cuandoVuelveAlInicio(caso);
    entoncesInicioDelNegocioElegido(caso, negocio);
  });
});
