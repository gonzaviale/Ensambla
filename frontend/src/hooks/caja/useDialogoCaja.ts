import { useEffect, useRef } from 'react';

const CONTROLES = 'button, input, select, textarea, a[href], [tabindex]';

function controlesDisponibles(raiz: HTMLElement) {
  return [...raiz.querySelectorAll<HTMLElement>(CONTROLES)].filter(
    (elemento) =>
      elemento.tabIndex >= 0 &&
      !elemento.matches(':disabled, [hidden]') &&
      elemento.getClientRects().length > 0,
  );
}

const esDialogoSuperior = (raiz: HTMLElement) =>
  [...document.querySelectorAll('[aria-modal="true"]')].at(-1) === raiz;

function retenerFoco(evento: KeyboardEvent, raiz: HTMLElement) {
  const controles = controlesDisponibles(raiz);
  const primero = controles[0] ?? raiz;
  const ultimo = controles.at(-1) ?? raiz;
  const extremo = evento.shiftKey ? primero : ultimo;
  if (document.activeElement !== extremo && raiz.contains(document.activeElement)) return;
  evento.preventDefault();
  (evento.shiftKey ? ultimo : primero).focus();
}

function interpretarTecla(
  evento: KeyboardEvent,
  raiz: HTMLElement,
  cerrar: () => void,
  bloqueado: boolean,
) {
  if (!esDialogoSuperior(raiz)) return;
  if (evento.key === 'Tab') retenerFoco(evento, raiz);
  if (evento.key !== 'Escape' || bloqueado) return;
  evento.stopPropagation();
  cerrar();
}

function ajustarAlTeclado(raiz: HTMLElement) {
  const ajustar = () =>
    raiz.style.setProperty(
      '--alto-dialogo',
      `${window.visualViewport?.height ?? window.innerHeight}px`,
    );
  ajustar();
  window.visualViewport?.addEventListener('resize', ajustar);
  return () => window.visualViewport?.removeEventListener('resize', ajustar);
}

function activarDialogo(raiz: HTMLElement, alTeclear: (evento: KeyboardEvent) => void) {
  const anterior = document.activeElement as HTMLElement | null;
  const scroll = document.documentElement.style.overflow;
  const liberarTeclado = ajustarAlTeclado(raiz);
  document.documentElement.style.overflow = 'hidden';
  (controlesDisponibles(raiz)[0] ?? raiz).focus();
  document.addEventListener('keydown', alTeclear, true);
  return () => {
    liberarTeclado();
    document.documentElement.style.overflow = scroll;
    document.removeEventListener('keydown', alTeclear, true);
    if (anterior?.isConnected) anterior.focus();
  };
}

export function useDialogoCaja(onCerrar: () => void, bloqueado: boolean) {
  const raiz = useRef<HTMLElement>(null);
  const actual = useRef({ onCerrar, bloqueado });
  useEffect(() => {
    actual.current = { onCerrar, bloqueado };
  });
  useEffect(() => {
    const dialogo = raiz.current;
    if (!dialogo) return;
    const alTeclear = (evento: KeyboardEvent) =>
      interpretarTecla(evento, dialogo, actual.current.onCerrar, actual.current.bloqueado);
    return activarDialogo(dialogo, alTeclear);
  }, []);
  return raiz;
}
