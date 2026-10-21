import { useCallback, useEffect, useRef, useState } from 'react';
import { MENSAJE_SIN_SOPORTE, mensajeDeErrorDeDictado, unirDictado } from '@/lib/dictado';

// Lo mínimo del reconocimiento de voz del navegador (Web Speech API) que usamos; TypeScript no lo trae.
type EventoDeResultado = { results: ArrayLike<ArrayLike<{ transcript: string }>> };
type EventoDeError = { error: string };
type Reconocimiento = {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  onresult: ((evento: EventoDeResultado) => void) | null;
  onerror: ((evento: EventoDeError) => void) | null;
  onend: (() => void) | null;
  start: () => void;
  stop: () => void;
  abort: () => void;
};
type ConstructorDeReconocimiento = new () => Reconocimiento;

const constructorDelNavegador = (): ConstructorDeReconocimiento | null => {
  const ventana = window as unknown as {
    SpeechRecognition?: ConstructorDeReconocimiento;
    webkitSpeechRecognition?: ConstructorDeReconocimiento;
  };
  return ventana.SpeechRecognition ?? ventana.webkitSpeechRecognition ?? null;
};

type Opciones = {
  /** Lo que ya está escrito cuando la persona empieza a dictar; lo dictado se suma a continuación. */
  obtenerBase: () => string;
  /** Recibe el texto completo (lo escrito más lo dictado hasta ahora) cada vez que el navegador transcribe algo. */
  alTexto: (texto: string) => void;
  /** Para decirle a la persona qué pasó cuando no se puede dictar. */
  alAvisar: (mensaje: string) => void;
};

// Dictado del chat de Sami (T-144): la voz se transcribe en el navegador y el texto queda en el campo para que la
// persona lo revise antes de enviarlo. No manda audio a ningún servidor.
export function useDictado({ obtenerBase, alTexto, alAvisar }: Opciones) {
  const [escuchando, setEscuchando] = useState(false);
  const reconocimiento = useRef<Reconocimiento | null>(null);
  const opciones = useRef({ obtenerBase, alTexto, alAvisar });
  useEffect(() => {
    opciones.current = { obtenerBase, alTexto, alAvisar };
  });

  const detener = useCallback(() => reconocimiento.current?.stop(), []);

  const iniciar = useCallback(() => {
    const Reconocedor = constructorDelNavegador();
    if (!Reconocedor) {
      opciones.current.alAvisar(MENSAJE_SIN_SOPORTE);
      return;
    }
    const nuevo = new Reconocedor();
    const base = opciones.current.obtenerBase();
    nuevo.lang = 'es-AR';
    nuevo.continuous = true;
    nuevo.interimResults = true;
    nuevo.onresult = (evento) => {
      const dictado = Array.from(
        evento.results,
        (resultado) => resultado[0]?.transcript ?? '',
      ).join('');
      opciones.current.alTexto(unirDictado(base, dictado));
    };
    nuevo.onerror = (evento) => {
      const mensaje = mensajeDeErrorDeDictado(evento.error);
      if (mensaje) opciones.current.alAvisar(mensaje);
    };
    nuevo.onend = () => {
      if (reconocimiento.current === nuevo) reconocimiento.current = null;
      setEscuchando(false);
    };
    try {
      nuevo.start();
    } catch {
      opciones.current.alAvisar(mensajeDeErrorDeDictado('desconocido') ?? '');
      return;
    }
    reconocimiento.current = nuevo;
    setEscuchando(true);
  }, []);

  const alternar = useCallback(() => {
    if (reconocimiento.current) detener();
    else iniciar();
  }, [detener, iniciar]);

  // Si la pantalla se cierra mientras se dicta, se corta el micrófono.
  useEffect(
    () => () => {
      reconocimiento.current?.abort();
    },
    [],
  );

  return { escuchando, alternar, detener };
}
