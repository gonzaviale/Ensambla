import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type * as SonidosEscaneo from '@/api/sonidosEscaneo';

type Audio = typeof SonidosEscaneo;
type Oscilador = ReturnType<typeof crearOsciladorDePrueba>;

const audioOriginal = Object.getOwnPropertyDescriptor(window, 'AudioContext');
let instancias: ContextoDePrueba[];
let audio: Audio;
let contexto: ContextoDePrueba;
let pip: Oscilador;
let habilitarElAudio = () => {};
let errorAlSilenciar: unknown;

describe('avisos sonoros del lector', () => {
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

  it('muestra la activación necesaria hasta que el navegador habilita el audio', async () => {
    await dadoElModuloDeAudio();

    entoncesSePideActivarElAudio(true);
  });

  it('deja de pedir la activación cuando el navegador habilita el audio', async () => {
    await dadoElModuloDeAudio();

    await cuandoSeHabilitaElAudio();

    entoncesSePideActivarElAudio(false);
  });

  it('silenciar tampoco pide la activación', async () => {
    await dadoUnAudioHabilitado();

    cuandoSeSilencia();

    entoncesSePideActivarElAudio(false);
  });

  it('descarta señales anteriores si llegan varias lecturas mientras se habilita el audio', async () => {
    await dadoUnAudioSuspendidoQueSeHabilitaCuandoSePide();

    await cuandoLlegaUnaLecturaReconocidaYOtraDeAtencionYSeHabilita();

    entoncesSuenaSoloLaUltimaSenal();
  });

  it('no suena antes de que se active el lector', async () => {
    await dadoElModuloDeAudio();

    cuandoSuenaUnaLecturaReconocida();

    entoncesNoSeCreoNingunContexto();
  });

  it('el artículo registrado suena con dos tonos', async () => {
    await dadoUnAudioHabilitado();

    cuandoSuenaUnaLecturaReconocida();

    entoncesHayOsciladores(2);
  });

  it('el aviso del artículo desconocido suena más grave que el del registrado', async () => {
    await dadoUnAudioHabilitado();

    cuandoSuenaUnaLecturaReconocidaYLuegoUnaDeAtencion();

    entoncesLosTonosDeAtencionSonMasGravesQueElDelReconocido();
  });

  it('el pip del artículo registrado se corta antes de que empiece el aviso', async () => {
    await dadoUnAudioHabilitado();

    cuandoSuenaUnaLecturaReconocidaYLuegoUnaDeAtencion();

    entoncesElPipSeCortaAntesDeQueEmpieceElAviso();
  });

  it('al terminar el pip libera sus nodos de audio', async () => {
    await dadoUnAudioHabilitado();
    cuandoSuenaUnaLecturaReconocida();

    cuandoTerminaElPip();

    entoncesSeLiberaElPipYSuGanancia();
  });

  it('recuerda el silencio', async () => {
    await dadoUnaMuestraDeSonidoEnCurso();

    cuandoSeSilencia();

    entoncesElSilencioQuedaGuardado();
  });

  it('el silencio detiene el audio activo', async () => {
    await dadoUnaMuestraDeSonidoEnCurso();

    cuandoSeSilencia();

    entoncesSeDesconectoElAudioActivo();
  });

  it('en silencio ya no suenan lecturas ni muestras pendientes', async () => {
    await dadoUnaMuestraDeSonidoEnCurso();

    await cuandoSeSilenciaYLlegaUnaLecturaDeAtencion();

    entoncesNoSumoNuevosTonosNiContextos();
  });

  it('cancela la muestra pendiente cuando llega una lectura real', async () => {
    await dadoUnaMuestraDeSonidoEnCurso();

    await cuandoLlegaUnaLecturaReconocidaAntesDeQueTermineLaMuestra();

    entoncesHayOsciladores(4);
  });

  it('permite silenciar aunque el navegador bloquee el almacenamiento', async () => {
    await dadoUnAlmacenamientoBloqueado();

    cuandoSeSilenciaYSePreparaElAudio();

    entoncesLosSonidosEstanSilenciadosYNoSeCreoContexto();
  });

  it('permite volver a activar los sonidos aunque el navegador bloquee el almacenamiento', async () => {
    await dadoUnAlmacenamientoBloqueado();

    cuandoSeSilenciaYLuegoSeReactivan();

    entoncesLosSonidosEstanActivados();
  });

  it('no reproduce un aviso antiguo al habilitarse el audio después de silenciar', async () => {
    await dadoUnAudioSuspendidoQueSeHabilitaCuandoSePide();

    await cuandoSuenaUnAvisoSeSilenciaSeReactivaYSeHabilita();

    entoncesNoSuenaNingunTono();
  });

  it('tolera que el navegador no soporte audio', async () => {
    await dadoElModuloDeAudio();
    dadoUnNavegadorSinAudio();

    cuandoSeIntentaPrepararYSonarSinAudio();

    entoncesNoFalla();
  });

  it('tolera que el navegador rechace la activación del audio', async () => {
    await dadoUnAudioSuspendidoQueElNavegadorRechaza();

    await cuandoSeIntentaSonarConElAudioRechazado();

    entoncesNoSuenaNingunTono();
  });
});

// --- dado ---

async function dadoElModuloDeAudio() {
  audio = await import('@/api/sonidosEscaneo');
}

async function dadoUnAudioHabilitado() {
  await dadoElModuloDeAudio();
  audio.prepararSonidosEscaneo();
  await Promise.resolve();
  contexto = primero(instancias);
}

async function dadoUnAudioSuspendidoQueSeHabilitaCuandoSePide() {
  await dadoElModuloDeAudio();
  audio.prepararSonidosEscaneo();
  contexto = primero(instancias);
  contexto.state = 'suspended';
  contexto.resume.mockImplementation(
    () =>
      new Promise<void>((resolve) => {
        habilitarElAudio = () => {
          contexto.state = 'running';
          resolve();
        };
      }),
  );
  audio.prepararSonidosEscaneo();
}

async function dadoUnAudioSuspendidoQueElNavegadorRechaza() {
  await dadoUnAudioHabilitado();
  contexto.state = 'suspended';
  contexto.resume.mockRejectedValue(new Error('Audio bloqueado'));
  audio.prepararSonidosEscaneo();
}

async function dadoUnaMuestraDeSonidoEnCurso() {
  await dadoElModuloDeAudio();
  vi.useFakeTimers();
  audio.probarSonidosEscaneo();
  await Promise.resolve();
  contexto = primero(instancias);
  pip = primero(contexto.osciladores);
}

async function dadoUnAlmacenamientoBloqueado() {
  await dadoElModuloDeAudio();
  const bloqueo = () => {
    throw new Error('Almacenamiento bloqueado');
  };
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation(bloqueo);
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation(bloqueo);
}

function dadoUnNavegadorSinAudio() {
  Reflect.deleteProperty(window, 'AudioContext');
}

// --- cuando ---

async function cuandoSeHabilitaElAudio() {
  audio.prepararSonidosEscaneo();
  await Promise.resolve();
}

function cuandoSeSilencia() {
  audio.cambiarSonidosEscaneo(false);
}

async function cuandoLlegaUnaLecturaReconocidaYOtraDeAtencionYSeHabilita() {
  audio.sonarEscaneo('reconocido');
  audio.sonarEscaneo('atencion');
  habilitarElAudio();
  await esperarMicrotareas(3);
}

function cuandoSuenaUnaLecturaReconocida() {
  audio.sonarEscaneo('reconocido');
  contexto = instancias[0] ?? contexto;
}

function cuandoSuenaUnaLecturaReconocidaYLuegoUnaDeAtencion() {
  audio.sonarEscaneo('reconocido');
  pip = primero(contexto.osciladores);
  contexto.currentTime += 0.04;
  audio.sonarEscaneo('atencion');
}

function cuandoTerminaElPip() {
  pip = primero(contexto.osciladores);
  pip.onended?.();
}

async function cuandoSeSilenciaYLlegaUnaLecturaDeAtencion() {
  audio.cambiarSonidosEscaneo(false);
  audio.sonarEscaneo('atencion');
  await vi.advanceTimersByTimeAsync(1000);
  audio.prepararSonidosEscaneo();
}

async function cuandoLlegaUnaLecturaReconocidaAntesDeQueTermineLaMuestra() {
  audio.sonarEscaneo('reconocido');
  await vi.advanceTimersByTimeAsync(1000);
}

function cuandoSeSilenciaYSePreparaElAudio() {
  audio.cambiarSonidosEscaneo(false);
  audio.prepararSonidosEscaneo();
}

function cuandoSeSilenciaYLuegoSeReactivan() {
  audio.cambiarSonidosEscaneo(false);
  audio.cambiarSonidosEscaneo(true);
}

async function cuandoSuenaUnAvisoSeSilenciaSeReactivaYSeHabilita() {
  audio.sonarEscaneo('atencion');
  audio.cambiarSonidosEscaneo(false);
  audio.cambiarSonidosEscaneo(true);
  habilitarElAudio();
  await esperarMicrotareas(2);
}

function cuandoSeIntentaPrepararYSonarSinAudio() {
  errorAlSilenciar = capturar(() => {
    audio.prepararSonidosEscaneo();
    audio.sonarEscaneo('atencion');
  });
}

async function cuandoSeIntentaSonarConElAudioRechazado() {
  audio.sonarEscaneo('atencion');
  await esperarMicrotareas(2);
}

// --- entonces ---

function entoncesSePideActivarElAudio(esperado: boolean) {
  expect(audio.requiereActivacionSonidosEscaneo()).toBe(esperado);
}

function entoncesSuenaSoloLaUltimaSenal() {
  expect(contexto.osciladores).toHaveLength(2);
  expect(primero(primero(contexto.osciladores).frequency.setValueAtTime.mock.calls)[0]).toBe(660);
}

function entoncesNoSeCreoNingunContexto() {
  expect(instancias).toHaveLength(0);
}

function entoncesHayOsciladores(cantidad: number) {
  expect(primero(instancias).osciladores).toHaveLength(cantidad);
}

function entoncesLosTonosDeAtencionSonMasGravesQueElDelReconocido() {
  const aviso = contexto.osciladores.slice(2);
  expect(aviso).toHaveLength(2);
  for (const tono of aviso) {
    expect(primero(tono.frequency.setValueAtTime.mock.calls)[0]).toBeLessThan(
      primero(pip.frequency.setValueAtTime.mock.calls)[0],
    );
  }
}

function entoncesElPipSeCortaAntesDeQueEmpieceElAviso() {
  const aviso = contexto.osciladores.slice(2);
  expect(pip.stop).toHaveBeenLastCalledWith(contexto.currentTime + 0.007);
  expect(primero(primero(aviso).start.mock.calls)[0]).toBeCloseTo(contexto.currentTime + 0.008);
}

function entoncesSeLiberaElPipYSuGanancia() {
  expect(pip.disconnect).toHaveBeenCalledOnce();
  expect(primero(contexto.ganancias).disconnect).toHaveBeenCalledOnce();
}

function entoncesElSilencioQuedaGuardado() {
  expect(audio.leerSonidosEscaneo()).toBe(false);
  expect(localStorage.getItem('ensambla.lector.sonidos')).toBe('no');
}

function entoncesSeDesconectoElAudioActivo() {
  expect(pip.disconnect).toHaveBeenCalled();
}

function entoncesNoSumoNuevosTonosNiContextos() {
  expect(contexto.osciladores).toHaveLength(2);
  expect(instancias).toHaveLength(1);
}

function entoncesLosSonidosEstanSilenciadosYNoSeCreoContexto() {
  expect(audio.leerSonidosEscaneo()).toBe(false);
  expect(instancias).toHaveLength(0);
}

function entoncesLosSonidosEstanActivados() {
  expect(audio.leerSonidosEscaneo()).toBe(true);
}

function entoncesNoSuenaNingunTono() {
  expect(contexto.osciladores).toHaveLength(0);
}

function entoncesNoFalla() {
  expect(errorAlSilenciar).toBeUndefined();
}

// --- datos ---

function primero<T>(elementos: readonly T[]): T {
  const elemento = elementos[0];
  if (elemento === undefined) throw new Error('Falta un resultado de audio');
  return elemento;
}

async function esperarMicrotareas(cantidad: number) {
  for (let veces = 0; veces < cantidad; veces++) await Promise.resolve();
}

function capturar(accion: () => void): unknown {
  try {
    accion();
    return undefined;
  } catch (error) {
    return error;
  }
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
  osciladores: Oscilador[] = [];
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
