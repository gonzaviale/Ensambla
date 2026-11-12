import { useEffect, useRef, useState } from 'react';
import type { IScannerControls } from '@zxing/browser';
import type { DecodeHintType as TipoHint } from '@zxing/library';
import {
  activarEnfoqueContinuo,
  leerCamaraComprobada,
  leerCamaraElegida,
  liberarCamara,
  listarCamarasDelLector,
  olvidarCamaraElegida,
  recordarCamaraComprobada,
  recordarCamaraElegida,
} from '@/api/camaraCodigo';
import { elegirCamaraAutomatica } from '@/lib/escaneo/camaras';
import type { CamaraDelLector } from '@/types/escaneo';

const PLAZO_INICIO_MS = 15_000;
const AVISO_SIN_LECTURA_MS = 8_000;

export function useCamaraCodigo(onCodigo: (codigo: string) => void) {
  const video = useRef<HTMLVideoElement>(null);
  const callback = useRef(onCodigo);
  const usarAutomatica = useRef(false);
  useEffect(() => {
    callback.current = onCodigo;
  }, [onCodigo]);
  const [abierta, setAbierta] = useState(false);
  const [iniciando, setIniciando] = useState(false);
  const [sinLectura, setSinLectura] = useState(false);
  const [error, setError] = useState('');
  const [sesion, setSesion] = useState(0);
  const [camaraId, setCamaraId] = useState('');
  const [camaras, setCamaras] = useState<CamaraDelLector[]>([]);
  const [camaraActiva, setCamaraActiva] = useState('');
  const [camaraActualId, setCamaraActualId] = useState('');

  useEffect(() => {
    const elemento = video.current;
    if (!abierta || !elemento) return;
    let cancelado = false;
    let recibido = false;
    let controles: IScannerControls | undefined;
    let controlDetenido = false;
    let stream: MediaStream | undefined;
    let aviso: ReturnType<typeof setTimeout> | undefined;
    let pista: MediaStreamTrack | undefined;

    const liberar = (control = controles) => {
      clearTimeout(plazo);
      clearTimeout(aviso);
      pista?.removeEventListener('ended', alTerminarPista);
      try {
        if (control && !controlDetenido) {
          controlDetenido = true;
          control.stop();
        }
      } catch {
        // También liberamos las pistas propias si el decodificador falló al detenerse.
      } finally {
        liberarCamara(stream, elemento);
      }
    };
    const fallar = (mensaje: string, control?: IScannerControls) => {
      if (cancelado) return;
      cancelado = true;
      liberar(control);
      setError(mensaje);
      setIniciando(false);
      setAbierta(false);
    };
    const alTerminarPista = () =>
      fallar('La cámara se interrumpió. Tocá «Reintentar cámara» para volver a iniciarla.');
    const plazo = setTimeout(
      () => fallar('La cámara no terminó de iniciar. Revisá el permiso y volvé a intentar.'),
      PLAZO_INICIO_MS,
    );

    const iniciar = async () => {
      try {
        if (window.isSecureContext === false || !navigator.mediaDevices?.getUserMedia)
          throw new Error(
            'La cámara necesita HTTPS en el celular. Podés escribir el código o usar un lector USB/Bluetooth.',
          );
        const [
          { BrowserMultiFormatReader },
          { BarcodeFormat, DecodeHintType, NotFoundException, ChecksumException, FormatException },
        ] = await Promise.all([import('@zxing/browser'), import('@zxing/library')]);
        if (cancelado) return;
        const previas = await listarCamarasDelLector();
        if (cancelado) return;
        setCamaras(previas);
        const preferida = leerCamaraComprobada();
        const manual = usarAutomatica.current ? undefined : leerCamaraElegida();
        let elegida = camaraId || elegirCamaraAutomatica(previas, preferida, manual)?.id || '';
        // Conservar la selección visible aunque el dispositivo falle antes de devolver un stream.
        setCamaraActualId(previas.some((camara) => camara.id === elegida) ? elegida : '');
        let descartada = '';
        const solicitar = (id: string) =>
          navigator.mediaDevices.getUserMedia({
            audio: false,
            video: {
              ...(id ? { deviceId: { exact: id } } : { facingMode: { ideal: 'environment' } }),
              width: { ideal: 1920 },
              height: { ideal: 1080 },
            },
          });
        try {
          stream = await solicitar(elegida);
        } catch (e) {
          if (cancelado) return;
          if (
            camaraId ||
            !elegida ||
            !(e instanceof DOMException) ||
            !['NotFoundError', 'OverconstrainedError'].includes(e.name)
          )
            throw e;
          descartada = elegida;
          elegida = '';
          stream = await solicitar('');
        }
        if (cancelado) {
          liberar();
          return;
        }
        const lista = await listarCamarasDelLector();
        if (cancelado) {
          liberar();
          return;
        }
        setCamaras(lista);
        const automatica =
          !camaraId &&
          elegirCamaraAutomatica(
            lista.filter((camara) => camara.id !== descartada),
            preferida,
            manual,
          );
        const idActual = elegida || stream.getVideoTracks()[0]?.getSettings?.().deviceId;
        // Tras el permiso aparecen las etiquetas. Liberar antes de cambiar evita cámaras ocupadas.
        if (automatica && automatica.id !== idActual) {
          liberarCamara(stream, elemento);
          try {
            stream = await solicitar(automatica.id);
            elegida = automatica.id;
          } catch (e) {
            if (cancelado) return;
            if (
              !(e instanceof DOMException) ||
              !['NotFoundError', 'OverconstrainedError', 'NotReadableError'].includes(e.name)
            )
              throw e;
            stream = await solicitar('');
            elegida = '';
          }
          if (cancelado) {
            liberar();
            return;
          }
        }
        pista = stream.getVideoTracks()[0];
        const idComprobado = elegida || pista?.getSettings?.().deviceId || '';
        setCamaraActualId(lista.some((camara) => camara.id === idComprobado) ? idComprobado : '');
        setCamaraActiva(
          lista.find((camara) => camara.id === idComprobado)?.nombre || pista?.label || '',
        );
        pista?.addEventListener('ended', alTerminarPista);
        void activarEnfoqueContinuo(stream);
        const lector = new BrowserMultiFormatReader(
          new Map<TipoHint, unknown>([
            [
              DecodeHintType.POSSIBLE_FORMATS,
              [BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.ITF],
            ],
            [DecodeHintType.TRY_HARDER, true],
          ]),
          { delayBetweenScanAttempts: 180, tryPlayVideoTimeout: 8_000 },
        );

        controles = await lector.decodeFromStream(
          stream,
          elemento,
          (resultado, errorDeLectura, control) => {
            if (cancelado || recibido) return;
            if (resultado) {
              recibido = true;
              cancelado = true;
              recordarCamaraComprobada(idComprobado);
              liberar(control);
              setIniciando(false);
              setAbierta(false);
              callback.current(resultado.getText());
            } else if (
              errorDeLectura &&
              !(errorDeLectura instanceof NotFoundException) &&
              !(errorDeLectura instanceof ChecksumException) &&
              !(errorDeLectura instanceof FormatException)
            ) {
              fallar('El lector se interrumpió. Volvé a abrir la cámara para continuar.', control);
            }
          },
        );
        if (cancelado || recibido) {
          liberar();
          return;
        }
        clearTimeout(plazo);
        setIniciando(false);
        aviso = setTimeout(() => {
          if (!cancelado) setSinLectura(true);
        }, AVISO_SIN_LECTURA_MS);
      } catch (e) {
        if (cancelado) {
          liberar();
          return;
        }
        if (
          e instanceof DOMException &&
          ['NotFoundError', 'OverconstrainedError'].includes(e.name)
        ) {
          setCamaraId('');
          fallar('La cámara elegida no está disponible. Elegí otra cámara o la opción automática.');
          return;
        }
        fallar(
          e instanceof DOMException && e.name === 'NotAllowedError'
            ? 'No se habilitó la cámara. Revisá el permiso del navegador o ingresá el código a mano.'
            : e instanceof DOMException && e.name === 'NotReadableError'
              ? 'La cámara no pudo iniciarse. Puede estar ocupada o desconectada. Elegí otra cámara o volvé a intentar.'
              : e instanceof Error
                ? e.message
                : 'No pudimos iniciar la cámara.',
        );
      }
    };
    const alOcultar = () => {
      if (document.visibilityState === 'hidden') setAbierta(false);
    };
    document.addEventListener('visibilitychange', alOcultar);
    void iniciar();
    return () => {
      cancelado = true;
      liberar();
      document.removeEventListener('visibilitychange', alOcultar);
    };
  }, [abierta, sesion, camaraId]);

  const abrir = () => {
    setError('');
    setSinLectura(false);
    setIniciando(true);
    setCamaraActiva('');
    setCamaraActualId('');
    setSesion((valor) => valor + 1);
    setAbierta(true);
  };
  return {
    video,
    abierta,
    iniciando,
    sinLectura,
    error,
    sesion,
    camaraId,
    camaras,
    camaraActiva,
    camaraActualId,
    abrir,
    reiniciar: abrir,
    seleccionarCamara: (id: string) => {
      usarAutomatica.current = !id;
      if (id)
        recordarCamaraElegida(camaras.find((camara) => camara.id === id) ?? { id, nombre: '' });
      else olvidarCamaraElegida();
      setCamaraId(id);
      abrir();
    },
    cerrar: () => setAbierta(false),
  };
}
