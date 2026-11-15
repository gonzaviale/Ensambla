import { useEffect, useId, useRef, type SyntheticEvent } from 'react';

export function useDialogoLector(onCerrar: () => void) {
  const raiz = useRef<HTMLDialogElement>(null);
  const tituloId = useId();
  useEffect(() => abrirDialogoYConservarFoco(raiz.current), []);
  const cancelar = (evento: SyntheticEvent) => {
    evento.preventDefault();
    onCerrar();
  };
  return { raiz, tituloId, cancelar };
}

function abrirDialogoYConservarFoco(dialogo: HTMLDialogElement | null) {
  const anterior = document.activeElement as HTMLElement | null;
  const overflow = document.documentElement.style.overflow;
  document.documentElement.style.overflow = 'hidden';
  dialogo?.showModal();
  dialogo?.querySelector<HTMLInputElement>('input')?.focus({ preventScroll: true });
  return () => restaurarPagina(dialogo, anterior, overflow);
}

function restaurarPagina(
  dialogo: HTMLDialogElement | null,
  anterior: HTMLElement | null,
  overflow: string,
) {
  dialogo?.close();
  document.documentElement.style.overflow = overflow;
  if (anterior?.isConnected) anterior.focus();
}
