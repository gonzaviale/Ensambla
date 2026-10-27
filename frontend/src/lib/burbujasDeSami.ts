const MAXIMO_DE_BURBUJAS = 6;
const LARGO_MAXIMO_DE_PARTE = 90;
const LARGO_MINIMO_DE_PARTE = 25;

const comaFueraDeComillas = (texto: string, indice: number) => {
  const antes = texto.slice(0, indice);
  return antes.split('«').length === antes.split('»').length;
};

/** Una oración larga se corta por la coma más cercana a la mitad; sin coma que sirva, queda como está. */
function cortarEnComa(oracion: string): string[] {
  if (oracion.length <= LARGO_MAXIMO_DE_PARTE) return [oracion];
  const medio = oracion.length / 2;
  const comas = [...oracion.matchAll(/,\s+/g)]
    .map((coma) => coma.index)
    .filter(
      (indice) =>
        indice >= LARGO_MINIMO_DE_PARTE &&
        oracion.length - indice >= LARGO_MINIMO_DE_PARTE &&
        comaFueraDeComillas(oracion, indice),
    );
  if (!comas.length) return [oracion];
  const corte = comas.reduce((mejor, indice) =>
    Math.abs(indice - medio) < Math.abs(mejor - medio) ? indice : mejor,
  );
  return [oracion.slice(0, corte + 1), oracion.slice(corte + 1).trim()].flatMap(cortarEnComa);
}

/** Con más partes que burbujas permitidas, une de a dos las vecinas más cortas. */
function unirLasMasCortas(partes: string[]): string[] {
  const unidas = [...partes];
  while (unidas.length > MAXIMO_DE_BURBUJAS) {
    const largos = unidas.map((parte, i) => parte.length + (unidas[i + 1]?.length ?? Infinity));
    const mejor = largos.indexOf(Math.min(...largos));
    unidas.splice(mejor, 2, `${unidas[mejor]} ${unidas[mejor + 1]}`);
  }
  return unidas;
}

/**
 * Parte lo que dice Sami en burbujas cortas: una por oración y, si una oración es larga, una por cláusula.
 * Nunca pasa de `MAXIMO_DE_BURBUJAS`; un texto largo sin puntos ni comas queda en una sola.
 */
export function burbujasDeSami(texto: string): string[] {
  const partes = texto
    .split(/(?<=[.?!…])\s+/)
    .map((oracion) => oracion.trim())
    .filter(Boolean)
    .flatMap(cortarEnComa);
  if (partes.length <= 1) return [texto.trim()];
  return unirLasMasCortas(partes).map((parte) => parte.replace(/,$/, ''));
}
