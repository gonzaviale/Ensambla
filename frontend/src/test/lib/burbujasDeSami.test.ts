import { describe, expect, it } from 'vitest';
import { burbujasDeSami } from '@/lib/burbujasDeSami';

const RESPUESTA_LARGA =
  'Entiendo lo que me contás: vendés velas aromáticas, las hacés vos misma y además atendés pedidos por encargo de tus clientas habituales. ' +
  'Para ordenarlo te propongo separar el stock de velas por aroma, llevar un registro de pedidos con la fecha de entrega y anotar cuánto te debe cada clienta. ' +
  'También podemos sumar una pestaña de gastos para controlar la cera, las esencias y los frascos. ' +
  'Si te parece bien arranco con eso y después lo vamos ajustando juntas. ' +
  '¿Querés que empiece por el stock o por los pedidos?';

describe('burbujasDeSami', () => {
  it('deja en una sola burbuja un texto corto de una oración', () => {
    expect(burbujasDeSami('¿Qué es lo principal que querés anotar?')).toEqual([
      '¿Qué es lo principal que querés anotar?',
    ]);
  });

  it('parte en una burbuja por oración', () => {
    expect(
      burbujasDeSami(
        'Hola, Facu. Contame qué hacés. Yo pienso una base y vos me decís si te sirve.',
      ),
    ).toEqual([
      'Hola, Facu.',
      'Contame qué hacés.',
      'Yo pienso una base y vos me decís si te sirve.',
    ]);
  });

  it('une las oraciones más cortas cuando hay más burbujas que las permitidas', () => {
    expect(burbujasDeSami('Uno. Dos. Tres. Cuatro. Cinco. Seis. Siete.')).toEqual([
      'Uno. Dos.',
      'Tres.',
      'Cuatro.',
      'Cinco.',
      'Seis.',
      'Siete.',
    ]);
  });

  it('corta una oración larga por la coma más cercana a la mitad', () => {
    expect(
      burbujasDeSami(
        'Armé tus velas con su aroma, las clientas y los pedidos, y conecté cada pedido con la vela que lleva.',
      ),
    ).toEqual([
      'Armé tus velas con su aroma, las clientas y los pedidos',
      'y conecté cada pedido con la vela que lleva.',
    ]);
  });

  it('no corta dentro de una oración corta ni dentro de «comillas»', () => {
    expect(
      burbujasDeSami(
        'Te propongo empezar con «Ventas» y sumarle la columna Zona. Mirá cómo queda.',
      ),
    ).toEqual(['Te propongo empezar con «Ventas» y sumarle la columna Zona.', 'Mirá cómo queda.']);
  });

  it('deja en una burbuja un texto largo sin puntos ni comas donde cortar', () => {
    const texto = 'palabra '.repeat(30).trim();
    expect(burbujasDeSami(texto)).toEqual([texto]);
  });

  it('reparte una respuesta larga en pocas burbujas más cortas que el texto entero', () => {
    const burbujas = burbujasDeSami(RESPUESTA_LARGA);
    expect(burbujas.length).toBeGreaterThan(1);
    expect(burbujas.length).toBeLessThanOrEqual(6);
    expect(Math.max(...burbujas.map((burbuja) => burbuja.length))).toBeLessThanOrEqual(125);
    expect(burbujas.join(' ').replace(/\s+/g, ' ')).toContain('¿Querés que empiece por el stock');
  });
});
