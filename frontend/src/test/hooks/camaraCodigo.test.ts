import { act, renderHook, waitFor } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { ChecksumException, FormatException, NotFoundException } from '@zxing/library';
import type { IScannerControls } from '@zxing/browser';
import { useCamaraCodigo } from '@/hooks/escaneo/useCamaraCodigo';
import { recordarCamaraComprobada } from '@/api/camaraCodigo';

const { decodificar, pedirCamara, enumerar } = vi.hoisted(() => ({
  decodificar: vi.fn(),
  pedirCamara: vi.fn(),
  enumerar: vi.fn(),
}));
vi.mock('@zxing/browser', () => ({
  BrowserMultiFormatReader: class {
    decodeFromStream = decodificar;
  },
}));
const mediaOriginal = Object.getOwnPropertyDescriptor(navigator, 'mediaDevices');
beforeAll(() =>
  Object.defineProperty(navigator, 'mediaDevices', {
    configurable: true,
    value: { getUserMedia: pedirCamara, enumerateDevices: enumerar },
  }),
);
afterAll(() => {
  if (mediaOriginal) Object.defineProperty(navigator, 'mediaDevices', mediaOriginal);
  else Reflect.deleteProperty(navigator, 'mediaDevices');
});
afterEach(() => {
  vi.restoreAllMocks();
  vi.useRealTimers();
});

function crearCamara(id = '') {
  const pista = Object.assign(new EventTarget(), {
    readyState: 'live',
    getCapabilities: vi.fn(() => ({ focusMode: ['continuous'] })),
    getSettings: vi.fn(() => ({ deviceId: id })),
    applyConstraints: vi.fn().mockResolvedValue(undefined),
    stop: vi.fn(() => {
      pista.readyState = 'ended';
    }),
  });
  const stream = {
    getTracks: () => [pista],
    getVideoTracks: () => [pista],
  } as unknown as MediaStream;
  return { stream, pista };
}
function emitir(indice: number, codigo?: string, error?: Error) {
  const callback = decodificar.mock.calls[indice]?.[2];
  if (typeof callback !== 'function') throw new Error('Falta el callback del lector');
  const control = decodificar.mock.results[indice];
  return Promise.resolve(control?.value as Promise<IScannerControls>).then((controles) => {
    callback(codigo ? { getText: () => codigo } : undefined, error, controles);
  });
}
beforeEach(() => {
  pedirCamara.mockReset();
  enumerar.mockReset().mockResolvedValue([]);
  decodificar.mockReset().mockImplementation((stream: MediaStream, video: HTMLVideoElement) => {
    video.srcObject = stream;
    return Promise.resolve({ stop: vi.fn() });
  });
});

async function abrirLector() {
  const camara = crearCamara();
  pedirCamara.mockResolvedValueOnce(camara.stream);
  const onCodigo = vi.fn();
  const hook = renderHook(() => useCamaraCodigo(onCodigo));
  const video = document.createElement('video');
  hook.result.current.video.current = video;
  act(() => hook.result.current.abrir());
  await waitFor(() => expect(decodificar).toHaveBeenCalledOnce());
  await waitFor(() => expect(hook.result.current.iniciando).toBe(false));
  return { ...hook, ...camara, onCodigo, video };
}

describe('ciclo de vida del lector con cámara', () => {
  const dispositivos = [
    { kind: 'videoinput', deviceId: 'ultra', label: 'camera 2, facing back ultra wide' },
    { kind: 'videoinput', deviceId: 'principal', label: 'camera 0, facing back' },
  ];

  it('descubre las etiquetas tras el permiso y cambia de la lente automática a la principal', async () => {
    enumerar.mockResolvedValueOnce([]).mockResolvedValue(dispositivos);
    const inicial = crearCamara('ultra');
    const principal = crearCamara('principal');
    pedirCamara.mockResolvedValueOnce(inicial.stream).mockImplementationOnce(() => {
      expect(inicial.pista.stop).toHaveBeenCalledOnce();
      return Promise.resolve(principal.stream);
    });
    const { result } = renderHook(() => useCamaraCodigo(vi.fn()));
    result.current.video.current = document.createElement('video');
    act(() => result.current.abrir());
    await waitFor(() => expect(decodificar).toHaveBeenCalledOnce());
    expect(decodificar.mock.calls[0]?.[0]).toBe(principal.stream);
    expect(result.current.camaraActiva).toBe('camera 0, facing back');
    expect(result.current.camaraActualId).toBe('principal');
    expect(result.current.camaraId).toBe('');
    expect(pedirCamara).toHaveBeenLastCalledWith(
      expect.objectContaining({
        video: expect.objectContaining({ deviceId: { exact: 'principal' } }),
      }),
    );
  });

  it('una preferencia automática antigua no vuelve a elegir la lente equivocada', async () => {
    recordarCamaraComprobada('ultra');
    enumerar.mockResolvedValue(dispositivos);
    const principal = crearCamara('principal');
    pedirCamara.mockResolvedValueOnce(principal.stream);
    const { result } = renderHook(() => useCamaraCodigo(vi.fn()));
    result.current.video.current = document.createElement('video');
    act(() => result.current.abrir());
    await waitFor(() => expect(decodificar).toHaveBeenCalledOnce());
    expect(pedirCamara).toHaveBeenCalledWith(
      expect.objectContaining({
        video: expect.objectContaining({ deviceId: { exact: 'principal' } }),
      }),
    );
    expect(result.current.camaraActualId).toBe('principal');
  });

  it('conserva la selección manual al volver a automático aunque todavía no haya leído', async () => {
    const { result, unmount } = await abrirLector();
    enumerar.mockResolvedValue(dispositivos);
    const elegida = crearCamara('ultra');
    pedirCamara.mockResolvedValueOnce(elegida.stream);
    result.current.video.current = document.createElement('video');
    act(() => result.current.seleccionarCamara('ultra'));
    await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(2));
    unmount();
    const nueva = crearCamara('ultra');
    pedirCamara.mockResolvedValueOnce(nueva.stream);
    const otro = renderHook(() => useCamaraCodigo(vi.fn()));
    otro.result.current.video.current = document.createElement('video');
    act(() => otro.result.current.abrir());
    await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(3));
    expect(otro.result.current.camaraActualId).toBe('ultra');
    expect(pedirCamara).toHaveBeenLastCalledWith(
      expect.objectContaining({ video: expect.objectContaining({ deviceId: { exact: 'ultra' } }) }),
    );
  });

  it('recuerda una lente que leyó incluso después de desmontar y volver a abrir en automático', async () => {
    const { result, unmount } = await abrirLector();
    enumerar.mockResolvedValue(dispositivos);
    const elegida = crearCamara('ultra');
    pedirCamara.mockResolvedValueOnce(elegida.stream);
    result.current.video.current = document.createElement('video');
    act(() => result.current.seleccionarCamara('ultra'));
    await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(2));
    await act(() => emitir(1, '77903754'));
    unmount();
    const nueva = crearCamara('ultra');
    pedirCamara.mockResolvedValueOnce(nueva.stream);
    const otro = renderHook(() => useCamaraCodigo(vi.fn()));
    otro.result.current.video.current = document.createElement('video');
    act(() => otro.result.current.abrir());
    await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(3));
    expect(pedirCamara).toHaveBeenLastCalledWith(
      expect.objectContaining({ video: expect.objectContaining({ deviceId: { exact: 'ultra' } }) }),
    );
    expect(otro.result.current.camaraId).toBe('');
  });

  it('continúa leyendo si el navegador bloquea guardar la preferencia', async () => {
    const { onCodigo } = await abrirLector();
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('Bloqueado');
    });
    await act(() => emitir(0, '77903754'));
    expect(onCodigo).toHaveBeenCalledWith('77903754');
  });

  it('si la lente automática desaparece vuelve a una disponible sin bloquear el lector', async () => {
    enumerar.mockResolvedValue(dispositivos);
    const disponible = crearCamara('ultra');
    pedirCamara
      .mockRejectedValueOnce(new DOMException('Ausente', 'NotFoundError'))
      .mockResolvedValueOnce(disponible.stream);
    const { result } = renderHook(() => useCamaraCodigo(vi.fn()));
    result.current.video.current = document.createElement('video');
    act(() => result.current.abrir());
    await waitFor(() => expect(decodificar).toHaveBeenCalledOnce());
    expect(pedirCamara).toHaveBeenCalledTimes(2);
    expect(result.current.error).toBe('');
    expect(result.current.camaraActiva).toBe('camera 2, facing back ultra wide');
  });

  it('libera la lente elegida automáticamente si se cierra mientras inicia el cambio', async () => {
    enumerar.mockResolvedValueOnce([]).mockResolvedValue(dispositivos);
    const inicial = crearCamara('ultra');
    let resolver: (stream: MediaStream) => void = () => undefined;
    pedirCamara.mockResolvedValueOnce(inicial.stream).mockReturnValueOnce(
      new Promise<MediaStream>((resolve) => {
        resolver = resolve;
      }),
    );
    const { result } = renderHook(() => useCamaraCodigo(vi.fn()));
    result.current.video.current = document.createElement('video');
    act(() => result.current.abrir());
    await waitFor(() => expect(pedirCamara).toHaveBeenCalledTimes(2));
    act(() => result.current.cerrar());
    const nueva = crearCamara('principal');
    await act(async () => {
      resolver(nueva.stream);
    });
    expect(nueva.pista.stop).toHaveBeenCalledOnce();
    expect(decodificar).not.toHaveBeenCalled();
  });

  it('no abre la cámara al cargar y explica una denegación de permiso', async () => {
    pedirCamara.mockRejectedValueOnce(new DOMException('denegado', 'NotAllowedError'));
    const { result } = renderHook(() => useCamaraCodigo(vi.fn()));
    result.current.video.current = document.createElement('video');
    expect(pedirCamara).not.toHaveBeenCalled();
    act(() => result.current.abrir());
    await waitFor(() => expect(result.current.error).toMatch(/No se habilitó/));
    expect(result.current.abierta).toBe(false);
  });

  it('lee tres productos al reabrir y libera por completo cada sesión anterior', async () => {
    const { result, pista, onCodigo, video } = await abrirLector();
    await act(() => emitir(0, '77903754'));
    expect(result.current.abierta).toBe(false);
    expect(pista.stop).toHaveBeenCalledOnce();
    expect(video.srcObject).toBeNull();
    for (const [indice, codigo] of [
      [1, '77905741'],
      [2, '3017620422003'],
    ] as const) {
      const nueva = crearCamara();
      pedirCamara.mockResolvedValueOnce(nueva.stream);
      result.current.video.current = document.createElement('video');
      act(() => result.current.abrir());
      await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(indice + 1));
      await act(() => emitir(indice, codigo));
      expect(nueva.pista.stop).toHaveBeenCalledOnce();
      expect(result.current.abierta).toBe(false);
    }
    expect(onCodigo.mock.calls.map((llamada) => llamada[0])).toEqual([
      '77903754',
      '77905741',
      '3017620422003',
    ]);
    await act(() => emitir(0, '77903754'));
    expect(onCodigo).toHaveBeenCalledTimes(3);
  });

  it('un error inesperado no deja el lector bloqueado y permite volver a abrirlo', async () => {
    const { result, pista, onCodigo } = await abrirLector();
    await act(() => emitir(0, undefined, new Error('Canvas desconectado')));
    expect(result.current.abierta).toBe(false);
    expect(result.current.error).toMatch(/lector se interrumpió/);
    expect(pista.stop).toHaveBeenCalledOnce();
    const nueva = crearCamara();
    pedirCamara.mockResolvedValueOnce(nueva.stream);
    result.current.video.current = document.createElement('video');
    act(() => result.current.abrir());
    await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(2));
    await act(() => emitir(1, '77905741'));
    expect(onCodigo).toHaveBeenCalledWith('77905741');
    expect(result.current.error).toBe('');
  });

  it('sigue intentando ante códigos fuera de foco o incompletos', async () => {
    const { result, onCodigo } = await abrirLector();
    for (const error of [new NotFoundException(), new ChecksumException(), new FormatException()])
      await act(() => emitir(0, undefined, error));
    expect(result.current.abierta).toBe(true);
    expect(result.current.error).toBe('');
    await act(() => emitir(0, '77903754'));
    expect(onCodigo).toHaveBeenCalledOnce();
  });

  it('libera un permiso de cámara que llega después de haber cerrado', async () => {
    let resolver: (stream: MediaStream) => void = () => undefined;
    pedirCamara.mockReturnValueOnce(
      new Promise<MediaStream>((resolve) => {
        resolver = resolve;
      }),
    );
    const { result } = renderHook(() => useCamaraCodigo(vi.fn()));
    result.current.video.current = document.createElement('video');
    act(() => result.current.abrir());
    await waitFor(() => expect(pedirCamara).toHaveBeenCalledOnce());
    act(() => result.current.cerrar());
    const camara = crearCamara();
    await act(async () => {
      resolver(camara.stream);
    });
    expect(camara.pista.stop).toHaveBeenCalledOnce();
    expect(decodificar).not.toHaveBeenCalled();
  });

  it('un decodificador viejo que responde tarde no apaga la cámara nueva', async () => {
    let resolver: (control: IScannerControls) => void = () => undefined;
    decodificar.mockImplementationOnce((stream: MediaStream, video: HTMLVideoElement) => {
      video.srcObject = stream;
      return new Promise<IScannerControls>((resolve) => {
        resolver = resolve;
      });
    });
    const vieja = crearCamara();
    pedirCamara.mockResolvedValueOnce(vieja.stream);
    const { result } = renderHook(() => useCamaraCodigo(vi.fn()));
    const videoViejo = document.createElement('video');
    result.current.video.current = videoViejo;
    act(() => result.current.abrir());
    await waitFor(() => expect(decodificar).toHaveBeenCalledOnce());
    const nueva = crearCamara();
    pedirCamara.mockResolvedValueOnce(nueva.stream);
    const videoNuevo = document.createElement('video');
    result.current.video.current = videoNuevo;
    act(() => result.current.reiniciar());
    await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(2));
    const stop = vi.fn(() => {
      videoViejo.srcObject = null;
    });
    await act(async () => {
      resolver({ stop });
    });
    expect(stop).toHaveBeenCalledOnce();
    expect(vieja.pista.stop).toHaveBeenCalledOnce();
    expect(videoNuevo.srcObject).toBe(nueva.stream);
    expect(nueva.pista.stop).not.toHaveBeenCalled();
    expect(result.current.abierta).toBe(true);
  });

  it('al cambiar de lente libera la anterior y solicita el dispositivo elegido', async () => {
    const { result, pista } = await abrirLector();
    const otra = crearCamara();
    pedirCamara.mockResolvedValueOnce(otra.stream);
    result.current.video.current = document.createElement('video');
    act(() => result.current.seleccionarCamara('trasera-principal'));
    await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(2));
    expect(pista.stop).toHaveBeenCalledOnce();
    expect(pedirCamara).toHaveBeenLastCalledWith(
      expect.objectContaining({
        video: expect.objectContaining({ deviceId: { exact: 'trasera-principal' } }),
      }),
    );
  });

  it('una lente que deja de estar disponible permite reintentar con la cámara automática', async () => {
    const { result } = await abrirLector();
    pedirCamara.mockRejectedValueOnce(
      new DOMException('Dispositivo ausente', 'OverconstrainedError'),
    );
    result.current.video.current = document.createElement('video');
    act(() => result.current.seleccionarCamara('lente-ausente'));
    await waitFor(() => expect(result.current.abierta).toBe(false));
    expect(result.current.error).toMatch(/cámara elegida no está disponible/);
    expect(result.current.camaraId).toBe('');
    const nueva = crearCamara();
    pedirCamara.mockResolvedValueOnce(nueva.stream);
    result.current.video.current = document.createElement('video');
    act(() => result.current.abrir());
    await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(2));
    expect(pedirCamara).toHaveBeenLastCalledWith(
      expect.objectContaining({
        video: expect.objectContaining({ facingMode: { ideal: 'environment' } }),
      }),
    );
  });

  it('solicita enfoque continuo si la cámara lo admite y tolera su rechazo', async () => {
    const camara = crearCamara();
    camara.pista.applyConstraints.mockRejectedValueOnce(new Error('No soportado'));
    pedirCamara.mockResolvedValueOnce(camara.stream);
    const { result } = renderHook(() => useCamaraCodigo(vi.fn()));
    result.current.video.current = document.createElement('video');
    act(() => result.current.abrir());
    await waitFor(() => expect(decodificar).toHaveBeenCalledOnce());
    expect(camara.pista.applyConstraints).toHaveBeenCalledWith({
      advanced: [{ focusMode: 'continuous' }],
    });
    expect(result.current.abierta).toBe(true);
    expect(result.current.error).toBe('');
  });

  it('cierra y libera la cámara al ocultar la pestaña o desmontar', async () => {
    const { result, pista, unmount } = await abrirLector();
    const visible = vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden');
    act(() => document.dispatchEvent(new Event('visibilitychange')));
    expect(result.current.abierta).toBe(false);
    expect(pista.stop).toHaveBeenCalledOnce();
    visible.mockRestore();
    unmount();
    expect(pista.stop).toHaveBeenCalledOnce();
  });

  it('un fallo de la pista se comunica en vez de dejar una imagen congelada', async () => {
    const { result, pista } = await abrirLector();
    act(() => pista.dispatchEvent(new Event('ended')));
    expect(result.current.abierta).toBe(false);
    expect(result.current.error).toMatch(/cámara se interrumpió/);
  });

  it('un permiso que no responde tiene plazo y no bloquea la pantalla indefinidamente', async () => {
    pedirCamara.mockImplementation(() => new Promise<MediaStream>(() => undefined));
    const { result, unmount } = renderHook(() => useCamaraCodigo(vi.fn()));
    result.current.video.current = document.createElement('video');
    act(() => result.current.abrir());
    await waitFor(() => expect(pedirCamara).toHaveBeenCalledOnce());
    vi.useFakeTimers();
    // El temporizador se crea al abrir: reiniciar instala uno controlado por este reloj.
    result.current.video.current = document.createElement('video');
    act(() => result.current.reiniciar());
    await act(async () => {
      await vi.advanceTimersByTimeAsync(15_001);
    });
    expect(result.current.abierta).toBe(false);
    expect(result.current.error).toMatch(/no terminó de iniciar/);
    unmount();
    vi.useRealTimers();
  });
});
