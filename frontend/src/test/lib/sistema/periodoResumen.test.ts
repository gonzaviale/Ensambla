import { describe, expect, it } from 'vitest';
import {
  MAXIMO_DIAS_DEL_RESUMEN,
  errorDelPeriodo,
  ultimosDias,
} from '@/lib/sistema/periodoResumen';

describe('ultimosDias', () => {
  it('cuenta el día de referencia: 7 días son ese día y los 6 anteriores', () => {
    expect(ultimosDias(7, '2026-10-19')).toEqual({ desde: '2026-10-13', hasta: '2026-10-19' });
  });

  it('cruza el cambio de mes y de año', () => {
    expect(ultimosDias(30, '2026-10-19')).toEqual({ desde: '2026-09-20', hasta: '2026-10-19' });
    expect(ultimosDias(10, '2026-01-05')).toEqual({ desde: '2025-12-27', hasta: '2026-01-05' });
  });

  it('usa el día de hoy cuando no se le da una referencia', () => {
    const periodo = ultimosDias(1);

    expect(periodo.desde).toBe(periodo.hasta);
  });
});

describe('errorDelPeriodo', () => {
  it('pide las dos fechas', () => {
    expect(errorDelPeriodo('', '2026-10-19')).toBe('Elegí las dos fechas.');
    expect(errorDelPeriodo('2026-10-01', '')).toBe('Elegí las dos fechas.');
  });

  it('no deja poner la fecha desde después de la fecha hasta', () => {
    expect(errorDelPeriodo('2026-10-08', '2026-10-07')).toBe(
      'La fecha desde no puede ser posterior a la fecha hasta.',
    );
  });

  it('acepta hasta un año y rechaza más', () => {
    expect(errorDelPeriodo('2025-10-19', '2026-10-19')).toBeNull();
    expect(errorDelPeriodo('2025-10-18', '2026-10-19')).toBe(
      `El resumen no puede cubrir más de ${MAXIMO_DIAS_DEL_RESUMEN} días.`,
    );
  });

  it('acepta un solo día', () => {
    expect(errorDelPeriodo('2026-10-19', '2026-10-19')).toBeNull();
  });
});
