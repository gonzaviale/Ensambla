import { describe, it } from 'vitest';
import {
  TIPOS_CON_VALIDACION,
  cuandoSeGuardaUnValorAusente,
  cuandoSeGuardaUnValorCargadoAMano,
  cuandoSeInterpreta,
  cuandoSeInterpretaAlgoQueNoSirve,
  cuandoSeInterpretaLoAnotadoAMano,
  cuandoSeInterpretaTextoMuyLargo,
  cuandoSeMuestra,
  cuandoSeMuestraComoTexto,
  cuandoSePreguntaSiFalta,
  cuandoSeProponeUnEjemploParaElRegistro,
  cuandoSeValida,
  cuandoSeValidaUnValorInvalido,
  cuandoSeValidaUnValorValido,
  dadoUnCampoConLosCasosDe,
  dadoUnCampoDe,
  entoncesElErrorEs,
  entoncesElErrorEsElDelTipo,
  entoncesElTextoEs,
  entoncesElTextoEsElEsperado,
  entoncesElTextoSeRecortaA,
  entoncesNoHayError,
  entoncesNoSeObtieneNada,
  entoncesSeGuardaConSuTipo,
  entoncesSeObtiene,
  entoncesSeObtieneElValorInterpretado,
} from '@/test/comportamientoDeCampo';

describe.each(TIPOS_CON_VALIDACION)('el campo $tipo', (caso) => {
  it('acepta un valor válido', () => {
    dadoUnCampoConLosCasosDe(caso);

    cuandoSeValidaUnValorValido();

    entoncesNoHayError();
  });

  it('rechaza un valor inválido con el mensaje de su tipo', () => {
    dadoUnCampoConLosCasosDe(caso);

    cuandoSeValidaUnValorInvalido();

    entoncesElErrorEsElDelTipo();
  });

  it('guarda lo que se carga con el tipo que le corresponde', () => {
    dadoUnCampoConLosCasosDe(caso);

    cuandoSeGuardaUnValorCargadoAMano();

    entoncesSeGuardaConSuTipo();
  });

  it('se muestra como texto', () => {
    dadoUnCampoConLosCasosDe(caso);

    cuandoSeMuestraComoTexto();

    entoncesElTextoEsElEsperado();
  });

  it('entiende lo que se anota a mano', () => {
    dadoUnCampoConLosCasosDe(caso);

    cuandoSeInterpretaLoAnotadoAMano();

    entoncesSeObtieneElValorInterpretado();
  });

  it('descarta lo anotado que no sirve', () => {
    dadoUnCampoConLosCasosDe(caso);

    cuandoSeInterpretaAlgoQueNoSirve();

    entoncesNoSeObtieneNada();
  });
});

describe.each(['text', 'textarea', 'tel'] as const)('el campo de texto %s', (tipo) => {
  it('acepta cualquier texto', () => {
    dadoUnCampoDe(tipo);

    cuandoSeValida('lo que sea');

    entoncesNoHayError();
  });

  it('guarda vacío lo que viene sin valor', () => {
    dadoUnCampoDe(tipo);

    cuandoSeGuardaUnValorAusente();

    entoncesSeObtiene('');
  });

  it('recorta el texto demasiado largo', () => {
    dadoUnCampoDe(tipo);

    cuandoSeInterpretaTextoMuyLargo();

    entoncesElTextoSeRecortaA(200);
  });

  it('propone un registro numerado si es obligatorio', () => {
    dadoUnCampoDe(tipo, true);

    cuandoSeProponeUnEjemploParaElRegistro(1);

    entoncesSeObtiene('Registro 2');
  });
});

describe('el campo checkbox', () => {
  it('falta cuando no está tildado', () => {
    dadoUnCampoDe('checkbox');

    cuandoSePreguntaSiFalta(false);

    entoncesSeObtiene(true);
  });

  it('se muestra como Sí cuando está tildado', () => {
    dadoUnCampoDe('checkbox');

    cuandoSeMuestra(true);

    entoncesElTextoEs('Sí');
  });

  it('entiende una x como tildado', () => {
    dadoUnCampoDe('checkbox');

    cuandoSeInterpreta('x');

    entoncesSeObtiene(true);
  });
});

describe('el campo relation', () => {
  it('rechaza un registro que no existe en el bloque vinculado', () => {
    dadoUnCampoDe('relation');

    cuandoSeValida('inexistente');

    entoncesElErrorEs('Elegí un registro del bloque vinculado.');
  });

  it('reparte los vínculos disponibles entre los registros de ejemplo', () => {
    dadoUnCampoDe('relation');

    cuandoSeProponeUnEjemploParaElRegistro(3, ['a', 'b']);

    entoncesSeObtiene('b');
  });
});

describe('el mínimo de un campo', () => {
  it('rechaza un número por debajo del mínimo', () => {
    dadoUnCampoDe('number', false, 5);

    cuandoSeValida(2);

    entoncesElErrorEs('El mínimo es 5.');
  });
});
