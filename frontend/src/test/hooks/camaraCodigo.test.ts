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

type Camara = ReturnType<typeof crearCamara>;
type Hook = ReturnType<typeof renderHook<ReturnType<typeof useCamaraCodigo>, unknown>>;
type Abierto = Hook & Camara & { onCodigo: ReturnType<typeof vi.fn>; video: HTMLVideoElement };

const DISPOSITIVOS = [
  { kind: 'videoinput', deviceId: 'ultra', label: 'camera 2, facing back ultra wide' },
  { kind: 'videoinput', deviceId: 'principal', label: 'camera 0, facing back' },
];

const mediaOriginal = Object.getOwnPropertyDescriptor(navigator, 'mediaDevices');

let lector: Abierto;
let hook: Hook;
let otro: Hook;
let onCodigo: ReturnType<typeof vi.fn>;
let camaraNueva: Camara;
let camaraInicial: Camara;
let videoViejo: HTMLVideoElement;
let videoNuevo: HTMLVideoElement;
let resolverPermiso: (stream: MediaStream) => void = () => undefined;
let resolverDecodificador: (control: IScannerControls) => void = () => undefined;
let controlViejo: IScannerControls;
let instante = 1000;

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
beforeEach(() => {
  pedirCamara.mockReset();
  enumerar.mockReset().mockResolvedValue([]);
  decodificar.mockReset().mockImplementation((stream: MediaStream, video: HTMLVideoElement) => {
    video.srcObject = stream;
    return Promise.resolve({ stop: vi.fn() });
  });
});

describe('ciclo de vida del lector con cámara', () => {
  it('en Caja ignora fotogramas repetidos del mismo código', async () => {
    await dadoUnLectorAbierto(true);

    await cuandoSeLeeElCodigo(0, '77903754');
    await cuandoSeLeeElCodigo(0, '77903754');

    entoncesSeAvisaronCodigos(1);
    entoncesElLectorSigueAbierto();
  });

  it('en Caja agrega de nuevo el código después de retirarlo de la vista', async () => {
    await dadoUnLectorAbierto(true);
    await cuandoSeLeeElCodigo(0, '77903754');

    await cuandoSeRetiraElCodigoYSeLeeDeNuevo();

    entoncesSeAvisaronCodigos(2);
  });

  it('en Caja agrega productos distintos sin reiniciar la cámara', async () => {
    await dadoUnLectorAbierto(true);

    await cuandoSeLeeElCodigo(0, '77903754');
    await cuandoSeLeeElCodigo(0, '7790895000997');

    entoncesSeAvisaronCodigos(2);
    entoncesLaCamaraNoSeDetuvo();
  });

  it('descubre las etiquetas tras el permiso y cambia de la lente automática a la principal', async () => {
    dadasLasEtiquetasQueSoloAparecenTrasElPermiso();

    await cuandoSeAbreElLectorEnAutomatico();

    entoncesSeUsaLaLentePrincipal();
  });

  it('muestra la lente principal como la activa y sigue en automático', async () => {
    dadasLasEtiquetasQueSoloAparecenTrasElPermiso();

    await cuandoSeAbreElLectorEnAutomatico();

    entoncesLaCamaraActivaEsLaPrincipalEnAutomatico();
  });

  it('una preferencia automática antigua no vuelve a elegir la lente equivocada', async () => {
    dadaUnaPreferenciaAutomaticaAntiguaDeLaLenteUltra();

    await cuandoSeAbreElLectorEnAutomatico();

    entoncesSePidioLaCamaraConId('principal');
    entoncesLaCamaraActualEs('principal');
  });

  it('conserva la selección manual al volver a automático aunque todavía no haya leído', async () => {
    await dadoUnLectorConLaLenteUltraElegidaAMano();

    await cuandoSeDesmontaYSeVuelveAAbrirEnAutomatico();

    entoncesLaCamaraActualDelOtroEs('ultra');
    entoncesSePidioLaCamaraConId('ultra');
  });

  it('recuerda una lente que leyó incluso después de desmontar y volver a abrir en automático', async () => {
    await dadoUnLectorConLaLenteUltraElegidaAMano();
    await cuandoSeLeeElCodigo(1, '77903754');

    await cuandoSeDesmontaYSeVuelveAAbrirEnAutomatico();

    entoncesSePidioLaCamaraConId('ultra');
    entoncesElOtroLectorSigueEnAutomatico();
  });

  it('continúa leyendo si el navegador bloquea guardar la preferencia', async () => {
    await dadoUnLectorAbierto();
    dadoUnAlmacenamientoBloqueado();

    await cuandoSeLeeElCodigo(0, '77903754');

    entoncesSeAvisoElCodigo('77903754');
  });

  it('si la lente automática desaparece vuelve a una disponible sin bloquear el lector', async () => {
    dadaUnaLenteAutomaticaQueDesaparecio();

    await cuandoSeAbreElLectorEnAutomatico();

    entoncesSeUsoUnaLenteDisponibleSinError();
  });

  it('libera la lente elegida automáticamente si se cierra mientras inicia el cambio', async () => {
    await dadoUnCambioDeLenteEnCursoQueTodaviaNoTerminoDeIniciar();

    await cuandoSeCierraYLlegaLaCamaraNueva();

    entoncesLaCamaraNuevaSeLiberoSinDecodificar();
  });

  it('no abre la cámara al cargar', () => {
    dadoUnLectorSinAbrir();

    entoncesNoSePidioNingunaCamara();
  });

  it('explica una denegación de permiso', async () => {
    dadoUnPermisoDenegado();

    await cuandoSeAbreElLectorEnAutomatico();

    entoncesSeExplicaQueNoSeHabilitoLaCamara();
  });

  it('lee tres productos al reabrir', async () => {
    await dadoUnLectorAbierto();

    await cuandoSeLeenTresProductosReabriendoEntreCadaUno();

    entoncesSeAvisaronLosCodigos('77903754', '77905741', '3017620422003');
  });

  it('libera por completo la sesión anterior al leer cada producto', async () => {
    await dadoUnLectorAbierto();

    await cuandoSeLeenTresProductosReabriendoEntreCadaUno();

    entoncesCadaSesionSeLiberoPorCompleto();
  });

  it('un error inesperado no deja el lector bloqueado', async () => {
    await dadoUnLectorAbierto();

    await cuandoOcurreUnErrorInesperadoDeLectura();

    entoncesElLectorSeCerroConElMensajeDeInterrupcion();
  });

  it('después de un error inesperado se puede volver a abrir el lector', async () => {
    await dadoUnLectorAbierto();
    await cuandoOcurreUnErrorInesperadoDeLectura();

    await cuandoSeReabreYSeLeeOtroCodigo();

    entoncesSeAvisoElCodigo('77905741');
    entoncesNoHayError();
  });

  it('sigue intentando ante códigos fuera de foco o incompletos', async () => {
    await dadoUnLectorAbierto();

    await cuandoLlegaronErroresDeCodigoFueraDeFoco();

    entoncesElLectorSigueAbiertoSinError();
  });

  it('lee un código después de errores de foco', async () => {
    await dadoUnLectorAbierto();
    await cuandoLlegaronErroresDeCodigoFueraDeFoco();

    await cuandoSeLeeElCodigo(0, '77903754');

    entoncesSeAvisaronCodigos(1);
  });

  it('libera un permiso de cámara que llega después de haber cerrado', async () => {
    dadoUnPermisoPendiente();

    await cuandoSeCierraYElPermisoLlegaTarde();

    entoncesLaCamaraNuevaSeLiberoSinDecodificar();
  });

  it('un decodificador viejo que responde tarde no apaga la cámara nueva', async () => {
    await dadoUnDecodificadorViejoPendienteYUnaCamaraNuevaIniciada();

    await cuandoRespondeElDecodificadorViejo();

    entoncesLaCamaraNuevaSigueEncendida();
  });

  it('un decodificador viejo que responde tarde se detiene y libera su cámara', async () => {
    await dadoUnDecodificadorViejoPendienteYUnaCamaraNuevaIniciada();

    await cuandoRespondeElDecodificadorViejo();

    entoncesElDecodificadorViejoSeDetuvoYLiberoSuCamara();
  });

  it('al cambiar de lente libera la anterior', async () => {
    await dadoUnLectorAbierto();

    await cuandoSeEligeLaLente('trasera-principal');

    entoncesLaCamaraAnteriorSeLibero();
  });

  it('al cambiar de lente solicita el dispositivo elegido', async () => {
    await dadoUnLectorAbierto();

    await cuandoSeEligeLaLente('trasera-principal');

    entoncesSePidioLaCamaraConId('trasera-principal');
  });

  it('una lente que deja de estar disponible lo explica y vuelve a automático', async () => {
    await dadoUnLectorAbierto();

    await cuandoSeEligeUnaLenteAusente();

    entoncesSeExplicaQueLaLenteNoEstaDisponible();
  });

  it('después de una lente ausente se puede reintentar con la cámara automática', async () => {
    await dadoUnLectorAbierto();
    await cuandoSeEligeUnaLenteAusente();

    await cuandoSeReabreElLector();

    entoncesSePidioLaCamaraTraseraDelTelefono();
  });

  it('solicita enfoque continuo si la cámara lo admite y tolera su rechazo', async () => {
    await cuandoSeAbreUnaCamaraCuyoEnfoqueContinuoSeRechaza();

    entoncesSePidioEnfoqueContinuoYElLectorSigueAbierto();
  });

  it('cierra y libera la cámara al ocultar la pestaña', async () => {
    await dadoUnLectorAbierto();

    cuandoSeOcultaLaPestana();

    entoncesElLectorSeCerroYLiberoLaCamaraUnaVez();
  });

  it('no vuelve a liberar la cámara al desmontar', async () => {
    await dadoUnLectorAbierto();
    cuandoSeOcultaLaPestana();

    cuandoSeDesmontaElLector();

    entoncesLaCamaraSeLiberoUnaSolaVez();
  });

  it('un fallo de la pista se comunica en vez de dejar una imagen congelada', async () => {
    await dadoUnLectorAbierto();

    cuandoSeCortaLaPista();

    entoncesSeComunicaQueLaCamaraSeInterrumpio();
  });

  it('un permiso que no responde tiene plazo y no bloquea la pantalla indefinidamente', async () => {
    await dadoUnPermisoQueNuncaResponde();

    await cuandoPasaElPlazoDeInicio();

    entoncesSeAvisaQueNoTerminoDeIniciar();
  });
});

// --- datos ---

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

const montarHook = (continua = false): Hook => {
  const montado = renderHook(() => useCamaraCodigo(vi.fn(), continua));
  montado.result.current.video.current = document.createElement('video');
  return montado;
};

const conVideoNuevo = (montado: Hook) => {
  montado.result.current.video.current = document.createElement('video');
};

const pidioLaCamara = (deviceId: string) =>
  expect.objectContaining({
    video: expect.objectContaining({ deviceId: { exact: deviceId } }),
  });

// --- dado ---

async function dadoUnLectorAbierto(continua = false) {
  const camara = crearCamara();
  pedirCamara.mockResolvedValueOnce(camara.stream);
  onCodigo = vi.fn();
  const montado = renderHook(() => useCamaraCodigo(onCodigo, continua));
  const video = document.createElement('video');
  montado.result.current.video.current = video;
  act(() => montado.result.current.abrir());
  await waitFor(() => expect(decodificar).toHaveBeenCalledOnce());
  await waitFor(() => expect(montado.result.current.iniciando).toBe(false));
  hook = montado;
  lector = { ...montado, ...camara, onCodigo, video };
}

function dadoUnLectorSinAbrir() {
  hook = montarHook();
}

function dadasLasEtiquetasQueSoloAparecenTrasElPermiso() {
  enumerar.mockResolvedValueOnce([]).mockResolvedValue(DISPOSITIVOS);
  camaraInicial = crearCamara('ultra');
  camaraNueva = crearCamara('principal');
  pedirCamara.mockResolvedValueOnce(camaraInicial.stream).mockImplementationOnce(() => {
    expect(camaraInicial.pista.stop).toHaveBeenCalledOnce();
    return Promise.resolve(camaraNueva.stream);
  });
}

function dadaUnaPreferenciaAutomaticaAntiguaDeLaLenteUltra() {
  recordarCamaraComprobada('ultra');
  enumerar.mockResolvedValue(DISPOSITIVOS);
  camaraNueva = crearCamara('principal');
  pedirCamara.mockResolvedValueOnce(camaraNueva.stream);
}

async function dadoUnLectorConLaLenteUltraElegidaAMano() {
  await dadoUnLectorAbierto();
  enumerar.mockResolvedValue(DISPOSITIVOS);
  pedirCamara.mockResolvedValueOnce(crearCamara('ultra').stream);
  conVideoNuevo(hook);
  act(() => hook.result.current.seleccionarCamara('ultra'));
  await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(2));
}

function dadoUnAlmacenamientoBloqueado() {
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
    throw new Error('Bloqueado');
  });
}

function dadaUnaLenteAutomaticaQueDesaparecio() {
  enumerar.mockResolvedValue(DISPOSITIVOS);
  pedirCamara
    .mockRejectedValueOnce(new DOMException('Ausente', 'NotFoundError'))
    .mockResolvedValueOnce(crearCamara('ultra').stream);
}

async function dadoUnCambioDeLenteEnCursoQueTodaviaNoTerminoDeIniciar() {
  enumerar.mockResolvedValueOnce([]).mockResolvedValue(DISPOSITIVOS);
  pedirCamara.mockResolvedValueOnce(crearCamara('ultra').stream).mockReturnValueOnce(
    new Promise<MediaStream>((resolve) => {
      resolverPermiso = resolve;
    }),
  );
  hook = montarHook();
  act(() => hook.result.current.abrir());
  await waitFor(() => expect(pedirCamara).toHaveBeenCalledTimes(2));
}

function dadoUnPermisoDenegado() {
  pedirCamara.mockRejectedValueOnce(new DOMException('denegado', 'NotAllowedError'));
}

function dadoUnPermisoPendiente() {
  pedirCamara.mockReturnValueOnce(
    new Promise<MediaStream>((resolve) => {
      resolverPermiso = resolve;
    }),
  );
}

async function dadoUnDecodificadorViejoPendienteYUnaCamaraNuevaIniciada() {
  decodificar.mockImplementationOnce((stream: MediaStream, video: HTMLVideoElement) => {
    video.srcObject = stream;
    return new Promise<IScannerControls>((resolve) => {
      resolverDecodificador = resolve;
    });
  });
  camaraInicial = crearCamara();
  pedirCamara.mockResolvedValueOnce(camaraInicial.stream);
  hook = renderHook(() => useCamaraCodigo(vi.fn()));
  videoViejo = document.createElement('video');
  hook.result.current.video.current = videoViejo;
  act(() => hook.result.current.abrir());
  await waitFor(() => expect(decodificar).toHaveBeenCalledOnce());
  camaraNueva = crearCamara();
  pedirCamara.mockResolvedValueOnce(camaraNueva.stream);
  videoNuevo = document.createElement('video');
  hook.result.current.video.current = videoNuevo;
  act(() => hook.result.current.reiniciar());
  await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(2));
}

async function dadoUnPermisoQueNuncaResponde() {
  pedirCamara.mockImplementation(() => new Promise<MediaStream>(() => undefined));
  hook = montarHook();
  act(() => hook.result.current.abrir());
  await waitFor(() => expect(pedirCamara).toHaveBeenCalledOnce());
}

// --- cuando ---

async function cuandoSeLeeElCodigo(indice: number, codigo: string) {
  await act(() => emitir(indice, codigo));
}

async function cuandoSeRetiraElCodigoYSeLeeDeNuevo() {
  instante = 1000;
  vi.spyOn(Date, 'now').mockImplementation(() => instante);
  await act(() => emitir(0, undefined, new NotFoundException()));
  instante += 601;
  await act(() => emitir(0, undefined, new NotFoundException()));
  await act(() => emitir(0, '77903754'));
}

async function cuandoSeAbreElLectorEnAutomatico() {
  hook = montarHook();
  act(() => hook.result.current.abrir());
  await waitFor(() => expect(pedirCamara).toHaveBeenCalled());
  await esperarAQueTermineDeIniciar();
}

async function esperarAQueTermineDeIniciar() {
  await waitFor(() => expect(hook.result.current.iniciando).toBe(false));
}

async function cuandoSeDesmontaYSeVuelveAAbrirEnAutomatico() {
  hook.unmount();
  pedirCamara.mockResolvedValueOnce(crearCamara('ultra').stream);
  otro = montarHook();
  act(() => otro.result.current.abrir());
  await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(3));
}

async function cuandoSeCierraYLlegaLaCamaraNueva() {
  act(() => hook.result.current.cerrar());
  camaraNueva = crearCamara('principal');
  await act(async () => {
    resolverPermiso(camaraNueva.stream);
  });
}

async function cuandoSeCierraYElPermisoLlegaTarde() {
  hook = montarHook();
  act(() => hook.result.current.abrir());
  await waitFor(() => expect(pedirCamara).toHaveBeenCalledOnce());
  await cuandoSeCierraYLlegaLaCamaraNueva();
}

async function cuandoSeLeenTresProductosReabriendoEntreCadaUno() {
  await act(() => emitir(0, '77903754'));
  for (const [indice, codigo] of [
    [1, '77905741'],
    [2, '3017620422003'],
  ] as const) {
    const nueva = crearCamara();
    pedirCamara.mockResolvedValueOnce(nueva.stream);
    conVideoNuevo(hook);
    act(() => hook.result.current.abrir());
    await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(indice + 1));
    await act(() => emitir(indice, codigo));
    expect(nueva.pista.stop).toHaveBeenCalledOnce();
  }
  await act(() => emitir(0, '77903754'));
}

async function cuandoOcurreUnErrorInesperadoDeLectura() {
  await act(() => emitir(0, undefined, new Error('Canvas desconectado')));
}

async function cuandoSeReabreYSeLeeOtroCodigo() {
  pedirCamara.mockResolvedValueOnce(crearCamara().stream);
  conVideoNuevo(hook);
  act(() => hook.result.current.abrir());
  await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(2));
  await act(() => emitir(1, '77905741'));
}

async function cuandoLlegaronErroresDeCodigoFueraDeFoco() {
  for (const error of [new NotFoundException(), new ChecksumException(), new FormatException()])
    await act(() => emitir(0, undefined, error));
}

async function cuandoRespondeElDecodificadorViejo() {
  controlViejo = {
    stop: vi.fn(() => {
      videoViejo.srcObject = null;
    }),
  } as unknown as IScannerControls;
  await act(async () => {
    resolverDecodificador(controlViejo);
  });
}

async function cuandoSeEligeLaLente(id: string) {
  pedirCamara.mockResolvedValueOnce(crearCamara().stream);
  conVideoNuevo(hook);
  act(() => hook.result.current.seleccionarCamara(id));
  await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(2));
}

async function cuandoSeEligeUnaLenteAusente() {
  pedirCamara.mockRejectedValueOnce(
    new DOMException('Dispositivo ausente', 'OverconstrainedError'),
  );
  conVideoNuevo(hook);
  act(() => hook.result.current.seleccionarCamara('lente-ausente'));
  await waitFor(() => expect(hook.result.current.abierta).toBe(false));
}

async function cuandoSeReabreElLector() {
  pedirCamara.mockResolvedValueOnce(crearCamara().stream);
  conVideoNuevo(hook);
  act(() => hook.result.current.abrir());
  await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(2));
}

async function cuandoSeAbreUnaCamaraCuyoEnfoqueContinuoSeRechaza() {
  camaraNueva = crearCamara();
  camaraNueva.pista.applyConstraints.mockRejectedValueOnce(new Error('No soportado'));
  pedirCamara.mockResolvedValueOnce(camaraNueva.stream);
  hook = montarHook();
  act(() => hook.result.current.abrir());
  await waitFor(() => expect(decodificar).toHaveBeenCalledOnce());
}

function cuandoSeOcultaLaPestana() {
  vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden');
  act(() => document.dispatchEvent(new Event('visibilitychange')));
}

function cuandoSeDesmontaElLector() {
  hook.unmount();
}

function cuandoSeCortaLaPista() {
  act(() => lector.pista.dispatchEvent(new Event('ended')));
}

async function cuandoPasaElPlazoDeInicio() {
  vi.useFakeTimers();
  // El temporizador se crea al abrir: reiniciar instala uno controlado por este reloj.
  conVideoNuevo(hook);
  act(() => hook.result.current.reiniciar());
  await act(async () => {
    await vi.advanceTimersByTimeAsync(15_001);
  });
}

// --- entonces ---

function entoncesSeAvisaronCodigos(veces: number) {
  expect(onCodigo).toHaveBeenCalledTimes(veces);
}

function entoncesSeAvisaronLosCodigos(...codigos: string[]) {
  expect(onCodigo.mock.calls.map((llamada) => llamada[0])).toEqual(codigos);
}

function entoncesSeAvisoElCodigo(codigo: string) {
  expect(onCodigo).toHaveBeenCalledWith(codigo);
}

function entoncesElLectorSigueAbierto() {
  expect(hook.result.current.abierta).toBe(true);
}

function entoncesLaCamaraNoSeDetuvo() {
  expect(lector.pista.stop).not.toHaveBeenCalled();
}

function entoncesSeUsaLaLentePrincipal() {
  expect(decodificar.mock.calls[0]?.[0]).toBe(camaraNueva.stream);
  expect(pedirCamara).toHaveBeenLastCalledWith(pidioLaCamara('principal'));
}

function entoncesLaCamaraActivaEsLaPrincipalEnAutomatico() {
  expect(hook.result.current.camaraActiva).toBe('camera 0, facing back');
  expect(hook.result.current.camaraActualId).toBe('principal');
  expect(hook.result.current.camaraId).toBe('');
}

function entoncesSePidioLaCamaraConId(id: string) {
  expect(pedirCamara).toHaveBeenLastCalledWith(pidioLaCamara(id));
}

function entoncesLaCamaraActualEs(id: string) {
  expect(hook.result.current.camaraActualId).toBe(id);
}

function entoncesLaCamaraActualDelOtroEs(id: string) {
  expect(otro.result.current.camaraActualId).toBe(id);
}

function entoncesElOtroLectorSigueEnAutomatico() {
  expect(otro.result.current.camaraId).toBe('');
}

function entoncesSeUsoUnaLenteDisponibleSinError() {
  expect(pedirCamara).toHaveBeenCalledTimes(2);
  expect(hook.result.current.error).toBe('');
  expect(hook.result.current.camaraActiva).toBe('camera 2, facing back ultra wide');
}

function entoncesLaCamaraNuevaSeLiberoSinDecodificar() {
  expect(camaraNueva.pista.stop).toHaveBeenCalledOnce();
  expect(decodificar).not.toHaveBeenCalled();
}

function entoncesNoSePidioNingunaCamara() {
  expect(pedirCamara).not.toHaveBeenCalled();
}

function entoncesSeExplicaQueNoSeHabilitoLaCamara() {
  expect(hook.result.current.error).toMatch(/No se habilitó/);
  expect(hook.result.current.abierta).toBe(false);
}

function entoncesCadaSesionSeLiberoPorCompleto() {
  expect(lector.pista.stop).toHaveBeenCalledOnce();
  expect(lector.video.srcObject).toBeNull();
  expect(hook.result.current.abierta).toBe(false);
}

function entoncesElLectorSeCerroConElMensajeDeInterrupcion() {
  expect(hook.result.current.abierta).toBe(false);
  expect(hook.result.current.error).toMatch(/lector se interrumpió/);
  expect(lector.pista.stop).toHaveBeenCalledOnce();
}

function entoncesNoHayError() {
  expect(hook.result.current.error).toBe('');
}

function entoncesElLectorSigueAbiertoSinError() {
  entoncesElLectorSigueAbierto();
  entoncesNoHayError();
}

function entoncesLaCamaraNuevaSigueEncendida() {
  expect(videoNuevo.srcObject).toBe(camaraNueva.stream);
  expect(camaraNueva.pista.stop).not.toHaveBeenCalled();
  expect(hook.result.current.abierta).toBe(true);
}

function entoncesElDecodificadorViejoSeDetuvoYLiberoSuCamara() {
  expect(controlViejo.stop).toHaveBeenCalledOnce();
  expect(camaraInicial.pista.stop).toHaveBeenCalledOnce();
}

function entoncesLaCamaraAnteriorSeLibero() {
  expect(lector.pista.stop).toHaveBeenCalledOnce();
}

function entoncesSeExplicaQueLaLenteNoEstaDisponible() {
  expect(hook.result.current.error).toMatch(/cámara elegida no está disponible/);
  expect(hook.result.current.camaraId).toBe('');
}

function entoncesSePidioLaCamaraTraseraDelTelefono() {
  expect(pedirCamara).toHaveBeenLastCalledWith(
    expect.objectContaining({
      video: expect.objectContaining({ facingMode: { ideal: 'environment' } }),
    }),
  );
}

function entoncesSePidioEnfoqueContinuoYElLectorSigueAbierto() {
  expect(camaraNueva.pista.applyConstraints).toHaveBeenCalledWith({
    advanced: [{ focusMode: 'continuous' }],
  });
  expect(hook.result.current.abierta).toBe(true);
  expect(hook.result.current.error).toBe('');
}

function entoncesElLectorSeCerroYLiberoLaCamaraUnaVez() {
  expect(hook.result.current.abierta).toBe(false);
  expect(lector.pista.stop).toHaveBeenCalledOnce();
}

function entoncesLaCamaraSeLiberoUnaSolaVez() {
  expect(lector.pista.stop).toHaveBeenCalledOnce();
}

function entoncesSeComunicaQueLaCamaraSeInterrumpio() {
  expect(hook.result.current.abierta).toBe(false);
  expect(hook.result.current.error).toMatch(/cámara se interrumpió/);
}

function entoncesSeAvisaQueNoTerminoDeIniciar() {
  expect(hook.result.current.abierta).toBe(false);
  expect(hook.result.current.error).toMatch(/no terminó de iniciar/);
}
