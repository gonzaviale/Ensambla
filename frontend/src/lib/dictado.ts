// Reglas puras del dictado por voz del chat de Sami: qué se le dice a la persona cuando algo falla y cómo se
// suma lo dictado a lo que ya había escrito.

export const MENSAJE_SIN_SOPORTE =
  'Tu navegador no permite dictar. Probá con Chrome o Edge, o escribí lo que querés contarme.';

const MENSAJE_SIN_PERMISO =
  'Para dictar, permití el micrófono: tocá el candadito junto a la dirección, activá «Micrófono» y volvé a tocar el botón. Mientras tanto podés escribir.';

const MENSAJES_DE_ERROR: Record<string, string> = {
  'not-allowed': MENSAJE_SIN_PERMISO,
  'service-not-allowed': MENSAJE_SIN_PERMISO,
  'no-speech': 'No te escuché. Probá de nuevo, un poco más cerca del micrófono.',
  'audio-capture': 'No encontramos un micrófono. Conectalo o escribí lo que querés contarme.',
  network: 'No hay conexión para dictar ahora. Podés escribir mientras tanto.',
};

const MENSAJE_GENERICO = 'No pudimos dictar ahora. Podés seguir escribiendo.';

/** Qué decirle a la persona ante un error del reconocimiento de voz; `null` si no hay nada que avisar. */
export function mensajeDeErrorDeDictado(codigo: string): string | null {
  if (codigo === 'aborted') return null;
  return MENSAJES_DE_ERROR[codigo] ?? MENSAJE_GENERICO;
}

/** Suma lo dictado a lo que ya estaba escrito, con un espacio de por medio y sin espacios sobrantes. */
export function unirDictado(base: string, dictado: string): string {
  const nuevo = dictado.trim();
  if (!nuevo) return base;
  if (!base.trim()) return nuevo;
  return `${base.replace(/\s+$/, '')} ${nuevo}`;
}
