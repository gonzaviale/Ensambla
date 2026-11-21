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
  cuandoCierraConLaCruz,
  entoncesNoHayUnBotonDeTextoParaCerrar,
  entoncesLaCruzDeCerrarEstaVisible,
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
  it('ya no ofrece un botón de texto para cerrar el panel', async () => {
    const caso = await dadoInicioConNegocioElegido();
    await cuandoAbreAccesos(caso);
    entoncesNoHayUnBotonDeTextoParaCerrar();
  });
  it('muestra una cruz para cerrar los accesos rápidos', async () => {
    const caso = await dadoInicioConNegocioElegido();
    await cuandoAbreAccesos(caso);
    entoncesLaCruzDeCerrarEstaVisible();
  });
  it('cierra con la cruz y devuelve el foco al acceso', async () => {
    const caso = await dadoInicioConNegocioElegido();
    await cuandoAbreAccesos(caso);
    await cuandoCierraConLaCruz(caso);
    entoncesRecuperaFoco();
  });
  it('cierra con Escape y devuelve el foco al acceso', async () => {
    const caso = await dadoInicioConNegocioElegido();
    await cuandoAbreAccesos(caso);
    entoncesPrimeraAccionEnfocada();
    await cuandoCierraConEscape(caso);
    entoncesRecuperaFoco();
  });
});
