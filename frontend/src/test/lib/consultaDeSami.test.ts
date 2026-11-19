import { describe, expect, it } from 'vitest';
import {
  diaSiguiente,
  esHoraDePreguntar,
  estaPospuesta,
  posponerHastaManana,
} from '@/lib/consultaDeSami';

const LUNES = '2026-11-16';
const MARTES = '2026-11-17';
const MIERCOLES = '2026-11-18';
const DOMINGO = '2026-11-22';
const LUNES_SIGUIENTE = '2026-11-23';

describe('cuándo pregunta Sami', () => {
  it.each([LUNES, MARTES])('no pregunta por lo de esta semana el %s', (dia) => {
    expect(esHoraDePreguntar(LUNES, dia)).toBe(false);
  });

  it.each([MIERCOLES, DOMINGO])('pregunta por lo de esta semana desde el miércoles: %s', (dia) => {
    expect(esHoraDePreguntar(LUNES, dia)).toBe(true);
  });

  it('pregunta siempre por lo que quedó de una semana anterior', () => {
    expect(esHoraDePreguntar(LUNES, LUNES_SIGUIENTE)).toBe(true);
  });

  it('el día siguiente cruza el fin de mes', () => {
    expect(diaSiguiente('2026-11-30')).toBe('2026-12-01');
  });
});

describe('dejar una pregunta para después', () => {
  it('queda en pausa el resto del día y vuelve al día siguiente', () => {
    posponerHastaManana('mini-1', MIERCOLES);

    expect(estaPospuesta('mini-1', MIERCOLES)).toBe(true);
    expect(estaPospuesta('mini-1', diaSiguiente(MIERCOLES))).toBe(false);
  });

  it('no afecta a las otras preguntas', () => {
    posponerHastaManana('mini-1', MIERCOLES);

    expect(estaPospuesta('mini-2', MIERCOLES)).toBe(false);
  });
});
