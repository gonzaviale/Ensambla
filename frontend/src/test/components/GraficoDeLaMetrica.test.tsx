import { describe, it } from 'vitest';
import {
  LA_SEMANA,
  SIETE_PRODUCTOS,
  SIN_NUMERO,
  cuandoSeMuestranLasPartesConElCalculo,
  cuandoSeMuestranLasPartesDe,
  dadoElGrafico,
  dadoElSelector,
  entoncesDiceElMotivo,
  entoncesEstaElegido,
  entoncesNoHayDibujo,
  entoncesNoHaySelector,
  entoncesNoOfreceVerTodas,
  entoncesOfreceLosGraficos,
  entoncesOfreceVerTodas,
  entoncesOtrosSuma,
  entoncesSeVeElDibujo,
  entoncesSeVeElNumero,
  entoncesSeVenLasPartes,
} from '@/test/graficosDeMetrica';

describe('las partes que se muestran de una métrica repartida', () => {
  it('muestra las cinco más grandes y junta el resto en «Otros»', () => {
    cuandoSeMuestranLasPartesDe(SIETE_PRODUCTOS);

    entoncesSeVenLasPartes(['Yerba', 'Coca', 'Pan', 'Sprite', 'Harina', 'Otros']);
  });

  it('«Otros» es lo que falta para llegar al total', () => {
    cuandoSeMuestranLasPartesDe(SIETE_PRODUCTOS);

    entoncesOtrosSuma(50);
  });

  it('con «Ver todo» muestra todas las partes', () => {
    cuandoSeMuestranLasPartesDe(SIETE_PRODUCTOS, true);

    entoncesSeVenLasPartes(['Yerba', 'Coca', 'Pan', 'Sprite', 'Harina', 'Arroz', 'Fideos']);
  });

  it('un promedio no se junta en «Otros»: sólo muestra las cinco más grandes', () => {
    cuandoSeMuestranLasPartesConElCalculo('PROMEDIO');

    entoncesSeVenLasPartes(['Yerba', 'Coca', 'Pan', 'Sprite', 'Harina']);
  });

  it('los días de la semana se muestran siempre los siete', () => {
    cuandoSeMuestranLasPartesDe(LA_SEMANA);

    entoncesSeVenLasPartes([
      'lunes',
      'martes',
      'miércoles',
      'jueves',
      'viernes',
      'sábado',
      'domingo',
    ]);
  });
});

describe('el gráfico de una métrica', () => {
  it('siempre muestra el número grande', () => {
    dadoElGrafico(SIETE_PRODUCTOS, 'NUMERO');

    entoncesSeVeElNumero('300');
  });

  it('como número no dibuja nada más', () => {
    dadoElGrafico(SIETE_PRODUCTOS, 'NUMERO');

    entoncesNoHayDibujo('Lo más vendido');
  });

  it.each(['BARRAS', 'DONA'] as const)('como %s dibuja el reparto', (grafico) => {
    dadoElGrafico(SIETE_PRODUCTOS, grafico);

    entoncesSeVeElDibujo('Lo más vendido');
  });

  it('como línea dibuja los días de la semana', () => {
    dadoElGrafico(LA_SEMANA, 'LINEA');

    entoncesSeVeElDibujo('Ventas por día');
  });

  it('con más de cinco partes ofrece verlas todas', () => {
    dadoElGrafico(SIETE_PRODUCTOS, 'BARRAS');

    entoncesOfreceVerTodas('Ver las 7');
  });

  it('con los días de la semana no ofrece «Ver todo»: ya están todos', () => {
    dadoElGrafico(LA_SEMANA, 'BARRAS');

    entoncesNoOfreceVerTodas();
  });

  it('si no se pudo calcular dice por qué y no muestra un cero', () => {
    dadoElGrafico(SIN_NUMERO, 'BARRAS');

    entoncesDiceElMotivo('Todavía no cargaste ventas.');
  });
});

describe('el selector de gráfico', () => {
  it('ofrece sólo los gráficos posibles para la métrica', () => {
    dadoElSelector('BARRAS', ['NUMERO', 'BARRAS', 'DONA']);

    entoncesOfreceLosGraficos(['Número', 'Barras', 'Torta']);
  });

  it('marca el gráfico elegido', () => {
    dadoElSelector('DONA', ['NUMERO', 'BARRAS', 'DONA']);

    entoncesEstaElegido('Torta');
  });

  it('con un solo gráfico posible no se muestra', () => {
    dadoElSelector('NUMERO', ['NUMERO']);

    entoncesNoHaySelector();
  });
});
