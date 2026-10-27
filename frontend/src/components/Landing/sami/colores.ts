// Colores de los bloques: los cinco de la marca (manual de identidad). Cada uno con su luz y su sombra.
export const COLORES_BLOQUE = {
  naranja: { base: '#ee8000', luz: '#ffa43a', sombra: '#b45f00' },
  amarillo: { base: '#ebce32', luz: '#fbe469', sombra: '#b99d12' },
  coral: { base: '#f85f5a', luz: '#ff8c87', sombra: '#c53f3b' },
  celeste: { base: '#4eb2d4', luz: '#84d1ea', sombra: '#2c88a9' },
  petroleo: { base: '#00506a', luz: '#1c7592', sombra: '#00364a' },
} as const;

export type ColorBloque = keyof typeof COLORES_BLOQUE;

export const COLORES: ColorBloque[] = ['naranja', 'amarillo', 'coral', 'celeste', 'petroleo'];
