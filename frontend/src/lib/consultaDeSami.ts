import { lunesDe } from './objetivosAutogestion';

const CLAVE_POSPUESTAS = 'ensambla.objetivos.preguntasPospuestas';
const MIERCOLES = 2;
const UN_DIA_EN_MS = 86_400_000;

export function diaDeLaSemana(dia: string): number {
  return (new Date(`${dia}T12:00:00Z`).getUTCDay() + 6) % 7;
}

export function diaSiguiente(dia: string): string {
  return new Date(new Date(`${dia}T12:00:00Z`).getTime() + UN_DIA_EN_MS).toISOString().slice(0, 10);
}

/** Lo de esta semana se pregunta desde el miércoles, cuando ya hubo tiempo de hacerlo; lo de semanas anteriores, siempre. */
export function esHoraDePreguntar(semanaInicio: string, dia: string): boolean {
  return semanaInicio < lunesDe(dia) || diaDeLaSemana(dia) >= MIERCOLES;
}

export function estaPospuesta(miniId: string, dia: string): boolean {
  return (pospuestas()[miniId] ?? '') > dia;
}

export function posponerHastaManana(miniId: string, dia: string) {
  const vigentes = Object.entries(pospuestas()).filter(([, hasta]) => hasta > dia);
  guardarPospuestas({ ...Object.fromEntries(vigentes), [miniId]: diaSiguiente(dia) });
}

function pospuestas(): Record<string, string> {
  try {
    const guardado: unknown = JSON.parse(localStorage.getItem(CLAVE_POSPUESTAS) ?? '{}');
    return guardado && typeof guardado === 'object' ? (guardado as Record<string, string>) : {};
  } catch {
    return {};
  }
}

function guardarPospuestas(valor: Record<string, string>) {
  try {
    localStorage.setItem(CLAVE_POSPUESTAS, JSON.stringify(valor));
  } catch {
    // Sin almacenamiento, la pregunta vuelve a aparecer: no es grave.
  }
}
