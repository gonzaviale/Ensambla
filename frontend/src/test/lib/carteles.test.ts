import { describe, expect, it } from 'vitest';
import {
  contenidoDe,
  dibujoComoImagen,
  enlaceDeWhatsapp,
  pedidoDeTextos,
  pesos,
} from '@/lib/carteles';
import { PROMOCION_DE_PRUEBA, TEXTOS_DE_PRUEBA } from '@/mocks/db/carteles';

describe('Carteles', () => {
  it('escribe los precios en pesos con el punto de miles', () => {
    expect(pesos(2500)).toBe('$2.500');
  });

  it('le pasa a la redacción sólo los números calculados de la promoción', () => {
    expect(pedidoDeTextos(PROMOCION_DE_PRUEBA)).toEqual({
      tiposPosibles: ['PROMO_PRECIO'],
      productos: ['Aceite de girasol 900 ml'],
      numeros: [2500, 2000, 500, 20],
    });
  });

  it('arma el contenido del cartel con el precio nuevo como protagonista', () => {
    const contenido = contenidoDe(PROMOCION_DE_PRUEBA, TEXTOS_DE_PRUEBA, 'A4');

    expect(contenido).toMatchObject({
      tipo: 'PROMO_PRECIO',
      formato: 'A4',
      protagonista: '$2.000',
      detalle: 'Antes $2.500 · Aceite de girasol 900 ml',
    });
  });

  it('muestra el dibujo del servidor como imagen y no como marcado', () => {
    expect(dibujoComoImagen('<svg/>')).toBe('data:image/svg+xml;charset=utf-8,%3Csvg%2F%3E');
  });

  it('arma el enlace de WhatsApp con el texto escapado', () => {
    expect(enlaceDeWhatsapp('Oferta 20 % hoy')).toBe('https://wa.me/?text=Oferta%2020%20%25%20hoy');
  });
});
