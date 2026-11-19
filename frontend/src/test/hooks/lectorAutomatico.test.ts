import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useLectorAutomatico } from '@/hooks/escaneo/useLectorAutomatico';
import {
  useConfirmacionLectura,
  type ConfirmacionLectura,
} from '@/hooks/escaneo/useConfirmacionLectura';

afterEach(() => {
  vi.restoreAllMocks();
  vi.useRealTimers();
});

describe('lector automático de caja', () => {
  it('abre al entrar y al regresar de un panel, sin reintentar permisos en cada render', () => {
    const abrir = vi.fn();
    const cerrar = vi.fn();
    const vista = renderHook(
      ({ habilitado }) =>
        useLectorAutomatico(habilitado, {
          abrir: () => abrir(),
          cerrar: () => cerrar(),
          abierta: false,
          iniciando: false,
        }),
      { initialProps: { habilitado: true } },
    );
    expect(abrir).toHaveBeenCalledOnce();
    vista.rerender({ habilitado: true });
    expect(abrir).toHaveBeenCalledOnce();
    vista.rerender({ habilitado: false });
    expect(cerrar).toHaveBeenCalled();
    vista.rerender({ habilitado: true });
    expect(abrir).toHaveBeenCalledTimes(2);
    vista.unmount();
  });

  it('libera la cámara al ocultar la app y vuelve a abrirla al regresar', () => {
    const abrir = vi.fn();
    const cerrar = vi.fn();
    const visibilidad = vi.spyOn(document, 'visibilityState', 'get');
    visibilidad.mockReturnValue('visible');
    renderHook(() =>
      useLectorAutomatico(true, { abrir, cerrar, abierta: false, iniciando: false }),
    );
    visibilidad.mockReturnValue('hidden');
    act(() => document.dispatchEvent(new Event('visibilitychange')));
    expect(cerrar).toHaveBeenCalledOnce();
    visibilidad.mockReturnValue('visible');
    act(() => document.dispatchEvent(new Event('visibilitychange')));
    expect(abrir).toHaveBeenCalledTimes(2);
  });

  it('no reinicia una cámara que ya está abierta al elegir otro dispositivo', () => {
    const abrir = vi.fn();
    const cerrar = vi.fn();
    const vista = renderHook(
      ({ habilitado, abierta }) =>
        useLectorAutomatico(habilitado, { abrir, cerrar, abierta, iniciando: false }),
      { initialProps: { habilitado: false, abierta: false } },
    );
    cerrar.mockClear();
    vista.rerender({ habilitado: true, abierta: true });
    expect(abrir).not.toHaveBeenCalled();
    expect(cerrar).not.toHaveBeenCalled();
  });

  it('retoma la cámara aunque se haya ocultado mientras todavía estaba iniciando', () => {
    const abrir = vi.fn();
    const cerrar = vi.fn();
    const vista = renderHook(
      ({ habilitado }) =>
        useLectorAutomatico(habilitado, {
          abrir,
          cerrar,
          abierta: false,
          iniciando: true,
        }),
      { initialProps: { habilitado: false } },
    );
    vista.rerender({ habilitado: true });
    expect(abrir).toHaveBeenCalledOnce();
  });
});

describe('confirmación de lectura', () => {
  it('renueva el marco por 1,4 segundos sin que el temporizador anterior borre una lectura rápida', () => {
    vi.useFakeTimers();
    const vista = renderHook(
      ({ lectura }: { lectura: ConfirmacionLectura | null }) => useConfirmacionLectura(lectura),
      {
        initialProps: { lectura: { id: 1, tipo: 'reconocido' } },
      },
    );
    act(() => vi.advanceTimersByTime(500));
    vista.rerender({ lectura: { id: 2, tipo: 'reconocido' } });
    act(() => vi.advanceTimersByTime(900));
    expect(vista.result.current?.id).toBe(2);
    act(() => vi.advanceTimersByTime(500));
    expect(vista.result.current).toBeNull();
  });
});
