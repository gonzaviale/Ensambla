import { describe, it } from 'vitest';
import {
  cuandoSeRespondeLaDuda,
  cuandoSeRespondenLasDosPrimerasDudas,
  cuandoSeRespondeSaltandoUnaDudaYEscribiendoOtra,
  cuandoSeRespondenLasTresDudas,
  cuandoSeRespondenLasTresDudasConLasPrimerasOpciones,
  cuandoSeSaltanLasTresDudas,
  dadoQueSamiPlanteoTresDudas,
  dadoQueSamiPlanteoUnaSolaDuda,
  entoncesLaBotoneraEsLaDeSiempreSinProgresoNiSaltear,
  entoncesLaHistoriaIncluyeLasDudasQueSamiPlanteo,
  entoncesLaRespuestaVaTalCual,
  entoncesLoSalteadoNoViaja,
  entoncesMandaTodasLasRespuestasJuntasEnUnSoloMensaje,
  entoncesMuestraLaPrimeraDudaConSuProgreso,
  entoncesMuestraLaTerceraDudaSinMandarNada,
  entoncesNoSeMandaNadaYLaBotoneraDesaparece,
} from '@/test/dudasDelTaller';

describe('varias dudas de Sami en un mismo turno', () => {
  it('las muestra de a una con su progreso', async () => {
    await dadoQueSamiPlanteoTresDudas();

    await entoncesMuestraLaPrimeraDudaConSuProgreso();
  });

  it('avanza de duda en duda sin mandar nada hasta la última', async () => {
    await dadoQueSamiPlanteoTresDudas();

    await cuandoSeRespondenLasDosPrimerasDudas();

    await entoncesMuestraLaTerceraDudaSinMandarNada();
  });

  it('manda todas las respuestas juntas en un solo mensaje', async () => {
    await dadoQueSamiPlanteoTresDudas();

    await cuandoSeRespondenLasTresDudas();

    await entoncesMandaTodasLasRespuestasJuntasEnUnSoloMensaje();
  });

  it('la historia que viaja a Sami incluye las dudas que había planteado', async () => {
    await dadoQueSamiPlanteoTresDudas();

    await cuandoSeRespondenLasTresDudasConLasPrimerasOpciones();

    entoncesLaHistoriaIncluyeLasDudasQueSamiPlanteo();
  });

  it('se puede saltear una duda y escribir otra respuesta; lo salteado no viaja', async () => {
    await dadoQueSamiPlanteoTresDudas();

    await cuandoSeRespondeSaltandoUnaDudaYEscribiendoOtra();

    await entoncesLoSalteadoNoViaja();
  });

  it('si se saltean todas no se manda nada y la botonera desaparece', async () => {
    await dadoQueSamiPlanteoTresDudas();

    await cuandoSeSaltanLasTresDudas();

    entoncesNoSeMandaNadaYLaBotoneraDesaparece();
  });

  it('una sola duda sigue siendo la botonera de siempre: sin progreso ni saltear', async () => {
    await dadoQueSamiPlanteoUnaSolaDuda();

    await entoncesLaBotoneraEsLaDeSiempreSinProgresoNiSaltear();
  });

  it('con una sola duda la respuesta va tal cual', async () => {
    await dadoQueSamiPlanteoUnaSolaDuda();

    await cuandoSeRespondeLaDuda(/^Sí/);

    await entoncesLaRespuestaVaTalCual();
  });
});
