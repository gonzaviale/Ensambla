import { useRef } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { LectorDeCodigos } from '@/components/escaneo/LectorDeCodigos';
import { useCamaraCodigo } from '@/hooks/escaneo/useCamaraCodigo';
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
let errorDeCamara = 'NotReadableError';
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

function LectorConCamara() {
  const camara = useCamaraCodigo(vi.fn());
  const entrada = useRef<HTMLInputElement>(null);
  return (
    <LectorDeCodigos
      codigo=""
      buscando={false}
      abierta={camara.abierta}
      iniciando={camara.iniciando}
      sinLectura={camara.sinLectura}
      sesion={camara.sesion}
      camaras={camara.camaras}
      camaraId={camara.camaraId}
      camaraActiva={camara.camaraActiva}
      camaraActualId={camara.camaraActualId}
      errorCamara={camara.error}
      video={camara.video}
      entradaCodigo={entrada}
      onCodigo={vi.fn()}
      onBuscar={vi.fn()}
      onAbrir={camara.abrir}
      onCerrar={camara.cerrar}
      onReiniciar={camara.reiniciar}
      onCamara={camara.seleccionarCamara}
    />
  );
}

beforeEach(() => {
  errorDeCamara = 'NotReadableError';
  enumerar.mockReset().mockResolvedValue([
    { kind: 'videoinput', deviceId: 'disponible', label: 'HD Webcam' },
    { kind: 'videoinput', deviceId: 'ocupada', label: 'Cámara virtual' },
  ]);
  pedirCamara.mockReset().mockImplementation((opciones: MediaStreamConstraints) => {
    const dispositivo = typeof opciones.video === 'object' ? opciones.video.deviceId : undefined;
    if (
      typeof dispositivo === 'object' &&
      !Array.isArray(dispositivo) &&
      dispositivo.exact === 'ocupada'
    ) {
      return Promise.reject(new DOMException('No se puede iniciar el dispositivo', errorDeCamara));
    }
    return Promise.resolve(crearStream('disponible'));
  });
  decodificar.mockReset().mockImplementation((stream: MediaStream, video: HTMLVideoElement) => {
    video.srcObject = stream;
    return Promise.resolve({ stop: vi.fn() });
  });
});

describe('recuperación del lector al fallar una cámara', () => {
  it.each(['NotReadableError', 'NotFoundError'])(
    'permite elegir otra cámara después de que la seleccionada falle con %s',
    async (nombreError) => {
      errorDeCamara = nombreError;
      const user = userEvent.setup();
      render(<LectorConCamara />);
      await user.click(screen.getByRole('button', { name: 'Escanear producto' }));
      const selector = await screen.findByRole('combobox', { name: 'Cámara del lector' });
      await waitFor(() => expect(selector).toBeEnabled());
      await user.selectOptions(selector, 'ocupada');
      expect(await screen.findByRole('alert')).toHaveTextContent(/cámara/i);

      const selectorTrasError = screen.getByRole('combobox', { name: 'Cámara del lector' });
      expect(selectorTrasError).toBeEnabled();
      expect(selectorTrasError).toHaveValue('ocupada');
      expect(screen.getByRole('button', { name: 'Reintentar cámara' })).toBeEnabled();
      await user.selectOptions(selectorTrasError, 'disponible');

      await waitFor(() => expect(decodificar).toHaveBeenCalledTimes(2));
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      expect(screen.getByLabelText('Vista previa del lector')).toBeInTheDocument();
      expect(pedirCamara).toHaveBeenLastCalledWith(
        expect.objectContaining({
          video: expect.objectContaining({ deviceId: { exact: 'disponible' } }),
        }),
      );
    },
  );

  it.each([
    { almacenamiento: 'disponible', bloqueado: false, recordada: undefined },
    {
      almacenamiento: 'bloqueado',
      bloqueado: true,
      recordada: { id: 'ocupada', nombre: 'Cámara virtual' },
    },
  ])(
    'permite volver a automático tras fallar la cámara recordada, con almacenamiento $almacenamiento',
    async ({ bloqueado, recordada }) => {
      recordarCamaraElegida({ id: 'ocupada', nombre: 'Cámara virtual' });
      if (bloqueado)
        vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
          throw new Error('Bloqueado');
        });
      const user = userEvent.setup();
      render(<LectorConCamara />);
      await user.click(screen.getByRole('button', { name: 'Escanear producto' }));
      await screen.findByRole('alert');

      const selector = screen.getByRole('combobox', { name: 'Cámara del lector' });
      expect(selector).toHaveValue('ocupada');
      await user.selectOptions(selector, '');

      await waitFor(() => expect(decodificar).toHaveBeenCalledOnce());
      expect(leerCamaraElegida()).toEqual(recordada);
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      expect(pedirCamara).toHaveBeenLastCalledWith(
        expect.objectContaining({
          video: expect.objectContaining({ facingMode: { ideal: 'environment' } }),
        }),
      );
    },
  );
});
