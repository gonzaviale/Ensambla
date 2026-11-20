import { describe, it } from 'vitest';
import {
  cuandoCambiaElOrden,
  cuandoSeArmaElPodioConLaElegida,
  cuandoSeArmaElPodioSinElegida,
  cuandoSeMuestranLosPuntosDe,
  cuandoSeTraducenLasRespuestas,
  dadoQueContestoQue,
  dadoUnRankingDeCincoZonas,
  entoncesHayPuntos,
  entoncesLaDistanciaMaximaEs,
  entoncesLaImportanciaDeLasEscuelasEs,
  entoncesLosCambiosSon,
  entoncesElPublicoEs,
  entoncesSeCompara,
  entoncesSinLimiteDeDistancia,
} from '@/test/recorridoDondeAbrir';

describe('Las respuestas de Sami se vuelven filtros', () => {
  it('buscar cerca deja la distancia de siempre', () => {
    const respuestas = dadoQueContestoQue({ alcance: 'cerca' });

    const filtros = cuandoSeTraducenLasRespuestas(respuestas);

    entoncesLaDistanciaMaximaEs(filtros, 4);
  });

  it('buscar en toda la zona saca el límite de distancia', () => {
    const respuestas = dadoQueContestoQue({ alcance: 'toda' });

    const filtros = cuandoSeTraducenLasRespuestas(respuestas);

    entoncesSinLimiteDeDistancia(filtros);
  });

  it('el público elegido pasa a los filtros', () => {
    const respuestas = dadoQueContestoQue({ publico: 'estudiantes' });

    const filtros = cuandoSeTraducenLasRespuestas(respuestas);

    entoncesElPublicoEs(filtros, 'estudiantes');
  });

  it('priorizar escuelas y plazas les da mucha importancia', () => {
    const respuestas = dadoQueContestoQue({ prioridad: 'escuelas' });

    const filtros = cuandoSeTraducenLasRespuestas(respuestas);

    entoncesLaImportanciaDeLasEscuelasEs(filtros, 2);
  });
});

describe('Las zonas que se comparan de un vistazo', () => {
  it('son las tres mejores cuando no se eligió ninguna', () => {
    const ranking = dadoUnRankingDeCincoZonas();

    const zonas = cuandoSeArmaElPodioSinElegida(ranking);

    entoncesSeCompara(zonas, ranking, [0, 1, 2]);
  });

  it('son las tres mejores si la elegida ya está en el podio', () => {
    const ranking = dadoUnRankingDeCincoZonas();

    const zonas = cuandoSeArmaElPodioConLaElegida(ranking, 1);

    entoncesSeCompara(zonas, ranking, [0, 1, 2]);
  });

  it('suman la elegida cuando está fuera del podio', () => {
    const ranking = dadoUnRankingDeCincoZonas();

    const zonas = cuandoSeArmaElPodioConLaElegida(ranking, 4);

    entoncesSeCompara(zonas, ranking, [0, 1, 2, 4]);
  });
});

describe('Los puntos que se dibujan en el mapa', () => {
  it('con «solo en la zona elegida» salen los de esa zona', () => {
    const ranking = dadoUnRankingDeCincoZonas();

    const puntos = cuandoSeMuestranLosPuntosDe('elegida', ranking, ranking[2]);

    entoncesHayPuntos(puntos, 1);
  });

  it('con «solo en la zona elegida» y sin zona elegida no sale ninguno', () => {
    const ranking = dadoUnRankingDeCincoZonas();

    const puntos = cuandoSeMuestranLosPuntosDe('elegida', ranking, undefined);

    entoncesHayPuntos(puntos, 0);
  });

  it('con «en todas las zonas» salen los de todo el ranking', () => {
    const ranking = dadoUnRankingDeCincoZonas();

    const puntos = cuandoSeMuestranLosPuntosDe('todas', ranking, ranking[0]);

    entoncesHayPuntos(puntos, 5);
  });
});

describe('Quién subió y quién bajó con el último cambio', () => {
  it('marca cuántos puestos se movió cada zona', () => {
    const cambios = cuandoCambiaElOrden(['a', 'b', 'c'], ['b', 'a', 'c']);

    entoncesLosCambiosSon(cambios, { b: 1, a: -1, c: 0 });
  });

  it('marca como nueva a la zona que antes estaba afuera', () => {
    const cambios = cuandoCambiaElOrden(['a', 'b'], ['a', 'z', 'b']);

    entoncesLosCambiosSon(cambios, { a: 0, z: 'nueva', b: -1 });
  });

  it('no marca nada cuando todavía no había un orden anterior', () => {
    const cambios = cuandoCambiaElOrden([], []);

    entoncesLosCambiosSon(cambios, {});
  });
});
