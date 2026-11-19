import { describe, expect, it } from 'vitest';
import {
  MAXIMO_DIAS_DEL_RESUMEN,
  errorDelPeriodo,
  ultimosDias,
} from '@/lib/sistema/periodoResumen';
import { cuandoSeCalcula, entoncesElResultadoEs } from '@/test/calculo';

const PIDE_LAS_DOS_FECHAS = 'Elegí las dos fechas.';

describe('ultimosDias', () => {
  it('cuenta el día de referencia: 7 días son ese día y los 6 anteriores', () => {
    cuandoSeCalcula(() => ultimosDias(7, '2026-10-19'));

    entoncesElResultadoEs({ desde: '2026-10-13', hasta: '2026-10-19' });
  });

  it('cruza el cambio de mes', () => {
    cuandoSeCalcula(() => ultimosDias(30, '2026-10-19'));

    entoncesElResultadoEs({ desde: '2026-09-20', hasta: '2026-10-19' });
  });

  it('cruza el cambio de año', () => {
    cuandoSeCalcula(() => ultimosDias(10, '2026-01-05'));

    entoncesElResultadoEs({ desde: '2025-12-27', hasta: '2026-01-05' });
  });

  it('usa el día de hoy cuando no se le da una referencia', () => {
    cuandoSeCalcula(() => ultimosDias(1));

    entoncesElPeriodoEsDeUnSoloDia();
  });
});

describe('errorDelPeriodo', () => {
  it('pide la fecha desde', () => {
    cuandoSeCalcula(() => errorDelPeriodo('', '2026-10-19'));

    entoncesElResultadoEs(PIDE_LAS_DOS_FECHAS);
  });

  it('pide la fecha hasta', () => {
    cuandoSeCalcula(() => errorDelPeriodo('2026-10-01', ''));

    entoncesElResultadoEs(PIDE_LAS_DOS_FECHAS);
  });

  it('no deja poner la fecha desde después de la fecha hasta', () => {
    cuandoSeCalcula(() => errorDelPeriodo('2026-10-08', '2026-10-07'));

    entoncesElResultadoEs('La fecha desde no puede ser posterior a la fecha hasta.');
  });

  it('acepta hasta un año', () => {
    cuandoSeCalcula(() => errorDelPeriodo('2025-10-19', '2026-10-19'));

    entoncesElResultadoEs(null);
  });

  it('rechaza más de un año', () => {
    cuandoSeCalcula(() => errorDelPeriodo('2025-10-18', '2026-10-19'));

    entoncesElResultadoEs(`El resumen no puede cubrir más de ${MAXIMO_DIAS_DEL_RESUMEN} días.`);
  });

  it('acepta un solo día', () => {
    cuandoSeCalcula(() => errorDelPeriodo('2026-10-19', '2026-10-19'));

    entoncesElResultadoEs(null);
  });
});

function entoncesElPeriodoEsDeUnSoloDia() {
  const periodo = ultimosDias(1);
  expect(periodo.desde).toBe(periodo.hasta);
}
