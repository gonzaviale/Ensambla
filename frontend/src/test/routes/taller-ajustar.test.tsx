import { describe, it } from 'vitest';
import {
  cuandoSeGuardanLosCambios,
  cuandoSePideAjustarElAlmacenConSami,
  cuandoSePideCambiarTelefonoPorCelular,
  cuandoSePidePonerClientePrimero,
  cuandoSePideRenombrarClienteYPonerloPrimero,
  cuandoSeAbreElTallerDeUnSistemaQueNoExiste,
  cuandoSeVeLaTablaDeClientesAPantallaCompleta,
  cuandoSeVuelveALaPropuesta,
  cuandoSeVuelveSinGuardar,
  dadoQueSamiRenombraClienteYLoPonePrimero,
  dadoQueSePidioPonerClientePrimero,
  dadoQueSePidioRenombrarClienteYPonerloPrimero,
  dadoQueSeAjustaConSamiUnAlmacenDeAnaConEjemplos,
  dadoQueSeVieronLasFilasDelSistema,
  dadoUnAlmacenDeAnaConEjemplosAbierto,
  entoncesElSistemaGuardadoNoCambio,
  entoncesElTallerSeAbreConElSistemaParaAjustarlo,
  entoncesLosCambiosViajanAlServidor,
  entoncesSamiAplicaLoQuePidioSinTocarElSistemaGuardado,
  entoncesSamiPusoClientePrimeroEnLaPropuesta,
  entoncesSeArmaUnSistemaNuevo,
  entoncesSeVeLaTablaDeClientesAPantallaCompleta,
  entoncesSeVuelveALaPropuestaSinTablaCompleta,
  entoncesSeVuelveAlSistemaConLosCambiosGuardados,
  entoncesTelefonoPasaALlamarseCelular,
} from '@/test/ajusteConSami';

describe('ajustar con Sami un sistema que ya existe', () => {
  it('abre el taller con el sistema para ajustarlo', async () => {
    await dadoUnAlmacenDeAnaConEjemplosAbierto();

    await cuandoSePideAjustarElAlmacenConSami();

    await entoncesElTallerSeAbreConElSistemaParaAjustarlo();
  });

  it('aplica lo que pide Sami y el sistema no cambia hasta guardar', async () => {
    dadoQueSamiRenombraClienteYLoPonePrimero();
    await dadoQueSeAjustaConSamiUnAlmacenDeAnaConEjemplos();

    await cuandoSePideRenombrarClienteYPonerloPrimero();

    await entoncesSamiAplicaLoQuePidioSinTocarElSistemaGuardado();
  });

  it('al guardar reemplaza el sistema, vuelve a él y viaja al servidor', async () => {
    dadoQueSamiRenombraClienteYLoPonePrimero();
    await dadoUnAlmacenDeAnaConEjemplosAbierto();
    dadoQueSeVieronLasFilasDelSistema();
    await cuandoSePideAjustarElAlmacenConSami();
    await dadoQueSePidioRenombrarClienteYPonerloPrimero();

    await cuandoSeGuardanLosCambios();

    await entoncesSeVuelveAlSistemaConLosCambiosGuardados();
    await entoncesLosCambiosViajanAlServidor();
  });

  it('con el Sami simulado: «poné Cliente primero»', async () => {
    await dadoQueSeAjustaConSamiUnAlmacenDeAnaConEjemplos();

    await cuandoSePidePonerClientePrimero();

    entoncesSamiPusoClientePrimeroEnLaPropuesta();
  });

  it('si se vuelve sin guardar, el sistema no cambia', async () => {
    await dadoQueSeAjustaConSamiUnAlmacenDeAnaConEjemplos();
    await dadoQueSePidioPonerClientePrimero();

    await cuandoSeVuelveSinGuardar();

    await entoncesElSistemaGuardadoNoCambio();
  });

  it('con el Sami simulado: «cambiale el nombre a Teléfono por Celular»', async () => {
    await dadoQueSeAjustaConSamiUnAlmacenDeAnaConEjemplos();

    await cuandoSePideCambiarTelefonoPorCelular();

    await entoncesTelefonoPasaALlamarseCelular();
  });

  it('deja ver la tabla de una pieza de la propuesta a pantalla completa, como en el sistema', async () => {
    await dadoQueSeAjustaConSamiUnAlmacenDeAnaConEjemplos();

    await cuandoSeVeLaTablaDeClientesAPantallaCompleta();

    await entoncesSeVeLaTablaDeClientesAPantallaCompleta();
  });

  it('al volver de la tabla a pantalla completa se cierra el diálogo', async () => {
    await dadoQueSeAjustaConSamiUnAlmacenDeAnaConEjemplos();
    await cuandoSeVeLaTablaDeClientesAPantallaCompleta();

    await cuandoSeVuelveALaPropuesta();

    entoncesSeVuelveALaPropuestaSinTablaCompleta();
  });

  it('un sistema que no existe abre el taller para armar uno nuevo', async () => {
    await cuandoSeAbreElTallerDeUnSistemaQueNoExiste();

    entoncesSeArmaUnSistemaNuevo();
  });
});
