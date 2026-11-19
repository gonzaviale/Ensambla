const FORMATO_ISO = /^\d{4}-\d{2}-\d{2}$/;
const FORMATO_DE_CUADERNO = /^(\d{1,2})[/-](\d{1,2})(?:[/-](\d{2}|\d{4}))?$/;

/**
 * Una fecha como se anota en un cuaderno, en AAAA-MM-DD: acepta AAAA-MM-DD, DD/MM/AAAA, DD/MM/AA y DD/MM (sin año,
 * el del año en curso). Una fecha que no existe (31/02) no sirve.
 */
export function fechaDeCuaderno(texto: string): string | undefined {
  if (FORMATO_ISO.test(texto)) return texto;
  const partes = FORMATO_DE_CUADERNO.exec(texto);
  if (!partes) return undefined;
  const [, dia = '', mes = '', anio] = partes;
  return fechaExistente(anioCompleto(anio), Number(mes), Number(dia));
}

function anioCompleto(anio: string | undefined): number {
  if (!anio) return new Date().getFullYear();
  return anio.length === 2 ? 2000 + Number(anio) : Number(anio);
}

function fechaExistente(anio: number, mes: number, dia: number): string | undefined {
  const fecha = new Date(Date.UTC(anio, mes - 1, dia));
  const coincide = fecha.getUTCMonth() === mes - 1 && fecha.getUTCDate() === dia;
  return coincide ? fecha.toISOString().slice(0, 10) : undefined;
}
