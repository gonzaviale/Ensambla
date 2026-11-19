import { describe, it } from 'vitest';
import {
  ALMACEN_DE_PRUEBA,
  KIOSCO_DE_PRUEBA,
  givenDosNegociosConCaja,
  givenNegocioRecordado,
  whenEntraDesdeInicio,
  whenAbreCaja,
  whenAgregaGaseosa,
  whenEligeNegocio,
  thenNavegacionConservaCajaYNegocios,
  thenSoloVeProductosDelKiosco,
  thenRecuperaCompraDelAlmacen,
} from '@/test/caja/negociosCajaHelpers';

describe('caja con la navegación de negocios', () => {
  it('conserva el selector de negocio y el acceso a Caja en el menú nuevo', async () => {
    givenDosNegociosConCaja();
    await whenEntraDesdeInicio();
    thenNavegacionConservaCajaYNegocios();
  });

  it('usa el negocio recordado en vez del primero de la cuenta', async () => {
    const { kiosco } = givenDosNegociosConCaja();
    givenNegocioRecordado(kiosco.id);
    await whenEntraDesdeInicio();
    await thenSoloVeProductosDelKiosco();
  });

  it('separa los productos y recupera la compra al volver al negocio anterior', async () => {
    givenDosNegociosConCaja();
    await whenEntraDesdeInicio();
    await whenAgregaGaseosa();
    await whenEligeNegocio(KIOSCO_DE_PRUEBA);
    await whenAbreCaja();
    await thenSoloVeProductosDelKiosco();
    await whenEligeNegocio(ALMACEN_DE_PRUEBA);
    await whenAbreCaja();
    thenRecuperaCompraDelAlmacen();
  });
});
