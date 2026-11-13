/** Sólo GTIN, como texto: los ceros a la izquierda forman parte del código. */
export function codigoGtin(entrada: string): string {
  const codigo = entrada.trim();
  if (!/^(?:\d{8}|\d{12}|\d{13}|\d{14})$/.test(codigo) || /^0+$/.test(codigo))
    throw new Error('Ingresá un código EAN/UPC de 8, 12, 13 o 14 dígitos.');
  let suma = 0;
  for (let i = codigo.length - 2, peso = 3; i >= 0; i--, peso = peso === 3 ? 1 : 3)
    suma += Number(codigo[i]) * peso;
  if ((10 - (suma % 10)) % 10 !== Number(codigo.at(-1)))
    throw new Error('El dígito de control del código no es válido. Revisá la lectura.');
  return codigo;
}

export const identidadGtin = (codigo: string) => codigoGtin(codigo).padStart(14, '0');
export function mismoGtin(a: unknown, b: string): boolean {
  try {
    return typeof a === 'string' && identidadGtin(a) === identidadGtin(b);
  } catch {
    return false;
  }
}
