import { describe, it } from 'vitest';
import {
  DOCE_EN_EL_REPARTO,
  LA_SEMANA,
  SIETE_PRODUCTOS,
  SIN_NUMERO,
  cuandoSeMuestranLasPartesDe,
  dadoElGrafico,
  dadoElSelector,
  entoncesDiceElMotivo,
  entoncesEstaElegido,
  entoncesNoHayDibujo,
  entoncesNoHaySelector,
  entoncesNoSeLee,
  entoncesOfreceLosGraficos,
  entoncesSeLee,
  entoncesSeVeElDibujo,
  entoncesSeVeElNumero,
  entoncesSeVenLasPartes,
} from '@/test/graficosDeMetrica';

describe('las partes que se muestran de una métrica repartida', () => {
  it('muestra todas las partes, sin «Otros» ni botón para ver más', () => {
    cuandoSeMuestranLasPartesDe(SIETE_PRODUCTOS);

    entoncesSeVenLasPartes(['Yerba', 'Coca', 'Pan', 'Sprite', 'Harina', 'Arroz', 'Fideos']);
  });

  it('con más de diez partes muestra las diez más grandes', () => {
    cuandoSeMuestranLasPartesDe(DOCE_EN_EL_REPARTO);

    entoncesSeVenLasPartes(Array.from({ length: 10 }, (_, lugar) => `Producto ${lugar + 1}`));
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

  it('dice qué es el número grande y quién encabeza el reparto', () => {
    dadoElGrafico(SIETE_PRODUCTOS, 'BARRAS');

    entoncesSeLee(/Total de todas las partes/);
    entoncesSeLee('Yerba, 70');
  });

  it('con los días de la semana no hay un «primero»', () => {
    dadoElGrafico(LA_SEMANA, 'LINEA');

    entoncesNoSeLee(/El primero/);
  });

  it('si no se pudo calcular dice por qué y no muestra un cero', () => {
    dadoElGrafico(SIN_NUMERO, 'BARRAS');

    entoncesDiceElMotivo('Todavía no cargaste ventas.');
  });
});

describe('el selector de gráfico', () => {
  it('ofrece sólo los gráficos posibles para la métrica', () => {
    dadoElSelector('BARRAS', ['NUMERO', 'BARRAS', 'DONA']);

    entoncesOfreceLosGraficos(['Barras', 'Torta']);
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
