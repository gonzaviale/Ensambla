import { describe, it } from 'vitest';
import {
  dadoInicioConNegocioElegido,
  cuandoAbreAccesos,
  cuandoEligeVenta,
  cuandoEligeInventario,
  entoncesCajaSinFlotante,
  entoncesLectorDelNegocioActual,
  cuandoCierraConEscape,
  entoncesRecuperaFoco,
  entoncesPrimeraAccionEnfocada,
  cuandoCambiaNegocioConAccesosAbiertos,
  cuandoSaleConTeclado,
  entoncesMenuDeAccesosCerrado,
} from '@/test/caja/accesosRapidosHelpers';

describe('accesos rápidos del negocio', () => {
  it('cierra las acciones anteriores cuando cambia de negocio en la misma pantalla', async () => {
    const caso = await dadoInicioConNegocioElegido();
    await cuandoAbreAccesos(caso);
    await cuandoCambiaNegocioConAccesosAbiertos();
    entoncesMenuDeAccesosCerrado();
  });
  it('cierra el panel cuando el teclado sale de sus acciones', async () => {
    const caso = await dadoInicioConNegocioElegido();
    await cuandoAbreAccesos(caso);
    await cuandoSaleConTeclado(caso);
    entoncesMenuDeAccesosCerrado();
  });
  it('abre Caja desde otra sección y oculta el flotante durante la venta', async () => {
    const caso = await dadoInicioConNegocioElegido();
    await cuandoAbreAccesos(caso);
    await cuandoEligeVenta(caso);
    await entoncesCajaSinFlotante();
  });
  it('abre el lector del negocio elegido y se oculta durante la carga', async () => {
    const caso = await dadoInicioConNegocioElegido();
    await cuandoAbreAccesos(caso);
    await cuandoEligeInventario(caso);
    await entoncesLectorDelNegocioActual(caso);
  });
  it('cierra con Escape y devuelve el foco al acceso', async () => {
    const caso = await dadoInicioConNegocioElegido();
    await cuandoAbreAccesos(caso);
    entoncesPrimeraAccionEnfocada();
    await cuandoCierraConEscape(caso);
    entoncesRecuperaFoco();
  });
});
