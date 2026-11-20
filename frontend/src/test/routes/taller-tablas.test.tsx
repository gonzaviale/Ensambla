import { describe, it } from 'vitest';
import {
  cuandoSamiCargaRegistrosNuevosDeUnaFoto,
  cuandoSeAbreUnAlmacenConMuchosRegistros,
  cuandoSeAgregaLaPestana,
  cuandoSeBuscaEnElBloque,
  cuandoSePideMostrarMasRegistros,
  cuandoSePideMostrarMasRegistrosDosVeces,
  cuandoSeQuitaLaPestanaSobrante,
  dadoQueLaSeccionTieneOtraPestana,
  dadoQueSamiCargoRegistrosNuevosDeUnaFoto,
  dadoQueSeMostraronDiezRegistrosMas,
  dadoUnAlmacenConMuchosRegistrosEnElTaller,
  entoncesElBuscadorFiltraSobreTodosLosRegistros,
  entoncesLaPestanaSeQuitaDeLaPropuesta,
  entoncesLosRegistrosNuevosVanPrimeroResaltadosYSeAvisaCuantosSon,
  entoncesNoSeMuestranLasPestanas,
  entoncesSeMuestranDiezRegistrosConScrollDentroDelBloque,
  entoncesSeMuestranLasDosPestanasSoloEnLaPropuesta,
  entoncesSeMuestranTodosLosRegistrosYDesapareceElBoton,
  entoncesSeMuestranVeinteRegistros,
  entoncesYaNoSeMuestranLosCartelesViejos,
} from '@/test/tablasDelTaller';

describe('las tablas de la propuesta de Sami', () => {
  it('muestran primero y resaltados los registros nuevos, y avisan cuántos son', async () => {
    await dadoUnAlmacenConMuchosRegistrosEnElTaller();

    await cuandoSamiCargaRegistrosNuevosDeUnaFoto();

    await entoncesLosRegistrosNuevosVanPrimeroResaltadosYSeAvisaCuantosSon();
  });

  it('muestran los registros de a 10 con el scroll dentro del bloque', async () => {
    await dadoUnAlmacenConMuchosRegistrosEnElTaller();

    await cuandoSamiCargaRegistrosNuevosDeUnaFoto();

    entoncesSeMuestranDiezRegistrosConScrollDentroDelBloque();
  });

  it('«Mostrar 10 más» suma otra tanda', async () => {
    await dadoUnAlmacenConMuchosRegistrosEnElTaller();
    await dadoQueSamiCargoRegistrosNuevosDeUnaFoto();

    await cuandoSePideMostrarMasRegistros();

    entoncesSeMuestranVeinteRegistros();
  });

  it('al pedir más hasta completar se muestran todos y desaparece el botón', async () => {
    await dadoUnAlmacenConMuchosRegistrosEnElTaller();
    await dadoQueSamiCargoRegistrosNuevosDeUnaFoto();

    await cuandoSePideMostrarMasRegistrosDosVeces();

    entoncesSeMuestranTodosLosRegistrosYDesapareceElBoton();
  });

  it('el buscador del bloque filtra sobre todos los registros y vuelve a la primera tanda', async () => {
    await dadoUnAlmacenConMuchosRegistrosEnElTaller();
    await dadoQueSamiCargoRegistrosNuevosDeUnaFoto();
    await dadoQueSeMostraronDiezRegistrosMas();

    await cuandoSeBuscaEnElBloque('guardado 24');

    entoncesElBuscadorFiltraSobreTodosLosRegistros();
  });

  it('con una sola pestaña no se muestran las pestañas', async () => {
    await cuandoSeAbreUnAlmacenConMuchosRegistros();

    entoncesNoSeMuestranLasPestanas();
  });

  it('con más de una pestaña se muestran, y es la propuesta la que cambia', async () => {
    await dadoUnAlmacenConMuchosRegistrosEnElTaller();

    await cuandoSeAgregaLaPestana('Ventas');

    await entoncesSeMuestranLasDosPestanasSoloEnLaPropuesta();
  });

  it('las pestañas se quitan desde el desplegable, sobre la propuesta', async () => {
    await dadoUnAlmacenConMuchosRegistrosEnElTaller();
    await dadoQueLaSeccionTieneOtraPestana();

    await cuandoSeQuitaLaPestanaSobrante();

    entoncesLaPestanaSeQuitaDeLaPropuesta();
  });

  it('ya no muestra «Distribuí tus piezas», «Sumar una pieza» ni el cartel de sin guardar', async () => {
    await cuandoSeAbreUnAlmacenConMuchosRegistros();

    entoncesYaNoSeMuestranLosCartelesViejos();
  });
});
