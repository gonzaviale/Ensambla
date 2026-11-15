import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterAll, afterEach, beforeAll, beforeEach, expect, vi } from 'vitest';
import { EscenarioLectorCamara } from '@/test/EscenarioLectorCamara';
import { leerCamaraElegida, recordarCamaraElegida } from '@/api/camaraCodigo';

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
const dialogoOriginal = {
  showModal: Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, 'showModal'),
  close: Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, 'close'),
};
let errorDeCamara = 'NotReadableError';
beforeAll(() => {
  prepararDispositivosDeVideo();
  prepararAperturaDeDialogos();
  prepararCierreDeDialogos();
});

function prepararDispositivosDeVideo() {
  Object.defineProperty(navigator, 'mediaDevices', {
    configurable: true,
    value: { getUserMedia: pedirCamara, enumerateDevices: enumerar },
  });
}

function prepararAperturaDeDialogos() {
  Object.defineProperty(HTMLDialogElement.prototype, 'showModal', {
    configurable: true,
    value: function (this: HTMLDialogElement) {
      this.setAttribute('open', '');
    },
  });
}

function prepararCierreDeDialogos() {
  Object.defineProperty(HTMLDialogElement.prototype, 'close', {
    configurable: true,
    value: function (this: HTMLDialogElement) {
      this.removeAttribute('open');
    },
  });
}
afterAll(() => {
  for (const nombre of ['showModal', 'close'] as const) {
    const original = dialogoOriginal[nombre];
    if (original) Object.defineProperty(HTMLDialogElement.prototype, nombre, original);
    else Reflect.deleteProperty(HTMLDialogElement.prototype, nombre);
  }
  if (mediaOriginal) Object.defineProperty(navigator, 'mediaDevices', mediaOriginal);
  else Reflect.deleteProperty(navigator, 'mediaDevices');
});
afterEach(() => vi.restoreAllMocks());

function crearStream(id: string) {
  const pista = Object.assign(new EventTarget(), {
    readyState: 'live',
    getCapabilities: () => ({}),
    getSettings: () => ({ deviceId: id }),
    stop: vi.fn(() => {
      pista.readyState = 'ended';
    }),
  });
  return { getTracks: () => [pista], getVideoTracks: () => [pista] } as unknown as MediaStream;
}

beforeEach(() => {
  errorDeCamara = 'NotReadableError';
  enumerarCamarasDisponibles();
  pedirCamara.mockReset().mockImplementation(solicitarCamaraSimulada);
  decodificar.mockReset().mockImplementation(mostrarCamaraSimulada);
});

function enumerarCamarasDisponibles() {
  enumerar.mockReset().mockResolvedValue([
    { kind: 'videoinput', deviceId: 'disponible', label: 'HD Webcam' },
    { kind: 'videoinput', deviceId: 'ocupada', label: 'Cámara virtual' },
  ]);
}

function solicitarCamaraSimulada(opciones: MediaStreamConstraints) {
  if (sePideLaCamaraOcupada(opciones))
    return Promise.reject(new DOMException('No se puede iniciar el dispositivo', errorDeCamara));
  return Promise.resolve(crearStream('disponible'));
}

function sePideLaCamaraOcupada(opciones: MediaStreamConstraints) {
  const dispositivo = typeof opciones.video === 'object' ? opciones.video.deviceId : undefined;
  return (
    typeof dispositivo === 'object' &&
    !Array.isArray(dispositivo) &&
    dispositivo.exact === 'ocupada'
  );
}

function mostrarCamaraSimulada(stream: MediaStream, video: HTMLVideoElement) {
  video.srcObject = stream;
  return Promise.resolve({ stop: vi.fn() });
}

type UsuarioLector = ReturnType<typeof userEvent.setup>;

export function givenLectorConCamaraQueFalla(nombreError: string) {
  errorDeCamara = nombreError;
  render(<EscenarioLectorCamara />);
  return userEvent.setup();
}
export async function whenSeEligeLaCamaraOcupada(user: UsuarioLector) {
  await user.click(screen.getByRole('button', { name: 'Escanear producto' }));
  await user.click(screen.getByRole('button', { name: 'Ajustes del lector' }));
  const selector = await screen.findByRole('combobox', { name: 'Cámara del lector' });
  await waitFor(() => expect(selector).toBeEnabled());
  await user.selectOptions(selector, 'ocupada');
  expect(await screen.findByRole('alert')).toHaveTextContent(/cámara/i);
  await user.click(screen.getByRole('button', { name: 'Ajustes del lector' }));
}
export function thenSePuedeCorregirLaCamara() {
  const selector = screen.getByRole('combobox', { name: 'Cámara del lector' });
  expect(selector).toBeEnabled();
  expect(selector).toHaveValue('ocupada');
  expect(screen.getByRole('button', { name: 'Reiniciar lector' })).toBeEnabled();
}
export async function whenSeEligeLaCamaraDisponible(user: UsuarioLector) {
  await user.selectOptions(
    screen.getByRole('combobox', { name: 'Cámara del lector' }),
    'disponible',
  );
  await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(2));
}
export function thenElLectorUsaLaCamaraDisponible() {
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  expect(screen.getByLabelText('Vista previa del lector')).toBeInTheDocument();
  expect(pedirCamara).toHaveBeenLastCalledWith(
    expect.objectContaining({
      video: expect.objectContaining({ deviceId: { exact: 'disponible' } }),
    }),
  );
}
function bloquearElAlmacenamiento() {
  vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
    throw new Error('Bloqueado');
  });
}
export const almacenamientosDelLector = [
  { almacenamiento: 'disponible', preparar: () => {}, recordada: undefined },
  {
    almacenamiento: 'bloqueado',
    preparar: bloquearElAlmacenamiento,
    recordada: { id: 'ocupada', nombre: 'Cámara virtual' },
  },
];
export function givenLectorConCamaraRecordada(preparar: () => void) {
  recordarCamaraElegida({ id: 'ocupada', nombre: 'Cámara virtual' });
  preparar();
  return givenLectorConCamaraQueFalla('NotReadableError');
}
export async function whenSeVuelveALaSeleccionAutomatica(user: UsuarioLector) {
  await user.click(screen.getByRole('button', { name: 'Escanear producto' }));
  await screen.findByRole('alert');
  await user.click(screen.getByRole('button', { name: 'Ajustes del lector' }));
  const selector = screen.getByRole('combobox', { name: 'Cámara del lector' });
  expect(selector).toHaveValue('ocupada');
  await user.selectOptions(selector, '');
  await waitFor(() => expect(decodificar).toHaveBeenCalledOnce());
}
export function thenLaCamaraAutomaticaFunciona(recordada: ReturnType<typeof leerCamaraElegida>) {
  expect(leerCamaraElegida()).toEqual(recordada);
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  expect(pedirCamara).toHaveBeenLastCalledWith(
    expect.objectContaining({
      video: expect.objectContaining({ facingMode: { ideal: 'environment' } }),
    }),
  );
}
