import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useVigenciaCobroQr } from '@/hooks/caja/useVigenciaCobroQr';

const VENCIMIENTO = '2026-11-19T18:45:00Z';

afterEach(() => vi.useRealTimers());

describe('vigencia del cobro con QR', () => {
  it('pasa a vencido cuando llega la hora sin recargar la pantalla', () => {
    const vigencia = dadoUnCobroPorVencer();
    entoncesSigueVigente(vigencia);

    cuandoLlegaLaHoraDeVencimiento();

    entoncesEstaVencido(vigencia);
  });
});

function dadoUnCobroPorVencer() {
  vi.useFakeTimers();
  vi.setSystemTime(new Date('2026-11-19T18:30:00Z'));
  return renderHook(() => useVigenciaCobroQr(VENCIMIENTO)).result;
}

function cuandoLlegaLaHoraDeVencimiento() {
  act(() => vi.advanceTimersByTime(15 * 60 * 1000));
}

function entoncesSigueVigente(vigencia: { current: boolean }) {
  expect(vigencia.current).toBe(false);
}

function entoncesEstaVencido(vigencia: { current: boolean }) {
  expect(vigencia.current).toBe(true);
}
