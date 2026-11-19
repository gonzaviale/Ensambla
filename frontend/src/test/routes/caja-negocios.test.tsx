import { describe, it } from 'vitest';
import {
  ALMACEN_DE_PRUEBA,
  KIOSCO_DE_PRUEBA,
  dadoDosNegociosConCaja,
  dadoNegocioRecordado,
  cuandoEntraDesdeInicio,
  cuandoAbreCaja,
  cuandoAgregaGaseosa,
  cuandoEligeNegocio,
  entoncesNavegacionConservaCajaYNegocios,
  entoncesSoloVeProductosDelKiosco,
  entoncesRecuperaCompraDelAlmacen,
} from '@/test/caja/negociosCajaHelpers';

describe('caja con la navegación de negocios', () => {
  it('conserva el selector de negocio y el acceso a Caja en el menú nuevo', async () => {
    dadoDosNegociosConCaja();
    await cuandoEntraDesdeInicio();
    entoncesNavegacionConservaCajaYNegocios();
  });

  it('usa el negocio recordado en vez del primero de la cuenta', async () => {
    const { kiosco } = dadoDosNegociosConCaja();
    dadoNegocioRecordado(kiosco.id);
    await cuandoEntraDesdeInicio();
    await entoncesSoloVeProductosDelKiosco();
  });

  it('separa los productos y recupera la compra al volver al negocio anterior', async () => {
    dadoDosNegociosConCaja();
    await cuandoEntraDesdeInicio();
    await cuandoAgregaGaseosa();
    await cuandoEligeNegocio(KIOSCO_DE_PRUEBA);
    await cuandoAbreCaja();
    await entoncesSoloVeProductosDelKiosco();
    await cuandoEligeNegocio(ALMACEN_DE_PRUEBA);
    await cuandoAbreCaja();
    entoncesRecuperaCompraDelAlmacen();
  });
});
