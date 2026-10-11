import {
  errorDeCelda,
  esDudosa,
  type CeldaRevision,
  type ColumnaRevision,
} from '@/lib/revisionLectura';

export type AvisoCelda = {
  tipo: 'error' | 'duda';
  mensaje: string;
};

// Lo que hay que decirle a la persona sobre una celda: un error que impide guardar, o una duda de la
// lectura. En lenguaje cotidiano y con el dato leído a la vista.
export function avisoDeCelda(
  columna: ColumnaRevision,
  celda: CeldaRevision,
  errorServidor?: string,
): AvisoCelda | undefined {
  const error = errorDeCelda(columna, celda) ?? errorServidor;
  if (error) return { tipo: 'error', mensaje: error };
  if (!esDudosa(celda)) return undefined;
  const texto = celda.texto.trim();
  return {
    tipo: 'duda',
    mensaje: texto
      ? `Leímos «${texto}», ¿es correcto?`
      : 'No pudimos leer este dato. Completalo si lo ves en la foto.',
  };
}
