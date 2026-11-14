import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const audioOriginal = Object.getOwnPropertyDescriptor(window, 'AudioContext');
let instancias: ContextoDePrueba[];

function primero<T>(elementos: readonly T[]): T {
  const elemento = elementos[0];
  if (elemento === undefined) throw new Error('Falta un resultado de audio');
  return elemento;
}

function parametro() {
  return {
    value: 0.16,
    cancelScheduledValues: vi.fn(),
    setValueAtTime: vi.fn<(valor: number, instante: number) => void>(),
    linearRampToValueAtTime: vi.fn<(valor: number, instante: number) => void>(),
  };
}

function crearOsciladorDePrueba() {
  return {
    type: '',
    frequency: parametro(),
    connect: vi.fn(),
    disconnect: vi.fn(),
    start: vi.fn<(instante?: number) => void>(),
    stop: vi.fn<(instante?: number) => void>(),
    onended: undefined as (() => void) | undefined,
  };
}

function crearGananciaDePrueba() {
  return { gain: parametro(), connect: vi.fn(), disconnect: vi.fn() };
}

class ContextoDePrueba {
  state = 'suspended';
  currentTime = 10;
  destination = {};
  osciladores: ReturnType<typeof crearOsciladorDePrueba>[] = [];
  ganancias: ReturnType<typeof crearGananciaDePrueba>[] = [];
  resume = vi.fn(async () => {
    this.state = 'running';
  });

  constructor() {
    instancias.push(this);
  }

  createOscillator() {
    const oscilador = crearOsciladorDePrueba();
    this.osciladores.push(oscilador);
    return oscilador;
  }

  createGain() {
    const ganancia = crearGananciaDePrueba();
    this.ganancias.push(ganancia);
    return ganancia;
  }
}

beforeEach(() => {
  vi.resetModules();
  instancias = [];
  Object.defineProperty(window, 'AudioContext', { configurable: true, value: ContextoDePrueba });
});

afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
  if (audioOriginal) Object.defineProperty(window, 'AudioContext', audioOriginal);
  else Reflect.deleteProperty(window, 'AudioContext');
});

describe('avisos sonoros del lector', () => {
  it('muestra la activación necesaria hasta que el navegador habilita el audio', async () => {
    const audio = await import('@/api/sonidosEscaneo');
    expect(audio.requiereActivacionSonidosEscaneo()).toBe(true);
    audio.prepararSonidosEscaneo();
    await Promise.resolve();
    expect(audio.requiereActivacionSonidosEscaneo()).toBe(false);
    audio.cambiarSonidosEscaneo(false);
    expect(audio.requiereActivacionSonidosEscaneo()).toBe(false);
  });

  it('descarta señales anteriores si llegan varias lecturas mientras se habilita el audio', async () => {
    const audio = await import('@/api/sonidosEscaneo');
    audio.prepararSonidosEscaneo();
    const contexto = primero(instancias);
    contexto.state = 'suspended';
    let reanudar = () => {};
    contexto.resume.mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          reanudar = () => {
            contexto.state = 'running';
            resolve();
          };
        }),
    );
    audio.prepararSonidosEscaneo();
    audio.sonarEscaneo('reconocido');
    audio.sonarEscaneo('atencion');
    reanudar();
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();
    expect(contexto.osciladores).toHaveLength(2);
    expect(primero(primero(contexto.osciladores).frequency.setValueAtTime.mock.calls)[0]).toBe(660);
  });
  it('espera la activación del lector y distingue el artículo registrado del desconocido', async () => {
    const audio = await import('@/api/sonidosEscaneo');
    audio.sonarEscaneo('reconocido');
    expect(instancias).toHaveLength(0);

    audio.prepararSonidosEscaneo();
    await Promise.resolve();
    const contexto = primero(instancias);
    audio.sonarEscaneo('reconocido');
    expect(contexto.osciladores).toHaveLength(2);
    const pip = primero(contexto.osciladores);
    contexto.currentTime += 0.04;
    audio.sonarEscaneo('atencion');
    const aviso = contexto.osciladores.slice(2);
    expect(aviso).toHaveLength(2);
    for (const tono of aviso) {
      expect(primero(tono.frequency.setValueAtTime.mock.calls)[0]).toBeLessThan(
        primero(pip.frequency.setValueAtTime.mock.calls)[0],
      );
    }
    expect(pip.stop).toHaveBeenLastCalledWith(contexto.currentTime + 0.007);
    expect(primero(primero(aviso).start.mock.calls)[0]).toBeCloseTo(contexto.currentTime + 0.008);
    pip.onended?.();
    expect(pip.disconnect).toHaveBeenCalledOnce();
    expect(primero(contexto.ganancias).disconnect).toHaveBeenCalledOnce();
  });

  it('recuerda el silencio y detiene tanto el audio activo como las muestras pendientes', async () => {
    const audio = await import('@/api/sonidosEscaneo');
    vi.useFakeTimers();
    audio.probarSonidosEscaneo();
    await Promise.resolve();
    const contexto = primero(instancias);
    const pip = primero(contexto.osciladores);
    audio.cambiarSonidosEscaneo(false);
    expect(audio.leerSonidosEscaneo()).toBe(false);
    expect(localStorage.getItem('ensambla.lector.sonidos')).toBe('no');
    expect(pip.disconnect).toHaveBeenCalled();
    audio.sonarEscaneo('atencion');
    await vi.advanceTimersByTimeAsync(1000);
    expect(contexto.osciladores).toHaveLength(2);
    audio.prepararSonidosEscaneo();
    expect(instancias).toHaveLength(1);
  });

  it('cancela la muestra pendiente cuando llega una lectura real', async () => {
    const audio = await import('@/api/sonidosEscaneo');
    vi.useFakeTimers();
    audio.probarSonidosEscaneo();
    await Promise.resolve();
    audio.sonarEscaneo('reconocido');
    await vi.advanceTimersByTimeAsync(1000);
    expect(primero(instancias).osciladores).toHaveLength(4);
  });

  it('permite silenciar aunque el navegador bloquee el almacenamiento', async () => {
    const audio = await import('@/api/sonidosEscaneo');
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('Almacenamiento bloqueado');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('Almacenamiento bloqueado');
    });
    audio.cambiarSonidosEscaneo(false);
    expect(audio.leerSonidosEscaneo()).toBe(false);
    audio.prepararSonidosEscaneo();
    expect(instancias).toHaveLength(0);
    audio.cambiarSonidosEscaneo(true);
    expect(audio.leerSonidosEscaneo()).toBe(true);
  });

  it('no reproduce un aviso antiguo al habilitarse el audio después de silenciar', async () => {
    const audio = await import('@/api/sonidosEscaneo');
    let reanudar = () => {};
    audio.prepararSonidosEscaneo();
    const contexto = primero(instancias);
    contexto.state = 'suspended';
    contexto.resume.mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          reanudar = () => {
            contexto.state = 'running';
            resolve();
          };
        }),
    );
    audio.prepararSonidosEscaneo();
    audio.sonarEscaneo('atencion');
    audio.cambiarSonidosEscaneo(false);
    audio.cambiarSonidosEscaneo(true);
    reanudar();
    await Promise.resolve();
    await Promise.resolve();
    expect(contexto.osciladores).toHaveLength(0);
  });

  it('tolera que el navegador no soporte audio o rechace su activación', async () => {
    const audio = await import('@/api/sonidosEscaneo');
    Reflect.deleteProperty(window, 'AudioContext');
    expect(() => audio.prepararSonidosEscaneo()).not.toThrow();
    expect(() => audio.sonarEscaneo('atencion')).not.toThrow();
    Object.defineProperty(window, 'AudioContext', { configurable: true, value: ContextoDePrueba });
    audio.prepararSonidosEscaneo();
    const contexto = primero(instancias);
    contexto.state = 'suspended';
    contexto.resume.mockRejectedValue(new Error('Audio bloqueado'));
    audio.prepararSonidosEscaneo();
    audio.sonarEscaneo('atencion');
    await Promise.resolve();
    await Promise.resolve();
    expect(contexto.osciladores).toHaveLength(0);
  });
});
