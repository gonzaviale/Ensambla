import { describe, it } from 'vitest';
import {
  cuandoSeBuscaElDestinoDelAviso,
  dadoUnAvisoDeLaZona,
  dadoUnAvisoSinRegistroConUnaAccionQueAplicaSobreUnaFila,
  dadoUnAvisoSinRegistroConUnaAccionQueSoloApuntaAUnaTabla,
  dadoUnAvisoSobreUnRegistroYUnaAccionQueRegistraEnOtraTabla,
  entoncesElDestinoEsElRegistroDelAviso,
  entoncesElDestinoEsLaFilaDeLaAccion,
  entoncesElDestinoEsSoloLaTabla,
  entoncesNoApuntaANingunObjeto,
} from '@/test/destinosDeAviso';

describe('destinoDeAviso', () => {
  it('prefiere el registro del aviso a cualquier tabla', () => {
    dadoUnAvisoSobreUnRegistroYUnaAccionQueRegistraEnOtraTabla();

    cuandoSeBuscaElDestinoDelAviso();

    entoncesElDestinoEsElRegistroDelAviso();
  });

  it('si no hay registro, usa el de la acción que lo tenga', () => {
    dadoUnAvisoSinRegistroConUnaAccionQueAplicaSobreUnaFila();

    cuandoSeBuscaElDestinoDelAviso();

    entoncesElDestinoEsLaFilaDeLaAccion();
  });

  it('si ninguna acción tiene registro, usa la tabla', () => {
    dadoUnAvisoSinRegistroConUnaAccionQueSoloApuntaAUnaTabla();

    cuandoSeBuscaElDestinoDelAviso();

    entoncesElDestinoEsSoloLaTabla();
  });

  it('un aviso de la zona no apunta a ningún objeto', () => {
    dadoUnAvisoDeLaZona();

    cuandoSeBuscaElDestinoDelAviso();

    entoncesNoApuntaANingunObjeto();
  });
});
