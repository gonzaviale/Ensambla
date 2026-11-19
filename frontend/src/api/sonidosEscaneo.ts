import {
  ATAQUE_ESCANEO,
  SALIDA_ESCANEO,
  TONOS_ESCANEO,
  VOLUMEN_ESCANEO,
} from '@/lib/escaneo/sonidos';
import type { ResultadoSonoroEscaneo } from '@/lib/escaneo/sonidos';

const CLAVE = 'ensambla.lector.sonidos';
const EVENTO = 'ensambla:sonidos-lector';
let habilitadoEnMemoria = true;
let contexto: AudioContext | undefined;
let reanudando: Promise<void> | undefined;
let generacion = 0;
let prueba: ReturnType<typeof setTimeout> | undefined;
let suscriptores = 0;
const voces = new Set<{ oscilador: OscillatorNode; ganancia: GainNode }>();

export function leerSonidosEscaneo(): boolean {
  try {
    habilitadoEnMemoria = localStorage.getItem(CLAVE) !== 'no';
    return habilitadoEnMemoria;
  } catch {
    return habilitadoEnMemoria;
  }
}

function cancelarPrueba() {
  clearTimeout(prueba);
  prueba = undefined;
}

function detenerSonidos() {
  generacion++;
  cancelarPrueba();
  for (const voz of voces) {
    try {
      voz.oscilador.stop();
    } catch {
      /* Puede haber terminado en este mismo instante. */
    }
    voz.oscilador.disconnect();
    voz.ganancia.disconnect();
  }
  voces.clear();
}

export function cambiarSonidosEscaneo(habilitado: boolean) {
  habilitadoEnMemoria = habilitado;
  try {
    localStorage.setItem(CLAVE, habilitado ? 'si' : 'no');
  } catch {
    /* La preferencia sigue funcionando durante esta sesión. */
  }
  if (!habilitado) detenerSonidos();
  window.dispatchEvent(new Event(EVENTO));
}

export function suscribirSonidosEscaneo(cambio: () => void) {
  suscriptores++;
  const alCambiar = () => {
    if (!leerSonidosEscaneo()) detenerSonidos();
    cambio();
  };
  const almacenamiento = (evento: StorageEvent) => {
    if (evento.key === CLAVE || evento.key === null) alCambiar();
  };
  window.addEventListener(EVENTO, alCambiar);
  window.addEventListener('storage', almacenamiento);
  return () => {
    window.removeEventListener(EVENTO, alCambiar);
    window.removeEventListener('storage', almacenamiento);
    suscriptores--;
    if (suscriptores === 0) detenerSonidos();
  };
}

function notificarEstado() {
  window.dispatchEvent(new Event(EVENTO));
}

export function requiereActivacionSonidosEscaneo(): boolean {
  const soporte =
    window.AudioContext ||
    (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  return !!soporte && leerSonidosEscaneo() && contexto?.state !== 'running';
}

/** Se llama desde el teclado o los controles del lector para habilitar audio también en el celular. */
export function prepararSonidosEscaneo() {
  if (!leerSonidosEscaneo() || typeof window === 'undefined') return;
  try {
    const Constructor =
      window.AudioContext ??
      (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!Constructor) return;
    if (!contexto || contexto.state === 'closed') {
      contexto = new Constructor({ latencyHint: 'interactive' });
      contexto.onstatechange = notificarEstado;
    }
    if (contexto.state !== 'running')
      reanudando = contexto
        .resume()
        .then(notificarEstado)
        .catch(() => undefined);
  } catch {
    // Un navegador sin audio disponible nunca impide leer ni registrar un artículo.
  }
}

function tocar(tipo: ResultadoSonoroEscaneo, actual: AudioContext) {
  try {
    // La lectura nueva reemplaza el aviso anterior, sin formar una cola de sonidos.
    for (const voz of voces) {
      voz.ganancia.gain.cancelScheduledValues(actual.currentTime);
      voz.ganancia.gain.setValueAtTime(voz.ganancia.gain.value, actual.currentTime);
      voz.ganancia.gain.linearRampToValueAtTime(0, actual.currentTime + 0.006);
      voz.oscilador.stop(actual.currentTime + 0.007);
    }
    const inicio = actual.currentTime + 0.008;
    const tonos = TONOS_ESCANEO[tipo];
    for (const tono of tonos) {
      const oscilador = actual.createOscillator();
      const ganancia = actual.createGain();
      const voz = { oscilador, ganancia };
      voces.add(voz);
      oscilador.type = 'triangle';
      oscilador.frequency.setValueAtTime(tono.frecuencia, inicio + tono.inicio);
      ganancia.gain.setValueAtTime(0, inicio + tono.inicio);
      ganancia.gain.linearRampToValueAtTime(VOLUMEN_ESCANEO, inicio + tono.inicio + ATAQUE_ESCANEO);
      ganancia.gain.setValueAtTime(
        VOLUMEN_ESCANEO,
        inicio + tono.inicio + tono.duracion - SALIDA_ESCANEO,
      );
      ganancia.gain.linearRampToValueAtTime(0, inicio + tono.inicio + tono.duracion);
      oscilador.connect(ganancia);
      ganancia.connect(actual.destination);
      oscilador.onended = () => {
        oscilador.disconnect();
        ganancia.disconnect();
        voces.delete(voz);
      };
      oscilador.start(inicio + tono.inicio);
      oscilador.stop(inicio + tono.inicio + tono.duracion + 0.005);
    }
  } catch {
    // El lector sigue disponible si el dispositivo de salida se desconecta.
  }
}

export function sonarEscaneo(tipo: ResultadoSonoroEscaneo) {
  cancelarPrueba();
  const pendiente = ++generacion;
  if (!leerSonidosEscaneo() || !contexto || contexto.state === 'closed') return;
  const actual = contexto;
  if (actual.state === 'running') {
    tocar(tipo, actual);
    return;
  }
  const solicitado = Date.now();
  void reanudando?.then(() => {
    if (
      pendiente === generacion &&
      leerSonidosEscaneo() &&
      actual.state === 'running' &&
      Date.now() - solicitado < 300
    )
      tocar(tipo, actual);
  });
}

export function probarSonidosEscaneo() {
  if (!leerSonidosEscaneo()) return;
  detenerSonidos();
  prepararSonidosEscaneo();
  sonarEscaneo('reconocido');
  prueba = setTimeout(() => {
    prueba = undefined;
    sonarEscaneo('atencion');
  }, 360);
}
