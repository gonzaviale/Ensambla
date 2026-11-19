import { describe, expect, it } from 'vitest';
import { burbujasDeSami } from '@/lib/burbujasDeSami';
import { cuandoSeCalcula, entoncesElResultadoEs } from '@/test/calculo';

const RESPUESTA_LARGA =
  'Entiendo lo que me contás: vendés velas aromáticas, las hacés vos misma y además atendés pedidos por encargo de tus clientas habituales. ' +
  'Para ordenarlo te propongo separar el stock de velas por aroma, llevar un registro de pedidos con la fecha de entrega y anotar cuánto te debe cada clienta. ' +
  'También podemos sumar una pestaña de gastos para controlar la cera, las esencias y los frascos. ' +
  'Si te parece bien arranco con eso y después lo vamos ajustando juntas. ' +
  '¿Querés que empiece por el stock o por los pedidos?';

const TEXTO_SIN_DONDE_CORTAR = 'palabra '.repeat(30).trim();

let burbujas: string[] = [];

describe('burbujasDeSami', () => {
  it('deja en una sola burbuja un texto corto de una oración', () => {
    cuandoSeCalcula(() => burbujasDeSami('¿Qué es lo principal que querés anotar?'));

    entoncesElResultadoEs(['¿Qué es lo principal que querés anotar?']);
  });

  it('parte en una burbuja por oración', () => {
    cuandoSeCalcula(() =>
      burbujasDeSami(
        'Hola, Facu. Contame qué hacés. Yo pienso una base y vos me decís si te sirve.',
      ),
    );

    entoncesElResultadoEs([
      'Hola, Facu.',
      'Contame qué hacés.',
      'Yo pienso una base y vos me decís si te sirve.',
    ]);
  });

  it('une las oraciones más cortas cuando hay más burbujas que las permitidas', () => {
    cuandoSeCalcula(() => burbujasDeSami('Uno. Dos. Tres. Cuatro. Cinco. Seis. Siete.'));

    entoncesElResultadoEs(['Uno. Dos.', 'Tres.', 'Cuatro.', 'Cinco.', 'Seis.', 'Siete.']);
  });

  it('corta una oración larga por la coma más cercana a la mitad', () => {
    cuandoSeCalcula(() =>
      burbujasDeSami(
        'Armé tus velas con su aroma, las clientas y los pedidos, y conecté cada pedido con la vela que lleva.',
      ),
    );

    entoncesElResultadoEs([
      'Armé tus velas con su aroma, las clientas y los pedidos',
      'y conecté cada pedido con la vela que lleva.',
    ]);
  });

  it('no corta dentro de una oración corta ni dentro de «comillas»', () => {
    cuandoSeCalcula(() =>
      burbujasDeSami(
        'Te propongo empezar con «Ventas» y sumarle la columna Zona. Mirá cómo queda.',
      ),
    );

    entoncesElResultadoEs([
      'Te propongo empezar con «Ventas» y sumarle la columna Zona.',
      'Mirá cómo queda.',
    ]);
  });

  it('deja en una burbuja un texto largo sin puntos ni comas donde cortar', () => {
    cuandoSeCalcula(() => burbujasDeSami(TEXTO_SIN_DONDE_CORTAR));

    entoncesElResultadoEs([TEXTO_SIN_DONDE_CORTAR]);
  });

  it('reparte una respuesta larga en varias burbujas, pero pocas', () => {
    cuandoSeReparteLaRespuestaLarga();

    entoncesSonEntreDosYSeisBurbujas();
  });

  it('las burbujas de una respuesta larga son más cortas que el texto entero', () => {
    cuandoSeReparteLaRespuestaLarga();

    entoncesNingunaBurbujaPasaLos125Caracteres();
  });

  it('no pierde texto al repartir una respuesta larga', () => {
    cuandoSeReparteLaRespuestaLarga();

    entoncesSeConservaLaUltimaPregunta();
  });
});

function cuandoSeReparteLaRespuestaLarga() {
  burbujas = burbujasDeSami(RESPUESTA_LARGA);
}

function entoncesSonEntreDosYSeisBurbujas() {
  expect(burbujas.length).toBeGreaterThan(1);
  expect(burbujas.length).toBeLessThanOrEqual(6);
}

function entoncesNingunaBurbujaPasaLos125Caracteres() {
  expect(Math.max(...burbujas.map((burbuja) => burbuja.length))).toBeLessThanOrEqual(125);
}

function entoncesSeConservaLaUltimaPregunta() {
  expect(burbujas.join(' ').replace(/\s+/g, ' ')).toContain('¿Querés que empiece por el stock');
}
