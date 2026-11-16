import type {
  OpcionDeCartel,
  PromocionSugerida,
  TextosDeCartel,
  VarianteDeCartel,
} from '@/types/carteles';

export const PROMOCION_DE_PRUEBA: PromocionSugerida = {
  tipo: 'PROMO_PRECIO',
  productos: ['Aceite de girasol 900 ml'],
  precioAnterior: 2500,
  precioNuevo: 2000,
  ahorro: 500,
  descuentoPorcentaje: 20,
};

export const TEXTOS_DE_PRUEBA: TextosDeCartel = {
  plantilla: 'PROMO_PRECIO',
  titular: 'Aceite a precio de vecino',
  bajada: '20 % menos esta semana',
  textoPosteo: 'El aceite de 900 ml pasa de $2.500 a $2.000. ¡Pasá por Don Rufino!',
  redactadoConIa: true,
};

const VARIANTES: VarianteDeCartel[] = ['BLOQUE', 'FRANJA', 'MARCO'];

const dibujoDe = (variante: VarianteDeCartel, titular: string) =>
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><text x="5" y="50">${variante}: ${titular}</text></svg>`;

export const opcionesDe = (titular: string): OpcionDeCartel[] =>
  VARIANTES.map((variante) => ({ variante, dibujo: dibujoDe(variante, titular) }));
