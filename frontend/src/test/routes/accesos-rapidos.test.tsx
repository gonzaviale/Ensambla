import { describe, it } from 'vitest';
import { givenInicioConNegocioElegido, whenAbreAccesos, whenEligeVenta, whenEligeInventario,
  thenCajaSinFlotante, thenLectorDelNegocioActual, whenCierraConEscape, thenRecuperaFoco,
  thenPrimeraAccionEnfocada,
} from '@/test/caja/accesosRapidosHelpers';

describe('accesos rápidos del negocio', () => {
  it('abre Caja desde otra sección y oculta el flotante durante la venta', async () => {
    const caso = await givenInicioConNegocioElegido();
    await whenAbreAccesos(caso);
    await whenEligeVenta(caso);
    await thenCajaSinFlotante();
  });
  it('abre el lector del negocio elegido y se oculta durante la carga', async () => {
    const caso = await givenInicioConNegocioElegido();
    await whenAbreAccesos(caso);
    await whenEligeInventario(caso);
    await thenLectorDelNegocioActual(caso);
  });
  it('cierra con Escape y devuelve el foco al acceso', async () => {
    const caso = await givenInicioConNegocioElegido();
    await whenAbreAccesos(caso);
    thenPrimeraAccionEnfocada();
    await whenCierraConEscape(caso);
    thenRecuperaFoco();
  });
});
