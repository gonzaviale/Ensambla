import type { CamaraDelLector } from '@/types/escaneo';
import { z } from 'zod';

const CLAVE_CAMARA = 'ensambla.lector.camara-comprobada';
const CLAVE_ELEGIDA = 'ensambla.lector.camara-elegida';
const preferidaSchema = z.object({ id: z.string().min(1), nombre: z.string() });

export function leerCamaraElegida(): CamaraDelLector | undefined {
  try {
    const dato = preferidaSchema.safeParse(
      JSON.parse(localStorage.getItem(CLAVE_ELEGIDA) ?? 'null'),
    );
    return dato.success ? dato.data : undefined;
  } catch {
    return undefined;
  }
}

/** La elección explícita de la persona tiene prioridad sobre una lectura automática aislada. */
export function recordarCamaraElegida(camara: CamaraDelLector) {
  try {
    localStorage.setItem(CLAVE_ELEGIDA, JSON.stringify(camara));
  } catch {
    // Sin almacenamiento sigue vigente la selección de esta sesión.
  }
}

export function olvidarCamaraElegida() {
  try {
    localStorage.removeItem(CLAVE_ELEGIDA);
  } catch {
    // La selección automática de esta sesión sigue disponible sin almacenamiento.
  }
}

export function leerCamaraComprobada(): string {
  try {
    return localStorage.getItem(CLAVE_CAMARA) ?? '';
  } catch {
    return '';
  }
}

/** Se recuerda sólo después de una lectura; nunca se guardan imágenes. */
export function recordarCamaraComprobada(id: string) {
  try {
    if (id) localStorage.setItem(CLAVE_CAMARA, id);
  } catch {
    // El modo privado o el almacenamiento bloqueado no impiden escanear.
  }
}

/** Las capacidades se consultan después del permiso; no todos los celulares exponen enfoque. */
export async function activarEnfoqueContinuo(stream: MediaStream): Promise<void> {
  const pista = stream.getVideoTracks()[0];
  if (!pista || pista.readyState === 'ended') return;
  try {
    const capacidades: MediaTrackCapabilities & { focusMode?: string[] } = pista.getCapabilities();
    if (!capacidades.focusMode?.includes('continuous')) return;
    const enfoque: MediaTrackConstraintSet & { focusMode: string } = { focusMode: 'continuous' };
    await pista.applyConstraints({ advanced: [enfoque] });
  } catch {
    // Un ajuste opcional no impide usar cámaras de Safari ni modelos sin enfoque configurable.
  }
}

export async function listarCamarasDelLector(): Promise<CamaraDelLector[]> {
  try {
    const dispositivos = await navigator.mediaDevices.enumerateDevices();
    return dispositivos
      .filter((d) => d.kind === 'videoinput')
      .map((d, i) => ({
        id: d.deviceId,
        nombre: d.label || `Cámara ${i + 1}`,
      }));
  } catch {
    return [];
  }
}

/** Sólo libera el stream de esta sesión; nunca limpia el video de una sesión más nueva. */
export function liberarCamara(stream: MediaStream | undefined, elemento: HTMLVideoElement) {
  if (!stream) return;
  for (const pista of stream.getTracks()) {
    if (pista.readyState !== 'ended') pista.stop();
  }
  if (elemento.srcObject === stream) {
    elemento.srcObject = null;
    elemento.removeAttribute('src');
  }
}
