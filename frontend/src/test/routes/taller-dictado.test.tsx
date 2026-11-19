import { describe, it } from 'vitest';
import {
  conUnNavegadorConReconocimientoDeVoz,
  cuandoElNavegadorNiegaElPermiso,
  cuandoElNavegadorNoEscuchaNada,
  cuandoLaPersonaCortaElDictado,
  cuandoSeDictan,
  cuandoSeEnviaElMensaje,
  cuandoSeSaleDeLaPantalla,
  cuandoSeTocaElMicrofono,
  dadoQueElNavegadorFallaAlEmpezarAEscuchar,
  dadoQueSeAbreElTallerParaDictar,
  dadoQueSeDicto,
  dadoQueSeEmpezoADictar,
  dadoQueSeEscribioEnElCampo,
  entoncesAvisaQueNoSeEscuchoNada,
  entoncesAvisaQueNoSePudoDictar,
  entoncesDejaDeEscucharYConservaLoDictado,
  entoncesElBotonNoQuedaApretado,
  entoncesElBotonQuedaApretado,
  entoncesEmpiezaAEscucharEnEspanolRioplatense,
  entoncesExplicaComoHabilitarElMicrofono,
  entoncesExplicaQueElNavegadorNoPermiteDictar,
  entoncesLoDictadoLlegaAlCampoParaRevisarlo,
  entoncesNoApareceNingunError,
  entoncesSeAbortaElReconocimiento,
  entoncesSeCortaElMicrofono,
  entoncesSePuedeSeguirEscribiendo,
} from '@/test/dictadoDelTaller';

describe('el dictado por voz del taller de Sami', () => {
  conUnNavegadorConReconocimientoDeVoz();

  it('empieza a escuchar en español rioplatense y el botón queda apretado', async () => {
    await dadoQueSeAbreElTallerParaDictar();

    await cuandoSeTocaElMicrofono();

    entoncesEmpiezaAEscucharEnEspanolRioplatense();
    entoncesElBotonQuedaApretado();
  });

  it('lo dictado llega al campo igual que si se hubiera escrito, para revisarlo antes de enviar', async () => {
    await dadoQueSeAbreElTallerParaDictar();
    await dadoQueSeEscribioEnElCampo('Tengo un kiosco.');
    await dadoQueSeEmpezoADictar();

    cuandoSeDictan('vendo golosinas', ' y cigarrillos');

    entoncesLoDictadoLlegaAlCampoParaRevisarlo();
  });

  it('al volver a tocar el botón deja de escuchar y lo dictado se conserva', async () => {
    await dadoQueSeAbreElTallerParaDictar();
    await dadoQueSeEmpezoADictar();
    dadoQueSeDicto('vendo velas');

    await cuandoSeTocaElMicrofono();

    entoncesDejaDeEscucharYConservaLoDictado();
    entoncesElBotonNoQuedaApretado();
  });

  it('si el navegador no tiene permiso, explica cómo habilitar el micrófono', async () => {
    await dadoQueSeAbreElTallerParaDictar();
    await dadoQueSeEmpezoADictar();

    cuandoElNavegadorNiegaElPermiso();

    await entoncesExplicaComoHabilitarElMicrofono();
    entoncesElBotonNoQuedaApretado();
  });

  it('sin permiso para el micrófono se puede seguir escribiendo', async () => {
    await dadoQueSeAbreElTallerParaDictar();
    await dadoQueSeEmpezoADictar();

    cuandoElNavegadorNiegaElPermiso();

    await entoncesSePuedeSeguirEscribiendo();
  });

  it('si no se escuchó nada, avisa sin molestar con códigos', async () => {
    await dadoQueSeAbreElTallerParaDictar();
    await dadoQueSeEmpezoADictar();

    cuandoElNavegadorNoEscuchaNada();

    await entoncesAvisaQueNoSeEscuchoNada();
  });

  it('cuando la persona corta el dictado no aparece ningún error', async () => {
    await dadoQueSeAbreElTallerParaDictar();
    await dadoQueSeEmpezoADictar();

    cuandoLaPersonaCortaElDictado();

    entoncesNoApareceNingunError();
  });

  it('si start falla, avisa y el botón no queda apretado', async () => {
    await dadoQueSeAbreElTallerParaDictar();
    dadoQueElNavegadorFallaAlEmpezarAEscuchar();

    await cuandoSeTocaElMicrofono();

    await entoncesAvisaQueNoSePudoDictar();
    entoncesElBotonNoQuedaApretado();
  });

  it('al enviar el mensaje corta el micrófono', async () => {
    await dadoQueSeAbreElTallerParaDictar();
    await dadoQueSeEmpezoADictar();
    dadoQueSeDicto('Tengo un almacén y vendo productos');

    await cuandoSeEnviaElMensaje();

    entoncesSeCortaElMicrofono();
  });

  it('al salir de la pantalla corta el micrófono', async () => {
    await dadoQueSeAbreElTallerParaDictar();
    await dadoQueSeEmpezoADictar();

    cuandoSeSaleDeLaPantalla();

    entoncesSeAbortaElReconocimiento();
  });
});

describe('el dictado en un navegador sin reconocimiento de voz', () => {
  it('el botón explica que no se puede', async () => {
    await dadoQueSeAbreElTallerParaDictar();

    await cuandoSeTocaElMicrofono();

    await entoncesExplicaQueElNavegadorNoPermiteDictar();
  });

  it('la persona sigue escribiendo', async () => {
    await dadoQueSeAbreElTallerParaDictar();

    await cuandoSeTocaElMicrofono();

    await entoncesSePuedeSeguirEscribiendo();
  });
});
